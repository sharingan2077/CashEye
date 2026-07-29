## Current Homework

local/plan/hw4-settings/README.md

## Current Project State

HW-3 and HW-4 are implemented in source. Do not plan Room/offline mode or the Settings flow from
scratch before inspecting the current implementation.

### Implemented HW-4 scope

- `:domain:settings`, `:data:settings` and `:feature:settings` own the vertical Settings slice;
  `:app` only composes its sheet, global configuration and Android biometric integration.
- Settings open from the navigation top bar in one `ModalBottomSheet`. Nested destinations replace
  its content: Back, scrim dismissal and swipe return to the root list; only the root dismisses
  the sheet.
- Reporting-currency selection reuses the existing finance use cases and returns to the root.
  Settings must not mutate account currencies or replace missing FX data with zero.
- Articles are a read-only, locally filtered list of finance categories. They use the existing
  local-first category contract, keep useful cached data after refresh errors, and do not call
  Retrofit, Room or WorkManager from the UI.
- Theme (light/dark/system) and language (system, Russian, English, German, French, Spanish) are
  DataStore-backed. The app applies language with `AppCompatDelegate.setApplicationLocales`; locale
  changes can recreate the Activity, so nested Settings state must be reset before applying one.
- PIN and biometric preferences are persisted without plaintext PIN storage: the data layer keeps
  a salted one-way verifier. PIN setup/change/disable uses four digits through the system numeric
  IME; disabling PIN also disables biometrics.
- A configured PIN activates the app lock at startup and when returning from background. Biometrics
  are available only with a configured PIN and usable enrolled hardware; PIN remains the fallback.

### HW-4 verification boundary

Source and test sources exist, but no Gradle, test, lint, build, or device verification has been
run under these instructions. Do not claim HW-4 runtime acceptance without manually checking the
Settings sheet navigation, reporting currency and offline cached articles, all themes/locales and
their persistence, locale recreation, PIN lifecycle, background lock, and biometric success,
failure, cancellation and unavailable-hardware paths.

### Implemented HW-3 scope

- Expense, income, and account create/edit/delete flows write locally first.
- `:data:finance` owns Retrofit, Room, the durable `pending_operations` outbox, WorkManager, and
  connectivity monitoring.
- Room is the UI source of truth. Remote refreshes update Room; cached content remains available
  when the network is unavailable.
- Offline writes use a `local-wins` policy. New entities receive negative temporary IDs. Account
  operations are synchronized before dependent transactions, and successful creates remap local
  IDs to server IDs.
- Editing an unsynced create updates/collapses its outbox payload instead of appending redundant
  updates. Preserve pending entities and account references while merging server refreshes.
- Account balances are updated immediately for local transaction create/edit/delete operations.
- Unique WorkManager jobs run immediately at app startup, after local writes, after reconnect, and
  periodically every two hours. Work requires network connectivity.
- HTTP 5xx requests use up to three attempts with two-second delays. Temporary worker failures use
  WorkManager retry; non-5xx HTTP failures are permanent.
- The UI reports offline state and requests a screen refresh after an offline-to-online transition.
- Unit and instrumented test sources cover repository fallback, sync ordering/collapsing, offline
  DAO writes, and Worker result mapping. Their presence is not proof they passed in the current
  checkout.

### Sync boundaries

- Screen Refresh is a pull operation: it downloads API data and merges it into Room.
- Outbox sync is a push-then-pull operation: it sends pending local writes, remaps IDs, then
  refreshes server history.
- Do not make feature ViewModels call WorkManager or Retrofit directly. Keep these responsibilities
  in `:data:finance`.
- The API has no idempotency key. POST synchronization is at-least-once and can theoretically
  duplicate a server record if a successful response is lost before the outbox is completed.

### Known refresh UX follow-up

`NavigationRoot` passes additive, persistent `refreshKey` counters to Routes. Each Route starts
`LaunchedEffect(refreshKey)` and refreshes whenever the value is greater than zero. If a Route
leaves and re-enters composition after any counter increment, the same positive key can replay
Refresh and briefly show the pull-to-refresh indicator. Treat this as event-consumption/lifecycle
behavior to fix, not as a requirement of outgoing synchronization.

### HW-3 acceptance flow

Runtime acceptance must cover this device scenario:

1. Disable the network.
2. Create an account, then create and edit a transaction that references it.
3. Confirm negative IDs, the collapsed durable outbox, and the immediate account balance in
   Database Inspector.
4. Restart the app and confirm cached data and pending operations survive.
5. Restore the network and inspect `finance_immediate_sync` / `finance_periodic_sync`.
6. Confirm account-before-transaction ordering, server-ID remapping, an empty completed outbox,
   correct balances, and no duplicates on the server.

Static inspection cannot confirm Room, KSP, DI, Worker startup, reconnect behavior, or device UI.
Do not claim HW-3 runtime acceptance without performing this flow.

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
    - Use Conventional Commits without scopes. Never add parentheses after the commit type.

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

## Kotlin imports and package moves

- Never leave the IDE pseudo-prefix `_root_ide_package_` in source files.
- Use regular Kotlin imports and short type names instead of fully qualified names in code whenever
  an import can resolve the type.
- After moving Kotlin files or changing package declarations, update imports in every affected
  file.
- Before completing a package refactor, search all tracked source files for
  `_root_ide_package_` and replace every occurrence with a correct import and short type name.
- Use a fully qualified name in code only to resolve a real short-name conflict, and explain that
  exception in the final response.

## Kotlin and coroutines conventions

- Prefer type-safe Kotlin duration APIs:
    - `delay(300.milliseconds)`, not `delay(300L)`.
    - `withTimeout(5.seconds)`, not `withTimeout(5_000L)`.
- Import duration extensions explicitly:
    - `kotlin.time.Duration.Companion.milliseconds`
    - `kotlin.time.Duration.Companion.seconds`
- Avoid unexplained raw numeric time values.
- Extract repeated delays and timeouts into named `Duration` constants.
