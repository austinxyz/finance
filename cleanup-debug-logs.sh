#!/bin/bash

# 清理前端和后端的调试日志
# 保留错误处理中的必要日志，只删除调试用的日志

set -e

echo "🧹 清理前端和后端的调试日志..."
echo ""

FRONTEND_DIR="frontend/src"
BACKEND_DIR="backend/src/main/java"

# 备份计数
FRONTEND_BACKUPS=0
BACKEND_BACKUPS=0

# 前端清理计数
CONSOLE_LOG_REMOVED=0
CONSOLE_WARN_REMOVED=0
CONSOLE_ERROR_REMOVED=0

# 后端清理计数
SYSTEM_OUT_REMOVED=0
PRINT_STACK_REMOVED=0

# ========================================
# 前端清理
# ========================================

echo "📦 清理前端调试日志..."

# 查找所有包含 console.log 的文件
while IFS= read -r file; do
    if [ -f "$file" ]; then
        # 创建备份
        cp "$file" "${file}.cleanup-backup"
        ((FRONTEND_BACKUPS++))

        # 统计删除数量
        count=$(grep -c "console.log" "$file" 2>/dev/null || echo 0)
        ((CONSOLE_LOG_REMOVED+=count))

        # 删除 console.log 行（保留在 catch 块中的错误日志）
        sed -i '' '/console\.log/d' "$file"

        # 删除 console.warn（除非是错误处理）
        count=$(grep -c "console.warn" "$file" 2>/dev/null || echo 0)
        ((CONSOLE_WARN_REMOVED+=count))
        sed -i '' '/console\.warn.*不需要有家庭选择/d' "$file"

        # 删除调试用的 console.error（保留真正的错误处理）
        # 只删除明显是调试用的，例如包含 "Response:", "错误响应:", "错误数据:" 等
        sed -i '' '/console\.error.*Response:/d' "$file"
        sed -i '' '/console\.error.*错误响应:/d' "$file"
        sed -i '' '/console\.error.*错误数据:/d' "$file"
        sed -i '' '/console\.error.*Loaded accounts:/d' "$file"
    fi
done < <(find "$FRONTEND_DIR" -type f \( -name "*.vue" -o -name "*.js" \) -exec grep -l "console\.log\|console\.warn" {} \;)

echo "  ✓ 前端文件已处理，备份文件数: $FRONTEND_BACKUPS"
echo "  ✓ 删除 console.log: $CONSOLE_LOG_REMOVED 行"
echo "  ✓ 删除 console.warn: $CONSOLE_WARN_REMOVED 行"
echo ""

# ========================================
# 后端清理
# ========================================

echo "📦 清理后端调试日志..."

# 查找所有包含 System.out.println 的文件
while IFS= read -r file; do
    if [ -f "$file" ]; then
        # 创建备份
        cp "$file" "${file}.cleanup-backup"
        ((BACKEND_BACKUPS++))

        # 统计删除数量
        count=$(grep -c "System.out.println" "$file" 2>/dev/null || echo 0)
        ((SYSTEM_OUT_REMOVED+=count))

        # 删除 System.out.println
        sed -i '' '/System\.out\.println/d' "$file"

        # 删除单独的 .printStackTrace() 行（保留在 catch 中有其他处理的）
        count=$(grep -c "\.printStackTrace()" "$file" 2>/dev/null || echo 0)
        ((PRINT_STACK_REMOVED+=count))
        sed -i '' '/\.printStackTrace();$/d' "$file"
    fi
done < <(find "$BACKEND_DIR" -type f -name "*.java" -exec grep -l "System\.out\.println\|\.printStackTrace" {} \;)

echo "  ✓ 后端文件已处理，备份文件数: $BACKEND_BACKUPS"
echo "  ✓ 删除 System.out.println: $SYSTEM_OUT_REMOVED 行"
echo "  ✓ 删除 .printStackTrace(): $PRINT_STACK_REMOVED 行"
echo ""

# ========================================
# 总结
# ========================================

echo "========================================="
echo "清理完成！"
echo "========================================="
echo ""
echo "清理统计："
echo "  前端备份文件: $FRONTEND_BACKUPS 个"
echo "  后端备份文件: $BACKEND_BACKUPS 个"
echo "  总计删除: $((CONSOLE_LOG_REMOVED + CONSOLE_WARN_REMOVED + SYSTEM_OUT_REMOVED + PRINT_STACK_REMOVED)) 行调试日志"
echo ""
echo "备份文件位置："
echo "  - 前端: $FRONTEND_DIR/**/*.cleanup-backup"
echo "  - 后端: $BACKEND_DIR/**/*.cleanup-backup"
echo ""
echo "验证更改："
echo "  git diff --stat"
echo ""
echo "如果确认无误，删除备份文件："
echo "  find $FRONTEND_DIR -name '*.cleanup-backup' -delete"
echo "  find $BACKEND_DIR -name '*.cleanup-backup' -delete"
echo ""
echo "如果需要恢复，运行："
echo "  ./restore-from-cleanup-backup.sh"
echo ""
