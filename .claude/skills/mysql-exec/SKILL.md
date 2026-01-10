---
name: MySQL Executor
description: Execute MySQL commands with automatic credential loading from backend/.env. Supports SQL files, inline queries, and interactive shell. Use this skill when the user asks to query the database, execute SQL, or check database schema.
---

# MySQL Executor Skill

Execute MySQL commands for the Finance project database with automatic credential loading.

## Usage

The skill accepts three modes of operation:

1. **Execute SQL file**:
   - User provides a file path
   - Example: "run the migration file database/migrations/V1_add_expense_tables.sql"

2. **Execute inline query**:
   - User provides a SQL query string
   - Example: "show me all asset accounts"
   - Example: "query the database: SELECT * FROM asset_accounts LIMIT 10"

3. **Interactive shell**:
   - User asks to open MySQL shell
   - Example: "open mysql shell"
   - Example: "connect to the database"

## Implementation Steps

When this skill is invoked:

1. **Locate MySQL Client**:
   ```bash
   MYSQL_CLIENT=$(brew --prefix mysql-client)/bin/mysql
   ```

2. **Load Credentials**:
   ```bash
   source backend/.env
   # Loads: DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD
   ```

3. **Execute Command**:
   ```bash
   # For SQL file
   $MYSQL_CLIENT -h $DB_HOST -P $DB_PORT -u $DB_USER -p$DB_PASSWORD $DB_NAME < file.sql

   # For inline query
   $MYSQL_CLIENT -h $DB_HOST -P $DB_PORT -u $DB_USER -p$DB_PASSWORD $DB_NAME -e "QUERY"

   # For interactive shell
   $MYSQL_CLIENT -h $DB_HOST -P $DB_PORT -u $DB_USER -p$DB_PASSWORD $DB_NAME
   ```

## Common SQL Queries

### Schema Inspection
```sql
-- List all tables
SHOW TABLES;

-- Describe table structure
DESCRIBE asset_accounts;
DESCRIBE liability_accounts;
DESCRIBE expense_records;

-- Show table creation
SHOW CREATE TABLE asset_accounts;
```

### Data Queries
```sql
-- View active asset accounts
SELECT * FROM asset_accounts WHERE is_active = 1;

-- View recent asset records with account names
SELECT ar.*, aa.account_name
FROM asset_records ar
JOIN asset_accounts aa ON ar.account_id = aa.id
ORDER BY ar.record_date DESC
LIMIT 10;

-- Check net asset categories
SELECT * FROM net_asset_categories ORDER BY display_order;

-- View expense summary
SELECT * FROM expense_summary_view WHERE year = 2024;
```

## Error Handling

- **Missing .env file**: Show error and instruct to create backend/.env
- **MySQL client not found**: Suggest `brew install mysql-client`
- **Connection failed**: Display parsed connection details for debugging
- **SQL error**: Show full error message and query context

## Prerequisites

- MySQL client installed: `brew install mysql-client`
- Database credentials in `backend/.env`
- Must run from project root directory

## Database Information

- **Database Name**: finance
- **Tables**: asset_accounts, asset_records, liability_accounts, liability_records, expense_records, etc.
- **Shared Server**: Same MySQL server as zjutennis project but different schema
