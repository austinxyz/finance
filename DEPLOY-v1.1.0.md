# Finance App v1.1.0 部署指南

## 版本信息

**版本号**: v1.1.0
**发布日期**: 2026-01-12
**Docker 镜像**: xuaustin/finance-*:v1.1.0, xuaustin/finance-*:latest

## 更新内容

### 🎉 Family Store 迁移完成 (100%)

**前端改进**:
- ✅ 完成所有 24 个页面的 family store 迁移
- ✅ 移除所有页面的独立家庭选择器
- ✅ 集中式家庭管理（系统设置 - 管理员面板）
- ✅ 自动同步（管理员切换家庭时，所有页面自动更新）

**后端安全增强**:
- ✅ 新增 `AuthHelper.getAuthorizedFamilyId()` 方法
- ✅ 更新 11 个 Controller 支持管理员多家庭访问
- ✅ 确保普通用户只能访问自己的家庭数据

**迁移覆盖**:
- 分析模块: 14 个页面 ✅
- 管理模块: 11 个页面 ✅
- 设置模块: 3 个页面（无需迁移，特殊场景）

**用户体验**:
- 界面更简洁（移除 24 个家庭选择器）
- 统一的家庭管理体验
- 更好的性能（单次家庭 API 调用/会话）
- 增强的安全性（后端验证家庭访问权限）

### 🔧 Backup Service 修复

**问题修复**:
- ✅ 修复 cron 定时任务无法访问环境变量的问题
- ✅ 环境变量持久化到 `/etc/environment`
- ✅ Cron 前台运行模式 (`cron -f`)
- ✅ 添加每小时心跳任务用于监控

**新增工具**:
- 自动诊断脚本 (`backup/diagnose-backup.sh`)
- 完整故障排查文档 (`backup/TROUBLESHOOTING.md`)
- 部署指南 (`backup/DEPLOY.md`)

## Docker 镜像

所有镜像已推送到 Docker Hub，支持 **amd64** 和 **arm64** 平台：

### Backend
- `xuaustin/finance-backend:v1.1.0`
- `xuaustin/finance-backend:latest`

### Frontend
- `xuaustin/finance-frontend:v1.1.0`
- `xuaustin/finance-frontend:latest`

### Backup
- `xuaustin/finance-backup:v1.1.0`
- `xuaustin/finance-backup:latest`

### 验证镜像

```bash
# 查看支持的平台
docker buildx imagetools inspect xuaustin/finance-backend:latest
docker buildx imagetools inspect xuaustin/finance-frontend:latest
docker buildx imagetools inspect xuaustin/finance-backup:latest
```

## 部署步骤

### 方法 1: 完整部署（推荐）

适用于新部署或重大更新。

```bash
# 1. 停止现有服务
docker-compose down

# 2. 拉取最新镜像
docker-compose pull

# 3. 启动所有服务
docker-compose up -d

# 4. 查看日志
docker-compose logs -f
```

### 方法 2: 滚动更新（零停机）

适用于生产环境，逐个服务更新。

```bash
# 1. 更新 backend
docker-compose pull backend
docker-compose up -d --no-deps backend

# 2. 更新 frontend
docker-compose pull frontend
docker-compose up -d --no-deps frontend

# 3. 更新 backup（重要：修复了定时任务问题）
docker-compose pull backup
docker-compose up -d --no-deps --force-recreate backup

# 4. 验证服务
docker-compose ps
```

### 方法 3: 仅更新 Backup 服务

如果只需要修复备份定时任务问题：

```bash
# 1. 拉取最新 backup 镜像
docker-compose pull backup

# 2. 重建并启动
docker-compose up -d --force-recreate backup

# 3. 查看日志确认
docker logs -f finance-backup
```

## 验证部署

### 1. 检查容器状态

```bash
# 查看所有容器
docker-compose ps

# 应该看到 3 个容器都在运行：
# - finance-backend
# - finance-frontend
# - finance-backup
```

### 2. 检查应用健康

```bash
# Backend 健康检查
curl http://localhost:8080/api/actuator/health

# Frontend 访问
curl http://localhost:3000/

# Backup webhook API
curl http://localhost:5000/health
```

### 3. 验证 Family Store 迁移

1. **登录应用**: http://localhost:3000
2. **管理员用户**:
   - 查看侧边栏是否有 "系统设置" 菜单项
   - 进入 设置 → 系统设置
   - 尝试切换家庭
   - 访问任意分析或管理页面，验证数据自动更新

3. **普通用户**:
   - 侧边栏不应该有 "系统设置"
   - 所有页面应该显示用户自己的家庭数据
   - 页面上不应该有家庭选择器

### 4. 验证备份定时任务修复

```bash
# 1. 检查 cron 进程
docker exec finance-backup pgrep cron

# 2. 检查环境变量
docker exec finance-backup cat /etc/environment
# 应该看到 DB_HOST, DB_USER, DB_PASSWORD, DB_NAME

# 3. 手动触发备份测试
docker exec finance-backup /scripts/backup.sh

# 4. 查看备份文件
docker exec finance-backup ls -lh /backups/daily/

# 5. 运行诊断工具（如果有代码）
./backup/diagnose-backup.sh
```

### 5. 等待定时任务执行

定时任务配置为每天凌晨 2:00（UTC）执行。

**第二天检查**:
```bash
# 查看 cron 日志
docker exec finance-backup tail -50 /backups/cron.log

# 应该看到成功的备份记录，而不是：
# "ERROR: 缺少必需的环境变量"
```

