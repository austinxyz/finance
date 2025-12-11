1# MySQL Client Access

This skill provides MySQL database client access for the finance project.

## Database Connection Information

Database credentials are stored in `backend/.env` file. Load them using:

```bash
source ./setup-env.sh
```

This will set the following environment variables:
- `DB_HOST` - Database host
- `DB_PORT` - Database port
- `DB_NAME` - Database name
- `DB_USERNAME` - Database username
- `DB_PASSWORD` - Database password

## Connecting to MySQL

MySQL client is installed at `/opt/homebrew/opt/mysql-client/bin/mysql`

Use the environment variables to connect:

```bash
# First, load credentials
source ./setup-env.sh

# Then connect
$MYSQL_CLIENT -h $DB_HOST -P $DB_PORT -u $DB_USERNAME -p$DB_PASSWORD $DB_NAME
```

Or use the convenient alias created by setup script:
```bash
source ./setup-env.sh
mysql-finance
```

## Common SQL Operations

### View Tables
```sql
SHOW TABLES;
```

### Describe Table Structure
```sql
DESCRIBE asset_accounts;
DESCRIBE liability_accounts;
DESCRIBE asset_records;
DESCRIBE liability_records;
DESCRIBE net_asset_categories;
```

### Query Data
```sql
-- View all asset accounts
SELECT * FROM asset_accounts WHERE is_active = 1;

-- View all liability accounts
SELECT * FROM liability_accounts WHERE is_active = 1;

-- View recent asset records
SELECT ar.*, aa.account_name
FROM asset_records ar
JOIN asset_accounts aa ON ar.account_id = aa.id
ORDER BY ar.record_date DESC
LIMIT 10;

-- View net asset categories
SELECT * FROM net_asset_categories ORDER BY display_order;
```

### Execute SQL File
```bash
# Load credentials first
source ./setup-env.sh

# Execute SQL file
$MYSQL_CLIENT -h $DB_HOST -P $DB_PORT -u $DB_USERNAME -p$DB_PASSWORD $DB_NAME < script.sql
```

### Execute SQL Command Directly
```bash
# Load credentials first
source ./setup-env.sh

# Execute command
$MYSQL_CLIENT -h $DB_HOST -P $DB_PORT -u $DB_USERNAME -p$DB_PASSWORD $DB_NAME -e "SELECT * FROM asset_accounts LIMIT 5;"
```

## Important Notes
- The database is shared with the zjutennis project (same server, different schema)
- Schema name is `finance`
- JPA is configured with `ddl-auto=update`, so schema changes are auto-applied
- Character set: utf8mb4
- Collation: utf8mb4_unicode_ci
