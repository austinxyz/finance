#!/bin/bash

# 备份服务诊断脚本
# 用于远端服务器上检查备份容器状态

echo "========================================="
echo "Finance Backup Service 诊断工具"
echo "========================================="
echo ""

CONTAINER_NAME="finance-backup"

# 1. 检查容器是否运行
echo "1. 检查容器状态..."
if docker ps --filter "name=$CONTAINER_NAME" --format "{{.Names}}" | grep -q "$CONTAINER_NAME"; then
    echo "✓ 容器正在运行"
    docker ps --filter "name=$CONTAINER_NAME" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
else
    echo "✗ 容器未运行"
    echo ""
    echo "尝试启动容器："
    echo "  docker-compose up -d backup"
    exit 1
fi

echo ""

# 2. 检查容器日志
echo "2. 最近的容器日志 (最后20行):"
echo "-----------------------------------"
docker logs --tail 20 "$CONTAINER_NAME"
echo "-----------------------------------"
echo ""

# 3. 检查 cron 进程
echo "3. 检查 cron 进程..."
if docker exec "$CONTAINER_NAME" pgrep cron > /dev/null; then
    echo "✓ cron 进程正在运行"
    docker exec "$CONTAINER_NAME" ps aux | grep cron | grep -v grep
else
    echo "✗ cron 进程未运行！"
    echo ""
    echo "解决方案："
    echo "  docker-compose restart backup"
fi

echo ""

# 4. 检查 crontab 配置
echo "4. 检查 crontab 配置..."
echo "-----------------------------------"
docker exec "$CONTAINER_NAME" crontab -l 2>/dev/null || echo "✗ 无法读取 crontab"
echo "-----------------------------------"
echo ""

# 5. 检查环境变量
echo "5. 检查环境变量（数据库连接）..."
docker exec "$CONTAINER_NAME" bash -c 'echo "DB_HOST=$DB_HOST, DB_NAME=$DB_NAME, DB_USER=$DB_USER"'
echo ""

# 6. 检查日志文件
echo "6. 检查日志文件..."
docker exec "$CONTAINER_NAME" ls -lh /backups/*.log 2>/dev/null || echo "暂无日志文件"
echo ""

# 7. 检查备份文件
echo "7. 检查备份文件..."
echo "每日备份:"
docker exec "$CONTAINER_NAME" find /backups/daily -name "*.sql.gz" -type f -exec ls -lh {} \; 2>/dev/null | tail -5 || echo "  暂无每日备份"
echo ""
echo "每周备份:"
docker exec "$CONTAINER_NAME" find /backups/weekly -name "*.sql.gz" -type f -exec ls -lh {} \; 2>/dev/null | tail -3 || echo "  暂无每周备份"
echo ""
echo "每月备份:"
docker exec "$CONTAINER_NAME" find /backups/monthly -name "*.sql.gz" -type f -exec ls -lh {} \; 2>/dev/null | tail -3 || echo "  暂无每月备份"
echo ""

# 8. 查看最近的 cron 日志
echo "8. 最近的 cron 执行日志 (最后20行):"
echo "-----------------------------------"
docker exec "$CONTAINER_NAME" tail -20 /backups/cron.log 2>/dev/null || echo "暂无 cron 日志"
echo "-----------------------------------"
echo ""

# 9. 查看最近的备份日志
echo "9. 最近的备份日志 (最后20行):"
echo "-----------------------------------"
docker exec "$CONTAINER_NAME" tail -20 /backups/backup.log 2>/dev/null || echo "暂无备份日志"
echo "-----------------------------------"
echo ""

# 10. 手动触发测试备份
echo "10. 手动触发测试备份 (可选)..."
echo "执行以下命令手动测试备份："
echo "  docker exec $CONTAINER_NAME /scripts/backup.sh"
echo ""
read -p "是否立即执行测试备份? (y/N): " answer
if [ "$answer" = "y" ] || [ "$answer" = "Y" ]; then
    echo "执行备份..."
    docker exec "$CONTAINER_NAME" /scripts/backup.sh
    echo ""
    echo "备份完成，检查日志："
    docker exec "$CONTAINER_NAME" tail -30 /backups/backup.log
fi

echo ""
echo "========================================="
echo "诊断完成"
echo "========================================="
echo ""
echo "常见问题解决："
echo "1. cron 未运行: docker-compose restart backup"
echo "2. 无备份文件: 检查数据库连接和权限"
echo "3. 环境变量缺失: 检查 .env 文件和 docker-compose.yml"
echo "4. 手动触发备份: docker exec finance-backup /scripts/backup.sh"
echo "5. 查看实时日志: docker logs -f finance-backup"
echo ""
