# 备份系统修复部署指南

## 问题诊断 ✅

你的备份系统遇到的问题已确认：

**症状**：定时备份不执行
**根本原因**：Cron 无法访问 Docker 环境变量
**证据**：
```
[2026-01-11 02:00:01] ERROR: 缺少必需的环境变量 (DB_HOST, DB_USER, DB_PASSWORD, DB_NAME)
[2026-01-12 02:00:01] ERROR: 缺少必需的环境变量 (DB_HOST, DB_USER, DB_PASSWORD, DB_NAME)
```

但手动触发成功：
```
[2026-01-11 04:34:07] 备份完成: /backups/daily/finance_daily_20260111_043406.sql.gz
```

这说明：
- ✅ Cron **正在按时执行**（凌晨2点运行了）
- ✅ 脚本本身正常（手动触发成功）
- ❌ **Cron 环境缺少环境变量**（定时执行时失败）

## 修复方案

### 已修复的内容

1. **环境变量持久化**
   - 将 Docker 环境变量写入 `/etc/environment`
   - Cron 任务执行前自动加载环境变量

2. **Cron 进程稳定性**
   - 使用 `cron -f` 前台运行，防止进程退出
   - 预创建日志文件，防止 tail 失败

3. **监控和诊断**
   - 添加每小时心跳任务
   - 提供自动诊断工具
   - 完整的故障排查文档

## 部署步骤（在远端服务器执行）

### 方法 1：完整部署（推荐）

```bash
# 1. 进入项目目录
cd /path/to/finance

# 2. 拉取最新代码
git pull

# 3. 重建备份镜像
docker-compose build backup

# 4. 停止并删除旧容器
docker-compose down backup

# 5. 启动新容器
docker-compose up -d backup

# 6. 查看启动日志
docker logs -f finance-backup
```

### 方法 2：快速部署（如果已有代码）

```bash
cd /path/to/finance

# 停止旧容器
docker-compose stop backup

# 重建并启动
docker-compose up -d --build --force-recreate backup

# 查看日志
docker logs -f finance-backup
```

## 验证修复

### 1. 检查容器启动日志

```bash
docker logs finance-backup
```

应该看到：
```
✓ MySQL服务已就绪
✓ Crontab已安装
✓ Webhook服务已启动
Backup服务已启动
```

### 2. 检查环境变量文件

```bash
docker exec finance-backup cat /etc/environment
```

应该看到所有数据库配置：
```
DB_HOST=10.0.0.7
DB_PORT=37719
DB_NAME=finance
DB_USER=...
DB_PASSWORD=...
```

### 3. 检查 Cron 配置

```bash
docker exec finance-backup crontab -l
```

应该看到每行任务都有 `. /etc/environment &&` 前缀：
```
0 2 * * 1-6 . /etc/environment && /scripts/backup.sh >> /backups/cron.log 2>&1 ...
```

### 4. 手动触发测试备份

```bash
docker exec finance-backup /scripts/backup.sh
```

应该成功并看到：
```
[日期时间] 备份完成: /backups/daily/finance_daily_YYYYMMDD_HHMMSS.sql.gz
```

### 5. 检查备份文件

```bash
docker exec finance-backup ls -lh /backups/daily/
```

应该看到新创建的 `.sql.gz` 文件。

### 6. 等待定时任务执行

定时任务配置为每天凌晨 2:00（UTC）执行。

**检查时间**：
```bash
# 查看容器时区
docker exec finance-backup date

# 如果是 UTC，凌晨2:00 = 北京时间10:00
# 如果是 Asia/Shanghai，凌晨2:00 = 北京时间2:00
```

**第二天检查**：
```bash
# 查看 cron 日志
docker exec finance-backup tail -50 /backups/cron.log

# 应该看到成功的备份记录（没有 ERROR）
```

## 使用诊断工具

### 自动诊断（推荐）

```bash
cd /path/to/finance

# 运行诊断脚本
chmod +x backup/diagnose-backup.sh
./backup/diagnose-backup.sh
```