## 配置说明

### 环境变量 (.env)

确保 `.env` 文件包含所有必需的变量：

```env
# Database
DB_HOST=your-db-host
DB_PORT=3306
DB_NAME=your-db-name
DB_USER=your-db-user
DB_PASSWORD=your-db-password

# JWT
JWT_SECRET=your-jwt-secret
JWT_EXPIRATION=86400000

# Backup
BACKUP_RETENTION_DAYS=7
BACKUP_RETENTION_WEEKS=4
BACKUP_RETENTION_MONTHS=6
RUN_INITIAL_BACKUP=false

# Timezone
TZ=UTC
```

### 端口映射

默认端口映射：
- Frontend: `3000:80`
- Backend: `8080:8080`
- Backup Webhook: `5000:5000`

如需修改，编辑 `docker-compose.yml` 的 `ports` 配置。

## 回滚方案

如果遇到问题需要回滚：

### 回滚到指定版本

```bash
# 1. 修改 docker-compose.yml，指定旧版本
# backend:
#   image: xuaustin/finance-backend:v1.0.0
# frontend:
#   image: xuaustin/finance-frontend:v1.0.0
# backup:
#   image: xuaustin/finance-backup:v1.0.0

# 2. 重新部署
docker-compose down
docker-compose up -d

# 3. 验证
docker-compose ps
```

### 快速回滚（使用 latest 之前的版本）

```bash
# 拉取特定版本
docker pull xuaustin/finance-backend:v1.0.0
docker pull xuaustin/finance-frontend:v1.0.0
docker pull xuaustin/finance-backup:v1.0.0

# 使用标签重启
docker-compose up -d
```

## 故障排查

### Backup 定时任务不执行

参考完整的故障排查文档：
```bash
# 查看故障排查文档
cat backup/TROUBLESHOOTING.md

# 运行自动诊断
./backup/diagnose-backup.sh

# 查看部署指南
cat backup/DEPLOY.md
```

### Frontend 无法连接 Backend

```bash
# 1. 检查 backend 是否运行
docker logs finance-backend

# 2. 检查网络连接
docker exec finance-frontend ping finance-backend

# 3. 检查 nginx 配置
docker exec finance-frontend cat /etc/nginx/conf.d/default.conf
```

### 数据库连接失败

```bash
# 检查环境变量
docker exec finance-backend env | grep DB_

# 测试数据库连接
docker exec finance-backend nc -zv $DB_HOST $DB_PORT
```

## 监控和日志

### 查看日志

```bash
# 所有服务
docker-compose logs -f

# 单个服务
docker logs -f finance-backend
docker logs -f finance-frontend
docker logs -f finance-backup

# 最近 100 行
docker logs --tail 100 finance-backend
```

### 备份监控

```bash
# 查看备份状态 API
curl http://localhost:5000/backup/status

# 查看备份列表
curl http://localhost:5000/backup/list

# 查看备份日志
curl http://localhost:5000/backup/logs?type=backup&lines=100
```

## 性能优化

### 资源限制（可选）

编辑 `docker-compose.yml` 添加资源限制：

```yaml
services:
  backend:
    # ...
    deploy:
      resources:
        limits:
          cpus: '2'
          memory: 2G
        reservations:
          cpus: '1'
          memory: 1G
```

### 日志轮转

```bash
# 配置 Docker 日志轮转
# 编辑 /etc/docker/daemon.json
{
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "10m",
    "max-file": "3"
  }
}

# 重启 Docker
sudo systemctl restart docker
```

## 数据备份

备份系统已修复，会自动执行：
- **每日备份**: 凌晨 2:00（工作日 + 中等验证，周日 + 完整验证）
- **每周备份**: 周日
- **每月备份**: 每月 1 号

备份文件位置：
```
./backups/
  ├── daily/     # 保留 7 天
  ├── weekly/    # 保留 4 周
  └── monthly/   # 保留 6 个月
```

手动触发备份：
```bash
docker exec finance-backup /scripts/backup.sh
```

## 安全建议

1. **修改默认密码**: 更改 `.env` 中的 `JWT_SECRET`
2. **数据库安全**: 使用强密码，限制访问 IP
3. **HTTPS**: 在生产环境使用反向代理（Nginx/Caddy）配置 HTTPS
4. **防火墙**: 限制端口访问，只暴露必要的端口
5. **备份加密**: 考虑对备份文件进行加密存储

## 下一步

部署完成后：

1. ✅ 验证所有服务运行正常
2. ✅ 测试 Family Store 功能（管理员切换家庭）
3. ✅ 等待第二天检查备份定时任务
4. ✅ 监控应用性能和错误日志
5. ✅ 配置外部监控和告警

## 获取帮助

- **GitHub Issues**: https://github.com/austinxyz/finance/issues
- **文档**:
  - `README.md` - 项目概述
  - `backup/TROUBLESHOOTING.md` - 备份故障排查
  - `backup/DEPLOY.md` - 备份部署指南
  - `docs/` - 其他文档

## 更新历史

### v1.1.0 (2026-01-12)
- ✅ Family Store 迁移完成（24/24 页面）
- ✅ Backup 定时任务修复
- ✅ 后端安全增强
- ✅ 多平台镜像支持（amd64 + arm64）

### v1.0.0 (Previous)
- 基础功能实现
- Docker 化部署
- 备份系统（需修复）
