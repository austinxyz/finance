# Finance Database Backup Service

Docker化的MySQL数据库备份服务，支持自动备份、验证和恢复。

## 功能特性

- ✅ **自动备份**: 每天凌晨2:00自动执行备份
- ✅ **多级备份**: 支持日备份（7天）、周备份（4周）、月备份（6个月）
- ✅ **备份验证**:
  - 平时：中等验证（检查SQL语法）
  - 周日：完整验证（导入临时数据库）
- ✅ **轻松恢复**: 提供list、latest、指定文件三种恢复方式
- ✅ **健康检查**: Docker健康检查确保服务正常运行
- ✅ **NAS支持**: 可挂载NAS作为备份存储

## 快速开始

### 1. 配置NAS挂载（推荐）

编辑 `docker-compose.yml`，将备份目录挂载到你的NAS：

```yaml
backup:
  volumes:
    # 方式1：直接挂载NAS（NAS已在宿主机挂载）
    - /mnt/nas/finance-backups:/backups

    # 方式2：使用NFS volume（推荐）
    - nas-backups:/backups

volumes:
  nas-backups:
    driver: local
    driver_opts:
      type: nfs
      o: addr=192.168.1.100,rw,sync
      device: ":/volume1/backups/finance"
```

### 2. 配置环境变量（可选）

在项目根目录的 `.env` 文件中添加（如果需要自定义）：

```env
# 备份保留策略（可选，有默认值）
BACKUP_RETENTION_DAYS=7
BACKUP_RETENTION_WEEKS=4
BACKUP_RETENTION_MONTHS=6

# 时区（可选）
TZ=Asia/Shanghai

# 首次启动时执行备份（仅用于测试）
RUN_INITIAL_BACKUP=false
```

### 3. 启动备份服务

```bash
# 构建并启动备份容器
docker-compose up -d backup

# 查看备份服务日志
docker-compose logs -f backup

# 检查备份服务状态
docker-compose ps backup
```

## 使用指南

### 手动执行备份

```bash
# 进入备份容器
docker exec -it finance-backup /bin/bash

# 执行备份
/scripts/backup.sh

# 验证最新备份（中等验证）
/scripts/verify.sh medium

# 验证最新备份（完整验证）
/scripts/verify.sh full
```

### 列出所有备份

```bash
docker exec -it finance-backup /scripts/restore.sh list
```

输出示例：
```
每日备份 (最近7天):
  2026-01-10 02:00    15.2MB  finance_daily_20260110_020000.sql.gz
  2026-01-09 02:00    14.8MB  finance_daily_20260109_020000.sql.gz
  ...

每周备份 (最近4周):
  2026-01-05 02:00    15.0MB  finance_weekly_20260105_020000.sql.gz
  ...

每月备份 (最近6个月):
  2026-01-01 02:00    14.5MB  finance_monthly_20260101_020000.sql.gz
  ...
```

### 恢复数据库

⚠️ **警告**: 恢复操作会覆盖现有数据库！请确保备份可靠。

```bash
# 方式1：恢复最新的每日备份
docker exec -it finance-backup /scripts/restore.sh latest daily

# 方式2：恢复最新的每周备份
docker exec -it finance-backup /scripts/restore.sh latest weekly

# 方式3：恢复指定的备份文件
docker exec -it finance-backup /scripts/restore.sh finance_daily_20260110_020000.sql.gz

# 方式4：进入容器交互式恢复
docker exec -it finance-backup /bin/bash
/scripts/restore.sh list
/scripts/restore.sh <选择的文件名>
```

**恢复过程**：
1. 创建当前数据库的快照（安全措施）
2. 5秒倒计时警告（可Ctrl+C取消）
3. 删除现有数据库并重新创建
4. 导入备份数据
5. 验证恢复结果
6. 如果失败，自动从快照恢复

## 备份策略

### 备份时间表

| 类型 | 执行时间 | 保留时间 | 验证方式 |
|------|---------|---------|---------|
| 每日备份 | 每天 2:00 AM | 7天 | 中等验证（周一至周六）<br>完整验证（周日） |
| 每周备份 | 每周日 2:00 AM | 4周 | 完整验证 |
| 每月备份 | 每月1日 2:00 AM | 6个月 | 中等验证 |

