#!/bin/bash

# 从备份恢复文件

set -e

echo "🔄 从 cleanup-backup 恢复文件..."
echo ""

restored=0

# 查找所有备份文件
while IFS= read -r backup_file; do
    if [ -f "$backup_file" ]; then
        original_file="${backup_file%.cleanup-backup}"

        # 恢复原文件
        mv "$backup_file" "$original_file"
        ((restored++))

        echo "恢复: $original_file"
    fi
done < <(find . -name "*.cleanup-backup")

echo ""
echo "========================================="
echo "恢复完成！"
echo "========================================="
echo "恢复文件数: $restored"
echo ""
