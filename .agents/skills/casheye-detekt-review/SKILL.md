---
name: casheye-detekt-review
description: Run and review Detekt across the CashEye Android project, then fix only findings that identify a real correctness, maintainability, or
---

# CashEye Detekt review

Treat Detekt as engineering feedback, not a score to maximize. A green report is not a goal by itself.

## Workflow

1. Read the repository's `AGENTS.md` and obey its current instructions.
2. Inspect `git status` before changes. Preserve unrelated user changes.
3. From the repository root, run only:

 ```powershell
 .\gradlew.bat detekt

4. Inspect every finding before editing and classify it:
  - Fix: a plausible bug, unsafe code, misleading/duplicated logic, or a local design problem whose fix improves the code independently of Detekt.
  - Discuss, do not change: a subjective threshold rule where refactoring would be artificial or reduce readability.
  - False positive / intentional: leave unchanged and explain why.

5. Make only minimal, justified fixes. Do not split Compose code or refactor solely to satisfy complexity or function-count metrics.
6. Do not weaken shared Detekt thresholds, edit config/detekt/detekt.yml, or add suppressions merely to make the report green. Ask the user before policy/
 configuration changes.

7. Re-run .\gradlew.bat detekt after fixes. If the command is blocked by the environment, report its exact failure and continue with source/diff
 inspection.

8. If files changed, run only git diff --check, git diff, and git status for final inspection.

## Report

For every finding, state: fixed, left intentionally, or needs a decision. Explain why each fix was useful independently of Detekt.

If files changed, include ready-to-run Conventional Commit commands following the repository rules. Do not stage or commit anything yourself.
