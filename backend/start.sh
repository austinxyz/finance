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
db_host = os.environ.get('DB_HOST', '')
db_port = os.environ.get('DB_PORT', '')
db_name = os.environ.get('DB_NAME', '')
db_user = os.environ.get('DB_USER', '')
jwt_secret = os.environ.get('JWT_SECRET', '')

print('✅ Environment variables loaded')
print(f'   DB: {db_user}@{db_host}:{db_port}/{db_name}')
print(f'   JWT_SECRET: {jwt_secret[:30]}...')

# Set development profile for local development
os.environ['SPRING_PROFILES_ACTIVE'] = 'dev'
print('🔧 Using profile: dev (development environment)')
print('🚀 Starting Spring Boot application...')

# Run Maven with loaded environment
result = subprocess.run(['mvn', 'spring-boot:run'])
sys.exit(result.returncode)
PYTHON_SCRIPT