诊断工具会自动检查：
- ✅ 容器运行状态
- ✅ Cron 进程状态
- ✅ Crontab 配置
- ✅ 环境变量
- ✅ 日志文件
- ✅ 备份文件
- ✅ 数据库连接

### 手动诊断命令

```bash
# 检查容器状态
docker ps | grep finance-backup

# 检查 cron 进程
docker exec finance-backup pgrep cron

# 查看最新日志
docker exec finance-backup tail -100 /backups/backup.log
docker exec finance-backup tail -100 /backups/cron.log

# 查看备份文件
docker exec finance-backup find /backups -name "*.sql.gz" -type f -exec ls -lh {} \;
```

## 测试定时任务（可选）

如果不想等到明天凌晨2点，可以临时修改为每5分钟执行一次：

```bash
# 1. 进入容器
docker exec -it finance-backup bash

# 2. 创建测试 crontab
cat > /tmp/test-crontab <<'EOF'
SHELL=/bin/bash
PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin

# 测试：每5分钟执行一次
*/5 * * * * . /etc/environment && /scripts/backup.sh >> /backups/cron.log 2>&1
EOF

# 3. 安装测试配置
crontab /tmp/test-crontab

# 4. 验证
crontab -l

# 5. 等待5分钟后查看日志
tail -f /backups/cron.log

# 6. 测试完成后恢复原始配置
crontab /scripts/crontab
```

## 监控心跳

修复后的系统每小时会记录一次心跳：

```bash
# 等待1小时后检查
docker exec finance-backup grep "heartbeat" /backups/cron.log
```

应该看到：
```
[2026-01-XX XX:00:00] Cron heartbeat
[2026-01-XX XX:00:00] Cron heartbeat
...
```

## 常见问题

### Q1: 部署后仍然失败

**检查步骤**：
1. 确认已经 `git pull` 拉取最新代码
2. 确认使用 `--build` 参数重建镜像
3. 查看容器日志：`docker logs finance-backup`
4. 运行诊断工具：`./backup/diagnose-backup.sh`

### Q2: 环境变量仍然缺失

**解决方案**：
```bash
# 检查 .env 文件
cat .env | grep DB_

# 检查 docker-compose.yml 配置
cat docker-compose.yml | grep -A 10 "backup:"

# 重新创建容器
docker-compose down backup
docker-compose up -d backup
```

### Q3: 时区问题

如果定时任务时间不对，检查时区：

```bash
# 查看容器时区
docker exec finance-backup date

# 如需修改，编辑 docker-compose.yml
# backup:
#   environment:
#     TZ: Asia/Shanghai  # 设置为北京时间
```

### Q4: MySQL 权限警告

日志中的这个警告可以忽略：
```
mysqldump: Error: 'Access denied; you need (at least one of) the PROCESS privilege(s) for this operation'
```

这不影响备份功能，只是缺少查看表空间的权限。备份仍然成功。

## 获取帮助

如果问题仍未解决，请收集诊断信息：

```bash
# 运行完整诊断
./backup/diagnose-backup.sh > diagnosis.txt

# 查看容器日志
docker logs finance-backup > container.log 2>&1

# 查看备份日志
docker exec finance-backup cat /backups/cron.log > cron.log
docker exec finance-backup cat /backups/backup.log > backup.log
```

然后提供这些文件以获取进一步帮助。

## 验证成功的标志

修复成功后，你应该看到：

1. ✅ 容器正常运行
2. ✅ Cron 进程存在
3. ✅ 环境变量正确加载
4. ✅ 手动备份成功
5. ✅ 每小时有心跳日志
6. ✅ 凌晨2点定时备份成功（无 ERROR）
7. ✅ 备份文件每天增加

## 下一步

修复完成后：

1. **监控备份**：设置外部监控检查备份文件是否每天更新
2. **测试恢复**：定期测试备份恢复功能
3. **查看报告**：访问 http://your-server:5000 查看备份状态
4. **配置告警**：在备份失败时发送通知

详细信息请查看 `backup/TROUBLESHOOTING.md`。
