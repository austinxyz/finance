#!/bin/bash

# Backup容器入口脚本
# 启动cron服务并保持容器运行

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

# 安装crontab
echo "安装定时任务..."
crontab /scripts/crontab
echo "✓ Crontab已安装"

# 显示crontab内容
echo ""
echo "定时任务配置:"
crontab -l

# 执行首次备份（可选，测试用）
if [ "${RUN_INITIAL_BACKUP:-true}" = "true" ]; then
    echo ""
    echo "执行首次备份..."
    /scripts/backup.sh
    echo "✓ 首次备份完成"
fi

# 启动cron服务
echo ""
echo "启动cron服务..."
cron

# 输出日志文件路径
echo ""
echo "日志文件:"
echo "  - 备份日志: /backups/backup.log"
echo "  - 验证日志: /backups/verify.log"
echo "  - 恢复日志: /backups/restore.log"
echo "  - Cron日志: /backups/cron.log"
echo ""
echo "========================================="
echo "Backup服务已启动"
echo "========================================="

# 实时输出日志（保持容器运行）
tail -f /backups/cron.log /backups/backup.log /backups/verify.log 2>/dev/null || sleep infinity
