---
name: Setup Java Environment
description: Configure Java 17 environment and load database credentials from backend/.env. Use this skill before running Maven commands or starting the Spring Boot backend.
---

# Setup Java Environment Skill

Automatically configure Java 17 environment and load all necessary environment variables for backend development.

## Usage

This skill is invoked when the user wants to:
- Start the backend server
- Run Maven commands (build, test, compile)
- Set up the development environment
- Example: "start the backend"
- Example: "run mvn spring-boot:run"
- Example: "setup the environment"

## What This Skill Does

1. **Set JAVA_HOME to Java 17**:
   - Uses `/usr/libexec/java_home -v 17` to locate Java 17
   - Exports JAVA_HOME environment variable

2. **Load Database Credentials**:
   - Reads `backend/.env` file
   - Exports DB_URL, DB_USERNAME, DB_PASSWORD

3. **Verify Setup**:
   - Confirms JAVA_HOME is set correctly
   - Confirms Java version is 17
   - Confirms database credentials are loaded

## Implementation

When this skill is invoked:

```bash
# Execute the setup script
source ./setup-env.sh
```

The `setup-env.sh` script performs:

```bash
#!/bin/bash

# Set Java 17
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export PATH=$JAVA_HOME/bin:$PATH

# Load database credentials
if [ -f backend/.env ]; then
  source backend/.env
  echo "✓ Database credentials loaded from backend/.env"
else
  echo "⚠️  Warning: backend/.env not found"
  echo "   Create it with: DB_URL, DB_USERNAME, DB_PASSWORD"
fi

# Verify setup
echo "✓ JAVA_HOME: $JAVA_HOME"
echo "✓ Java version: $(java -version 2>&1 | head -n 1)"
```

## After Setup

Once the environment is configured, the user can run:

```bash
# Navigate to backend
cd backend

# Clean build
mvn clean install

# Run Spring Boot with hot reload
mvn spring-boot:run

# Run tests
mvn test
```

## Important Notes

- **Java 17 Required**: Project uses Spring Boot 3.2.0 which requires Java 17
- **Always Run First**: Execute this skill before any Maven commands
- **Project Root**: Must be at project root (`/Users/yanzxu/claude/finance`) when executing
- **Hot Reload**: Spring Boot DevTools enabled - backend auto-restarts on code changes

## Error Handling

- **Java 17 not found**: Suggest installing with `brew install openjdk@17`
- **backend/.env missing**: Show example .env format and instruct to create it
- **Wrong directory**: Confirm current directory is project root

## Database Credentials Format

The `backend/.env` file should contain:

```bash
DB_URL=jdbc:mysql://host:port/finance?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
DB_USERNAME=your_username
DB_PASSWORD=your_password
```

**Note**: This file is in `.gitignore` and should never be committed.
