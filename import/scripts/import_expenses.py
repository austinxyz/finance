#!/usr/bin/env python3
"""
Excel支出数据导入工具

使用方法:
  步骤1 - 预览数据:
    python3 import_expenses.py preview --file 2024.xlsx --sheet "2024总帐 (US)" --family 1 --year 2024 --currency USD

  步骤2 - 执行导入:
    python3 import_expenses.py import --preview-file preview_2024_USD.json

参数说明:
  --file: Excel文件路径
  --sheet: Excel工作表名称
  --family: 家庭ID (默认: 1)
  --year: 年份
  --currency: 货币 (USD/CNY)
  --preview-file: 预览文件路径（导入时使用）
  --mapping: 分类映射文件 (默认: category_mapping_corrected.json)
"""

import pandas as pd
import json
import sys
import argparse
from datetime import datetime
from collections import defaultdict
import os

# 默认映射文件
DEFAULT_MAPPING_FILE = 'category_mapping_corrected.json'

def load_mapping(mapping_file):
    """加载分类映射表"""
    if not os.path.exists(mapping_file):
        print(f"❌ 错误: 映射文件不存在: {mapping_file}")
        sys.exit(1)

    with open(mapping_file, 'r', encoding='utf-8') as f:
        return json.load(f)

def preview_data(excel_file, sheet_name, family_id, year, currency, mapping_file):
    """步骤1: 预览Excel数据，生成导入预览"""

    print("="*80)
    print("步骤1: 数据预览")
    print("="*80)
    print(f"Excel文件: {excel_file}")
    print(f"工作表: {sheet_name}")
    print(f"家庭ID: {family_id}")
    print(f"年份: {year}")
    print(f"货币: {currency}")
    print(f"映射文件: {mapping_file}")
    print()

    # 检查文件
    if not os.path.exists(excel_file):
        print(f"❌ 错误: Excel文件不存在: {excel_file}")
        sys.exit(1)

    # 加载映射
    mapping = load_mapping(mapping_file)

    # 读取Excel
    try:
        df = pd.read_excel(excel_file, sheet_name=sheet_name, header=None)
        print(f"✅ 成功读取Excel: {len(df)} 行")
    except Exception as e:
        print(f"❌ 错误: 无法读取Excel文件: {e}")
        sys.exit(1)

    # 分析Excel结构
    print("\n分析Excel结构...")
    print(f"  行数: {len(df)}")
    print(f"  列数: {len(df.columns)}")

    # 检测数据段（1-6月 和 7-12月）
    sections = []

    # 检查是否有7-12月数据段
    has_second_section = False
    for idx in range(40, min(60, len(df))):
        row = df.iloc[idx]
        # 检查是否有"七月"、"八月"等标记
        for col_idx in range(8, 14):
            val = str(row.iloc[col_idx]) if pd.notna(row.iloc[col_idx]) else ""
            if "七月" in val or "八月" in val:
                has_second_section = True
                section2_start = idx + 1
                break
        if has_second_section:
            break

    # 定义数据段
    if has_second_section:
        sections = [
            {'name': '1-6月', 'start_row': 1, 'end_row': 40, 'month_offset': 0, 'exclude_rows': set(range(40, 47))},
            {'name': '7-12月', 'start_row': section2_start, 'end_row': 86, 'month_offset': 6, 'exclude_rows': set()}
        ]
        print(f"  检测到两个数据段: 1-6月 和 7-12月")
    else:
        # 只有单个数据段，可能是1-6月或7-12月
        # 检查第一行的月份标记
        first_month_row = df.iloc[0]
        is_second_half = False
        for col_idx in range(8, 14):
            val = str(first_month_row.iloc[col_idx]) if pd.notna(first_month_row.iloc[col_idx]) else ""
            if "七月" in val or "八月" in val:
                is_second_half = True
                break

        if is_second_half:
            sections = [
                {'name': '7-12月', 'start_row': 1, 'end_row': len(df), 'month_offset': 6, 'exclude_rows': set()}
            ]
            print(f"  检测到单个数据段: 7-12月")
        else:
            sections = [
                {'name': '1-6月', 'start_row': 1, 'end_row': len(df), 'month_offset': 0, 'exclude_rows': set()}
            ]
            print(f"  检测到单个数据段: 1-6月")

    # 月份列映射 (Column I-N = indices 8-13)
    month_cols = {
        8: 1, 9: 2, 10: 3, 11: 4, 12: 5, 13: 6
    }

    # 解析数据
    import_records = []
    record_id = 1
    skipped_categories = set()
    unmapped_categories = set()

    for section in sections:
        for idx in range(section['start_row'], min(section['end_row'], len(df))):
            if idx in section['exclude_rows']:
                continue

            row = df.iloc[idx]

            # Column C (index 2) 是分类
            excel_subcat = str(row.iloc[2]).strip() if pd.notna(row.iloc[2]) else None

            if not excel_subcat or excel_subcat == 'nan':
                continue

            # 跳过数字行和汇总行
            if excel_subcat.replace('.', '').replace('-', '').isdigit() or excel_subcat == '小类':
                continue

            # 检查映射
            if excel_subcat not in mapping:
                unmapped_categories.add(excel_subcat)
                continue

            map_info = mapping[excel_subcat]

            # 跳过排除的分类
            if map_info['confidence'] == 'low' and 'note' in map_info and '排除' in map_info['note']:
                skipped_categories.add(excel_subcat)
                continue

            # 提取每个月的金额
            for col_idx, month_in_section in month_cols.items():
                amount = row.iloc[col_idx]

                # 跳过空值或0
                if pd.isna(amount) or amount == 0:
                    continue

                try:
                    amount_value = float(amount)
                except:
                    continue

                # 计算实际月份
                month = month_in_section + section['month_offset']
                expense_period = f"{year}-{month:02d}"

                # 创建导入记录
                record = {
                    'id': record_id,
                    'family_id': family_id,
                    'year': year,
                    'month': month,
                    'expense_period': expense_period,
                    'minor_category_id': map_info['db_id'],
                    'minor_category_name': map_info['db_name'],
                    'excel_category': excel_subcat,
                    'amount': amount_value,
                    'currency': currency,
                    'description': f"从Excel导入 - {excel_subcat}",
                    'mapping_confidence': map_info['confidence'],
                    'excel_row': idx,
                    'section': section['name']
                }

                import_records.append(record)
                record_id += 1

    # 生成预览报告
    print("\n" + "="*80)
    print("预览结果")
    print("="*80)
    print(f"总记录数: {len(import_records)}")

    if unmapped_categories:
        print(f"\n⚠️  未映射的分类 ({len(unmapped_categories)}个):")
        for cat in sorted(unmapped_categories):
            print(f"  - {cat}")

    if skipped_categories:
        print(f"\n✓ 已跳过的分类 ({len(skipped_categories)}个):")
        for cat in sorted(skipped_categories):
            print(f"  - {cat}")

    if len(import_records) == 0:
        print("\n❌ 错误: 没有找到可导入的数据")
        sys.exit(1)

    # 统计汇总
    total_amount = sum(r['amount'] for r in import_records)
    print(f"\n总金额: {currency} {total_amount:,.2f}")

    # 按月统计
    month_summary = defaultdict(lambda: {'count': 0, 'total': 0})
    for rec in import_records:
        month_summary[rec['month']]['count'] += 1
        month_summary[rec['month']]['total'] += rec['amount']

    print("\n按月统计:")
    print("-"*60)
    month_names = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']
    for month in sorted(month_summary.keys()):
        info = month_summary[month]
        print(f"  {month:2d} - {month_names[month-1]:<5s}: {info['count']:2d} records, {currency} {info['total']:>10,.2f}")

    # 按分类统计（Top 10）
    category_summary = defaultdict(lambda: {'count': 0, 'total': 0})
    for rec in import_records:
        category_summary[rec['minor_category_name']]['count'] += 1
        category_summary[rec['minor_category_name']]['total'] += rec['amount']

    print("\nTop 10 支出分类:")
    print("-"*60)
    for cat, info in sorted(category_summary.items(), key=lambda x: x[1]['total'], reverse=True)[:10]:
        print(f"  {cat:<20s}: {info['count']:2d} records, {currency} {info['total']:>10,.2f}")

    # 保存预览文件
    preview_filename = f"preview_{year}_{currency}.json"
    with open(preview_filename, 'w', encoding='utf-8') as f:
        json.dump({
            'metadata': {
                'excel_file': excel_file,
                'sheet_name': sheet_name,
                'family_id': family_id,
                'year': year,
                'currency': currency,
                'total_records': len(import_records),
                'total_amount': total_amount,
                'created_at': datetime.now().isoformat()
            },
            'records': import_records
        }, f, ensure_ascii=False, indent=2)

    # 保存Excel预览
    excel_preview_filename = f"preview_{year}_{currency}.xlsx"
    df_preview = pd.DataFrame(import_records)
    df_export = df_preview[['id', 'month', 'expense_period', 'excel_category', 'minor_category_name', 'amount', 'section']]
    df_export.columns = ['ID', '月份', '期间', 'Excel分类', '数据库分类', f'金额({currency})', '时间段']
    df_export.to_excel(excel_preview_filename, index=False, engine='openpyxl')

    print("\n" + "="*80)
    print("✅ 预览完成")
    print("="*80)
    print(f"预览文件已生成:")
    print(f"  - {preview_filename} (JSON格式)")
    print(f"  - {excel_preview_filename} (Excel格式)")
    print()
    print("下一步:")
    print(f"  1. 查看预览文件确认数据正确")
    print(f"  2. 执行导入命令:")
    print(f"     python3 import_expenses.py import --preview-file {preview_filename}")
    print()

