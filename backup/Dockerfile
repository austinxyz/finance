# Finance Backup Service Dockerfile
# 基于Debian的MySQL 8.0兼容备份容器

FROM debian:bookworm-slim

# 安装必需的软件包（使用MySQL官方客户端，支持caching_sha2_password）
RUN apt-get update && apt-get install -y \
    bash \
    default-mysql-client \
    coreutils \
    findutils \
    gzip \
    jq \
    tzdata \
    curl \
    ca-certificates \
    cron \
    python3 \
    python3-pip \
    python3-venv \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# 安装Python依赖
RUN pip3 install --no-cache-dir flask==3.0.0 --break-system-packages

# 设置时区为UTC（可通过环境变量覆盖）
ENV TZ=UTC

# 创建备份目录
RUN mkdir -p /backups /scripts

# 复制脚本文件
COPY scripts/*.sh /scripts/
COPY scripts/crontab /scripts/crontab
COPY entrypoint.sh /entrypoint.sh
COPY webhook.py /webhook.py

# 设置脚本权限
RUN chmod +x /scripts/*.sh /entrypoint.sh /webhook.py

# 设置工作目录
WORKDIR /backups

# 默认环境变量（可通过docker-compose覆盖）
ENV BACKUP_RETENTION_DAYS=7 \
    BACKUP_RETENTION_WEEKS=4 \
    BACKUP_RETENTION_MONTHS=6 \
    RUN_INITIAL_BACKUP=false

# 暴露webhook端口
EXPOSE 5000

# 健康检查：检查最近24小时内是否有备份
HEALTHCHECK --interval=1h --timeout=10s --start-period=5m \
    CMD find /backups/daily -name "*.sql.gz" -type f -mtime -1 | grep -q . || exit 1

# 容器入口点
ENTRYPOINT ["/entrypoint.sh"]
