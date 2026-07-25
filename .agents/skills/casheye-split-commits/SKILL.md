---
name: casheye-split-commits
description: Use only when explicitly invoked to analyze the current CashEye Git diff and print suggested commit commands.
---

# Split current diff into commits

Inspect the current uncommitted changes using only the Git commands allowed by the repository instructions.

Analyze the actual diff and split the changes into logical atomic commits.

Follow all commit rules from `AGENTS.md`, including:

* Conventional Commits;
* English commit messages;
* no scopes;
* explicit file paths;
* one `git add <path>` command per file;
* no `git add .`;
* do not execute `git add`, `git commit`, or `git push`;
* only print ready-to-run commands.

Do not modify source files.

If all changes belong to one logical commit, propose one commit instead of splitting them artificially.