def import_data(preview_file):
    """步骤2: 从预览文件导入数据到数据库"""

    print("="*80)
    print("步骤2: 执行导入")
    print("="*80)
    print(f"预览文件: {preview_file}")
    print()

    # 检查预览文件
    if not os.path.exists(preview_file):
        print(f"❌ 错误: 预览文件不存在: {preview_file}")
        sys.exit(1)

    # 加载预览数据
    with open(preview_file, 'r', encoding='utf-8') as f:
        preview_data = json.load(f)

    metadata = preview_data['metadata']
    records = preview_data['records']

    print(f"元数据:")
    print(f"  Excel文件: {metadata['excel_file']}")
    print(f"  工作表: {metadata['sheet_name']}")
    print(f"  家庭ID: {metadata['family_id']}")
    print(f"  年份: {metadata['year']}")
    print(f"  货币: {metadata['currency']}")
    print(f"  记录数: {metadata['total_records']}")
    print(f"  总金额: {metadata['currency']} {metadata['total_amount']:,.2f}")
    print()

    # 生成SQL
    sql_statements = []
    sql_statements.append("START TRANSACTION;")
    sql_statements.append("")

    for rec in records:
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

    sql_statements.append("")
    sql_statements.append("COMMIT;")

    # 保存SQL文件
    sql_filename = f"import_{metadata['year']}_{metadata['currency']}.sql"
    with open(sql_filename, 'w', encoding='utf-8') as f:
        f.write('\n'.join(sql_statements))

    print(f"✅ SQL脚本已生成: {sql_filename}")
    print(f"   包含 {len(records)} 条INSERT语句")
    print()

    # 询问是否执行
    print("⚠️  准备执行导入到数据库")
    print(f"   将导入 {len(records)} 条记录")
    print(f"   总金额: {metadata['currency']} {metadata['total_amount']:,.2f}")
    print()

    # 在实际执行前需要数据库连接信息
    print("数据库连接配置:")
    print("  请确保 backend/.env 文件存在并包含正确的数据库连接信息")
    print()

    response = input("是否继续执行导入? (yes/no): ").strip().lower()

    if response != 'yes':
        print("\n❌ 导入已取消")
        print(f"   SQL脚本已保存: {sql_filename}")
        print(f"   如需手动导入，可执行: mysql ... < {sql_filename}")
        return

    # 执行SQL
    print("\n开始执行导入...")

    # 加载数据库配置
    import subprocess

    # 从.env加载配置
    env_file = '../backend/.env'
    if not os.path.exists(env_file):
        print(f"❌ 错误: 找不到数据库配置文件: {env_file}")
        sys.exit(1)

    # 解析.env文件
    db_config = {}
    with open(env_file, 'r') as f:
        for line in f:
            line = line.strip()
            if line and not line.startswith('#') and '=' in line:
                key, value = line.split('=', 1)
                db_config[key] = value

    # 解析DB_URL
    db_url = db_config.get('DB_URL', '')
    import re

    match = re.search(r'//([^:]+):(\d+)/([^?]+)', db_url)
    if not match:
        print(f"❌ 错误: 无法解析数据库URL: {db_url}")
        sys.exit(1)

    db_host = match.group(1)
    db_port = match.group(2)
    db_name = match.group(3)
    db_user = db_config.get('DB_USERNAME', '')
    db_pass = db_config.get('DB_PASSWORD', '')

    # 查找mysql客户端
    try:
        mysql_prefix = subprocess.check_output(['brew', '--prefix', 'mysql-client'], text=True).strip()
        mysql_client = f"{mysql_prefix}/bin/mysql"
    except:
        mysql_client = 'mysql'

    # 执行SQL
    cmd = [
        mysql_client,
        f'-h{db_host}',
        f'-P{db_port}',
        f'-u{db_user}',
        f'-p{db_pass}',
        db_name
    ]

    try:
        with open(sql_filename, 'r') as f:
            result = subprocess.run(cmd, stdin=f, capture_output=True, text=True)

        if result.returncode == 0:
            print("\n✅ 导入成功！")
            print(f"   已导入 {len(records)} 条记录")
            print(f"   总金额: {metadata['currency']} {metadata['total_amount']:,.2f}")

            # 生成成功报告
            report_filename = f"import_success_{metadata['year']}_{metadata['currency']}.txt"
            with open(report_filename, 'w', encoding='utf-8') as f:
                f.write(f"导入成功报告\n")
                f.write(f"="*60 + "\n")
                f.write(f"导入时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
                f.write(f"Excel文件: {metadata['excel_file']}\n")
                f.write(f"工作表: {metadata['sheet_name']}\n")
                f.write(f"年份: {metadata['year']}\n")
                f.write(f"货币: {metadata['currency']}\n")
                f.write(f"记录数: {metadata['total_records']}\n")
                f.write(f"总金额: {metadata['currency']} {metadata['total_amount']:,.2f}\n")

            print(f"\n✅ 成功报告已生成: {report_filename}")
        else:
            print(f"\n❌ 导入失败")
            print(f"错误信息: {result.stderr}")
            sys.exit(1)

    except Exception as e:
        print(f"\n❌ 执行导入时出错: {e}")
        sys.exit(1)

def main():
    parser = argparse.ArgumentParser(
        description='Excel支出数据导入工具',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=__doc__
    )

    subparsers = parser.add_subparsers(dest='command', help='命令')

    # 预览命令
    preview_parser = subparsers.add_parser('preview', help='预览Excel数据')
    preview_parser.add_argument('--file', required=True, help='Excel文件路径')
    preview_parser.add_argument('--sheet', required=True, help='工作表名称')
    preview_parser.add_argument('--family', type=int, default=1, help='家庭ID (默认: 1)')
    preview_parser.add_argument('--year', type=int, required=True, help='年份')
    preview_parser.add_argument('--currency', required=True, choices=['USD', 'CNY'], help='货币')
    preview_parser.add_argument('--mapping', default=DEFAULT_MAPPING_FILE, help='分类映射文件')

    # 导入命令
    import_parser = subparsers.add_parser('import', help='导入数据到数据库')
    import_parser.add_argument('--preview-file', required=True, help='预览文件路径')

    args = parser.parse_args()

    if args.command == 'preview':
        preview_data(
            excel_file=args.file,
            sheet_name=args.sheet,
            family_id=args.family,
            year=args.year,
            currency=args.currency,
            mapping_file=args.mapping
        )
    elif args.command == 'import':
        import_data(preview_file=args.preview_file)
    else:
        parser.print_help()
        sys.exit(1)

if __name__ == '__main__':
    main()
