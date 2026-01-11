# Database Backup & Restore System

The Finance app includes a comprehensive automated backup system for MySQL databases, providing scheduled backups, manual triggers, and safe restoration capabilities.

## Architecture

The backup system runs as an isolated Docker container with:
- **Python Flask** web service for API endpoints
- **Cron** scheduler for automated backups
- **MySQL Client** for database operations
- **Health checks** for monitoring

## Features

### Automated Backups
- **Daily backups** - Run at 2:00 AM UTC
- **Weekly backups** - Run every Saturday at 3:00 AM UTC
- **Monthly backups** - Run on the 1st of each month at 4:00 AM UTC

### Retention Policy
- **Daily**: Keep last 7 days
- **Weekly**: Keep last 4 weeks
- **Monthly**: Keep last 12 months

### Manual Operations
- **Manual Backup** - Trigger on-demand backups via admin panel or API
- **Safe Restore** - Database name confirmation required before restore
- **Backup Monitoring** - Disk usage tracking, backup history, and logs

### Security
- **Database Name Verification** - User must type exact database name before restore
- **Admin-Only Access** - Only admin users can trigger backups or restores
- **Isolated Container** - Backup service runs in separate Docker container

## Setup

### 1. Build Docker Image

```bash
cd backup
docker build -t finance-backup:local .
```

For multi-architecture builds (amd64 + arm64):
```bash
./build-multiarch.sh
# This builds and pushes to xuaustin/finance-backup:latest
```

### 2. Run Backup Container

**Local Development:**
```bash
docker run -d \
  --name finance-backup \
  -e DB_HOST=10.0.0.7 \
  -e DB_PORT=3306 \
  -e DB_USER=austinxu \
  -e DB_PASSWORD=your_password \
  -e DB_NAME=finance \
  -v /path/to/backups:/backups \
  -p 5000:5000 \
  finance-backup:local
```

**Production (Docker Hub):**
```bash
docker run -d \
  --name finance-backup \
  -e DB_HOST=your_mysql_host \
  -e DB_PORT=3306 \
  -e DB_USER=your_db_user \
  -e DB_PASSWORD=your_db_password \
  -e DB_NAME=finance \
  -v /mnt/backups:/backups \
  -p 5000:5000 \
  xuaustin/finance-backup:latest
```

### 3. Configure Backend

Add to `backend/.env`:
```bash
# Backup Webhook URL (for backend to communicate with backup service)
BACKUP_WEBHOOK_URL=http://localhost:5000
```

For remote deployment, use the actual hostname:
```bash
BACKUP_WEBHOOK_URL=http://backup-service:5000
```

### 4. Verify Installation

```bash
# Check health
curl http://localhost:5000/health

# Check status
curl http://localhost:5000/backup/status

# List backups
curl http://localhost:5000/backup/list
```

## Usage

### Via Admin Panel (Recommended)

1. Login as admin user
2. Navigate to **Settings → Backup Management**
3. View backup files with details (filename, type, size, timestamp)
4. **Trigger Manual Backup**: Click "手动备份" button
5. **Restore**: Click "恢复" button, type database name to confirm

### Via API

#### Health Check
```bash
curl http://localhost:5000/health
```

Response:
```json
{
  "status": "healthy",
  "timestamp": "2024-01-10T20:00:00Z"
}
```

#### Get Status
```bash
curl http://localhost:5000/backup/status
```

Response:
```json
{
  "success": true,
  "status": {
    "healthy": true,
    "disk_usage": {
      "total": "500G",
      "used": "120G",
      "available": "380G",
      "use_percent": "24%"
    },
    "latest_backups": {
      "daily": {
        "filename": "finance_daily_20240110.sql.gz",
        "size": 12457600,
        "timestamp": "2024-01-10T02:00:00Z"
      },
      "weekly": {
        "filename": "finance_weekly_20240106.sql.gz",
        "size": 12450000,
        "timestamp": "2024-01-06T03:00:00Z"
      },
      "monthly": {
        "filename": "finance_monthly_20240101.sql.gz",
        "size": 12400000,
        "timestamp": "2024-01-01T04:00:00Z"
      }
    },
    "retention_policy": {
      "daily": "7 days",
      "weekly": "4 weeks",
      "monthly": "12 months"
    }
  }
}
```

#### List Backups
```bash
# List all backups
curl http://localhost:5000/backup/list

# List by type
curl http://localhost:5000/backup/list?type=daily
curl http://localhost:5000/backup/list?type=weekly
curl http://localhost:5000/backup/list?type=monthly
```

Response:
```json
{
  "success": true,
  "total": 15,
  "backups": [
    {
      "filename": "finance_daily_20240110.sql.gz",
      "type": "daily",
      "size": 12457600,
      "timestamp": "2024-01-10T02:00:00Z"
    },
    {
      "filename": "finance_weekly_20240106.sql.gz",
      "type": "weekly",
      "size": 12450000,
      "timestamp": "2024-01-06T03:00:00Z"
    }
  ]
}
```

