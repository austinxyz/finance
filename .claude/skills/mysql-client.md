# MySQL Client Access

This skill provides MySQL database client access for the finance project.

## Database Connection Information

- **Host**: 10.0.0.7
- **Port**: 37719
- **Database**: finance
- **Username**: austinxu
- **Password**: helloworld

## Connecting to MySQL

MySQL client is installed at `/opt/homebrew/opt/mysql-client/bin/mysql`

Use the full path to connect:

```bash
/opt/homebrew/opt/mysql-client/bin/mysql -h 10.0.0.7 -P 37719 -u austinxu -phelloworld finance
```

Or add to PATH (recommended):
```bash
export PATH="/opt/homebrew/opt/mysql-client/bin:$PATH"
mysql -h 10.0.0.7 -P 37719 -u austinxu -phelloworld finance
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
/opt/homebrew/opt/mysql-client/bin/mysql -h 10.0.0.7 -P 37719 -u austinxu -phelloworld finance < script.sql
```

### Execute SQL Command Directly
```bash
/opt/homebrew/opt/mysql-client/bin/mysql -h 10.0.0.7 -P 37719 -u austinxu -phelloworld finance -e "SELECT * FROM asset_accounts LIMIT 5;"
```

## Important Notes
- The database is shared with the zjutennis project (same server, different schema)
- Schema name is `finance`
- JPA is configured with `ddl-auto=update`, so schema changes are auto-applied
- Character set: utf8mb4
- Collation: utf8mb4_unicode_ci
