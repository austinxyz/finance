# Backup Container 本地测试指南

## 快速测试

### 1. 构建镜像

```bash
cd /Users/yanzxu/claude/finance
docker build -t finance-backup:test ./backup
```

### 2. 运行测试脚本

```bash
./test-backup.sh
```

测试脚本会：
- ✅ 加载数据库配置（从 `backend/.env`）
- ✅ 创建本地备份目录 `./backups/`
- ✅ 执行一次性数据库备份
- ✅ 显示备份文件信息和元数据
- ✅ 预览备份内容

### 3. 查看结果

备份文件位置：`./backups/daily/finance_daily_YYYYMMDD_HHMMSS.sql.gz`

```bash
# 查看备份文件列表
ls -lh ./backups/daily/

# 查看元数据
cat ./backups/metadata.json | jq

# 查看备份日志
cat ./backups/backup.log
```

## 手动测试

### 执行单次备份

```bash
source ./backend/.env

docker run --rm \
    -e DB_HOST="$DB_HOST" \
    -e DB_PORT="$DB_PORT" \
    -e DB_NAME="$DB_NAME" \
    -e DB_USER="$DB_USER" \
    -e DB_PASSWORD="$DB_PASSWORD" \
    -v "$(pwd)/backups:/backups" \
    --entrypoint "" \
    finance-backup:test \
    /scripts/backup.sh
```

### 测试恢复功能

```bash
# 查看可用的备份文件
docker run --rm \
    -v "$(pwd)/backups:/backups" \
    --entrypoint "" \
    finance-backup:test \
    ls -lh /backups/daily/

# 恢复备份（危险操作！会覆盖现有数据库）
docker run --rm \
    -e DB_HOST="$DB_HOST" \
    -e DB_PORT="$DB_PORT" \
    -e DB_NAME="$DB_NAME" \
    -e DB_USER="$DB_USER" \
    -e DB_PASSWORD="$DB_PASSWORD" \
    -v "$(pwd)/backups:/backups" \
    --entrypoint "" \
    finance-backup:test \
    /scripts/restore.sh /backups/daily/finance_daily_20260110_231422.sql.gz
```

### 验证备份完整性

```bash
# 快速验证（解压测试）
docker run --rm \
    -v "$(pwd)/backups:/backups" \
    --entrypoint "" \
    finance-backup:test \
    /scripts/verify.sh quick

# 中等验证（解压 + SQL语法检查）
docker run --rm \
    -e DB_HOST="$DB_HOST" \
    -e DB_PORT="$DB_PORT" \
    -v "$(pwd)/backups:/backups" \
    --entrypoint "" \
    finance-backup:test \
    /scripts/verify.sh medium

# 完整验证（解压 + 试恢复到临时数据库）
docker run --rm \
    -e DB_HOST="$DB_HOST" \
    -e DB_PORT="$DB_PORT" \
    -e DB_USER="$DB_USER" \
    -e DB_PASSWORD="$DB_PASSWORD" \
    -v "$(pwd)/backups:/backups" \
    --entrypoint "" \
    finance-backup:test \
    /scripts/verify.sh full
```

## 清理测试文件

```bash
# 删除所有测试备份
rm -rf ./backups/*

# 删除Docker镜像
docker rmi finance-backup:test
```

## 生产环境部署

在生产环境中，使用 `docker-compose.yml` 启动备份服务：

```bash
# 编辑 docker-compose.yml，配置备份卷挂载路径
# 默认: ./backups:/backups
# 生产环境建议: /mnt/nas/finance-backups:/backups

# 启动备份服务（会自动启动定时任务）
docker-compose up -d backup

# 查看备份日志
docker-compose logs -f backup

# 检查备份服务状态
docker-compose ps backup
```

## 注意事项

1. **权限问题**：备份用户需要以下MySQL权限
   - SELECT（读取数据）
   - LOCK TABLES（锁表保证一致性）
   - SHOW VIEW（导出视图）
   - TRIGGER（导出触发器）
   - EVENT（导出事件）
   - PROCESS（可选，用于导出tablespaces）

2. **存储空间**：确保备份目录有足够空间
   - 日备份保留7天
   - 周备份保留4周
   - 月备份保留6个月

3. **备份时间**：默认每天凌晨2:00执行备份
   - 修改时间：编辑 `backup/scripts/crontab`

4. **安全性**：
   - 不要将 `.env` 文件提交到版本控制
   - 备份文件包含敏感数据，注意访问控制
