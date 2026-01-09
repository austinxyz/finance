#!/bin/bash
# MySQL Executor Skill - Execute SQL files or queries with automatic credential loading

set -e

# Change to project root directory
cd "$(dirname "$0")/../../.."

# Locate MySQL client
MYSQL_CLIENT=$(brew --prefix mysql-client)/bin/mysql
if [ ! -f "$MYSQL_CLIENT" ]; then
    echo "Error: MySQL client not found. Please install it with: brew install mysql-client"
    exit 1
fi

# Load credentials from backend/.env using grep (handles special characters)
if [ ! -f "backend/.env" ]; then
    echo "Error: backend/.env file not found"
    exit 1
fi

DB_HOST=$(grep '^DB_HOST=' backend/.env | cut -d'=' -f2-)
DB_PORT=$(grep '^DB_PORT=' backend/.env | cut -d'=' -f2-)
DB_NAME=$(grep '^DB_NAME=' backend/.env | cut -d'=' -f2-)
DB_USER=$(grep '^DB_USER=' backend/.env | cut -d'=' -f2-)
DB_PASSWORD=$(grep '^DB_PASSWORD=' backend/.env | cut -d'=' -f2-)

# Check if we have all required parameters
if [ -z "$DB_HOST" ] || [ -z "$DB_PORT" ] || [ -z "$DB_NAME" ] || [ -z "$DB_USER" ] || [ -z "$DB_PASSWORD" ]; then
    echo "Error: Missing database connection details from .env file"
    echo "DB_HOST: $DB_HOST"
    echo "DB_PORT: $DB_PORT"
    echo "DB_NAME: $DB_NAME"
    exit 1
fi

echo "Connecting to: $DB_HOST:$DB_PORT/$DB_NAME as $DB_USER"

# Execute based on arguments
if [ $# -eq 0 ]; then
    # Interactive shell
    echo "Opening MySQL interactive shell..."
    $MYSQL_CLIENT -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" "$DB_NAME"
elif [ -f "$1" ]; then
    # Execute SQL file
    echo "Executing SQL file: $1"
    $MYSQL_CLIENT -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" < "$1"
    echo "SQL file executed successfully"
else
    # Execute inline query
    echo "Executing query: $1"
    $MYSQL_CLIENT -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" -e "$1"
fi
