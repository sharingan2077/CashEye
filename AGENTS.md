# Repository Instructions

## Commit Suggestions

When working in this Git repository, if code or project files were changed during the task, check for uncommitted changes before the final response and include ready-to-run commit commands.

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
- Each command must be a single line that can be pasted and executed.

Example format:

```bash
git add app/src/main/java/com/example/feature/SearchScreen.kt app/src/main/java/com/example/feature/SearchViewModel.kt && git commit -m "feat: add search screen state handling"
git add app/src/main/java/com/example/data/TransactionRepository.kt && git commit -m "fix: preserve transaction cache after refresh"
```
