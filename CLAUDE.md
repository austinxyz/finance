# Finance App - Claude Code Guide

> **Context Recovery**: When resuming sessions, read this file first.
> **Project Root**: `/Users/yanzxu/claude/finance/`

## Stack

Java 17 + Spring Boot 3.2 + Vue 3 + MySQL 8.0

## Critical Guardrails

### Environment Setup

**ALWAYS run `/setup-java` at session start** - It configures Java 17, loads DB credentials from `backend/.env`, and exports required environment variables.

**NEVER commit `backend/.env`** - Contains DB credentials. Already in `.gitignore`.

### Backend Development

**ALWAYS use Maven wrapper** - `./mvnw` instead of `mvn` for consistency.

**NEVER modify JPA entities without checking existing records** - `ddl-auto=update` auto-migrates schema. For breaking changes, write migration SQL first.

**NEVER use `SELECT *`** - Specify columns in JPA queries. Database has 50+ columns across tables.

**ALWAYS use TimeService.getCurrentTimestamp()** - Ensures consistent timezone handling across time-series data.

### Frontend Development

**ALWAYS use Composition API** - No Options API. All new components use `<script setup>`.

**NEVER use inline styles** - Use Tailwind classes or CSS variables. See `docs/frontend-best-practices.md` for theme system.

**ALWAYS format currency with symbols** - Use `formatCurrency(value, currency)` helper. Never display raw numbers.

**NEVER hardcode colors** - Use CSS variables (`--primary`, `--secondary`) for theme support.

### Database Operations

**Use `/mysql-exec` skill for all DB operations**:
```bash
/mysql-exec path/to/script.sql    # Execute SQL file
/mysql-exec "SELECT ..."          # Quick query
/mysql-exec                       # Interactive shell
```

**NEVER run raw `mysql` commands** - The skill handles credentials and connection.

**ALWAYS create new time-series records** - Never UPDATE existing asset/liability records. Create new records with new timestamps.

### Git Workflow

**Use `/git-commit-push` skill** - Stages, commits, and pushes in one step. Follows conventional commits format.

**NEVER force push** - This is a personal project but maintain clean history.

**ALWAYS run tests before commit** - Backend: `./mvnw test`, Frontend: `npm test` (when tests exist).

## Common Anti-Patterns

❌ **Don't**: Create comprehensive documentation in CLAUDE.md for every feature
✅ **Do**: Document what Claude gets wrong. Point to external docs for details.

❌ **Don't**: Write "Never use feature X" without alternatives
✅ **Do**: Write "Never use X, prefer Y because [reason]"

❌ **Don't**: Execute complex multi-step operations manually
✅ **Do**: Use skills or write simple bash scripts

❌ **Don't**: Update CLAUDE.md with temporary workarounds
✅ **Do**: Fix the underlying issue and document the pattern

## Architecture Quick Reference

```
backend/
  src/main/java/com/finance/app/
    controller/     # REST endpoints
    service/        # Business logic
    repository/     # Data access (Spring Data JPA)
    model/          # JPA entities
    dto/            # Data transfer objects
    config/         # Spring configuration

frontend/
  src/
    components/     # Reusable UI components
    views/          # Page components (lazy-loaded routes)
    router/         # Vue Router config
    api/            # Axios client + API calls
```

**Time-Series Data Model**: Asset/Liability accounts have multiple records with timestamps. Never update - always create new records.

**Multi-Currency**: All amounts stored in original currency + converted to USD. Use `ExchangeRateService` for conversions.

## Development Workflow

```bash
# 1. Start session
/setup-java

# 2. Backend development
cd backend
./mvnw spring-boot:run          # Auto-reload with DevTools

# 3. Frontend development
cd frontend
npm run dev                     # HMR at localhost:3000

# 4. Database changes
/mysql-exec path/to/migration.sql

# 5. Commit changes
/git-commit-push
```

## When Things Go Wrong

**"No Java compiler available"** → Run `/setup-java` to set JAVA_HOME

**JPA schema mismatch** → Check `backend/.env` credentials. You may be pointing to wrong DB.

**Frontend proxy errors** → Ensure backend is running on port 8080. Check `vite.config.js` proxy settings.

**Currency conversion errors** → Verify exchange rate exists for the date. Use `ExchangeRateService.getOrFetchRate()`.

**Tests failing** → Check if time-series logic creates records instead of updating. Common mistake.

## External Documentation

For detailed information not covered by these guardrails:

- **Feature requirements**: `requirement/需求说明.md`
- **API contracts**: `requirement/API文档.md`
- **Frontend patterns**: `docs/frontend-best-practices.md`
- **API explorer**: http://localhost:8080/api/swagger-ui/index.html (when backend running)

## Skills Available

- `/setup-java` - Configure environment + load DB credentials
- `/mysql-exec` - Database operations (SQL files/queries/shell)
- `/git-commit-push` - Atomic git workflow
- `/docker-build-push` - Multi-arch Docker images (amd64/arm64)
- `/catchup` - Resume session by reading changed files (after `/clear`)

## Context Management

**Session getting slow?** Use this workflow:

1. `/clear` - Clear conversation history
2. `/catchup` - Auto-reads all changed files in current git branch
3. Continue working

**For complex multi-day features**:

1. Ask Claude to document progress in `docs/wip/[feature-name].md`
2. `/clear` to reset
3. Tell Claude to read the WIP doc and continue

**Never use `/compact`** - It's opaque and error-prone. Use `/clear` + `/catchup` instead.
