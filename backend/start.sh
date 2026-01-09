#!/bin/bash

# Get the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Check if .env exists
if [ ! -f "$SCRIPT_DIR/.env" ]; then
    echo "❌ Error: .env file not found in $SCRIPT_DIR"
    exit 1
fi

echo "🔐 Loading environment variables from .env..."

# Use Python to load .env and start Maven
# Python handles special characters in environment variables correctly
cd "$SCRIPT_DIR"
python3 <<'PYTHON_SCRIPT'
import os
import sys
import subprocess

# Load environment variables from .env file
with open('.env', 'r') as f:
    for line in f:
        line = line.strip()
        # Skip comments and empty lines
        if not line or line.startswith('#'):
            continue
        # Parse key=value
        if '=' in line:
            key, value = line.split('=', 1)
            os.environ[key] = value

# Display loaded variables (truncated for security)
db_url = os.environ.get('DB_URL', '')
db_username = os.environ.get('DB_USERNAME', '')
jwt_secret = os.environ.get('JWT_SECRET', '')

print('✅ Environment variables loaded')
print(f'   DB_URL: {db_url[:50]}...')
print(f'   DB_USERNAME: {db_username}')
print(f'   JWT_SECRET: {jwt_secret[:30]}...')
print('🚀 Starting Spring Boot application...')

# Run Maven with loaded environment
result = subprocess.run(['mvn', 'spring-boot:run'])
sys.exit(result.returncode)
PYTHON_SCRIPT