#### Trigger Manual Backup
```bash
curl -X POST http://localhost:5000/backup/trigger \
  -H "Content-Type: application/json" \
  -d '{"type":"manual"}'
```

Response:
```json
{
  "success": true,
  "message": "Backup completed successfully",
  "filename": "finance_manual_20240110_143022.sql.gz",
  "size": 12457600
}
```

#### Restore Backup
```bash
curl -X POST http://localhost:5000/backup/restore \
  -H "Content-Type: application/json" \
  -d '{
    "filename": "finance_daily_20240110.sql.gz",
    "confirmDbName": "finance"
  }'
```

Response:
```json
{
  "success": true,
  "message": "Database restored successfully",
  "filename": "finance_daily_20240110.sql.gz"
}
```

Error (wrong database name):
```json
{
  "success": false,
  "error": "Database name confirmation failed"
}
```

#### Get Logs
```bash
# Get backup logs (last 100 lines)
curl http://localhost:5000/backup/logs?type=backup&lines=100

# Get restore logs
curl http://localhost:5000/backup/logs?type=restore&lines=100
```

Response:
```json
{
  "success": true,
  "logs": [
    "2024-01-10 02:00:00 - Starting daily backup...",
    "2024-01-10 02:00:05 - Backup completed: finance_daily_20240110.sql.gz (12.45 MB)",
    "2024-01-09 02:00:00 - Starting daily backup...",
    "2024-01-09 02:00:05 - Backup completed: finance_daily_20240109.sql.gz (12.42 MB)"
  ],
  "total": 100
}
```

## Environment Variables

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `DB_HOST` | Yes | - | MySQL host address |
| `DB_PORT` | Yes | - | MySQL port (usually 3306) |
| `DB_NAME` | Yes | - | Database name to backup |
| `DB_USER` | Yes | - | MySQL username |
| `DB_PASSWORD` | Yes | - | MySQL password |
| `TZ` | No | `UTC` | Timezone for cron jobs |

## File Structure

### Backup Directory

```
/backups/
├── daily/
│   ├── finance_daily_20240110_020000.sql.gz
│   ├── finance_daily_20240109_020000.sql.gz
│   └── ... (last 7 days)
├── weekly/
│   ├── finance_weekly_20240106_030000.sql.gz
│   ├── finance_weekly_20231230_030000.sql.gz
│   └── ... (last 4 weeks)
├── monthly/
│   ├── finance_monthly_20240101_040000.sql.gz
│   ├── finance_monthly_20231201_040000.sql.gz
│   └── ... (last 12 months)
├── pre_restore_snapshot_YYYYMMDD_HHMMSS.sql.gz  # Auto-created before restore
└── backup.log
```

### Log Files

```
/backups/
├── backup.log          # Backup operation logs
└── restore.log         # Restore operation logs
```

## Backup Naming Convention

Format: `finance_{type}_{timestamp}.sql.gz`

- **Type**: `daily`, `weekly`, `monthly`, `manual`
- **Timestamp**: `YYYYMMDD_HHMMSS` in UTC

Examples:
- `finance_daily_20240110_020000.sql.gz` - Daily backup on Jan 10, 2024 at 2:00 AM
- `finance_weekly_20240106_030000.sql.gz` - Weekly backup on Jan 6, 2024 at 3:00 AM
- `finance_monthly_20240101_040000.sql.gz` - Monthly backup on Jan 1, 2024 at 4:00 AM
- `finance_manual_20240110_143022.sql.gz` - Manual backup triggered at 2:30:22 PM

## Cron Schedule

The backup container runs these cron jobs (all times in UTC):

```cron
# Daily backup at 2:00 AM UTC
0 2 * * * /app/backup.sh daily

# Weekly backup at 3:00 AM UTC on Saturday
0 3 * * 6 /app/backup.sh weekly

# Monthly backup at 4:00 AM UTC on the 1st
0 4 1 * * /app/backup.sh monthly
```

To customize schedules, modify `backup/crontab` before building the Docker image.

## Retention Management

Old backups are automatically cleaned up:

- **Daily**: Backups older than 7 days are deleted
- **Weekly**: Backups older than 4 weeks are deleted
- **Monthly**: Backups older than 12 months are deleted

Manual backups are **not** automatically deleted and must be removed manually if needed.

## Restore Process

When restoring a backup:

1. **Pre-Restore Snapshot**: Automatically creates a snapshot of current database
   - Saved as `pre_restore_snapshot_YYYYMMDD_HHMMSS.sql.gz`
   - Allows rollback if restore fails or produces unexpected results

2. **Database Name Verification**: User must type exact database name to confirm

3. **Restore Execution**:
   - Drops existing tables (if any)
   - Restores from backup file
   - Logs all operations to `restore.log`

4. **Post-Restore**: Application restarts automatically to reload database connections

## Troubleshooting

### Backup container not starting

**Check logs:**
```bash
docker logs finance-backup
```

**Common issues:**
- Missing environment variables (DB_HOST, DB_USER, etc.)
- Invalid MySQL credentials
- Network connectivity issues

