## Code search

For any code search or project exploration in CashEye, use the
`casheye-code-search` skill first. It defines the Android/Kotlin-specific
workflow and uses `ast-index` as the primary CLI.

Use the general `ast-index` skill only when the CashEye skill does not cover
the requested language, command, or workflow. Use `rg` only for string
literals, comments, regex searches, or when `ast-index` returns no results.

## Architecture

Project uses multi-module Clean Architecture with MVVM + MVI-style presentation.

### Module structure

- `:app` — application entry point, DI composition, root navigation. Must not contain feature UI, domain logic, DTOs, repositories, or shared UI components.
- `:core:*` — reusable code without feature business logic:
    - `:core:model` — shared domain models used by multiple features;
    - `:core:ui` / `:core:designsystem` — reusable Compose components, theme, formatting;
    - `:core:common` — shared utilities only when genuinely cross-feature.
- `:feature:<name>` — UI/presentation of one feature: Route, Screen, ViewModel, `UiState`, `Intent`, `Effect`, feature navigation contract.
- `:domain:<name>` — feature business contracts: use cases, repository interfaces, feature-specific domain models.
- `:data:<name>` — implementations of `:domain:<name>` contracts, API/DB DTOs, mappers, data sources.

### Dependency rules

Dependencies must point inward:

`app -> feature:<name>, data:<name>`
`feature:<name> -> domain:<name>, core:*`
`data:<name> -> domain:<name>, core:*`
`domain:<name> -> core:model` only when model is truly shared.

- A feature must never depend on another feature implementation.
- Cross-feature interaction uses a small public contract module or a navigation contract, never another feature's internal classes.
- `domain` must not depend on Android, Compose, Retrofit, Room, DTOs, or DI frameworks.
- `data` must not depend on `feature` or `app`.
- Feature-specific models must stay in that feature's `domain` module; move a model to `core:model` only after at least two features need it.
- `app` composes implementations and DI bindings; it does not own feature code.

### Presentation pattern

Each screen follows MVVM + MVI-style unidirectional state flow:

- `ViewModel` exposes immutable `StateFlow<...UiState>`.
- UI sends user actions only through `onIntent(intent)`.
- One-time events use `SharedFlow<...Effect>`; do not encode navigation, snackbars, or other transient events as persistent state.
- Composables render state and delegate actions. They must not call repositories or use cases directly.
- `ViewModel` depends on domain use cases or repository interfaces, never on data implementations.
- Keep UI text/resources outside domain and data layers.


# Repository Instructions

## Verification Commands

Do not run compilation, tests, lint, builds, or any other Gradle verification tasks.

This includes, but is not limited to:

- `./gradlew compile...`
- `./gradlew test`
- `./gradlew lint`
- `./gradlew build`
- Windows equivalents such as `.\gradlew.bat ...`

Only inspect source files and diffs unless the user explicitly asks to run a specific verification
command.

## Commit Suggestions

When working in this Git repository, if code or project files were changed during the task, check
for uncommitted changes before the final response and include ready-to-run commit commands.

Use only:

- `git status`
- `git diff`
- `git diff --staged`, only when needed

Based on the real changes, propose commits in Conventional Commits format.

Requirements:

- Commit messages must be in English.
- Commit messages must be short and specific.
- Group commits by logical categories.
- Messages must describe the actual change, not vague phrases like `update ui`.
- Do not use `git add .`.
- Explicitly list the files that belong to each commit.
- Do not run `git add`, `git commit`, or `git push`; only print commands as text in the chat.
- Print a separate `git add <path>` command for every file so that each file appears on its own
  line.
- Print the corresponding `git commit` command on a separate line after all of its `git add`
  commands.
- Each command must be a single line that can be pasted and executed.

Example format:

```bash
git add app/src/main/java/com/example/feature/SearchScreen.kt
git add app/src/main/java/com/example/feature/SearchViewModel.kt
git commit -m "feat: add search screen state handling"

git add app/src/main/java/com/example/data/TransactionRepository.kt
git commit -m "fix: preserve transaction cache after refresh"
```

# Additional ast-index rules

## Keep Index Up To Date

After `git pull`, `git rebase`, `git checkout`, or `git switch`, run
`ast-index update`.

For active development, run the watcher in a separate terminal:

```bash
ast-index watch
```

## Mandatory Read Rules

1. **ALWAYS run `ast-index outline <file>` BEFORE `Read`** for any file longer than 500 lines.
2. Use the outline to identify the specific symbol or range you need, then `Read` only that slice with `offset` / `limit`.
3. This rule is mandatory — do not bulk-read large files without an outline first.

## Rules For Subagents

When spawning any agent for code search, ALWAYS include these instructions in
the prompt. Many agent systems do not automatically pass project rules to
subagents.

```text
For CashEye Android/Kotlin code search, use the `casheye-code-search` skill
first. It uses `ast-index` as the primary CLI. If the skill is unavailable or
does not cover the requested workflow, use `ast-index` via Bash before
grep/Grep:
- search "query" — universal search
- file "Name" — find file
- usages "Name" — find all usages
- implementations "Name" — find implementations
- class "Name" — find definition
- callers "func" — find callers

Use Grep only if ast-index returns empty or when regex/string-literal search is required.

Before using the Read tool on any file longer than 500 lines, first run
`ast-index outline <file>` to get its structure, then Read only the targeted
slice via offset/limit. Never bulk-read large files.
```
