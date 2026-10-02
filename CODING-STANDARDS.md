# DOT Studio: Coding Standards

Adopted 2026-10-02. Changes to this file go through a PR of their own.
Every rule below is something a reviewer can check in a diff, in a CI log, or by running one command. Each rule cites its primary source. Where the sources are silent or something is unverified, the rule says so.

Key words: **MUST** means a PR is rejected without it. **SHOULD** means a deviation needs a one-line reason in the PR.

---

## 1. IntelliJ Platform rules

### 1.1 Threading
- **MUST NOT block the EDT.** No parsing, PSI traversal, file I/O, viz-js rendering or waiting on futures on the EDT. The platform lists "VFS traversal, PSI parsing, reference resolution, index queries" as off-EDT work. https://plugins.jetbrains.com/docs/intellij/threading-model.html
- **MUST read PSI/documents inside a read action and write only inside a write action on the EDT.** "Writing data is only allowed on EDT." (same page)
- **MUST use coroutines for new background work.** All our targets are 2025.2+, and "Plugins targeting 2024.1+ should use Kotlin coroutines" https://plugins.jetbrains.com/docs/intellij/background-processes.html. Use `readAction {}` / `smartReadAction {}` from a coroutine, and `Dispatchers.EDT` for UI updates. https://plugins.jetbrains.com/docs/intellij/coroutine-read-actions.html
  - Do not `suspend` inside a read-action block. "Read actions must be short." (same page)
  - `ReadAction.nonBlocking(...)` is allowed only where a non-suspending API requires it, with a comment saying why.
- **MUST take coroutine scopes from a service constructor (`CoroutineScope` injection).** Never `GlobalScope`, `runBlocking` on the EDT, or a hand-made `CoroutineScope(...)`. https://plugins.jetbrains.com/docs/intellij/kotlin-coroutines.html, https://plugins.jetbrains.com/docs/intellij/plugin-services.html
- **MUST make long loops cancellable.** Call `ProgressManager.checkCanceled()` (or `ensureActive()` in coroutines) every N iterations. https://plugins.jetbrains.com/docs/intellij/background-processes.html
- **MUST NOT swallow or log `ProcessCanceledException`/`CancellationException`.** "PCE … must never be logged or swallowed. In case of catching it for some reason, it must be rethrown." (same page)
- **Preview rendering (project-specific):** each document change cancels the previous render job (debounced, latest-wins). Rendering runs off-EDT. Only the final SVG string crosses to the EDT/JCEF. A render that is cancelled or superseded MUST NOT update the UI.

### 1.2 Services and state
- **SHOULD use light services (`@Service` / `@Service(Service.Level.PROJECT)`).** The class MUST be `final` (the Kotlin default). Do not register light services in plugin.xml. https://plugins.jetbrains.com/docs/intellij/plugin-services.html
- **MUST NOT do heavy work in constructors.** "Avoid any heavy initializations in the constructor." Load the wasm, browsers or caches lazily on first use. (same page)
- **MUST NOT store service instances in fields.** "Always obtain service instances directly and only at the location where they're needed." (same page)
- **MUST NOT use static mutable state** (top-level `var`, mutable collections in `companion object`/`object`). It survives plugin unload; see 1.4.
- **MUST NOT use Kotlin `object` for extensions** (except `FileType`, which may be one). `companion object` holds only constants and a `Logger`. https://plugins.jetbrains.com/docs/intellij/using-kotlin.html

### 1.3 Disposables
- **MUST give every `Disposable` resource (JCEF browser, `JBCefJSQuery`, alarms, listeners) a parent.** That parent is a plugin service, the editor's disposable, or `Disposer.newDisposable()` owned by one of those. https://plugins.jetbrains.com/docs/intellij/disposers.html
- **MUST NOT use `Application` or `Project` as a parent disposable.** They "must never be used as parent disposables in plugin code", because doing so leaks on unload. (same page)
- **MUST use the overload that takes a `parentDisposable`** when subscribing listeners and message-bus topics.
- Extensions registered in plugin.xml "are not automatically disposed". Their cleanup goes through a service's `dispose()`. (same page)

