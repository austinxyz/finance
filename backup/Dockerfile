# Finance Backup Service Dockerfile
# 基于Alpine Linux的轻量级MySQL备份容器

FROM alpine:3.19

# 安装必需的软件包
RUN apk add --no-cache \
    bash \
    mysql-client \
    coreutils \
    findutils \
    gzip \
    jq \
    tzdata \
    && rm -rf /var/cache/apk/*

# 设置时区为UTC（可通过环境变量覆盖）
ENV TZ=UTC

# 创建备份目录
RUN mkdir -p /backups /scripts

# 复制脚本文件
COPY scripts/*.sh /scripts/
COPY scripts/crontab /scripts/crontab
COPY entrypoint.sh /entrypoint.sh

# 设置脚本权限
RUN chmod +x /scripts/*.sh /entrypoint.sh

# 设置工作目录
WORKDIR /backups

# 默认环境变量（可通过docker-compose覆盖）
ENV BACKUP_RETENTION_DAYS=7 \
    BACKUP_RETENTION_WEEKS=4 \
    BACKUP_RETENTION_MONTHS=6 \
    RUN_INITIAL_BACKUP=false

# 健康检查：检查最近24小时内是否有备份
HEALTHCHECK --interval=1h --timeout=10s --start-period=5m \
    CMD find /backups/daily -name "*.sql.gz" -type f -mtime -1 | grep -q . || exit 1

# 容器入口点
ENTRYPOINT ["/entrypoint.sh"]
