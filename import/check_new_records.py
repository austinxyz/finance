#!/usr/bin/env python3
"""
检查preview Excel中的新记录详情

使用方法:
  python3 check_new_records.py --file preview_2024.xlsx
"""

import pandas as pd
import sys
import argparse
import os
import re
import subprocess

def load_db_config():
    """从backend/.env加载数据库配置"""
    env_file = '../backend/.env'
    if not os.path.exists(env_file):
        print(f"❌ 错误: 找不到数据库配置文件: {env_file}")
        sys.exit(1)

    db_config = {}
    with open(env_file, 'r') as f:
        for line in f:
            line = line.strip()
            if line and not line.startswith('#') and '=' in line:
                key, value = line.split('=', 1)
                db_config[key] = value

    db_url = db_config.get('DB_URL', '')
    match = re.search(r'//([^:]+):(\d+)/([^?]+)', db_url)
    if not match:
        print(f"❌ 错误: 无法解析数据库URL: {db_url}")
        sys.exit(1)

    return {
        'host': match.group(1),
        'port': match.group(2),
        'database': match.group(3),
        'user': db_config.get('DB_USERNAME', ''),
        'password': db_config.get('DB_PASSWORD', '')
    }

def get_mysql_client():
    """获取mysql客户端路径"""
    try:
        mysql_prefix = subprocess.check_output(['brew', '--prefix', 'mysql-client'], text=True).strip()
        return f"{mysql_prefix}/bin/mysql"
    except:
        return 'mysql'

def execute_sql(db_config, sql):
    """执行SQL语句"""
    mysql_client = get_mysql_client()
    cmd = [
        mysql_client,
        f'-h{db_config["host"]}',
        f'-P{db_config["port"]}',
        f'-u{db_config["user"]}',
        f'-p{db_config["password"]}',
        db_config['database'],
        '-N', '-B', '-e', sql
    ]

    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode != 0:
        return None
    return result.stdout.strip()

def check_new_records(excel_file, sheet_name=None):
    """检查新记录详情"""

    print(f"\n{'='*80}")
    print(f"📊 检查新记录详情")
    print(f"{'='*80}")
    print(f"文件: {excel_file}\n")

    # 加载数据库配置
    db_config = load_db_config()

    # 读取Excel
    xl_file = pd.ExcelFile(excel_file)
    sheets = [sheet_name] if sheet_name else xl_file.sheet_names

    for sheet in sheets:
        print(f"\n{'='*80}")
        print(f"📄 Sheet: {sheet}")
        print(f"{'='*80}")

        # 解析sheet名称
        parts = sheet.split('-')
        if len(parts) != 3:
            continue

        year = int(parts[0])
        record_type = parts[1]  # expense 或 budgets
        currency = parts[2]

        # 读取数据
        df = pd.read_excel(excel_file, sheet_name=sheet)

        new_records = []

        if record_type == 'expense':
            # 费用记录
            for _, row in df.iterrows():
                check_sql = f"""
                SELECT COUNT(*) FROM expense_records
                WHERE family_id = {int(row['family_id'])}
                  AND expense_year = {int(row['year'])}
                  AND expense_month = {int(row['month'])}
                  AND expense_period = '{row['expense_period']}'
                  AND minor_category_id = {int(row['minor_category_id'])}
                  AND expense_type = 'ACTUAL'
                """

                result = execute_sql(db_config, check_sql)
                if result and int(result) == 0:
                    new_records.append({
                        '月份': row['month'],
                        '期间': row['expense_period'],
                        '分类': row['minor_category_name'],
                        '金额': f"{row['currency']} {row['amount']:,.2f}",
                        'Excel分类': row.get('excel_category', ''),
                        '行号': row.get('excel_row', '')
                    })
        else:
            # 预算记录
            for _, row in df.iterrows():
                check_sql = f"""
                SELECT COUNT(*) FROM expense_budgets
                WHERE family_id = {int(row['family_id'])}
                  AND budget_year = {int(row['year'])}
                  AND minor_category_id = {int(row['minor_category_id'])}
                  AND currency = '{row['currency']}'
                """

                result = execute_sql(db_config, check_sql)
                if result and int(result) == 0:
                    new_records.append({
                        '年份': row['year'],
                        '分类': row['minor_category_name'],
                        '预算金额': f"{row['currency']} {row['budget_amount']:,.2f}",
                        'Excel分类': row.get('excel_category', ''),
                        '行号': row.get('excel_row', '')
                    })

        if new_records:
            print(f"\n✨ 发现 {len(new_records)} 条新记录:\n")
            df_new = pd.DataFrame(new_records)
            print(df_new.to_string(index=False))
            print()
        else:
            print(f"\n✓ 所有记录已存在\n")

def main():
    parser = argparse.ArgumentParser(description='检查preview Excel中的新记录详情')
    parser.add_argument('--file', type=str, required=True, help='预览Excel文件路径')
    parser.add_argument('--sheet', type=str, default=None, help='指定sheet名称（可选）')

    args = parser.parse_args()
    check_new_records(args.file, args.sheet)

if __name__ == '__main__':
    main()
