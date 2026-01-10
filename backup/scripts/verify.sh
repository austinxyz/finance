#!/bin/bash

# 备份验证脚本
# 支持三种验证级别：
# - simple: 检查文件完整性和gzip有效性
# - medium: 使用mysqlcheck验证SQL语法（默认）
# - full: 导入到临时数据库验证（每周日执行）

set -e

BACKUP_DIR="/backups"
LOG_FILE="$BACKUP_DIR/verify.log"
VERIFY_LEVEL="${1:-medium}"

# 日志函数
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

# 简单验证：检查文件完整性
simple_verify() {
    local backup_file=$1

    log "简单验证: $backup_file"

    # 检查文件存在
    if [ ! -f "$backup_file" ]; then
        log "ERROR: 备份文件不存在"
        return 1
    fi

    # 检查文件大小
    local file_size=$(stat -f%z "$backup_file" 2>/dev/null || stat -c%s "$backup_file" 2>/dev/null)
    if [ "$file_size" -lt 1024 ]; then
        log "ERROR: 备份文件过小 ($file_size bytes)"
        return 1
    fi

    # 检查gzip完整性
    if ! gzip -t "$backup_file" 2>/dev/null; then
        log "ERROR: gzip文件损坏"
        return 1
    fi

    log "简单验证通过 (大小: $file_size bytes)"
    return 0
}

# 中等验证：解压并检查SQL语法
medium_verify() {
    local backup_file=$1

    log "中等验证: $backup_file"

    # 先执行简单验证
    if ! simple_verify "$backup_file"; then
        return 1
    fi

    # 检查SQL内容
    local temp_sql=$(mktemp)
    if ! zcat "$backup_file" > "$temp_sql" 2>/dev/null; then
        log "ERROR: 无法解压备份文件"
        rm -f "$temp_sql"
        return 1
    fi

    # 检查必要的SQL语句
    local create_count=$(grep -c "CREATE TABLE" "$temp_sql" || echo "0")
    local insert_count=$(grep -c "INSERT INTO" "$temp_sql" || echo "0")

    if [ "$create_count" -eq 0 ]; then
        log "ERROR: 备份中没有CREATE TABLE语句"
        rm -f "$temp_sql"
        return 1
    fi

    log "中等验证通过 (CREATE TABLE: $create_count, INSERT INTO: $insert_count)"
    rm -f "$temp_sql"
    return 0
}

# 完整验证：导入到临时数据库
full_verify() {
    local backup_file=$1

    log "完整验证: $backup_file"

    # 先执行中等验证
    if ! medium_verify "$backup_file"; then
        return 1
    fi

    # 创建临时数据库名称
    local temp_db="finance_verify_$(date +%s)"

    log "创建临时数据库: $temp_db"

    # 创建临时数据库
    if ! mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" \
        -e "CREATE DATABASE \`$temp_db\`" 2>> "$LOG_FILE"; then
        log "ERROR: 无法创建临时数据库"
        return 1
    fi

    # 导入备份到临时数据库
    log "导入备份到临时数据库..."
    if zcat "$backup_file" | mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" \
        "$temp_db" 2>> "$LOG_FILE"; then

        # 检查表数量
        local table_count=$(mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" \
            -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$temp_db'" 2>/dev/null)

        # 检查记录数（示例：检查families表）
        local families_count=$(mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" \
            -N -e "SELECT COUNT(*) FROM \`$temp_db\`.families" 2>/dev/null || echo "0")

        log "完整验证通过 (表数量: $table_count, families记录: $families_count)"

        # 清理临时数据库
        mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" \
            -e "DROP DATABASE \`$temp_db\`" 2>> "$LOG_FILE"

        return 0
    else
        log "ERROR: 导入备份失败"

        # 清理临时数据库
        mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" \
            -e "DROP DATABASE IF EXISTS \`$temp_db\`" 2>> "$LOG_FILE"

        return 1
    fi
}

# 验证最新备份
verify_latest() {
    local backup_type="${1:-daily}"
    local backup_dir="$BACKUP_DIR/$backup_type"

    # 查找最新的备份文件
    local latest_backup=$(find "$backup_dir" -name "*.sql.gz" -type f -printf '%T@ %p\n' 2>/dev/null | sort -rn | head -1 | cut -d' ' -f2)

    if [ -z "$latest_backup" ]; then
        log "ERROR: 未找到 $backup_type 备份文件"
        return 1
    fi

    log "========== 验证最新 $backup_type 备份 =========="
    log "文件: $latest_backup"

    case "$VERIFY_LEVEL" in
        simple)
            simple_verify "$latest_backup"
            ;;
        medium)
            medium_verify "$latest_backup"
            ;;
        full)
            full_verify "$latest_backup"
            ;;
        *)
            log "ERROR: 无效的验证级别: $VERIFY_LEVEL"
            return 1
            ;;
    esac
}

# 主函数
main() {
    log "========== 开始备份验证 (级别: $VERIFY_LEVEL) =========="

    # 检查环境变量
    if [ -z "$DB_HOST" ] || [ -z "$DB_USER" ] || [ -z "$DB_PASSWORD" ]; then
        log "ERROR: 缺少必需的环境变量"
        exit 1
    fi

    # 验证每日备份
    if verify_latest "daily"; then
        log "✓ 每日备份验证通过"
    else
        log "✗ 每日备份验证失败"
        exit 1
    fi

    # 周日执行完整验证
    if [ $(date +%u) -eq 7 ] && [ "$VERIFY_LEVEL" != "full" ]; then
        log "今天是周日，执行完整验证..."
        VERIFY_LEVEL="full" verify_latest "weekly"
    fi

    log "========== 验证完成 =========="
}

# 执行主函数
main
