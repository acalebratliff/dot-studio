# Vendored viz-js

`@viz-js/viz` **3.31.0**, the WebAssembly build of Graphviz that the preview renders with. Rules: CODING-STANDARDS.md §5.3.

## Source

| | |
|---|---|
| npm tarball | https://registry.npmjs.org/@viz-js/viz/-/viz-3.31.0.tgz |
| Registry `integrity` | `sha512-r7zlQdRvcwvIpjHGgs+KNWHeE/P5/Dq7k8ZAKaMCbZqomxCBJV78gVuQYaHzFVca+kB0mX0fMW9UFevCOBG50A==` |
| Git tag | [`release-viz-3.31.0`](https://github.com/mdaines/viz-js/tree/release-viz-3.31.0) (commit `c8ce050d28eb0dc1ed1629ea32ab7f3dc117db0f`) |
| Licence | MIT; the bundled Graphviz 16.1.0 is EPL-2.0, Expat 2.8.5 is MIT, and the Emscripten 6.0.10 runtime is MIT/NCSA with musl (MIT) and libc++ (Apache-2.0 WITH LLVM-exception). Texts and source links: `src/main/resources/META-INF/third-party/` |

The tarball was downloaded on 2026-10-02 and its sha512 matched the registry `integrity` above. Files were copied out of it unmodified.

## Shipped files

Paths are relative to this directory. `processResources` copies `dist/` into the plugin jar at `preview/viz-js/`.

| File | SHA-256 | Bytes | Why it ships |
|---|---|---|---|
| `dist/viz-global.js` | `c9e0b310f9883910e01c66054b48b7ff4be3d8695141116635e72b0af152692e` | 1329882 | The renderer. It sets a global `Viz`, so the render Web Worker loads it with `importScripts`. The wasm is embedded in it, so there is no separate `.wasm` file. |

The `verifyVizJsChecksums` task (part of `check`, and run before `processResources`) recomputes these hashes. It fails on a mismatch, on a file missing from the table, and on a table row with no file.

## Package files not shipped

- `dist/viz.js`: the same renderer as an ES module. Loading it in a worker needs `new Worker(url, { type: "module" })`. The day-1 preview spike measured the classic worker with `importScripts` on 2025.2 (CEF 122) and 2026.2 (CEF 144); the module-worker path was not tested, so we ship the global build.
- `dist/viz.cjs`: CommonJS build for Node.
- `lib/`, `src/`: the sources the `dist/` builds are made from.
- `types/`: TypeScript declarations.
- `package.json`, `README.md`: package metadata. The package has no `LICENSE` file; `META-INF/third-party/viz-js-LICENSE` is the `LICENSE` from the git tag.

## Updating

1. Download the new tarball and check its sha512 against `npm view @viz-js/viz@<version> dist.integrity`.
2. Read `packages/viz/backend/Dockerfile` at the new tag for the Graphviz, Expat and emsdk versions. Update `META-INF/third-party/` (NOTICE, licence texts, embedded notices) from those source tarballs.
3. Copy the files, then update this README's version, URLs, `integrity` and hash table. `./gradlew check` must pass.
