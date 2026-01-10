#!/bin/bash

# Finance Project Environment Setup Script
# This script sets up JAVA_HOME and loads database credentials from backend/.env

echo "🔧 Setting up Finance project environment..."

# 1. Set JAVA_HOME to Java 17 (required for Spring Boot 3.2.0)
if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS
    export JAVA_HOME=$(/usr/libexec/java_home -v 17 2>/dev/null)
    if [ -z "$JAVA_HOME" ]; then
        echo "❌ Error: Java 17 not found. Please install Java 17:"
        echo "   brew install openjdk@17"
        return 1
    fi
else
    # Linux
    export JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))
fi

echo "✅ JAVA_HOME set to: $JAVA_HOME"
java -version 2>&1 | head -1

# 2. Load database credentials from backend/.env
ENV_FILE="./backend/.env"
if [ ! -f "$ENV_FILE" ]; then
    echo "⚠️  Warning: $ENV_FILE not found!"
    echo "   Creating template .env file..."

    cat > "$ENV_FILE" << 'EOF'
# Database Configuration
DB_HOST=localhost
DB_PORT=3306
DB_NAME=finance
DB_USER=your_username
DB_PASSWORD=your_password

# JWT Configuration
JWT_SECRET=your-secret-key-at-least-256-bits-long-please-change-this
JWT_EXPIRATION=86400000

# Claude API Configuration (optional)
CLAUDE_API_KEY=
EOF

    echo "   Please edit backend/.env with your actual credentials"
    return 1
fi

# Load .env file (export variables without executing them in shell)
while IFS='=' read -r key value; do
    # Skip empty lines and comments
    [[ -z "$key" || "$key" =~ ^#.* ]] && continue
    # Remove leading/trailing whitespace from value
    value=$(echo "$value" | sed -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//')
    # Export the variable
    export "$key=$value"
done < "$ENV_FILE"

echo "✅ Loaded database credentials from $ENV_FILE"
echo "   DB: $DB_USER@$DB_HOST:$DB_PORT/$DB_NAME"

# 3. Set MySQL client path (Homebrew)
if command -v brew &> /dev/null; then
    MYSQL_PREFIX=$(brew --prefix mysql-client 2>/dev/null)
    if [ -n "$MYSQL_PREFIX" ]; then
        export MYSQL_CLIENT="$MYSQL_PREFIX/bin/mysql"
        export PATH="$MYSQL_PREFIX/bin:$PATH"
        echo "✅ MySQL client found at: $MYSQL_CLIENT"
    else
        echo "⚠️  MySQL client not found. Install with: brew install mysql-client"
    fi
fi

# 4. Create mysql command alias
if [ -n "$DB_HOST" ] && [ -n "$DB_PORT" ] && [ -n "$DB_NAME" ] && [ -n "$DB_USER" ]; then
    alias mysql-finance="$MYSQL_CLIENT -h $DB_HOST -P $DB_PORT -u $DB_USER -p$DB_PASSWORD $DB_NAME"

    echo "✅ MySQL connection configured"
    echo "   Quick access: mysql-finance"
fi

echo ""
echo "🎉 Environment setup complete!"
echo ""
echo "📝 Next steps:"
echo "   Backend:  ./backend/start.sh (recommended - loads .env automatically)"
echo "   Frontend: cd frontend && npm run dev"
echo ""
echo "⚠️  Note: This script is deprecated. Use ./backend/start.sh for backend development."
echo ""
