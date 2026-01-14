#!/bin/bash

# Backup容器入口脚本（修复版）
# 确保cron能获取环境变量并正确执行

set -e

echo "========================================="
echo "Finance Database Backup Service"
echo "========================================="
echo "启动时间: $(date)"
echo "备份目录: /backups"
echo "数据库: $DB_HOST:$DB_PORT/$DB_NAME"
echo ""

# 检查必需的环境变量
if [ -z "$DB_HOST" ] || [ -z "$DB_USER" ] || [ -z "$DB_PASSWORD" ] || [ -z "$DB_NAME" ]; then
    echo "ERROR: 缺少必需的环境变量"
    echo "必需: DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD"
    exit 1
fi

# 等待MySQL可用
echo "等待MySQL服务启动..."
max_attempts=30
attempt=0

while ! mysqladmin ping -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" --silent 2>/dev/null; do
    attempt=$((attempt + 1))
    if [ $attempt -ge $max_attempts ]; then
        echo "ERROR: MySQL服务在${max_attempts}秒后仍不可用"
        exit 1
    fi
    echo "等待MySQL... ($attempt/$max_attempts)"
    sleep 1
done

echo "✓ MySQL服务已就绪"

# 设置脚本权限
chmod +x /scripts/*.sh

# 🔧 关键修复：将环境变量写入文件供cron使用
echo "导出环境变量到 /etc/environment 供 cron 使用..."
cat > /etc/environment <<EOF
DB_HOST=$DB_HOST
DB_PORT=$DB_PORT
DB_NAME=$DB_NAME
DB_USER=$DB_USER
DB_PASSWORD=$DB_PASSWORD
BACKUP_RETENTION_DAYS=${BACKUP_RETENTION_DAYS:-7}
BACKUP_RETENTION_WEEKS=${BACKUP_RETENTION_WEEKS:-4}
BACKUP_RETENTION_MONTHS=${BACKUP_RETENTION_MONTHS:-6}
TZ=${TZ:-UTC}
PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
EOF

# 创建日志文件（防止tail失败）
touch /backups/cron.log /backups/backup.log /backups/verify.log /backups/restore.log /backups/webhook.log

# 🔧 关键修复：更新 crontab 确保加载环境变量
echo "安装定时任务..."
cat > /tmp/crontab <<'CRON_EOF'
# Finance数据库备份定时任务
# 每天凌晨2:00执行备份

# 加载环境变量
SHELL=/bin/bash
PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin

# 每日备份 + 中等验证（工作日）
0 2 * * 1-6 . /etc/environment && /scripts/backup.sh >> /backups/cron.log 2>&1 && /scripts/verify.sh medium >> /backups/cron.log 2>&1

# 每日备份 + 完整验证（周日）
0 2 * * 0 . /etc/environment && /scripts/backup.sh >> /backups/cron.log 2>&1 && /scripts/verify.sh full >> /backups/cron.log 2>&1

# 每周日凌晨3:00执行完整验证（额外保障）
0 3 * * 0 . /etc/environment && /scripts/verify.sh full >> /backups/cron.log 2>&1

# 心跳任务（每小时检查一次 cron 是否运行）
0 * * * * echo "[$(date '+%Y-%m-%d %H:%M:%S')] Cron heartbeat" >> /backups/cron.log 2>&1

CRON_EOF

crontab /tmp/crontab
echo "✓ Crontab已安装"

# 显示crontab内容
echo ""
echo "定时任务配置:"
crontab -l

# 执行首次备份（可选，测试用）
if [ "${RUN_INITIAL_BACKUP:-false}" = "true" ]; then
    echo ""
    echo "执行首次备份..."
    /scripts/backup.sh
    echo "✓ 首次备份完成"
fi

# 🔧 关键修复：使用 -f 参数启动 cron（前台运行，不会退出）
echo ""
echo "启动cron服务（前台模式）..."

# 启动webhook服务（后台运行）
echo "启动webhook API服务 (端口5000)..."
python3 /webhook.py > /backups/webhook.log 2>&1 &
WEBHOOK_PID=$!

# 等待webhook服务启动
sleep 2
if kill -0 $WEBHOOK_PID 2>/dev/null; then
    echo "✓ Webhook服务已启动 (PID: $WEBHOOK_PID)"
else
    echo "WARNING: Webhook服务启动失败"
fi

# 输出日志文件路径
echo ""
echo "日志文件:"
echo "  - 备份日志: /backups/backup.log"
echo "  - 验证日志: /backups/verify.log"
echo "  - 恢复日志: /backups/restore.log"
echo "  - Cron日志: /backups/cron.log"
echo "  - Webhook日志: /backups/webhook.log"
echo ""
echo "API接口: http://localhost:5000"
echo ""
echo "========================================="
echo "Backup服务已启动"
echo "========================================="

# 🔧 关键修复：使用 cron -f 前台运行，确保容器不会退出
# 同时在后台实时输出日志
tail -f /backups/cron.log /backups/backup.log /backups/webhook.log &

# 启动 cron 服务（前台运行）
exec cron -f
