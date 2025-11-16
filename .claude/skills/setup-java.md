# Setup Java Environment

This skill sets up the correct Java environment for the finance project.

## Java Version Required
- JDK 17 (OpenJDK)

## Setup Instructions

**RECOMMENDED**: Use the setup script to configure everything automatically:

```bash
# From project root
source ./setup-env.sh
```

This script automatically:
- Sets JAVA_HOME to Java 17
- Loads database credentials from backend/.env
- Exports all necessary environment variables

## Manual Setup (if needed)

If you need to set JAVA_HOME manually:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
```

## Common Maven Commands

**Always run setup script first**, then you can run Maven commands:

```bash
# Setup environment (do this first!)
source ./setup-env.sh

# Clean and compile
mvn clean compile

# Run tests
mvn test

# Run Spring Boot application
mvn spring-boot:run
```

## Important Notes
- **Use `source ./setup-env.sh` instead of manually setting variables**
- The script uses `/usr/libexec/java_home -v 17` to find Java 17 automatically
- Database credentials are loaded from backend/.env (not tracked in git)
- All environment variables are exported by the setup script
