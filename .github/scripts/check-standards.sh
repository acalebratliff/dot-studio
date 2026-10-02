#!/usr/bin/env bash
# CI checks for CODING-STANDARDS 7.3 and 7.14. Both fail closed: a grep error (status >= 2) fails the check.
# Bad and good samples live in .github/lint-fixtures/ (excluded from the real scan) and drive the self-test.
# Usage: check-standards.sh [selftest]
set -uo pipefail

scan_todo() { # $1 = repo dir. Returns 0 clean, 1 offenders, 2 scan error.
  local out rc bad
  out=$(git -C "$1" grep -InoE '\b(TODO|FIXME)\b(\(#[0-9]+\))?' -- . ':!gradlew*' ':!*.md' ':!.github/workflows/*' ':!.github/lint-fixtures/*' ':!.github/scripts/*')
  rc=$?
  if [ "$rc" -ge 2 ]; then echo "scan error (git grep status $rc)" >&2; return 2; fi
  [ "$rc" -eq 1 ] && return 0
  bad=$(printf '%s\n' "$out" | grep -vE '\(#[0-9]+\)$') || true
  if [ -n "$bad" ]; then printf 'Marker without issue link:\n%s\n' "$bad" >&2; return 1; fi
  return 0
}

scan_strings() { # $1 = repo dir. Returns 0 clean, 1 offenders, 2 scan error.
  local out rc
  [ -d "$1/src/main" ] || { echo "scan error: $1/src/main missing" >&2; return 2; }
  # A string literal as a direct argument; literals nested inside a bundle call such as DotStudioBundle.message("k") are fine.
  # Line-based, so a call split over several lines is not caught.
  local arg='(([^(),"]|\([^()]*\))*,)*[[:space:]]*"'
  local pat='Messages\.show[[:alnum:]_]*\('$arg'|Notification\('$arg'|text[[:space:]]*=[[:space:]]*"|label\("|button\("'
  out=$(git -C "$1" grep -InE "$pat" -- src/main)
  rc=$?
  if [ "$rc" -ge 2 ]; then echo "scan error (git grep status $rc)" >&2; return 2; fi
  [ "$rc" -eq 1 ] && return 0
  printf 'Hardcoded user-visible string:\n%s\n' "$out" >&2
  return 1
}

expect() { # $1 = expected status, rest = command
  local want=$1 got
  shift
  "$@" 2>/dev/null
  got=$?
  if [ "$got" -ne "$want" ]; then echo "selftest FAILED: '$*' returned $got, expected $want" >&2; exit 1; fi
}

FIXTURES="$(cd "$(dirname "$0")/.." && pwd)/lint-fixtures"

fixture() { # $1 = fixture file name; builds a fresh tracked repo holding it under src/main
  local d
  d=$(mktemp -d -p "$SELFTEST_TMP")
  git -C "$d" init -q
  mkdir -p "$d/src/main"
  cp "$FIXTURES/$1" "$d/src/main/$1"
  git -C "$d" add -A
  echo "$d"
}

selftest() {
  local f d
  SELFTEST_TMP=$(mktemp -d)
  trap 'rm -rf "$SELFTEST_TMP"' EXIT
  for f in bare-todo.bnf bare-fixme.kts bare-todo.xml mixed-line.kt; do
    d=$(fixture "$f"); expect 1 scan_todo "$d"
  done
  for f in literal-text.kt literal-text-nospace.kt literal-messages-first.kt literal-messages-later.kt \
    literal-notification.kt literal-label.kt literal-button.kt; do
    d=$(fixture "$f"); expect 1 scan_strings "$d"
  done
  d=$(fixture linked-todo.kt); expect 0 scan_todo "$d"
  d=$(fixture bundle-ok.kt); expect 0 scan_strings "$d"; expect 0 scan_todo "$d"
  expect 2 scan_todo /nonexistent-dir
  expect 2 scan_strings /nonexistent-dir
  echo "selftest passed"
}

if [ "${1:-}" = selftest ]; then selftest; exit 0; fi
scan_todo . && scan_strings .
