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

## Error Handling

- If `backend/.env` doesn't exist, show error message
- If MySQL client not found, suggest: `brew install mysql-client`
- If connection fails, show connection details for debugging

## Examples

### Check database tables
```
/mysql-exec "SHOW TABLES;"
```

### Query asset accounts
```
/mysql-exec "SELECT account_name, balance FROM asset_accounts WHERE is_active = 1;"
```

### Execute schema migration
```
/mysql-exec schema/migration_v2.sql
```

### Interactive session
```
/mysql-exec
> SHOW TABLES;
> DESCRIBE asset_accounts;
> exit
```

## Notes

- Database credentials are stored in `backend/.env` (not tracked in git)
- The skill is project-specific and uses the Finance database
- Connection details (host, port, database name) are read from the .env file