### 验证级别

1. **简单验证** (simple)
   - 检查文件存在性
   - 检查文件大小
   - 检查gzip完整性

2. **中等验证** (medium) - 平时使用
   - 包含简单验证
   - 解压检查SQL语法
   - 统计表数量和INSERT语句

3. **完整验证** (full) - 周日执行
   - 包含中等验证
   - 导入到临时数据库
   - 验证表数量和数据完整性
   - 自动清理临时数据库

## 目录结构

```
/backups/
  ├── daily/              # 每日备份（7天）
  │   └── finance_daily_20260110_020000.sql.gz
  ├── weekly/             # 每周备份（4周）
  │   └── finance_weekly_20260105_020000.sql.gz
  ├── monthly/            # 每月备份（6个月）
  │   └── finance_monthly_20260101_020000.sql.gz
  ├── backup.log          # 备份日志
  ├── verify.log          # 验证日志
  ├── restore.log         # 恢复日志
  ├── cron.log            # Cron执行日志
  └── metadata.json       # 备份元数据（文件大小、表数量等）
```

## 监控和维护

### 查看日志

```bash
# 实时查看所有日志
docker exec -it finance-backup tail -f /backups/*.log

# 查看备份日志
docker exec -it finance-backup cat /backups/backup.log

# 查看验证日志
docker exec -it finance-backup cat /backups/verify.log

# 查看cron日志
docker exec -it finance-backup cat /backups/cron.log
```

### 健康检查

Docker会每小时检查一次备份服务的健康状态：

```bash
# 查看健康状态
docker inspect finance-backup | grep -A5 Health

# 如果不健康，检查是否有最近的备份
docker exec -it finance-backup find /backups/daily -name "*.sql.gz" -type f -mtime -1
```

### 磁盘空间管理

```bash
# 查看备份目录大小
docker exec -it finance-backup du -sh /backups/*

# 查看各类备份的文件数量和总大小
docker exec -it finance-backup sh -c 'for dir in /backups/{daily,weekly,monthly}; do echo "=== $dir ==="; ls -lh $dir/*.sql.gz 2>/dev/null | wc -l; du -sh $dir; done'
```

## 故障排除

### 备份失败

1. 检查数据库连接：
   ```bash
   docker exec -it finance-backup mysqladmin ping -h $DB_HOST -u $DB_USER -p$DB_PASSWORD
   ```

2. 检查磁盘空间：
   ```bash
   docker exec -it finance-backup df -h /backups
   ```

3. 查看详细错误：
   ```bash
   docker exec -it finance-backup cat /backups/backup.log
   ```

### 验证失败

检查验证日志：
```bash
docker exec -it finance-backup cat /backups/verify.log
```

### Cron任务未执行

1. 检查cron服务状态：
   ```bash
   docker exec -it finance-backup ps aux | grep cron
   ```

2. 检查crontab配置：
   ```bash
   docker exec -it finance-backup crontab -l
   ```

3. 手动触发备份测试：
   ```bash
   docker exec -it finance-backup /scripts/backup.sh
   ```

## 安全建议

1. **加密NAS传输**: 如果使用NFS，建议配置VPN或使用加密的NFS版本
2. **备份加密**: 考虑对敏感备份文件进行GPG加密
3. **访问控制**: 限制NAS备份目录的访问权限
4. **异地备份**: 定期将关键备份复制到异地存储

## 升级和更新

```bash
# 重新构建备份镜像
docker-compose build backup

# 重启备份服务（应用新配置）
docker-compose restart backup

# 查看新镜像版本
docker images | grep finance-backup
```

## 技术细节

- **基础镜像**: Alpine Linux 3.19
- **关键依赖**: mysql-client, bash, cron, gzip, jq
- **备份方法**: mysqldump with --single-transaction
- **压缩方式**: gzip (默认压缩级别)
- **健康检查**: 检查24小时内是否有备份文件

## 许可证

与主项目相同
