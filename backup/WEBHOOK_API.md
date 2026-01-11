# Backup Webhook API 文档

## 概述

备份容器提供HTTP Webhook API，用于远程管理数据库备份和恢复操作。

**服务地址**: `http://backup:5000` (Docker内网) 或 `http://localhost:5000` (本地测试)

## API端点

### 1. 健康检查

**GET** `/health`

检查webhook服务是否正常运行。

**响应示例**:
```json
{
  "status": "healthy",
  "timestamp": "2026-01-10T12:00:00"
}
```

---

### 2. 手动触发备份

**POST** `/backup/trigger`

立即执行数据库备份。

**请求Body**:
```json
{
  "type": "manual"
}
```

**响应示例** (成功):
```json
{
  "success": true,
  "message": "Backup completed successfully",
  "type": "manual",
  "timestamp": "2026-01-10T12:00:00"
}
```

**响应示例** (失败):
```json
{
  "success": false,
  "message": "Backup failed",
  "error": "mysqldump: [ERROR] Access denied for user..."
}
```

---

### 3. 列出备份文件

**GET** `/backup/list?type=<type>`

获取所有备份文件列表。

**查询参数**:
- `type` (可选): 过滤类型 - `all`, `daily`, `weekly`, `monthly`

**响应示例**:
```json
{
  "success": true,
  "backups": [
    {
      "filename": "finance_daily_20260110_120000.sql.gz",
      "filepath": "/backups/daily/finance_daily_20260110_120000.sql.gz",
      "type": "daily",
      "size": 1048576,
      "timestamp": "2026-01-10T12:00:00",
      "mtime": 1704888000.0
    }
  ],
  "total": 1
}
```

---

### 4. 恢复备份

**POST** `/backup/restore`

从备份文件恢复数据库。

⚠️ **危险操作**: 会删除并重建整个数据库！

**请求Body**:
```json
{
  "filename": "finance_daily_20260110_120000.sql.gz",
  "confirmDbName": "finance"
}
```

**参数说明**:
- `filename`: 备份文件名（从`/backup/list`获取）
- `confirmDbName`: 必须匹配环境变量`DB_NAME`，用于二次确认

**响应示例** (成功):
```json
{
  "success": true,
  "message": "Database restored successfully",
  "filename": "finance_daily_20260110_120000.sql.gz",
  "timestamp": "2026-01-10T12:05:00"
}
```

**响应示例** (确认失败):
```json
{
  "success": false,
  "error": "Database name confirmation failed"
}
```

---

### 5. 获取备份日志

**GET** `/backup/logs?type=<type>&lines=<lines>`

获取备份或恢复日志。

**查询参数**:
- `type` (可选): 日志类型 - `backup` (默认), `restore`
- `lines` (可选): 行数，默认100

**响应示例**:
```json
{
  "success": true,
  "logs": [
    "[2026-01-10 12:00:00] ========== 开始备份流程 ==========",
    "[2026-01-10 12:00:01] 开始 daily 备份: finance_daily_20260110_120000.sql",
    "[2026-01-10 12:00:15] mysqldump完成，开始压缩...",
    "[2026-01-10 12:00:20] 备份完成: /backups/daily/finance_daily_20260110_120000.sql.gz"
  ],
  "total": 4
}
```

---

### 6. 获取备份元数据

**GET** `/backup/metadata`

获取所有备份的元数据（JSON格式）。

**响应示例**:
```json
{
  "success": true,
  "metadata": [
    {
      "timestamp": "2026-01-10 12:00:20",
      "type": "daily",
      "file": "finance_daily_20260110_120000.sql.gz",
      "size": 1048576,
      "tables": 25
    }
  ]
}
```

---

### 7. 获取备份服务状态

**GET** `/backup/status`

获取备份服务整体状态信息。

**响应示例**:
```json
{
  "success": true,
  "status": {
    "healthy": true,
    "disk_usage": {
      "total": "100G",
      "used": "10G",
      "available": "90G",
      "use_percent": "10%"
    },
    "latest_backups": {
      "daily": {
        "filename": "finance_daily_20260110_120000.sql.gz",
        "size": 1048576,
        "timestamp": "2026-01-10T12:00:00"
      },
      "weekly": null,
      "monthly": null
    },
    "retention_policy": {
      "daily": "7 days",
      "weekly": "4 weeks",
      "monthly": "6 months"
    }
  }
}
```

---

## 错误处理

所有API在发生错误时返回HTTP 500状态码，并提供错误信息：

```json
{
  "success": false,
  "error": "详细错误信息"
}
```

常见错误：
- `Missing request body` - 缺少请求体
- `Missing filename` - 缺少文件名参数
- `Database name confirmation failed` - 数据库名称确认失败
- `Backup file not found` - 备份文件不存在
- `Command timeout` - 命令执行超时

---

## 安全注意事项

1. **webhook端口仅对内网开放** - 不要暴露到公网
2. **恢复操作需要二次确认** - 必须提供正确的数据库名称
3. **备份文件访问控制** - 确保备份卷权限正确设置
4. **日志审计** - 所有操作都会记录到日志文件

---

## 测试示例

### 使用curl测试

```bash
# 1. 健康检查
curl http://localhost:5000/health

# 2. 触发备份
curl -X POST http://localhost:5000/backup/trigger \
  -H "Content-Type: application/json" \
  -d '{"type": "manual"}'

# 3. 列出备份
curl http://localhost:5000/backup/list

# 4. 获取日志
curl "http://localhost:5000/backup/logs?type=backup&lines=50"

# 5. 获取状态
curl http://localhost:5000/backup/status

# 6. 恢复备份（危险！）
curl -X POST http://localhost:5000/backup/restore \
  -H "Content-Type: application/json" \
  -d '{
    "filename": "finance_daily_20260110_120000.sql.gz",
    "confirmDbName": "finance"
  }'
```

### 从Spring Boot调用

参见 `BackupService.java` 实现。

---

## 部署说明

1. **环境变量**:
   - 必需: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`
   - 可选: `BACKUP_RETENTION_DAYS`, `BACKUP_RETENTION_WEEKS`, `BACKUP_RETENTION_MONTHS`

2. **端口配置**:
   - 容器内部: 5000
   - Docker映射: `5000:5000` (可选，仅用于调试)

3. **网络**:
   - 必须在同一Docker network中与backend通信
   - 默认配置: `finance-network`

4. **健康检查**:
   - 探测路径: `/health`
   - 间隔: 30秒
   - 超时: 10秒

---

## 维护

### 查看日志

```bash
# Docker日志
docker logs finance-backup

# Webhook日志
docker exec finance-backup tail -f /backups/webhook.log

# 备份日志
docker exec finance-backup tail -f /backups/backup.log

# 恢复日志
docker exec finance-backup tail -f /backups/restore.log
```

### 重启服务

```bash
# 重启backup容器
docker-compose restart backup

# 重新构建并启动
docker-compose up -d --build backup
```
