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

To be bundled (not yet included):

| Component | Licence |
|---|---|
| viz-js | MIT |
| Graphviz (compiled to WebAssembly inside viz-js) | EPL-2.0 |
| libexpat | MIT |

## Contributing

Read [CODING-STANDARDS.md](CODING-STANDARDS.md). Report bugs and request features through GitHub Issues.

## Licence

Apache-2.0, see [LICENSE](LICENSE).
