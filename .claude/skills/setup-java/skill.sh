#!/bin/bash

# Setup Java Environment Skill
# Executes the environment setup script

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

cd "$PROJECT_ROOT"
source "$SCRIPT_DIR/setup-env.sh"
