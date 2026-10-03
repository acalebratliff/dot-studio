# Changelog

All notable changes to this project are documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- `.dot` and `.gv` files open as DOT files, with syntax highlighting for keywords, IDs, numbers, quoted and HTML strings, comments and edge operators
- DOT colours can be changed in Settings | Editor | Color Scheme | DOT
- Comment and uncomment lines (`//`) and blocks (`/* */`) in DOT files
- Matching `{}` and `[]` are highlighted, and typing `[` inserts the closing `]`
- Syntax errors in DOT files are highlighted. A mistake in one statement is reported there and doesn't mark the rest of the file
- `->` in an undirected `graph` and `--` in a `digraph` are highlighted as errors, as Graphviz rejects them
- Unclosed quoted strings, HTML strings and `/* */` comments are highlighted as errors
- Graph and subgraph bodies, attribute lists and `/* */` comments that span several lines can be folded
- The Structure tool window lists each graph's subgraphs and node and edge statements, with edge chains such as `a -> b -> c`; selecting one moves the caret to it
- Live preview: DOT files open in a split editor that renders the graph as you type, using the bundled Graphviz, so no Graphviz installation is needed. The editor toolbar switches between editor only, editor and preview, and preview only
- The preview page follows the editor colour scheme and updates when the theme changes. Graphs keep the colours set in the DOT and Graphviz's white background unless the DOT sets `bgcolor`, so they stay readable in dark themes
- Export the rendered graph as SVG, or as PNG at 1x or 2x its actual size, from the export button in the editor tab or the editor context menu. The SVG is exactly what the preview rendered, and the PNG is drawn by the same browser engine as the preview. Export is disabled, with the reason shown, while the graph has no successful render or JCEF is unavailable. A graph too large for a PNG at the chosen scale is refused with a message suggesting 1x or SVG
- Zoom and pan the preview: Ctrl+mouse wheel zooms around the pointer, and Zoom In, Zoom Out, Actual Size (100%) and Zoom to Fit are in the editor tab (new UI) or the editor toolbar (classic UI). Drag the graph or use the scrollbars to pan. A new preview fits the graph to the window, never above 100%, and keeps it fitted as you type until you zoom; after that, re-renders keep your zoom and scroll position. Zoom goes from 10% (lower when a large graph needs it to fit) to 800%. The zoom actions have no default shortcut, because the usual zoom keys fold code in the editor; they can be assigned in the keymap. Export always saves the whole graph at its actual size, whatever the zoom
- The preview says "Only the first graph is previewed" under the graph when a file holds more than one graph, because Graphviz renders only the first
- An empty file, or one with only comments, shows "Nothing to preview yet" in the preview instead of an error
- The notice shown when the IDE has no embedded browser (JCEF) wraps to the width of the preview instead of running off its edge
- On IDE 2026.2 the plugin can be updated or uninstalled without restarting the IDE. Open DOT files are closed during an update and reopened when the new version loads
