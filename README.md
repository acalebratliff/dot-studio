# DOT Studio

A JetBrains IDE plugin for Graphviz DOT files. **In development: nothing is released yet.**

Planned for version 1.0:

- `.dot` and `.gv` file type with syntax highlighting, folding, structure view, brace matching and commenting
- Live preview in a split editor, rendered in the background, with zoom, pan and a layout engine picker
- A bundled renderer, so no Graphviz installation is needed
- SVG and PNG export

Target IDEs: 2025.2 (build 252) through 2026.2 (build 262).

## Building

Requires JDK 21.

```
./gradlew buildPlugin   # plugin zip in build/distributions
./gradlew check         # tests
./gradlew lintKotlin    # ktlint
./gradlew verifyPlugin  # JetBrains Plugin Verifier
```

## Third-party components

The plugin bundles one file, `preview/viz-js/viz-global.js` (1,329,882 bytes), taken unmodified from the `@viz-js/viz` npm package. Source, checksums and the reason for each file: [third_party/viz-js/README.md](third_party/viz-js/README.md). Licence texts and source links ship in the plugin under `META-INF/third-party/`.

| Component | Version | Licence | Source |
|---|---|---|---|
| viz-js (`@viz-js/viz`) | 3.31.0 | MIT | https://github.com/mdaines/viz-js/tree/release-viz-3.31.0 |
| Graphviz (compiled to WebAssembly inside viz-js) | 16.1.0 | EPL-2.0 | https://gitlab.com/api/v4/projects/4207231/packages/generic/graphviz-releases/16.1.0/graphviz-16.1.0.tar.gz |
| Expat (compiled to WebAssembly inside viz-js) | 2.8.5 | MIT | https://github.com/libexpat/libexpat/releases/download/R_2_8_5/expat-2.8.5.tar.gz |
| Emscripten runtime (compiled into viz-js) | 6.0.10 | MIT or NCSA | https://github.com/emscripten-core/emscripten/tree/6.0.10 |
| musl libc (compiled into viz-js, via Emscripten) | Emscripten 6.0.10 copy | MIT | https://github.com/emscripten-core/emscripten/tree/6.0.10/system/lib/libc/musl |
| libc++, libc++abi (compiled into viz-js, via Emscripten) | Emscripten 6.0.10 copy | Apache-2.0 WITH LLVM-exception | https://github.com/emscripten-core/emscripten/tree/6.0.10/system/lib/libcxx |

## Contributing

Read [CODING-STANDARDS.md](CODING-STANDARDS.md). Report bugs and request features through GitHub Issues.

## Licence

Apache-2.0, see [LICENSE](LICENSE).
