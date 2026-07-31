# CashEye Agent Guide

## Working approach

- Read `README.md`, the relevant source and the current Git diff before planning or changing code.
- If a task names a document under `local/plan/`, read that document first. Treat plans as task
  context, not as a replacement for inspecting the current implementation.
- The finance offline flow and Settings vertical slice already exist. Extend them; do not redesign
  them from scratch.
- Preserve unrelated worktree changes. Do not reset, restore, reformat or move files outside the
  requested scope.
- Ask one focused question only when a product or visual decision cannot be derived from source,
  design material or the task.

## Architecture

The project uses multi-module Clean Architecture with MVVM and MVI-style presentation.

```text
app
├── core:model
├── core:designsystem
├── core:common
├── domain:finance
├── domain:settings
├── data:finance
├── data:settings
├── feature:expenses
├── feature:income
├── feature:accounts
├── feature:analytics
├── feature:settings
└── feature:splash
```

Dependencies point inward:

```text
app -> feature:<name>, data:<name>
feature:<name> -> domain:<name>, core:*
data:<name> -> domain:<name>, core:*
domain:<name> -> core:model only for genuinely shared models
```

- `:app` owns application composition, root navigation and Android-only integration. It must not
  contain feature UI, repositories, DTOs or feature business logic.
- `:feature:<name>` owns Routes, Screens, ViewModels, immutable `UiState`, intents, effects and
  feature navigation contracts. A feature never depends on another feature implementation.
- `:domain:<name>` owns use cases, repository interfaces and feature-domain models. It must not
  depend on Android, Compose, Retrofit, Room, DTOs or DI frameworks.
- `:data:<name>` implements domain contracts and owns data sources, DTOs, mappers and platform data
  infrastructure. It must not depend on `:feature` or `:app`.
- Move a feature model to `:core:model` only after at least two features need it.

### Presentation

- A ViewModel exposes immutable `StateFlow<...UiState>`.
- UI sends user actions exclusively through `onIntent(intent)`.
- Use `SharedFlow<...Effect>` for navigation, snackbars and other one-time events; do not store them
  in persistent state.
- Composables render state and delegate events. They do not call repositories or use cases.
- ViewModels depend on domain use cases or repository interfaces, never data implementations.
- Keep UI strings and Android resources out of domain and data modules.

## Finance and synchronization invariants

- Room is the UI source of truth. A refresh is a pull: download API data and merge it into Room.
- Local financial writes are atomic local-first writes and add/update the durable
  `pending_operations` outbox. The sync is push-then-pull: send pending writes, remap temporary IDs,
  then refresh Room.
- Preserve `local-wins`, negative temporary IDs, account-before-transaction synchronization and
  immediate account-balance updates.
- Feature ViewModels must not call Retrofit or WorkManager directly; `:data:finance` owns them.
- Cached data must remain useful after refresh failures. Do not replace unavailable FX data with
  zero.
- The API has no idempotency key. POST synchronization is at-least-once and may theoretically
  duplicate a server record if a successful response is lost.

## Settings and app lock invariants

- `:domain:settings`, `:data:settings` and `:feature:settings` own Settings. `:app` composes the
  sheet, global configuration, app-lock lifecycle and Android biometric prompt.
- Settings use one `ModalBottomSheet`; nested destinations replace its content. Back, scrim dismissal
  and swipe return to the root destination; only the root dismisses the sheet.
- Reporting currency reuses finance use cases and must not change account currencies.
- Articles are read-only, locally filtered categories. UI must not call Retrofit, Room or WorkManager
  directly, and cached articles remain visible after refresh errors.
- Theme and language are DataStore-backed. Reset nested Settings state before applying a language,
  because `AppCompatDelegate.setApplicationLocales` can recreate the Activity.
- Store no plaintext PIN. PIN setup/change/disable uses four digits; disabling PIN also disables
  biometrics. A configured PIN locks the app on startup and when it returns from background.
- Biometrics require a configured PIN and enrolled supported hardware; PIN remains the fallback.

## Search and source reading

For any CashEye code search or project exploration, use the `casheye-code-search` skill first. It
uses `ast-index` as the primary CLI. Use `rg` only for literals, comments, regular expressions or
when `ast-index` has no result.

- Run `ast-index update` after `git pull`, `git rebase`, `git checkout` or `git switch`.
- For unfamiliar code, start with `ast-index explore`; use `usages` before changing public symbols
  and `implementations` for interfaces.
- Before reading a file longer than 500 lines, run `ast-index outline <file>` and read only the
  relevant range.
- If delegating code search, give the subagent the same skill-first and long-file rules.

## Kotlin and Compose conventions

- In every emitting `@Composable`, declare `modifier: Modifier = Modifier` immediately after all
  required parameters. No required or optional parameter follows it.
- Use present-tense callbacks: `onClick`, `onPeriodChange`, `onTextChange`; never `onClicked`,
  `onPeriodSelected` or `onTextChanged`.
- Prefer Kotlin durations: `delay(300.milliseconds)` and `withTimeout(5.seconds)`. Import duration
  extensions explicitly and name repeated delays or timeouts.
- For a no-op `when` branch, use `else -> Unit`.
- Use ordinary Kotlin imports and short names. After a package move, update all imports and search
  tracked sources for `_root_ide_package_`.

## Verification, security and Git

- Do not run Gradle compilation, tests, lint, builds or device checks unless the user explicitly
  requests a specific command. Static inspection does not prove compilation, DI, Room, workers,
  lifecycle behavior, accessibility or device UI.
- Report exactly what was run. The existence of test sources is not proof that they passed.
- Never expose `local/api_key.txt`, `local.properties`, keystores, passwords or other local secrets.
- The current `release` variant uses R8 and a debug signing key for local testing only. Do not present
  it as a production-distribution configuration; a production release needs a separate keystore kept
  outside Git.
- After any change, inspect `git diff --check` and the relevant Git diff. Do not discard unrelated
  changes.
- Do not stage, commit or push unless explicitly asked. When suggesting commits, use short English
  Conventional Commits without scopes, list one `git add <path>` command per file, and never use
  `git add .`.
