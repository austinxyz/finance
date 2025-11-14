# Git Commit and Push

This skill enables committing and pushing changes to GitHub for the finance project.

## GitHub Account Information

- **GitHub Username**: austinxyz
- **Repository**: https://github.com/austinxyz/finance.git

## Git Configuration

For this project, use the austinxyz GitHub account (not the eBay corporate account):

- **Git User**: austinxyz
- **Email**: (use the email associated with austinxyz GitHub account)

Before committing, ensure the correct user is set for this repository:

```bash
# Check current configuration
git config user.name
git config user.email

# Set for this repository only (if needed)
git config user.name "austinxyz"
git config user.email "your-github-email@example.com"
```

## Common Git Operations

### Check Status
```bash
git status
```

### View Differences
```bash
# View unstaged changes
git diff

# View staged changes
git diff --cached

# View all changes
git diff HEAD
```

### Stage and Commit Changes

```bash
# Stage specific files
git add backend/src/main/java/com/finance/app/controller/AnalysisController.java
git add frontend/src/views/analysis/AssetAllocation.vue

# Or stage all changes
git add .

# Commit with message
git commit -m "Add net asset category drill-down feature

- Added new API endpoint to get accounts by net asset category
- Updated frontend to display asset and liability accounts in tables
- Improved UI with color-coded tables for better visualization"
```

### Push to GitHub

```bash
# Push to main branch
git push origin main

# Or push current branch
git push
```

### View Commit History
```bash
# View recent commits
git log --oneline -10

# View commits with changes
git log -p -2
```

## Best Practices

1. **Before committing**: Always check `git status` and `git diff` to review changes
2. **Commit messages**: Write clear, descriptive commit messages
3. **Atomic commits**: Each commit should represent a single logical change
4. **Test before push**: Ensure the code compiles and tests pass
5. **Pull before push**: Run `git pull` before pushing to avoid conflicts

## Common Workflows

### Feature Development
```bash
# Check current status
git status

# Stage your changes
git add <files>

# Commit with descriptive message
git commit -m "feat: add new feature description"

# Pull latest changes
git pull origin main

# Push your changes
git push origin main
```

### Bug Fix
```bash
git add <fixed-files>
git commit -m "fix: resolve issue description"
git push origin main
```

## Important Notes
- Authentication is already configured (likely using SSH keys or credential manager)
- The repository uses the `main` branch as the primary branch
- Always pull before pushing to avoid conflicts
- Use meaningful commit messages following conventional commits format when possible
