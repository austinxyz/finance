---
name: Setup Java Environment
description: Configure Java 17 environment and load database credentials from backend/.env. Use this skill before running Maven commands or starting the Spring Boot backend.
---

# Setup Java Environment Skill

Automatically configure Java 17 environment and load all necessary environment variables for backend development.

## Usage

**IMPORTANT**: Always run this skill at the start of a new session before starting the backend.

This skill should be invoked:
- **At session start** - Before any backend development
- When environment variables are not loaded
- When you see "URL must start with 'jdbc'" error
- Example: User says "start the backend" or "setup the environment"

## What This Skill Does

1. **Configures Java 17**:
   - Locates and sets JAVA_HOME to Java 17
   - macOS: Uses `/usr/libexec/java_home -v 17`
   - Linux: Searches common JVM locations
   - Validates Java 17 is available

2. **Loads Database Credentials**:
   - Reads `backend/.env` file line by line
   - Properly handles special characters in JDBC URLs (&, ?, =)
   - Exports: DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD, CLAUDE_API_KEY
   - Masks sensitive values in output

3. **Validates Setup**:
   - Confirms JAVA_HOME is set correctly
   - Shows Java version
   - Lists loaded environment variables (with masked passwords)

## Implementation

The skill executes `skill.sh` which sources `setup-env.sh`:

```bash
#!/bin/bash
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
cd "$PROJECT_ROOT"
source "$SCRIPT_DIR/setup-env.sh"
```

## After Running This Skill

The current shell session now has all required environment variables. You can:

```bash
# Start backend (environment variables are available)
cd backend && mvn spring-boot:run

# Run tests
cd backend && mvn test

# Clean build
cd backend && mvn clean install

# Start frontend
cd frontend && npm run dev
```

## Important Notes

- **Run in Current Session**: This skill exports environment variables to the current shell session
- **Background Tasks**: For background Maven tasks, use `backend/start.sh` instead which loads .env internally
- **Java 17 Required**: Spring Boot 3.2.0 requires Java 17
- **Auto-reload Enabled**: Spring Boot DevTools auto-restarts on code changes
- **Not Git Tracked**: `backend/.env` is in `.gitignore`

## Error Handling

- **Java 17 not found**:
  - macOS: `brew install openjdk@17`
  - Linux: `sudo apt install openjdk-17-jdk` or `sudo yum install java-17-openjdk`

- **backend/.env missing**: Creates example format:
  ```bash
  DB_HOST, DB_PORT, DB_NAME=jdbc:mysql://localhost:3306/finance?useSSL=false&serverTimezone=UTC
  DB_USER=your_username
  DB_PASSWORD=your_password
  CLAUDE_API_KEY=
  ```

## Why This Skill Exists

Direct sourcing of `.env` files often fails with JDBC URLs due to special shell characters (&, ?, =). This skill:
- Parses .env files line by line
- Handles special characters correctly
- Provides clear error messages
- Validates the setup

## Alternative: backend/start.sh

For background processes that can't inherit environment variables from the skill, use:
```bash
./backend/start.sh  # Loads .env internally and starts Spring Boot
```
