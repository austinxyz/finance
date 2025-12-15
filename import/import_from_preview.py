#!/usr/bin/env python3
"""
从预览Excel文件直接导入数据到数据库

使用方法:
  python3 import_from_preview.py --file preview_2024.xlsx

功能:
  1. 读取preview Excel文件中的所有sheets
  2. 检查数据库中是否已存在相同记录（避免重复导入）
  3. 只导入新记录
  4. 显示详细的导入统计

参数说明:
  --file: 预览Excel文件路径 (必填)
  --sheets: 指定要导入的sheets，逗号分隔 (可选，默认导入所有sheets)
  --dry-run: 只检查不执行导入 (可选)
"""

import pandas as pd
import sys
import argparse
import os
import re
import subprocess
from collections import defaultdict
from datetime import datetime

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

    # 解析DB_URL
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

def execute_sql(db_config, sql, fetch=False):
    """执行SQL语句"""
    mysql_client = get_mysql_client()

    cmd = [
        mysql_client,
        f'-h{db_config["host"]}',
        f'-P{db_config["port"]}',
        f'-u{db_config["user"]}',
        f'-p{db_config["password"]}',
        db_config['database'],
        '-N',  # 不显示列名
        '-B'   # 批处理模式
    ]

    if fetch:
        cmd.append('-e')
        cmd.append(sql)
        result = subprocess.run(cmd, capture_output=True, text=True)
        if result.returncode != 0:
            print(f"❌ SQL执行错误: {result.stderr}")
            return None
        return result.stdout.strip()
    else:
        result = subprocess.run(cmd, input=sql, capture_output=True, text=True)
        if result.returncode != 0:
            print(f"❌ SQL执行错误: {result.stderr}")
            return False
        return True

def check_existing_records(db_config, records, record_type):
    """检查哪些记录已经存在于数据库中"""
    if not records:
        return set(), set()

    print(f"\n🔍 检查{record_type}记录是否已存在...")

    existing = set()
    new_records = set()

    if record_type == '费用':
        # 费用记录表：expense_records
        # 唯一约束：(family_id, expense_period, minor_category_id, currency)
        for idx, rec in enumerate(records):
            check_sql = f"""
            SELECT COUNT(*) FROM expense_records
            WHERE family_id = {rec['family_id']}
              AND expense_period = '{rec['expense_period']}'
              AND minor_category_id = {rec['minor_category_id']}
              AND currency = '{rec['currency']}'
            """

            result = execute_sql(db_config, check_sql, fetch=True)
            if result is None:
                print(f"⚠️  无法检查记录 {idx + 1}")
                continue

            count = int(result)
            record_key = (rec['family_id'], rec['expense_period'], rec['minor_category_id'], rec['currency'])

            if count > 0:
                existing.add(record_key)
            else:
                new_records.add(record_key)
    else:
        # 预算记录表：expense_budgets
        for idx, rec in enumerate(records):
            check_sql = f"""
            SELECT COUNT(*) FROM expense_budgets
            WHERE family_id = {rec['family_id']}
              AND budget_year = {rec['year']}
              AND minor_category_id = {rec['minor_category_id']}
              AND currency = '{rec['currency']}'
            """

            result = execute_sql(db_config, check_sql, fetch=True)
            if result is None:
                print(f"⚠️  无法检查记录 {idx + 1}")
                continue

            count = int(result)
            record_key = (rec['family_id'], rec['year'], rec['minor_category_name'], rec['currency'])

            if count > 0:
                existing.add(record_key)
            else:
                new_records.add(record_key)

    return existing, new_records

def parse_sheet_name(sheet_name):
    """解析sheet名称，提取年份、类型、货币"""
    # 格式: 2024-expense-USD 或 2024-budgets-CNY
    parts = sheet_name.split('-')
    if len(parts) != 3:
        return None

    year = int(parts[0])
    record_type = 'expense' if parts[1] == 'expense' else 'budget'
    currency = parts[2]

    return {
        'year': year,
        'type': record_type,
        'currency': currency,
        'type_cn': '费用' if record_type == 'expense' else '预算'
    }