### 1.4 Dynamic plugin (install, update, uninstall without restart)
- **MUST stay dynamic.** CI fails on `NOT_DYNAMIC` (see 4.4). That means:
  - no components;
  - no `overrides="true"` services;
  - every `<group>` has an `id`;
  - no PSI references held in long-lived objects (use `SmartPsiElementPointer`);
  - no `Language`/`FileType` objects as map keys (use `Language.getID()` / `FileType.getName()`).
  https://plugins.jetbrains.com/docs/intellij/dynamic-plugins.html
- Long-running work (renders) MUST be cancelled when the owning disposable is disposed.

### 1.5 plugin.xml and API status
- **MUST register every extension in `plugin.xml`.** Dependencies are `<depends>com.intellij.modules.platform</depends>` plus exactly one optional dependency, `<depends optional="true" config-file="…">com.intellij.modules.jcef</depends>`, which exists only to put JCEF classes on the plugin's classpath on 2026.2+. From 2026.2 (262) JCEF is a separate bundled plugin, and without this dependency the plugin throws `NoClassDefFoundError: JBCefApp` (preview spike, 2026-10-02). **2025.2 (252) has no `com.intellij.modules.jcef` module, so the optional config file never loads there.** Therefore preview registrations (such as the split-editor provider) MUST go in the main `plugin.xml`, never in the optional config file. Every preview PR proves the preview with `runIde` on 252, on 262, and on 262 with the JCEF plugin disabled. Actions and settings take their text from the resource bundle. https://plugins.jetbrains.com/docs/intellij/plugin-configuration-file.html
- **The plugin `<id>` cannot be changed after the first public upload** (same page). The Product Owner confirms it before the first upload (see DECISIONS.md).
- **MUST NOT use APIs marked `@ApiStatus.Internal`, `@ApiStatus.Experimental`, `@ApiStatus.ScheduledForRemoval`, `@ApiStatus.Obsolete` or `@Deprecated`.** Do not call `@OverrideOnly` methods or extend `@NonExtendable` types. https://plugins.jetbrains.com/docs/intellij/verifying-plugin-compatibility.html. Plugin Verifier enforces this (4.4). If no alternative exists, the PR MUST say so and link the API.
- `sinceBuild` = 252 (2025.2). `untilBuild` is closed at the highest branch Plugin Verifier has passed (`<branch>.*`). JetBrains warns that an open range "will include all future builds". https://plugins.jetbrains.com/docs/intellij/build-number-ranges.html. Raising `untilBuild` is its own PR, with a green verifier run attached.
- **Build against the lowest supported platform (2025.2).** Per that page, 2026.2 runs on Java 25 while 2025.2 uses Java 21. So we compile to Java 21 and verify on 2026.2. Branch numbers (confirmed from the JetBrains releases API, 2026-10-02): 2025.2 = 252, 2026.2 = 262.

### 1.6 Custom language (lexer, parser, PSI)
- The grammar lives in `Dot.bnf` and the lexer in `Dot.flex`. Both are generated at build time by the IntelliJ Platform Gradle Plugin's Grammar-Kit subplugin (`org.jetbrains.intellij.platform.grammarkit`, tasks `generateLexer` and `generateParser`). https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-plugins.html. The standalone `org.jetbrains.grammarkit` plugin is archived and MUST NOT be used.
  - Generated output goes to `build/generated/`. It is **not committed** and not put under `src/`.
  - The subplugin is applied in the first grammar PR, not before. That PR proves `generateLexer` and `generateParser` under the configuration cache.