### Backups not running on schedule

**Check cron logs:**
```bash
docker exec finance-backup cat /var/log/cron.log
```

**Verify timezone:**
```bash
docker exec finance-backup date
```

### Restore fails with "Database name confirmation failed"

**Cause**: Typed database name doesn't match `DB_NAME` environment variable

**Solution**:
1. Check `DB_NAME` in container: `docker exec finance-backup env | grep DB_NAME`
2. Type exact database name in confirmation dialog

### Out of disk space

**Check disk usage:**
```bash
curl http://localhost:5000/backup/status | jq '.status.disk_usage'
```

**Solution**:
1. Delete old manual backups
2. Adjust retention policy (rebuild image with modified cleanup script)
3. Increase backup volume size

### Backup files corrupted

**Verify backup integrity:**
```bash
# Test decompression
gunzip -t /backups/daily/finance_daily_20240110.sql.gz

# If successful, test SQL syntax (without executing)
zcat /backups/daily/finance_daily_20240110.sql.gz | mysql --help > /dev/null
```

**Prevention**:
- Ensure sufficient disk space during backup
- Monitor backup logs for errors

## Performance Considerations

### Backup Duration

For a typical finance database:
- **Small** (< 100 MB): ~5-10 seconds
- **Medium** (100-500 MB): ~15-30 seconds
- **Large** (> 500 MB): ~1-2 minutes

Compression reduces file size by ~70-80%.

### Network Impact

Backups use local MySQL connection, minimal network impact if:
- Backup container and MySQL are on same host/network
- MySQL configured for local socket connections

### Database Lock

`mysqldump` uses `--single-transaction` flag to avoid locking tables during backup. This works for InnoDB tables (which Finance app uses).

## Security Best Practices

1. **Restrict backup directory permissions**:
   ```bash
   chmod 700 /path/to/backups
   ```

2. **Use strong MySQL credentials**:
   - Dedicated backup user with minimum required privileges
   - Strong password (not in version control)

3. **Encrypt backups** (optional):
   ```bash
   # Modify backup.sh to add encryption
   mysqldump ... | gzip | openssl enc -aes-256-cbc -salt -out backup.sql.gz.enc
   ```

4. **Offsite backups**:
   - Copy backups to remote storage (S3, NAS, etc.)
   - Use volume mounts or backup sync tools

5. **Audit logs**:
   - Review backup/restore logs regularly
   - Monitor unauthorized access attempts

## Integration with Backend

The Finance app backend integrates with the backup service via REST API:

**Backend Service**: `BackupService.java`
- Proxies requests from frontend to backup webhook
- Adds database name extraction from datasource URL
- Handles timeout configuration (10 minutes for long operations)

**Backend Controller**: `BackupController.java`
- Exposes `/api/backup/*` endpoints
- Requires admin role for all operations
- Logs all backup/restore actions

**Frontend**: `BackupManagement.vue`
- Admin-only page for backup management
- Real-time status, backup list, and logs
- Confirmation dialog with database name verification

## Docker Health Check

The backup container includes a health check:

```dockerfile
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:5000/health || exit 1
```

Check health status:
```bash
docker inspect finance-backup --format='{{.State.Health.Status}}'
```

## Monitoring

### Prometheus Metrics (Future)

Planned metrics:
- `backup_last_success_timestamp` - Last successful backup timestamp
- `backup_duration_seconds` - Backup operation duration
- `backup_file_size_bytes` - Backup file size
- `restore_total` - Total restore operations
- `backup_errors_total` - Total backup errors

### Alert Rules (Future)

Recommended alerts:
- No successful backup in 25 hours (daily backup missed)
- Disk usage > 80%
- Backup duration > 5 minutes (potential performance issue)
- Restore operation executed (notify for awareness)

## Migration Guide

### From Manual Backups

If you have existing manual backups:

1. Copy to backup directory structure:
   ```bash
   mkdir -p /backups/daily /backups/weekly /backups/monthly
   cp old_backup.sql.gz /backups/daily/finance_daily_YYYYMMDD_HHMMSS.sql.gz
   ```

2. Adjust filename to match naming convention

### To New Database

To migrate to a new database instance:

1. Backup current database
2. Update `DB_HOST` and `DB_NAME` in container environment
3. Restart container
4. Restore from backup to new database

## FAQ

**Q: Can I backup multiple databases?**
A: Currently supports one database per container. Run multiple containers for multiple databases.

**Q: How do I change backup schedule?**
A: Modify `backup/crontab` and rebuild Docker image.

**Q: Can I backup to cloud storage directly?**
A: Not built-in. Use volume mounts and sync tools (rclone, aws s3 sync, etc.).

**Q: What happens if restore fails mid-way?**
A: Pre-restore snapshot is created automatically. Restore it manually if needed.

**Q: Can I restore to a different database name?**
A: Yes, but you must update `DB_NAME` environment variable and restart container first.

## License

Same as main Finance app - MIT License