def import_from_preview_excel(excel_file, selected_sheets=None, dry_run=False):
    """从预览Excel文件导入数据"""

    print(f"\n{'='*80}")
    print(f"📊 从预览Excel文件导入数据")
    print(f"{'='*80}")
    print(f"文件: {excel_file}")
    if dry_run:
        print(f"模式: 🔍 检查模式（不会实际导入）")
    print()

    # 检查文件
    if not os.path.exists(excel_file):
        print(f"❌ 错误: Excel文件不存在: {excel_file}")
        sys.exit(1)

    # 加载数据库配置
    db_config = load_db_config()
    print(f"✅ 数据库配置加载成功")
    print(f"   主机: {db_config['host']}:{db_config['port']}")
    print(f"   数据库: {db_config['database']}")

    # 读取Excel文件
    try:
        xl_file = pd.ExcelFile(excel_file)
        all_sheets = xl_file.sheet_names
        print(f"✅ Excel文件读取成功")
        print(f"   包含sheets: {len(all_sheets)}")
    except Exception as e:
        print(f"❌ 错误: 无法读取Excel文件: {e}")
        sys.exit(1)

    # 确定要处理的sheets
    if selected_sheets:
        sheets_to_process = [s.strip() for s in selected_sheets.split(',') if s.strip() in all_sheets]
        if not sheets_to_process:
            print(f"❌ 错误: 指定的sheets不存在")
            sys.exit(1)
    else:
        sheets_to_process = all_sheets

    print(f"\n准备处理的sheets: {', '.join(sheets_to_process)}")

    # 统计信息
    total_stats = {
        'sheets_processed': 0,
        'total_records': 0,
        'existing_records': 0,
        'new_records': 0,
        'imported_records': 0,
        'failed_records': 0
    }

    import_summary = []

    # 处理每个sheet
    for sheet_name in sheets_to_process:
        print(f"\n{'='*80}")
        print(f"📄 处理Sheet: {sheet_name}")
        print(f"{'='*80}")

        # 解析sheet名称
        sheet_info = parse_sheet_name(sheet_name)
        if not sheet_info:
            print(f"⚠️  跳过: 无法解析sheet名称格式")
            continue

        # 读取sheet数据
        try:
            df = pd.read_excel(excel_file, sheet_name=sheet_name)
            print(f"✅ Sheet读取成功: {len(df)} 条记录")
        except Exception as e:
            print(f"❌ 错误: 无法读取sheet: {e}")
            continue

        # 准备记录
        records = []
        is_budget = sheet_info['type'] == 'budget'

        for _, row in df.iterrows():
            if is_budget:
                # 预算记录：年度数据，直接保存到expense_budgets表
                rec = {
                    'family_id': int(row['family_id']),
                    'year': int(row['year']),
                    'minor_category_id': int(row['minor_category_id']),
                    'minor_category_name': row['minor_category_name'],
                    'amount': float(row['budget_amount']),
                    'currency': row['currency'],
                    'notes': str(row.get('description', f"预算-{row['minor_category_name']}")),
                    'record_type': sheet_info['type']
                }
                records.append(rec)
            else:
                # 费用记录：有month, expense_period, amount
                rec = {
                    'family_id': int(row['family_id']),
                    'year': int(row['year']),
                    'month': int(row['month']),
                    'expense_period': row['expense_period'],
                    'major_category_id': int(row.get('major_category_id', 0)),
                    'minor_category_id': int(row['minor_category_id']),
                    'minor_category_name': row['minor_category_name'],
                    'amount': float(row['amount']),
                    'currency': row['currency'],
                    'description': str(row.get('description', f"费用-{row['minor_category_name']}")),
                    'record_type': sheet_info['type']
                }
                records.append(rec)

        total_stats['total_records'] += len(records)

        # 聚合具有相同唯一键的记录（合并金额）
        if not is_budget:
            # 费用记录：按 (family_id, expense_period, minor_category_id, currency) 聚合
            from collections import defaultdict
            aggregated = defaultdict(lambda: {
                'amount': 0.0,
                'descriptions': [],
                'record': None
            })

            for rec in records:
                key = (rec['family_id'], rec['expense_period'], rec['minor_category_id'], rec['currency'])
                aggregated[key]['amount'] += rec['amount']
                aggregated[key]['descriptions'].append(rec['description'])
                if aggregated[key]['record'] is None:
                    aggregated[key]['record'] = rec.copy()

            # 重建records列表
            records = []
            for key, data in aggregated.items():
                rec = data['record']
                rec['amount'] = data['amount']
                # 合并描述信息
                unique_descs = list(set(data['descriptions']))
                if len(unique_descs) > 1:
                    rec['description'] = '; '.join(unique_descs)
                records.append(rec)

            if len(aggregated) < total_stats['total_records']:
                print(f"\n📝 聚合重复记录:")
                print(f"   原始记录: {total_stats['total_records']} 条")
                print(f"   聚合后: {len(records)} 条")
                total_stats['total_records'] = len(records)

        # 检查已存在的记录
        existing_keys, new_keys = check_existing_records(db_config, records, sheet_info['type_cn'])

        existing_count = len(existing_keys)
        new_count = len(new_keys)

        print(f"\n📊 检查结果:")
        print(f"   总记录数: {len(records)}")
        print(f"   已存在: {existing_count} 条 (将跳过)")
        print(f"   新记录: {new_count} 条 (将导入)")

        total_stats['existing_records'] += existing_count
        total_stats['new_records'] += new_count

        if new_count == 0:
            print(f"\n✓ 所有记录已存在，跳过导入")
            import_summary.append({
                'sheet': sheet_name,
                'total': len(records),
                'existing': existing_count,
                'new': 0,
                'imported': 0,
                'status': 'skipped'
            })
            continue

        if dry_run:
            print(f"\n🔍 检查模式: 跳过实际导入")
            import_summary.append({
                'sheet': sheet_name,
                'total': len(records),
                'existing': existing_count,
                'new': new_count,
                'imported': 0,
                'status': 'dry-run'
            })
            continue

        # 生成INSERT语句（只针对新记录）
        print(f"\n📝 生成SQL语句...")
        sql_statements = []
        sql_statements.append("START TRANSACTION;")
        sql_statements.append("")

        imported_count = 0
        is_budget = sheet_info['type'] == 'budget'

        for rec in records:
            if is_budget:
                # 预算记录
                record_key = (rec['family_id'], rec['year'], rec['minor_category_name'], rec['currency'])

                # 跳过已存在的记录
                if record_key in existing_keys:
                    continue

                notes = rec['notes'].replace("'", "''")

                sql = f"""INSERT INTO expense_budgets (
  family_id,
  budget_year,
  minor_category_id,
  budget_amount,
  currency,
  notes,
  created_at,
  updated_at
) VALUES (
  {rec['family_id']},
  {rec['year']},
  {rec['minor_category_id']},
  {rec['amount']},
  '{rec['currency']}',
  '{notes}',
  NOW(),
  NOW()
);"""
            else:
                # 费用记录
                # 唯一约束：(family_id, expense_period, minor_category_id, currency)
                record_key = (rec['family_id'], rec['expense_period'], rec['minor_category_id'], rec['currency'])

                # 跳过已存在的记录
                if record_key in existing_keys:
                    continue

                description = rec['description'].replace("'", "''")

                sql = f"""INSERT INTO expense_records (
  family_id,
  expense_year,
  expense_month,
  expense_period,
  major_category_id,
  minor_category_id,
  amount,
  currency,
  expense_type,
  description,
  created_at
) VALUES (
  {rec['family_id']},
  {rec['year']},
  {rec['month']},
  '{rec['expense_period']}',
  (SELECT major_category_id FROM expense_categories_minor WHERE id = {rec['minor_category_id']}),
  {rec['minor_category_id']},
  {rec['amount']},
  '{rec['currency']}',
  'ACTUAL',
  '{description}',
  NOW()
);"""

            sql_statements.append(sql)
            imported_count += 1

        sql_statements.append("")
        sql_statements.append("COMMIT;")

        # 执行导入
        print(f"\n🚀 开始导入 {imported_count} 条新记录...")

        sql_script = '\n'.join(sql_statements)
        success = execute_sql(db_config, sql_script)

        if success:
            print(f"✅ 导入成功！")
            total_stats['imported_records'] += imported_count
            total_stats['sheets_processed'] += 1
            import_summary.append({
                'sheet': sheet_name,
                'total': len(records),
                'existing': existing_count,
                'new': new_count,
                'imported': imported_count,
                'status': 'success'
            })
        else:
            print(f"❌ 导入失败！")
            total_stats['failed_records'] += imported_count
            import_summary.append({
                'sheet': sheet_name,
                'total': len(records),
                'existing': existing_count,
                'new': new_count,
                'imported': 0,
                'status': 'failed'
            })

    # 打印总结
    print(f"\n{'='*80}")
    print(f"📊 导入总结")
    print(f"{'='*80}")
    print(f"\n处理的Sheets:")
    for summary in import_summary:
        status_icon = {
            'success': '✅',
            'skipped': '⊝',
            'dry-run': '🔍',
            'failed': '❌'
        }.get(summary['status'], '?')

        print(f"\n  {status_icon} {summary['sheet']}")
        print(f"     总记录: {summary['total']}")
        print(f"     已存在: {summary['existing']}")
        print(f"     新记录: {summary['new']}")
        print(f"     已导入: {summary['imported']}")

    print(f"\n总计:")
    print(f"  📋 处理sheets: {len(import_summary)}")
    print(f"  📝 总记录数: {total_stats['total_records']}")
    print(f"  ⊝ 已存在: {total_stats['existing_records']} (跳过)")
    print(f"  ✨ 新记录: {total_stats['new_records']}")
    if not dry_run:
        print(f"  ✅ 已导入: {total_stats['imported_records']}")
        if total_stats['failed_records'] > 0:
            print(f"  ❌ 导入失败: {total_stats['failed_records']}")

    print(f"\n{'='*80}")

    if dry_run:
        print("🔍 这是检查模式，没有实际执行导入")
        print("   如需导入，请去掉 --dry-run 参数")
    elif total_stats['imported_records'] > 0:
        print("✅ 导入完成！")
    elif total_stats['existing_records'] > 0 and total_stats['new_records'] == 0:
        print("✓ 所有记录已存在，无需导入")
    else:
        print("⚠️  没有记录被导入")

    print()

def main():
    parser = argparse.ArgumentParser(
        description='从预览Excel文件导入数据到数据库',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
示例:
  # 导入preview_2024.xlsx中的所有数据
  python3 import_from_preview.py --file preview_2024.xlsx

  # 只检查，不实际导入
  python3 import_from_preview.py --file preview_2024.xlsx --dry-run

  # 只导入特定的sheets
  python3 import_from_preview.py --file preview_2024.xlsx --sheets "2024-expense-USD,2024-budgets-USD"
        """
    )

    parser.add_argument('--file', type=str, required=True,
                        help='预览Excel文件路径 (例如: preview_2024.xlsx)')
    parser.add_argument('--sheets', type=str, default=None,
                        help='指定要导入的sheets，逗号分隔 (可选，默认导入所有sheets)')
    parser.add_argument('--dry-run', action='store_true',
                        help='只检查不执行导入')

    args = parser.parse_args()

    import_from_preview_excel(args.file, args.sheets, args.dry_run)

if __name__ == '__main__':
    main()
