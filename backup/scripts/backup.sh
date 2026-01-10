#!/bin/bash

# MySQL备份脚本
# 自动执行每日备份，并管理备份轮转（日/周/月）

set -e

# 配置
BACKUP_DIR="/backups"
DAILY_DIR="$BACKUP_DIR/daily"
WEEKLY_DIR="$BACKUP_DIR/weekly"
MONTHLY_DIR="$BACKUP_DIR/monthly"
LOG_FILE="$BACKUP_DIR/backup.log"
METADATA_FILE="$BACKUP_DIR/metadata.json"

# 创建必要的目录
mkdir -p "$DAILY_DIR" "$WEEKLY_DIR" "$MONTHLY_DIR"

# 日志函数
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

# 备份函数
perform_backup() {
    local backup_type=$1  # daily, weekly, monthly
    local backup_dir=$2

    local timestamp=$(date '+%Y%m%d_%H%M%S')
    local filename="finance_${backup_type}_${timestamp}.sql"
    local filepath="$backup_dir/$filename"
    local compressed_file="${filepath}.gz"

    log "开始 $backup_type 备份: $filename"

    # 执行mysqldump
    if mysqldump \
        -h "$DB_HOST" \
        -P "$DB_PORT" \
        -u "$DB_USER" \
        -p"$DB_PASSWORD" \
        --single-transaction \
        --routines \
        --triggers \
        --events \
        "$DB_NAME" > "$filepath" 2>> "$LOG_FILE"; then

        log "mysqldump完成，开始压缩..."

        # 压缩备份文件
        gzip "$filepath"

        # 获取备份元数据
        local file_size=$(stat -f%z "$compressed_file" 2>/dev/null || stat -c%s "$compressed_file" 2>/dev/null)
        local table_count=$(zcat "$compressed_file" | grep -c "CREATE TABLE" || echo "0")

        log "备份完成: $compressed_file (大小: $file_size bytes, 表数量: $table_count)"

        # 记录元数据
        record_metadata "$backup_type" "$compressed_file" "$file_size" "$table_count"

        return 0
    else
        log "ERROR: mysqldump失败"
        return 1
    fi
}

# 记录备份元数据
record_metadata() {
    local backup_type=$1
    local filepath=$2
    local file_size=$3
    local table_count=$4

    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    local filename=$(basename "$filepath")

    # 创建或更新元数据JSON
    local metadata="{\"timestamp\":\"$timestamp\",\"type\":\"$backup_type\",\"file\":\"$filename\",\"size\":$file_size,\"tables\":$table_count}"

    # 追加到元数据文件
    if [ -f "$METADATA_FILE" ]; then
        # 读取现有内容，添加新记录
        temp_file=$(mktemp)
        jq ". += [$metadata]" "$METADATA_FILE" > "$temp_file" 2>/dev/null || echo "[$metadata]" > "$temp_file"
        mv "$temp_file" "$METADATA_FILE"
    else
        echo "[$metadata]" > "$METADATA_FILE"
    fi
}

# 清理旧备份
cleanup_old_backups() {
    local backup_dir=$1
    local retention_days=$2

    log "清理 $backup_dir 中超过 $retention_days 天的备份..."

    find "$backup_dir" -name "*.sql.gz" -type f -mtime +$retention_days -delete

    local remaining=$(find "$backup_dir" -name "*.sql.gz" -type f | wc -l)
    log "清理完成，剩余 $remaining 个备份文件"
}

# 主逻辑
main() {
    log "========== 开始备份流程 =========="

    # 检查必需的环境变量
    if [ -z "$DB_HOST" ] || [ -z "$DB_USER" ] || [ -z "$DB_PASSWORD" ] || [ -z "$DB_NAME" ]; then
        log "ERROR: 缺少必需的环境变量 (DB_HOST, DB_USER, DB_PASSWORD, DB_NAME)"
        exit 1
    fi

    # 每日备份
    if perform_backup "daily" "$DAILY_DIR"; then
        cleanup_old_backups "$DAILY_DIR" "${BACKUP_RETENTION_DAYS:-7}"
    else
        log "ERROR: 每日备份失败"
        exit 1
    fi

    # 周日创建周备份
    if [ $(date +%u) -eq 7 ]; then
        log "今天是周日，执行周备份..."
        if perform_backup "weekly" "$WEEKLY_DIR"; then
            cleanup_old_backups "$WEEKLY_DIR" "$((${BACKUP_RETENTION_WEEKS:-4} * 7))"
        fi
    fi

    # 每月1号创建月备份
    if [ $(date +%d) -eq 01 ]; then
        log "今天是月初，执行月备份..."
        if perform_backup "monthly" "$MONTHLY_DIR"; then
            cleanup_old_backups "$MONTHLY_DIR" "$((${BACKUP_RETENTION_MONTHS:-6} * 30))"
        fi
    fi

    log "========== 备份流程完成 =========="
}

# 执行主函数
main
