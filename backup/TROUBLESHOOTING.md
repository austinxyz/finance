# 备份系统故障排查指南

## 问题：定时备份没有执行

### 常见原因

1. **Cron 进程未运行** - 容器内的 cron 服务可能已退出
2. **环境变量丢失** - cron 无法访问 Docker 传递的环境变量
3. **日志文件权限问题**
4. **数据库连接失败**

### 诊断步骤

#### 1. 使用自动诊断工具（推荐）

```bash
# 在远端服务器上执行
chmod +x backup/diagnose-backup.sh
./backup/diagnose-backup.sh
```

诊断工具会自动检查：
- 容器运行状态
- cron 进程状态
- crontab 配置
- 环境变量
- 日志文件
- 备份文件
- 数据库连接

#### 2. 手动检查容器状态

```bash
# 查看容器是否运行
docker ps | grep finance-backup

# 查看容器日志
docker logs finance-backup

# 查看最近50行日志
docker logs --tail 50 finance-backup

# 实时跟踪日志
docker logs -f finance-backup
```

#### 3. 检查 cron 进程

```bash
# 进入容器
docker exec -it finance-backup bash

# 检查 cron 进程
ps aux | grep cron

# 查看 crontab 配置
crontab -l

# 查看 cron 日志
tail -f /backups/cron.log
```

#### 4. 检查环境变量

```bash
# 在容器内检查环境变量
docker exec finance-backup env | grep DB_

# 检查环境变量文件
docker exec finance-backup cat /etc/environment
```

#### 5. 检查备份文件

```bash
# 列出所有备份文件
docker exec finance-backup find /backups -name "*.sql.gz" -type f -exec ls -lh {} \;

# 检查最新的备份
docker exec finance-backup ls -lht /backups/daily/ | head -5
```

### 解决方案

#### 方案 1: 重启备份容器（最简单）

```bash
# 停止并重新启动备份容器
docker-compose restart backup

# 或完全重建
docker-compose up -d --force-recreate backup
```

#### 方案 2: 手动触发备份测试

```bash
# 手动执行一次备份
docker exec finance-backup /scripts/backup.sh

# 查看备份结果
docker exec finance-backup tail -50 /backups/backup.log
```

#### 方案 3: 检查并修复环境变量

1. 检查 `.env` 文件是否包含所有必需的变量：
   ```env
   DB_HOST=your-db-host
   DB_PORT=3306
   DB_NAME=your-db-name
   DB_USER=your-db-user
   DB_PASSWORD=your-db-password
   ```

2. 重新加载配置：
   ```bash
   docker-compose down
   docker-compose up -d
   ```

#### 方案 4: 应用修复后的 entrypoint.sh

本次更新修复了以下问题：

1. ✅ **环境变量持久化** - 将环境变量写入 `/etc/environment`
2. ✅ **Cron 前台运行** - 使用 `cron -f` 确保进程不退出
3. ✅ **日志文件预创建** - 防止 tail 命令失败
4. ✅ **心跳任务** - 每小时记录一次，确认 cron 正常运行

**部署修复**：

```bash
# 1. 拉取最新代码
git pull

# 2. 重建备份镜像
docker-compose build backup

# 3. 重启服务
docker-compose up -d backup

# 4. 查看日志确认
docker logs -f finance-backup
```

### 验证修复

#### 1. 检查 cron 是否运行

```bash
docker exec finance-backup pgrep cron
# 应该返回一个进程ID
```

#### 2. 检查心跳日志

等待1小时后检查：

```bash
docker exec finance-backup grep "heartbeat" /backups/cron.log
# 应该每小时有一条记录
```

#### 3. 手动触发备份

```bash
docker exec finance-backup /scripts/backup.sh
docker exec finance-backup ls -lh /backups/daily/
# 应该看到新创建的备份文件
```

#### 4. 等待定时任务执行

定时任务配置为每天凌晨 2:00 执行，第二天检查：

```bash
# 查看最新备份时间
docker exec finance-backup find /backups/daily -name "*.sql.gz" -type f -exec ls -lh {} \; | sort
```

### 调整定时任务时间（测试用）

如果想立即测试定时任务，可以临时修改 crontab：

```bash
# 1. 进入容器
docker exec -it finance-backup bash

# 2. 编辑 crontab（设置为每5分钟执行一次）
cat > /tmp/test-crontab <<'EOF'
SHELL=/bin/bash
PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin

# 测试：每5分钟执行一次备份
*/5 * * * * . /etc/environment && /scripts/backup.sh >> /backups/cron.log 2>&1
EOF

crontab /tmp/test-crontab

# 3. 验证
crontab -l

# 4. 等待5分钟后检查
tail -f /backups/cron.log
```

**记得测试完成后恢复原始配置！**

### 监控和告警

#### Webhook API 健康检查

```bash
# 检查 webhook 服务状态
curl http://localhost:5000/health

# 查看备份列表
curl http://localhost:5000/backups

# 查看最新备份
curl http://localhost:5000/backups/latest
```

#### 设置外部监控

可以使用 cron 或监控工具定期检查：

```bash
# 每天检查是否有新备份
0 3 * * * docker exec finance-backup find /backups/daily -name "*.sql.gz" -type f -mtime -1 | grep -q . || echo "WARNING: No backup in last 24 hours" | mail -s "Backup Alert" admin@example.com
```

### 常见错误信息

#### "mysqldump: Access denied"

**原因**：数据库密码错误或用户权限不足

**解决**：
```bash
# 检查环境变量
docker exec finance-backup env | grep DB_

# 手动测试连接
docker exec finance-backup mysqladmin ping -h $DB_HOST -u $DB_USER -p$DB_PASSWORD
```

#### "No such file or directory: /backups/cron.log"

**原因**：日志文件未创建

**解决**：使用修复后的 entrypoint.sh（已包含日志文件预创建）

#### "cron: can't lock /var/run/crond.pid"

**原因**：cron 进程已经在运行

**解决**：
```bash
docker-compose restart backup
```

### 获取帮助

如果问题仍未解决：

1. 收集完整诊断信息：
   ```bash
   ./backup/diagnose-backup.sh > diagnosis.txt
   ```

2. 查看完整日志：
   ```bash
   docker logs finance-backup > container.log
   docker exec finance-backup cat /backups/cron.log > cron.log
   docker exec finance-backup cat /backups/backup.log > backup.log
   ```

3. 检查容器配置：
   ```bash
   docker inspect finance-backup > inspect.json
   ```
