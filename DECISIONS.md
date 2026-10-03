# Decisions

Product Owner rulings are quoted verbatim. Lead calls are marked **Lead** and can be overruled.

| Date | Decision | By |
|---|---|---|
| 2026-10-02 | Build the free DOT plugin as a distribution probe. "A. And let's keep an eye on differentiation if another copy or paid plugin shows up in the interim." | Product Owner |
| 2026-10-02 | Public repo under `acalebratliff`; CI on GitHub-hosted runners, not the homelab runner ("A") | Product Owner |
| 2026-10-02 | "adhere to coding standards. I don't want any AI sloppiness in the thing we build. If we're going to build it, we'll build it correctly." This became `CODING-STANDARDS.md` | Product Owner |
| 2026-10-02 | Licence: Apache-2.0 ("A") | Product Owner |
| 2026-10-02 | Marketplace trader status: "For our purposes, we will identify as a non trader." | Product Owner |
| 2026-10-02 | Team structure in `CLAUDE.md`, approved as proposed | Product Owner |
| 2026-10-02 | Gradle 9.8.0 (wrapper with checksum); IntelliJ Platform Gradle Plugin 2.19.0; compile against IntelliJ IDEA 2025.2.6.3 (build 252); verify up to 2026.2 (262) | Lead |
| 2026-10-02 | Plugin ID `io.github.acalebratliff.dotstudio`. It's permanent after the first upload, so the Product Owner confirms it before upload | Lead |
| 2026-10-02 | Gradle dependency-verification file comes in its own reviewed PR once scaffold dependencies settle, not on day one | Lead |
| 2026-10-02 | No release or publish workflows until the release PR. Version 1.0 is uploaded by hand by the Product Owner | Lead |
| 2026-10-02 | Day-1 preview spike: **GO** (see vault `dot-plugin/preview-spike-findings.md`). Renders run in a Web Worker with a timeout, cancelled by `terminate()`; JCEF is an optional dependency on `com.intellij.modules.jcef` for 2026.2 | Lead |
| 2026-10-02 | Standards amended from the scaffold review (A–E): Grammar-Kit subplugin instead of the archived plugin; Kotlin `apiVersion`/`languageVersion` 2.1 to match 2025.2's stdlib; a single source for plugin metadata; grep checks that fail closed and are proven by a bad sample; no per-file licence headers | Lead |
| 2026-10-02 | First change split into two PRs: `docs/process` (process docs and standards) and `feat/scaffold` (build and CI) | Lead |
| 2026-10-02 | No Claude session URLs in commits or PRs (Product Owner's global CLAUDE.md rule; the scaffold commits are reworded before the first push) | Lead |
| 2026-10-02 | JCEF availability: preview registrations go in the main plugin.xml (252 has no jcef module). The optional jcef dependency only provides classes on 262. JCEF code is isolated behind a class-availability check plus `isSupported()`, and the fallback is a notice | Lead |
| 2026-10-02 | Release name: "DOT Studio" | Product Owner |
| 2026-10-02 | Plugin ID `io.github.acalebratliff.dotstudio`, permanent once uploaded | Product Owner |
| 2026-10-02 | Vendor email: a new dedicated address, not yet created (address to be filled in here and in `plugin.xml`) | Product Owner |
| 2026-10-02 | "Build zoom/pan; drop picker." Zoom and pan are in v1.0 (#27); the layout engine picker is dropped, and engines are chosen with `layout=` in the DOT | Product Owner |
