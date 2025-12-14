# MySQL Executor Skill

Execute MySQL commands for the Finance project database with automatic credential loading.

## Usage

```bash
# Execute SQL file
/mysql-exec path/to/script.sql

# Execute inline SQL query
/mysql-exec "SELECT * FROM asset_accounts LIMIT 10;"

# Interactive MySQL shell
/mysql-exec
```

## What This Skill Does

1. Locates MySQL client from Homebrew installation
2. Reads database credentials from `backend/.env` file
3. Parses connection details (host, port, database name)
4. Executes the SQL command or opens interactive shell

## Implementation

When the user invokes this skill:

1. **Locate MySQL Client**:
   - Use `brew --prefix mysql-client` to find installation path
   - MySQL binary is at `$(brew --prefix mysql-client)/bin/mysql`

2. **Load Credentials from .env**:
   ```bash
   source backend/.env
   # This loads: DB_URL, DB_USERNAME, DB_PASSWORD
   ```

3. **Parse Connection Details**:
   - Extract host from DB_URL (e.g., `10.0.0.7`)
   - Extract port from DB_URL (e.g., `37719`)
   - Extract database from DB_URL (e.g., `finance`)

4. **Execute MySQL Command**:
   ```bash
   MYSQL_CLIENT=$(brew --prefix mysql-client)/bin/mysql

   # For SQL file
   $MYSQL_CLIENT -h $DB_HOST -P $DB_PORT -u $DB_USERNAME -p$DB_PASSWORD $DB_NAME < file.sql

   # For inline query
   $MYSQL_CLIENT -h $DB_HOST -P $DB_PORT -u $DB_USERNAME -p$DB_PASSWORD $DB_NAME -e "QUERY"

   # For interactive shell
   $MYSQL_CLIENT -h $DB_HOST -P $DB_PORT -u $DB_USERNAME -p$DB_PASSWORD $DB_NAME
   ```

## Common Use Cases

### Check Database Schema

```bash
# List all tables
/mysql-exec "SHOW TABLES;"

# Describe table structure
/mysql-exec "DESCRIBE asset_accounts;"
/mysql-exec "DESCRIBE liability_accounts;"
```

### Query Data

```bash
# View active asset accounts
/mysql-exec "SELECT * FROM asset_accounts WHERE is_active = 1;"

# View recent asset records
/mysql-exec "SELECT ar.*, aa.account_name FROM asset_records ar JOIN asset_accounts aa ON ar.account_id = aa.id ORDER BY ar.record_date DESC LIMIT 10;"

# Check net asset categories
/mysql-exec "SELECT * FROM net_asset_categories ORDER BY display_order;"
```

### Execute Schema Migrations

```bash
# Execute SQL migration file
/mysql-exec database/migrations/V1_add_expense_tables.sql

# Execute schema updates
/mysql-exec schema/update_asset_types.sql
```

### Interactive Session

```bash
/mysql-exec
> SHOW TABLES;
> DESCRIBE asset_accounts;
> SELECT COUNT(*) FROM asset_accounts WHERE is_active = 1;
> exit
```

## Error Handling

- If `backend/.env` doesn't exist, show error message with instructions
- If MySQL client not found, suggest: `brew install mysql-client`
- If connection fails, display connection details for debugging
- If SQL execution fails, show full error message and query context

## Prerequisites

- MySQL client must be installed via Homebrew: `brew install mysql-client`
- Database credentials must exist in `backend/.env`
- Project must be at root directory when executing

## Notes

- Database credentials are stored in `backend/.env` (not tracked in git)
- The skill is project-specific and uses the Finance database
- Connection details (host, port, database name) are parsed from DB_URL in .env
- Shares database server with zjutennis project but uses separate `finance` schema
