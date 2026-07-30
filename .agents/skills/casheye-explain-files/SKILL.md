---
name: casheye-explain-files
description: Use only when explicitly invoked as $casheye-explain-files to explain CashEye files selected with @, line by line.
---

# Explain selected CashEye files

1. Read `AGENTS.md` and follow its current repository rules.
2. Explain only files explicitly selected by the user with `@`, unless a directly required dependency must be inspected to avoid a misleading explanation. State that dependency before using it.
3. Inspect the selected source before explaining it. For CashEye code search, use `casheye-code-search` first. For a file longer than 500 lines, run `ast-index outline <file>` before reading its relevant ranges.
4. Explain the source in its actual project context: module ownership, package/imports, declarations, control flow, data flow, Compose/UI behavior, and external calls where applicable.
5. Go line by line in source order. Combine adjacent lines only when they form one indivisible construct. Include line numbers/ranges and distinguish confirmed behavior from inference.
6. Do not change source unless the user separately asks.
7. Do not run Gradle, tests, lint, builds, or device checks unless explicitly authorized.

## Response format

Start with the file purpose and module/package role. Then use compact bullets:

`<line or range> — <what this line or construct does and why it matters>.`

Finish with relevant outside dependencies and static-only limitations.
