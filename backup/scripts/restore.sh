#!/bin/bash

# 数据库恢复脚本
# 用法:
#   restore.sh list                    - 列出所有可用备份
#   restore.sh latest [daily|weekly|monthly]  - 恢复最新备份
#   restore.sh <filename>              - 恢复指定备份文件

set -e

BACKUP_DIR="/backups"
LOG_FILE="$BACKUP_DIR/restore.log"

# 日志函数
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

# 列出所有备份
list_backups() {
    log "========== 可用备份列表 =========="

    echo ""
    echo "每日备份 (最近7天):"
    find "$BACKUP_DIR/daily" -name "*.sql.gz" -type f -printf '%T@ %p\n' 2>/dev/null | \
        sort -rn | head -7 | while read timestamp filepath; do
        local filename=$(basename "$filepath")
        local date=$(date -d "@$timestamp" '+%Y-%m-%d %H:%M' 2>/dev/null || date -r "$timestamp" '+%Y-%m-%d %H:%M')
        local size=$(stat -f%z "$filepath" 2>/dev/null || stat -c%s "$filepath" 2>/dev/null)
        printf "  %s  %10s  %s\n" "$date" "$(numfmt --to=iec-i --suffix=B $size 2>/dev/null || echo ${size}B)" "$filename"
    done

    echo ""
    echo "每周备份 (最近4周):"
    find "$BACKUP_DIR/weekly" -name "*.sql.gz" -type f -printf '%T@ %p\n' 2>/dev/null | \
        sort -rn | head -4 | while read timestamp filepath; do
        local filename=$(basename "$filepath")
        local date=$(date -d "@$timestamp" '+%Y-%m-%d %H:%M' 2>/dev/null || date -r "$timestamp" '+%Y-%m-%d %H:%M')
        local size=$(stat -f%z "$filepath" 2>/dev/null || stat -c%s "$filepath" 2>/dev/null)
        printf "  %s  %10s  %s\n" "$date" "$(numfmt --to=iec-i --suffix=B $size 2>/dev/null || echo ${size}B)" "$filename"
    done

    echo ""
    echo "每月备份 (最近6个月):"
    find "$BACKUP_DIR/monthly" -name "*.sql.gz" -type f -printf '%T@ %p\n' 2>/dev/null | \
        sort -rn | head -6 | while read timestamp filepath; do
        local filename=$(basename "$filepath")
        local date=$(date -d "@$timestamp" '+%Y-%m-%d %H:%M' 2>/dev/null || date -r "$timestamp" '+%Y-%m-%d %H:%M')
        local size=$(stat -f%z "$filepath" 2>/dev/null || stat -c%s "$filepath" 2>/dev/null)
        printf "  %s  %10s  %s\n" "$date" "$(numfmt --to=iec-i --suffix=B $size 2>/dev/null || echo ${size}B)" "$filename"
    done

    echo ""
}

# 查找备份文件
find_backup_file() {
    local search_term=$1

    # 如果是完整路径且文件存在
    if [ -f "$search_term" ]; then
        echo "$search_term"
        return 0
    fi

    # 在所有备份目录中搜索
    local found_file=$(find "$BACKUP_DIR" -name "$search_term" -o -name "*$search_term*" | head -1)

    if [ -n "$found_file" ]; then
        echo "$found_file"
        return 0
    fi

    return 1
}

# 恢复备份
perform_restore() {
    local backup_file=$1

    log "========== 开始恢复数据库 =========="
    log "备份文件: $backup_file"

    # 检查文件存在
    if [ ! -f "$backup_file" ]; then
        log "ERROR: 备份文件不存在: $backup_file"
        return 1
    fi

    # 检查gzip完整性
    if ! gzip -t "$backup_file" 2>/dev/null; then
        log "ERROR: 备份文件损坏"
        return 1
    fi

    # 警告：恢复会覆盖现有数据
    log "WARNING: 此操作将覆盖数据库 $DB_NAME 的所有数据"
    log "按 Ctrl+C 取消，或等待5秒后自动继续..."
    sleep 5

    # 创建当前数据库的快照备份（安全措施）
    local snapshot_file="$BACKUP_DIR/pre_restore_snapshot_$(date +%Y%m%d_%H%M%S).sql.gz"
    log "创建恢复前快照: $snapshot_file"

    if mysqldump -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" \
        --single-transaction "$DB_NAME" 2>> "$LOG_FILE" | gzip > "$snapshot_file"; then
        log "快照创建成功"
    else
        log "WARNING: 快照创建失败，但继续恢复..."
    fi

    # 删除现有数据库并重新创建
    log "删除现有数据库..."
    if ! mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" \
        -e "DROP DATABASE IF EXISTS \`$DB_NAME\`; CREATE DATABASE \`$DB_NAME\`" 2>> "$LOG_FILE"; then
        log "ERROR: 无法重建数据库"
        return 1
    fi

    # 导入备份
    log "开始导入备份数据..."
    if zcat "$backup_file" | mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" \
        "$DB_NAME" 2>> "$LOG_FILE"; then

        # 验证恢复结果
        local table_count=$(mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" \
            -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$DB_NAME'" 2>/dev/null)

        log "恢复成功！表数量: $table_count"
        log "快照备份保存在: $snapshot_file"
        return 0
    else
        log "ERROR: 导入备份失败"
        log "尝试从快照恢复..."

        # 尝试从快照恢复
        if [ -f "$snapshot_file" ]; then
            zcat "$snapshot_file" | mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" \
                "$DB_NAME" 2>> "$LOG_FILE"
            log "已从快照恢复原始数据"
        fi

        return 1
    fi
}

# 主函数
main() {
    local command=$1
    local param=$2

    # 检查环境变量
    if [ -z "$DB_HOST" ] || [ -z "$DB_USER" ] || [ -z "$DB_PASSWORD" ] || [ -z "$DB_NAME" ]; then
        log "ERROR: 缺少必需的环境变量"
        exit 1
    fi

    case "$command" in
        list)
            list_backups
            ;;
        latest)
            local backup_type="${param:-daily}"
            local backup_dir="$BACKUP_DIR/$backup_type"
            local latest=$(find "$backup_dir" -name "*.sql.gz" -type f -printf '%T@ %p\n' 2>/dev/null | \
                sort -rn | head -1 | cut -d' ' -f2)

            if [ -z "$latest" ]; then
                log "ERROR: 未找到 $backup_type 备份"
                exit 1
            fi

            perform_restore "$latest"
            ;;
        *)
            # 尝试作为文件名查找
            if [ -z "$command" ]; then
                echo "用法:"
                echo "  $0 list                          - 列出所有可用备份"
                echo "  $0 latest [daily|weekly|monthly] - 恢复最新备份"
                echo "  $0 <filename>                    - 恢复指定备份"
                exit 1
            fi

            local backup_file=$(find_backup_file "$command")
            if [ -z "$backup_file" ]; then
                log "ERROR: 未找到备份文件: $command"
                exit 1
            fi

            perform_restore "$backup_file"
            ;;
    esac
}

# 执行主函数
main "$@"