- The grammar MUST recover from errors: a typo in one statement must not turn the rest of the file into an error. Each recovery rule needs a parsing test (4.1).
- **No stubs or indexes in v1.** Stubs are "needed for things like methods or fields visible from other files" https://plugins.jetbrains.com/docs/intellij/stub-indexes.html. DOT has no cross-file references, so adding them would be speculative code (section 7).
- PSI performance: no `getText()` on large subtrees (use `textMatches()`). Cache expensive derived values with `CachedValuesManager`. No PSI in long-lived collections. https://plugins.jetbrains.com/docs/intellij/psi-performance.html
- Names come from the DOT spec (https://graphviz.org/doc/info/lang.html): `graph`, `digraph`, `subgraph`, `node_stmt`, `edge_stmt`, `attr_stmt`, `a_list`, `ID`, `edgeop`, `compass_pt`. PSI types and grammar rules use these names. Do not invent synonyms ("connection" for edge).

### 1.7 JCEF preview
- **MUST check JCEF availability before touching any JCEF class,** using a check that does not itself reference JCEF types. First confirm the JCEF classes are loadable through the plugin's classloader. Catch only `ClassNotFoundException`/`LinkageError`, because on 262 the JCEF plugin can be disabled. Then call `JBCefApp.isSupported()`. Only code that runs after both checks pass may reference JCEF classes. If either fails, show the fallback: an explanatory notice. Nothing can render DOT without JCEF, so there is no image fallback. https://plugins.jetbrains.com/docs/intellij/embedded-browser-jcef.html
- **MUST register `JBCefBrowser` and every `JBCefJSQuery` with the split editor's disposable.** Both are `JBCefDisposable`. (same page)
- **MUST load the page, viz-js and the wasm from plugin resources only.** Serve them with a `CefRequestHandler`/`CefResourceRequestHandler` mapped to a fixed internal origin, as the page's `JCefImageViewer` reference does. (same page)
  - No remote URLs, no CDN, no `file://` paths into the user's filesystem.
  - The CSP is sent as a response header and allows only that origin plus `'wasm-unsafe-eval'`: `default-src 'none'`; `script-src`, `worker-src`, `style-src`, `img-src` and `connect-src` set to the origin. The CSP is our hardening rule; the JetBrains doc does not specify one.
  - Navigation away from that origin is cancelled in the request handler.
- **MUST render in a Web Worker with a timeout.** Cancellation is `worker.terminate()`, the only way to stop a running Graphviz layout. That is what latest-wins depends on. Pathological graphs run for tens of seconds and then throw `RangeError` (preview spike, 2026-10-02).
- **DOT source reaches JS only as data.** Pass it through a JSON-encoded argument, never by concatenating it into `executeJavaScript` source.
- **JS→Kotlin calls go only through `JBCefJSQuery`.** Handlers validate their input and run nothing heavy on the CEF thread.
- **MUST NOT ship tests that need a real JCEF browser in CI.** The preview logic (debounce, cancellation, error mapping) lives in plain Kotlin behind an interface and is unit-tested. The JCEF adapter stays thin and is checked manually with `runIde` (PR template item).

### 1.8 User-visible strings, settings, logging, errors
- **MUST put every user-visible string in `messages/DotStudioBundle.properties`,** accessed through a `DynamicBundle` subclass. Parameters are annotated `@Nls` / `@PropertyKey`. https://plugins.jetbrains.com/docs/intellij/internationalization.html
  - Keys carry a context prefix (`action.`, `settings.`, `preview.error.`).
  - Never build sentences by concatenation; use `{0}` placeholders. (same page)
- **Settings use `SimplePersistentStateComponent<BaseState>`** with `@State`/`@Storage` in a named file (not `other.xml`). The settings UI is a `BoundConfigurable` (Kotlin UI DSL). No logic in `getState`/`loadState`. https://plugins.jetbrains.com/docs/intellij/persisting-state-of-components.html
- **Logging uses `com.intellij.openapi.diagnostic.Logger`** (`logger<T>()` / `thisLogger()`). Never `println`, `System.err`, `java.util.logging` or SLF4J. https://plugins.jetbrains.com/docs/intellij/ide-infrastructure.html
- **Log levels.** An invalid DOT file is a *user* error: show it in the preview, and log it at `debug` at most. `LOG.error` is only for plugin bugs, because it surfaces in the IDE's Fatal Errors dialog (same page). `LOG.warn` is for recoverable environment problems such as JCEF being unavailable.
- **Error reporting.** v1 ships no `ErrorReportSubmitter` (no backend, no telemetry). The README points users to GitHub Issues. Adding one later needs its own PR and a privacy note. Marketplace requires user permission before any data collection (2.1).

## 2. JetBrains Marketplace requirements
Source: https://plugins.jetbrains.com/docs/marketplace/jetbrains-marketplace-approval-guidelines.html (approval) and https://plugins.jetbrains.com/docs/marketplace/best-practices-for-listing.html (listing).
- **Name:**
  - original;
  - ≤30 characters (the hard rule; the listing guide recommends ≤20);
  - no "Plugin", "IntelliJ", "JetBrains" or JetBrains product names;
  - no third-party trademarks. "Graphviz" may appear in the description, not the name.
  - "DOT Studio" passes all of these.
- **Logo:** `META-INF/pluginIcon.svg`, 40×40 SVG, not the template logo, not resembling JetBrains logos.
- **Description:** in English. The first 40 characters are a standalone summary. Uses bullet points. Makes no unverifiable claims ("fastest").
- **Change notes:** come from `CHANGELOG.md` (3.3). They must never contain template placeholder text.
- **Links:**
  - vendor URL and email that work;
  - public source URL (required, since the plugin is open source);
  - issue tracker.
  - Every link must resolve.
- **Screenshots:** at least 1200×760 and the same aspect ratio throughout. IDE content only: no desktop, no browser chrome.
- **Behaviour:**
  - compatible with the declared range, verified by Plugin Verifier;
  - no internal API;
  - no significant performance impact;
  - no interfering with IDE features;
  - no data collection without explicit permission. DOT Studio collects nothing and makes **no network calls at all**.
- **Legal:**
  - a Developer EULA (the repo's OSS licence);
  - a source link;
  - no privacy policy needed while nothing is collected;
  - EEA trader-status declaration (Aaron, at upload).

## 3. Kotlin, linting and Gradle

### 3.1 Kotlin style
- Follow https://kotlinlang.org/docs/coding-conventions.html. `kotlin.code.style=official` goes in `gradle.properties`.
  - Prefer `val`.
  - Use read-only collection types in signatures.
  - Use expression bodies where they fit.
  - Use named arguments for Boolean/primitive parameter lists.
  - No `Util`/`Helper`/`Manager` file names; the conventions call `Util` "meaningless".
  - Use trailing commas at declaration sites.
- **Visibility: `internal` by default.** Use `private` where possible. `public` only when the platform needs it. (Classes registered in plugin.xml may be `internal`: Kotlin compiles them public in bytecode.) Declare return types explicitly on non-private functions. The conventions recommend explicit visibility and types to avoid accidental API.
- Kotlin compiler: `allWarningsAsErrors = true` from the first commit.
- **MUST set `apiVersion` to the Kotlin stdlib bundled with `sinceBuild`** (2025.2 bundles 2.1, so `KOTLIN_2_1`). A plugin supporting several platform versions "must either target the lowest bundled stdlib version" or ship its own. https://plugins.jetbrains.com/docs/intellij/using-kotlin.html. `languageVersion` SHOULD match, if it compiles without warnings.
  - The Kotlin compiler supports only about three previous API versions. So the Kotlin Gradle plugin is held at the last release that supports API 2.1 while `sinceBuild` is 252, enforced by a Dependabot ignore rule. Raising `sinceBuild` is the only reason to raise these. `jvmToolchain(21)` sets the JVM target, so do not also set `jvmTarget`.

### 3.2 Linter choice: **ktlint, through the kotlinter Gradle plugin**
- ktlint 1.8.0 (https://github.com/pinterest/ktlint) via `org.jmailen.kotlinter` 5.7.0 (https://github.com/jeremymailen/kotlinter-gradle). Tasks `lintKotlin` and `formatKotlin`.
- Config lives in `.editorconfig` with `ktlint_code_style = intellij_idea`. That style matches the IDE formatter under `kotlin.code.style=official`, so "Reformat Code" and CI never disagree.
- **Why not detekt:**
  - The stable line is 1.23.8 (https://github.com/detekt/detekt/releases). The 2.x line is still `2.0.0-alpha.6` (https://detekt.dev/docs/intro), and we do not gate CI on alpha tools.
  - detekt embeds its own Kotlin compiler and documents a "detekt was compiled with Kotlin X but is currently running with Y" failure mode. That fails when it can't parse our Kotlin 2.x sources.
  - The smell checks it adds overlap with the IDE inspections, with `allWarningsAsErrors`, and with Plugin Verifier.
  - Re-evaluate when detekt 2.0 ships a stable release.
- Not yet verified: kotlinter's configuration-cache compatibility under Gradle 9. The scaffold PR proves it with `--configuration-cache`.

### 3.3 Gradle and the official plugin template
Source: https://github.com/JetBrains/intellij-platform-plugin-template, read 2026-10-02. Its `CHANGELOG.md` explains each choice.

**Adopt from the template:**
- Plugin versions declared inline in `settings.gradle.kts` `pluginManagement`. These are the IntelliJ Platform settings plugin 2.19.0, Kotlin JVM, `org.jetbrains.changelog` and the foojay toolchain resolver. The template deliberately *removed* `libs.versions.toml` in 2.5.0. With one module and a handful of dependencies, a version catalog adds indirection without benefit, so **no version catalog**.
- `gradle.properties`:
  - `kotlin.stdlib.default.dependency=false` (the IDE provides the stdlib; https://plugins.jetbrains.com/docs/intellij/using-kotlin.html);
  - `org.gradle.configuration-cache=true`;
  - `org.gradle.caching=true`;
  - Gradle's own `group` and `version` keys live here and nowhere else. The plugin version defaults to the project version, so it is not set again.
- **Plugin id, name and vendor live only in `plugin.xml`.** They are not set in `pluginConfiguration` and not derived from `group`, because the id can never change after the first upload.
- Never declare `kotlinx-coroutines` or the Kotlin stdlib as dependencies, and check transitive dependencies for them. "Plugins must always use the bundled library." (using-kotlin page)
- Gradle Changelog Plugin plus Keep a Changelog. `getChangelog --unreleased` feeds the release draft, and `patchChangelog` runs on release. The IntelliJ Platform Gradle Plugin now preconfigures this, so no changelog block is needed in `build.gradle.kts`.
- CI shape: separate **build**, **test** (`./gradlew check`) and **verify** (`./gradlew verifyPlugin`) jobs; `concurrency` with `cancel-in-progress`; reports uploaded on failure. *Release PR only (not before):* a release draft on push to main, and publishing triggered by a GitHub release.
- Dependabot for `gradle` and `github-actions`. Change the template's `target-branch: "next"` to `main`.
- `.run/` shared run configurations (Run Plugin, Run Tests, Run Verifications).
- Signing and publishing configured from env vars only (section 5).

**Do not adopt:**
- Tag-pinned actions (`actions/checkout@v7`): pin to SHAs instead (5.1).
- `jlumbroso/free-disk-space`: a third-party action with no need shown yet. Add it only if a verifier run actually runs out of disk, and pin it by SHA.
- Missing workflow `permissions`: we add a top-level `permissions: contents: read` and raise permissions per job only where needed (5.1).
- `template-cleanup.yml`, `template-verify.yml`, `.github/template-cleanup/`, `.github/readme/` images.
- Sample code (`MyBundle`, `MyToolWindowFactory`, `MyProjectActivity`).
- Qodana and Kover: the template itself removed both in 2.5.0.
- The `junit:junit` line is kept only because the platform test framework (`TestFrameworkType.Platform`) runs JUnit 3/4-style `BasePlatformTestCase` tests. The comment in the build script says exactly that.
- Gradle wrapper: update it only with `./gradlew wrapper --gradle-version=X && ./gradlew wrapper` (template guidance). Commit the result. Never hand-edit `gradle-wrapper.properties`.

## 4. Testing

### 4.1 Lexer and parser
- **Parser tests:** extend `ParsingTestCase("", "dot", DotParserDefinition())`. Input files live in `src/test/testData/parser/*.dot` with expected PSI trees in `*.txt`. Run `doTest(true)` and set `includeRanges()` to true. https://plugins.jetbrains.com/docs/intellij/parsing-test.html
- **Lexer tests:** extend `LexerTestCase`, one test data file per token family (IDs, quoted strings with escapes, HTML strings `<...>`, comments `//` `/* */` `#`, edge operators).
- **Required cases:**
  - every grammar production;
  - every keyword's case-insensitivity (the DOT spec says keywords are case-independent);
  - every error-recovery rule;
  - each bug in the incumbent plugin that we claim to fix, one test each, named after the bug.
- **Expected `.txt` files.** They may be generated by the first run or copied from the PSI viewer, but the reviewer reads every new or changed `.txt` diff. A regenerated expectation file with no explanation in the PR is rejected.

### 4.2 Editor features
- **Use light tests** (`BasePlatformTestCase`): "we recommend plugin developers to write light tests whenever possible." https://plugins.jetbrains.com/docs/intellij/light-and-heavy-tests.html
- **Highlighting:**
  - `myFixture.testHighlighting(...)` with `<error descr="…">` markup for annotator/parser errors;
  - `EditorTestUtil.testFileSyntaxHighlighting()` for lexer-based colouring.
  - https://plugins.jetbrains.com/docs/intellij/testing-highlighting.html
- **Folding:** `myFixture.testFolding(...)` with `<fold>` markup.
- **Structure view:** `myFixture.testStructureView {}`.
- **Brace matching and commenter:** fixture-based tests.
- Tests use real platform components and avoid mocks. The platform's approach is "model-level functional tests". https://plugins.jetbrains.com/docs/intellij/testing-plugins.html

### 4.3 What coverage means here
- There is no line-coverage percentage gate (the template dropped Kover). Coverage is judged by the **feature matrix**: every grammar rule, token type, highlighter attribute, fold region type, structure-view element and preview state (rendering, ok, DOT error, engine error, JCEF unavailable) has at least one test.
- Every bug fix lands with a test that failed before the fix.
- Preview pipeline unit tests cover: debounce coalesces bursts; a stale render is discarded; cancellation on dispose; a viz-js error message maps to a bundle message.

### 4.4 Plugin Verifier in CI
- `./gradlew verifyPlugin` runs on every PR, against `select { sinceBuild = "252"; untilBuild = "<top>.*" }` plus `recommended()`. https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-tasks.html
- `failureLevel` includes `COMPATIBILITY_PROBLEMS`, `INTERNAL_API_USAGES`, `DEPRECATED_API_USAGES`, `SCHEDULED_FOR_REMOVAL_API_USAGES`, `EXPERIMENTAL_API_USAGES`, `OVERRIDE_ONLY_API_USAGES`, `NON_EXTENDABLE_API_USAGES`, `PLUGIN_STRUCTURE_WARNINGS`, `MISSING_DEPENDENCIES`, `INVALID_PLUGIN` and `NOT_DYNAMIC`. That is every level; none are exempted.

## 5. Supply chain and security

### 5.1 GitHub Actions
- **MUST pin every `uses:` to a full 40-character commit SHA,** with the tag in a trailing comment (`# v7.0.0`). "Pinning an action to a full-length commit SHA is currently the only way to use an action as an immutable release." https://docs.github.com/en/actions/reference/security/secure-use. Dependabot keeps the SHAs current.
- **Permissions:** top-level `permissions: contents: read`. Only the release-draft and publish jobs (added in the release PR) get `contents: write`.
- **Triggers and inputs:** no `pull_request_target`. Untrusted values (`github.event.*.body`, titles, branch names) reach shell steps **only through `env:`**, never through `${{ }}` inside `run:`. (same page)

### 5.2 Gradle
- **Wrapper validation:** `gradle/actions/setup-gradle` validates the wrapper by default ("the same wrapper validation as … the dedicated wrapper-validation action"). Never set `validate-wrappers: false`. https://github.com/gradle/actions/blob/main/docs/setup-gradle.md
- **Dependency verification: SHOULD enable `gradle/verification-metadata.xml` (sha256)** once the scaffold dependency set is stable. https://docs.gradle.org/current/userguide/dependency_verification.html
  - The doc warns that bootstrapping "trusts whatever is currently in your repositories", so the first file gets a reviewed PR of its own.
  - Every later change to it shows up as a reviewable diff.
  - Decided (DECISIONS.md): the verification file comes in its own reviewed PR once scaffold dependencies settle.

### 5.3 Bundled viz-js (and the Graphviz inside it)
- **Source of the files:**
  - The `@viz-js/viz` version is pinned exactly (latest release tag at writing: `release-viz-3.31.0`).
  - The files are taken from the npm registry tarball. Recorded in `third_party/viz-js/README.md`: the tarball URL, the registry `integrity` (sha512), and the SHA-256 of every file we ship.
  - A Gradle task recomputes those SHA-256s and fails `check` on mismatch.
- **What is inside it:** viz-js is MIT (https://github.com/mdaines/viz-js/blob/main/LICENSE). Its wasm is compiled from **Graphviz 16.1.0** and **libexpat 2.8.5** (`packages/viz/backend/Dockerfile` in that repo).
- **Licence obligations for the bundled wasm:**
  - Graphviz is **EPL-2.0** (https://graphviz.org/license/, https://www.eclipse.org/legal/epl-2.0/). Distributing it in object form requires:
    - including a copy of the EPL-2.0;
    - preserving copyright notices;
    - stating where the corresponding source is available. A link to the exact Graphviz 16.1.0 source tarball and to the viz-js tag satisfies this.
  - libexpat is MIT and needs its notice.
- **Where the licences ship:** inside the plugin distribution under `META-INF/third-party/` (`viz-js-LICENSE`, `graphviz-EPL-2.0.txt`, `graphviz-embedded-notices.txt`, `expat-COPYING`, `emscripten-LICENSE`, `musl-COPYRIGHT`, and a `NOTICE` listing component, version, licence and source URL, plus the libc++/libc++abi note). **Licence notices cover everything compiled into a bundled binary, not just the named library.** That includes per-file notices inside the library's sources and toolchain runtimes, such as the Emscripten runtime, musl libc and libc++. The same list goes in the README.
  - Verify the expat and Graphviz licence texts against the actual 16.1.0 and 2.8.5 tarballs when bundling. This was not done for this draft.
- **Our own licence:** the repo's licence must be compatible with shipping EPL-2.0 object code alongside it. Decided: Apache-2.0 (DECISIONS.md).

### 5.4 Signing and publishing secrets
- **Generating the key:** RSA 4096, encrypted, made locally with `openssl genpkey -aes-256-cbc …`. The key and certificate live under `~/.config/dot-studio/signing/` (mode 600). https://plugins.jetbrains.com/docs/intellij/plugin-signing.html
- **Supplying it to the build:** only through the env vars `PRIVATE_KEY`, `PRIVATE_KEY_PASSWORD`, `CERTIFICATE_CHAIN` and `PUBLISH_TOKEN`, held as GitHub Actions secrets that Aaron adds. "Never commit your credentials to the Version Control System!" (same page)
- **Release check:** the release job runs `verifyPluginSignature` before `publishPlugin`.
- **Secret scanning:** enabled on the repo.

## 6. Repo hygiene
- **Licence:** the repo-level Apache-2.0 `LICENSE` covers our code. No per-file licence headers are required.
- **Commits:** Conventional Commits (`feat:`, `fix:`, `refactor:`, `test:`, `build:`, `ci:`, `docs:`, `chore:`; `!` for breaking changes). https://www.conventionalcommits.org/en/v1.0.0/
- **Versioning:** SemVer (https://semver.org/). `version` in `gradle.properties` changes only in release PRs.
- **Changelog:** `CHANGELOG.md` in Keep a Changelog format (https://keepachangelog.com/en/1.1.0/). Every user-visible change adds a line under `[Unreleased]` in the same PR. Entries are written for users ("Preview no longer freezes on 5k-node graphs"), not as commit subjects.
- **Branches:**
  - `main` is protected: PRs only, required checks (build, test, verify, lint), linear history, no force pushes, no deletions.
  - Aaron merges.
  - Branch names: `feat/…`, `fix/…`, `ci/…`, `docs/…`.
- **PRs:**
  - `.github/pull_request_template.md` contains the checklist in section 8.
  - One concern per PR, and SHOULD stay under ~400 changed lines excluding test data. A larger PR states why it can't be split.
- **Issue templates:** bug report (IDE version, plugin version, the DOT input, expected vs actual result) and feature request.

## 7. No AI slop: concrete, checkable rules
1. **Comments explain *why*, never *what*.** A comment that restates the next line is deleted. KDoc only on non-obvious contracts, such as threading, nullability meaning and units.
2. **No dead or speculative code.**
   - No unused parameters, functions, classes, settings or extension points.
   - No interfaces with a single implementation, unless that implementation is the test seam named in 1.7.
   - No "for future use" hooks.
3. **No `TODO`/`FIXME` without an issue link:** `// TODO(#42): …`. A CI check fails on a bare `TODO`/`FIXME` in any tracked text file (including `.bnf`, `.flex`, `.xml`, `.kts`; excluding `gradlew*`, `*.md`, `.github/workflows/*` and the self-test fixtures in `.github/lint-fixtures/`), matching every occurrence, not just the first on a line.
4. **No catch-all swallowing.**
   - No `catch (e: Exception) {}`, no `catch (e: Throwable)`, no `runCatching` that discards the failure.
   - Catch the specific exception, then handle it, rethrow it, or log it with context.
   - Never catch PCE or `CancellationException` without rethrowing (1.1).
5. **Every dependency is justified in the PR description:** what it does, why the platform can't, its licence and its size. Bundled jars are listed in the README third-party section.
6. **No unused template boilerplate** (3.3, "Do not adopt"). Nothing named `My*` or `Sample*`.
7. **Every behaviour change has a test** that fails without the change. Refactors keep the tests green without editing expectations.
8. **Domain names:** use DOT/Graphviz vocabulary from the spec (`edge_stmt`, `subgraph`, `layout engine`, `port`, `compass point`). Banned in identifiers: `data`, `info`, `item`, `handle`, `process`, `manager`, `util` and `helper` as stand-alone nouns.
9. **Minimal surface:** `internal` by default (3.1). Nothing public that tests alone need; use `@TestOnly` or test fixtures instead.
10. **No generated files committed,** except the Gradle wrapper (`gradlew`, `gradlew.bat`, `gradle/wrapper/*`), which the toolchain requires. Grammar-Kit output, `build/` and `.intellijPlatform/` are git-ignored.
11. **No invented APIs.** Every platform API used must exist in the 2025.2 SDK and pass the verifier. When a PR uses an unfamiliar API, its description links the SDK doc or source.
12. **No unverified claims** in README, listing, changelog or commit messages ("fast", "robust", "production-ready"). Performance statements carry a measured number and how it was measured.
13. **No suppressions without a reason.** `@Suppress`, ktlint disables and baseline entries each carry a comment explaining why the rule is wrong *here*.
14. **No hardcoded user-visible strings** (1.8). A CI check (`.github/scripts/check-standards.sh`) flags a string literal passed *directly* as an argument to `Messages.show*(…)` or `Notification(…)`, and the forms `text = "…"`, `label("…")` and `button("…")`, in `src/main`. Literals nested inside a bundle call, such as `DotStudioBundle.message("key")`, are allowed. Known limits: the check is line-based, and it misses arguments nested two parentheses deep (#2).
   - Every grep-based check MUST fail closed (a grep error fails the job).
   - Every grep-based check MUST be proven in CI by a bad sample it catches.
15. **Consistency over novelty.** A new pattern (a new concurrency primitive, a new way to talk to JCEF) needs a PR that introduces it alone, with the reason.

## 8. PR checklist (copy into `.github/pull_request_template.md`)
```
- [ ] One concern; <~400 lines excl. test data (or reason given)
- [ ] Conventional Commit title; CHANGELOG [Unreleased] updated if user-visible
- [ ] CI green: lintKotlin, check (tests), verifyPlugin (all failure levels), buildPlugin
- [ ] Behaviour change → test that fails without it; new/changed testData .txt diffs read
- [ ] Nothing heavy on EDT; reads in readAction, writes in writeAction on EDT; coroutines from injected scope
- [ ] Every Disposable/listener/JBCefJSQuery has a non-Application/Project parent
- [ ] No static mutable state; no Kotlin `object` extensions; services not cached in fields
- [ ] No Internal/Experimental/Deprecated/ScheduledForRemoval API (verifier clean)
- [ ] User-visible strings in DotStudioBundle; no string concatenation of sentences
- [ ] JCEF: isSupported() guarded; local resources only; DOT passed as data, not code
- [ ] No PCE/CancellationException swallowed; no catch-all; Logger used, LOG.error only for bugs
- [ ] No new dependency, or dependency justified (purpose, licence, size); actions SHA-pinned
- [ ] No comments restating code; no dead/speculative code; TODOs link an issue
- [ ] Names from DOT spec; visibility internal/private unless required
- [ ] If preview/UI touched: checked in runIde on 2025.2 and latest, light + dark theme, JCEF-off fallback
```
