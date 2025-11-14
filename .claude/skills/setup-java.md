# Setup Java Environment

This skill sets up the correct Java environment for the finance project.

## Java Version Required
- JDK 17 (OpenJDK)

## Setup Instructions

When running Maven commands (mvn), always set JAVA_HOME first:

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
```

## Common Maven Commands

After setting JAVA_HOME, you can run:

```bash
# Clean and compile
export JAVA_HOME=/opt/homebrew/opt/openjdk@17 && mvn clean compile

# Run tests
export JAVA_HOME=/opt/homebrew/opt/openjdk@17 && mvn test

# Run Spring Boot application
export JAVA_HOME=/opt/homebrew/opt/openjdk@17 && \
export DB_URL="jdbc:mysql://10.0.0.7:37719/finance?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" && \
export DB_USERNAME="austinxu" && \
export DB_PASSWORD="helloworld" && \
mvn spring-boot:run
```

## Important Notes
- Always use JDK 17 located at `/opt/homebrew/opt/openjdk@17`
- Set JAVA_HOME before any mvn command
- Database credentials are also required when running the application
