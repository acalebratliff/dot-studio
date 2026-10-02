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
