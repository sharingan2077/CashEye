---
name: casheye-code-search
description: Use for code search, symbol lookup, usages, implementations, callers, project structure, architecture exploration, and module dependency analysis in the CashEye Android/Kotlin project.   
---

# CashEye code search

Use ast-index as the primary code-search tool.

## Project context

- Android
- Kotlin
- Jetpack Compose
- Gradle multi-module project
- Feature-first architecture

## Mandatory workflow

1. Run `ast-index update` when the index may be stale.
2. Use `ast-index explore` to understand an unfamiliar feature.
3. Use `ast-index usages` before changing public symbols.
4. Use `ast-index implementations` for interfaces and abstractions.
5. Use `ast-index map --module <path>` for module exploration.
6. Use `rg` only for string literals, comments, regex, or empty ast-index results.

## Relevant commands

- `explore`
- `search`
- `symbol`
- `class`
- `refs`
- `usages`
- `implementations`
- `callers`
- `outline`
- `changed`
- `map`
- `conventions`
- `deps`
- `dependents`
