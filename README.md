# DOT Studio

A JetBrains IDE plugin for [Graphviz](https://graphviz.org/) DOT files, with a live preview. It works in IDEs from 2025.2 (build 252) to 2026.2 (build 262).

## Features

- `.dot` and `.gv` files open as DOT files, with syntax highlighting (colours are set in Settings | Editor | Color Scheme | DOT), syntax error highlighting, folding, brace matching, line and block commenting, and a Structure view of subgraphs, nodes and edges
- A live preview in a split editor that renders the graph as you type, using the Graphviz layout engines bundled with the plugin, so no Graphviz installation is needed. Set `layout=` in the graph to choose an engine. The preview needs the IDE's embedded browser (JCEF)
- Export of the rendered graph as SVG or PNG
- No network calls and no data collection

Zoom and pan in the preview are tracked in [#27](https://github.com/acalebratliff/dot-studio/issues/27).

## Install

From the JetBrains Marketplace: Settings | Plugins | Marketplace, search for "DOT Studio". To install a build you made yourself, use Settings | Plugins | gear icon | Install Plugin from Disk and pick the zip from `build/distributions`.

## Building

Requires JDK 21.

```
./gradlew buildPlugin   # plugin zip in build/distributions
./gradlew check         # tests
./gradlew lintKotlin    # ktlint
./gradlew verifyPlugin  # JetBrains Plugin Verifier
```

## Third-party components

The plugin bundles one file, `preview/viz-js/viz-global.js` (1,329,882 bytes), taken unmodified from the `@viz-js/viz` npm package. Source, checksums and the reason for each file: [third_party/viz-js/README.md](third_party/viz-js/README.md). Licence texts, notices and source links ship in the plugin under `META-INF/third-party/`.

| Component | Version | Licence | Source |
|---|---|---|---|
| viz-js (`@viz-js/viz`) | 3.31.0 | MIT | https://github.com/mdaines/viz-js/tree/release-viz-3.31.0 |
| Graphviz (compiled to WebAssembly inside viz-js) | 16.1.0 | EPL-2.0 | https://gitlab.com/api/v4/projects/4207231/packages/generic/graphviz-releases/16.1.0/graphviz-16.1.0.tar.gz |
| Expat (compiled to WebAssembly inside viz-js) | 2.8.5 | MIT | https://github.com/libexpat/libexpat/releases/download/R_2_8_5/expat-2.8.5.tar.gz |
| Emscripten runtime (compiled into viz-js) | 6.0.10 | MIT or NCSA | https://github.com/emscripten-core/emscripten/tree/6.0.10 |
| musl libc (compiled into viz-js, via Emscripten) | Emscripten 6.0.10 copy | MIT | https://github.com/emscripten-core/emscripten/tree/6.0.10/system/lib/libc/musl |
| libc++, libc++abi (compiled into viz-js, via Emscripten) | Emscripten 6.0.10 copy | Apache-2.0 WITH LLVM-exception | https://github.com/emscripten-core/emscripten/tree/6.0.10/system/lib/libcxx |

## Contributing

Read [CODING-STANDARDS.md](CODING-STANDARDS.md) first; CI enforces it. Work happens on a feature branch and goes in through a pull request, and every user-visible change adds a line to [CHANGELOG.md](CHANGELOG.md). Report bugs and request features through [GitHub Issues](https://github.com/acalebratliff/dot-studio/issues).

## Licence

Apache-2.0, see [LICENSE](LICENSE).
