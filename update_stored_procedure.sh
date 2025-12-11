#!/bin/bash

# 更新存储过程的脚本
# 使用本地MySQL客户端更新存储过程

cd "$(dirname "$0")"

# 加载环境变量
source ./setup-env.sh

# 更新存储过程
echo "正在更新存储过程..."
mysql -h "$DB_HOST" -P "$DB_PORT" -u root -p"$MYSQL_ROOT_PASSWORD" "$DB_NAME" < database/create_annual_summary_with_year_end_rate.sql

if [ $? -eq 0 ]; then
    echo "存储过程更新成功！"
    echo ""
    echo "现在需要重新计算年度数据。请在浏览器中点击'刷新数据'按钮。"
else
    echo "存储过程更新失败，请检查错误信息。"
    exit 1
fi
