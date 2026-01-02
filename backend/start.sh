#!/bin/bash

# Get the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Load environment variables from .env
if [ -f "$SCRIPT_DIR/.env" ]; then
    echo "🔐 Loading environment variables from .env..."

    # Read .env file line by line
    while IFS='=' read -r key value; do
        # Skip comments and empty lines
        [[ $key =~ ^#.*$ ]] && continue
        [[ -z $key ]] && continue

        # Remove quotes and export
        value=$(echo "$value" | sed -e 's/^"//' -e 's/"$//' -e "s/^'//" -e "s/'$//")
        export "$key=$value"
    done < "$SCRIPT_DIR/.env"

    echo "✅ Environment variables loaded"
else
    echo "❌ Error: .env file not found in $SCRIPT_DIR"
    exit 1
fi

# Change to backend directory and start Spring Boot
cd "$SCRIPT_DIR"
echo "🚀 Starting Spring Boot application..."
mvn spring-boot:run
