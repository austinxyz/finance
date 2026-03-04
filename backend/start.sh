#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if [ ! -f "$SCRIPT_DIR/.env" ]; then
    echo "Error: .env file not found in $SCRIPT_DIR"
    exit 1
fi

echo "Loading environment variables from .env..."

# Parse .env manually — handles Windows CRLF, quoted values, export prefix, and = in values
while IFS= read -r line || [[ -n "$line" ]]; do
    # Remove carriage return (Windows CRLF)
    line="${line//$'\r'/}"
    # Skip comments and blank lines
    [[ -z "$line" || "$line" == \#* ]] && continue
    # Remove optional 'export ' prefix
    line="${line#export }"
    # Split on FIRST '=' only — preserves '=' characters in values (e.g. base64 secrets)
    key="${line%%=*}"
    value="${line#*=}"
    # Skip if key is empty
    [[ -z "$key" ]] && continue
    # Strip surrounding single/double quotes from value
    value="${value%\"}" ; value="${value#\"}"
    value="${value%\'}" ; value="${value#\'}"
    export "$key=$value"
done < "$SCRIPT_DIR/.env"

echo "  DB: ${DB_USER:-<not set>}@${DB_HOST:-<not set>}:${DB_PORT:-<not set>}/${DB_NAME:-<not set>}"
echo "  JWT_SECRET: ${JWT_SECRET:+[set, ${#JWT_SECRET} chars]}${JWT_SECRET:-<not set>}"

export SPRING_PROFILES_ACTIVE=dev
echo "Using profile: dev"
echo "Starting Spring Boot..."

# Find mvn
MVN_CMD=""
if command -v mvn &>/dev/null; then
    MVN_CMD="mvn"
elif [ -f "/c/Users/lorra/tools/apache-maven-3.9.6/bin/mvn" ]; then
    MVN_CMD="/c/Users/lorra/tools/apache-maven-3.9.6/bin/mvn"
elif [ -f "$HOME/tools/apache-maven-3.9.6/bin/mvn" ]; then
    MVN_CMD="$HOME/tools/apache-maven-3.9.6/bin/mvn"
else
    echo "Error: mvn not found. Add Maven to PATH or install it."
    exit 1
fi

cd "$SCRIPT_DIR"
"$MVN_CMD" spring-boot:run
