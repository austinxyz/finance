#!/bin/bash
# Helper script to export environment variables from .env file
# Handles special characters like & in URLs correctly

while IFS= read -r line || [[ -n "$line" ]]; do
    # Skip comments and empty lines
    [[ "$line" =~ ^[[:space:]]*# ]] && continue
    [[ -z "$line" ]] && continue

    # Check if line contains =
    if echo "$line" | grep -q '='; then
        # Extract key (everything before first =)
        key=$(echo "$line" | cut -d'=' -f1)
        # Extract value (everything after first =, preserving special chars)
        value=$(echo "$line" | sed "s/^$key=//")

        # Export the variable
        export "$key=$value"
    fi
done < "$1"
