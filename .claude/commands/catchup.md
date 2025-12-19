# Catchup Command

Read all files that have been modified in the current git branch to restore context after `/clear`.

Steps:

1. Run `git diff --name-only $(git merge-base HEAD origin/master)..HEAD` to get all changed files in the current branch
2. Filter out files that shouldn't be read (node_modules, build artifacts, etc.)
3. Read each file to understand recent changes
4. Provide a brief summary of what has changed in this branch

This allows you to quickly restore context after clearing the conversation history.
