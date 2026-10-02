# DOT Studio: How We Work

**Product Owner:** Aaron. Sets direction, rules on questions, merges to `main`.
**Engineering Lead:** Claude. Plans, briefs agents, integrates, keeps CI green, brings decisions and finished PRs to the Product Owner.

## Team (separation of duties: an author never reviews its own change)
| Role | Model | Owns |
|---|---|---|
| Language Engineer | Opus | Lexer, parser, PSI, editor features |
| Preview Engineer | Opus | Live preview: render pipeline, JCEF, viz-js |
| Build & Release Engineer | Sonnet | Gradle, CI, supply chain, signing, packaging |
| QA Engineer | Sonnet | Test matrix, Plugin Verifier, runIde checks, screenshots |
| Code Reviewer | Opus | Independent review against `CODING-STANDARDS.md`; can block |
| Market Analyst | Sonnet | Competitor watch, post-launch metrics |

## Rules
- **Scope** comes from `~/Vaults/projects/dot-plugin/build-execution-brief.md`. Work outside it goes to the Product Owner first.
- **Standards:** every change follows `CODING-STANDARDS.md`.
- **PR path:** engineer (feature branch, own git worktree) → Code Reviewer → Lead (integration, CI green) → Product Owner merges. Never push to `main`, never force-push.
- **Decisions:** calls the Product Owner hasn't made are recorded in `DECISIONS.md`. His rulings are quoted verbatim.
- **Cost:** 1–2 agents active at a time. Briefs point at docs rather than restating them. Reports are a few lines long.
- **Agents check their own output** (test runs, verifier reports, screenshots) before reporting done.
