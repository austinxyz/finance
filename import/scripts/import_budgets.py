#!/usr/bin/env python3
"""
Excel预算数据导入工具

使用方法:
  步骤1 - 预览数据:
    python3 import_budgets.py preview --file 2024.xlsx --sheet "2024总帐 (US)" --family 1 --year 2024 --currency USD

  步骤2 - 执行导入:
    python3 import_budgets.py import --preview-file budget_preview_2024_USD.json

参数说明:
  --file: Excel文件路径
  --sheet: Excel工作表名称
  --family: 家庭ID (默认: 1)
  --year: 年份
  --currency: 货币 (USD/CNY)
  --preview-file: 预览文件路径（导入时使用）
  --mapping: 分类映射文件 (默认: category_mapping_corrected.json)
  --start-row: 预算数据起始行 (默认: 86, 即第87行)
"""

import pandas as pd
import json
import sys
import argparse
from datetime import datetime
from collections import defaultdict
import os

# 默认配置
DEFAULT_MAPPING_FILE = 'category_mapping_corrected.json'
DEFAULT_START_ROW = 86  # Excel第87行，索引86

def load_mapping(mapping_file):
    """加载分类映射表"""
    if not os.path.exists(mapping_file):
        print(f"❌ 错误: 映射文件不存在: {mapping_file}")
        sys.exit(1)

    with open(mapping_file, 'r', encoding='utf-8') as f:
        return json.load(f)

def preview_data(excel_file, sheet_name, family_id, year, currency, mapping_file, start_row):
    """步骤1: 预览Excel预算数据"""

    print("="*80)
    print("步骤1: 预算数据预览")
    print("="*80)
    print(f"Excel文件: {excel_file}")
    print(f"工作表: {sheet_name}")
    print(f"家庭ID: {family_id}")
    print(f"年份: {year}")
    print(f"货币: {currency}")
    print(f"映射文件: {mapping_file}")
    print(f"起始行: {start_row + 1} (索引{start_row})")
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

    print(f"\n分析预算数据 (从第{start_row + 1}行开始)...")

    # 解析预算数据
    budget_records = []
    record_id = 1
    skipped_categories = set()
    unmapped_categories = set()
    zero_budgets = []

    for idx in range(start_row, len(df)):
        row = df.iloc[idx]

        # Column C (index 2) 是分类
        excel_subcat = str(row.iloc[2]).strip() if pd.notna(row.iloc[2]) else None

        if not excel_subcat or excel_subcat == 'nan':
            continue

        # Column D (index 3) 是预算值
        budget_value = row.iloc[3]

        # 跳过空值
        if pd.isna(budget_value):
            continue

        try:
            budget_amount = float(budget_value)
        except:
            continue

        # 检查映射
        if excel_subcat not in mapping:
            unmapped_categories.add(excel_subcat)
            continue

        map_info = mapping[excel_subcat]

        # 跳过明确标记为"排除"或"重复"的分类
        if 'note' in map_info and ('排除' in map_info['note'] or '重复' in map_info['note']):
            skipped_categories.add(excel_subcat)
            continue

        # 记录预算为0的分类（仅供参考，仍然导入）
        if budget_amount == 0:
            zero_budgets.append(excel_subcat)

        # 创建预算记录
        record = {
            'id': record_id,
            'family_id': family_id,
            'year': year,
            'minor_category_id': map_info['db_id'],
            'minor_category_name': map_info['db_name'],
            'excel_category': excel_subcat,
            'budget_amount': budget_amount,
            'currency': currency,
            'description': f"从Excel导入预算 - {excel_subcat}",
            'mapping_confidence': map_info['confidence'],
            'excel_row': idx
        }

        budget_records.append(record)
        record_id += 1

    # 生成预览报告
    print("\n" + "="*80)
    print("预览结果")
    print("="*80)
    print(f"总预算记录数: {len(budget_records)}")

    if unmapped_categories:
        print(f"\n⚠️  未映射的分类 ({len(unmapped_categories)}个):")
        for cat in sorted(unmapped_categories):
            print(f"  - {cat}")

    if skipped_categories:
        print(f"\n✓ 已跳过的分类 ({len(skipped_categories)}个):")
        for cat in sorted(skipped_categories):
            print(f"  - {cat}")

    if zero_budgets:
        print(f"\n📝 预算为0的分类 ({len(zero_budgets)}个，已包含):")
        for cat in sorted(zero_budgets):
            print(f"  - {cat}")

    if len(budget_records) == 0:
        print("\n❌ 错误: 没有找到可导入的预算数据")
        sys.exit(1)

    # 统计汇总
    total_budget = sum(r['budget_amount'] for r in budget_records)
    non_zero_budget = sum(r['budget_amount'] for r in budget_records if r['budget_amount'] > 0)

    print(f"\n总预算金额: {currency} {total_budget:,.2f}")
    print(f"非零预算金额: {currency} {non_zero_budget:,.2f}")
    print(f"预算为0的分类: {len(zero_budgets)}个")

    # 按分类统计（Top 10非零预算）
    category_budgets = [(r['minor_category_name'], r['budget_amount'])
                        for r in budget_records if r['budget_amount'] > 0]
    category_budgets.sort(key=lambda x: x[1], reverse=True)

    print(f"\nTop 10 预算分类:")
    print("-"*60)
    for cat, amount in category_budgets[:10]:
        print(f"  {cat:<20s}: {currency} {amount:>10,.2f}")

    # 保存预览文件
    preview_filename = f"budget_preview_{year}_{currency}.json"
    with open(preview_filename, 'w', encoding='utf-8') as f:
        json.dump({
            'metadata': {
                'excel_file': excel_file,
                'sheet_name': sheet_name,
                'family_id': family_id,
                'year': year,
                'currency': currency,
                'total_records': len(budget_records),
                'total_budget': total_budget,
                'non_zero_budget': non_zero_budget,
                'zero_count': len(zero_budgets),
                'created_at': datetime.now().isoformat()
            },
            'records': budget_records
        }, f, ensure_ascii=False, indent=2)

    # 保存Excel预览
    excel_preview_filename = f"budget_preview_{year}_{currency}.xlsx"
    df_preview = pd.DataFrame(budget_records)
    df_export = df_preview[['id', 'excel_category', 'minor_category_name', 'budget_amount']]
    df_export.columns = ['ID', 'Excel分类', '数据库分类', f'预算金额({currency})']
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
    print(f"     python3 import_budgets.py import --preview-file {preview_filename}")
    print()

def import_data(preview_file):
    """步骤2: 从预览文件导入预算数据到数据库"""

    print("="*80)
    print("步骤2: 执行预算导入")
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
    print(f"  预算记录数: {metadata['total_records']}")
    print(f"  总预算: {metadata['currency']} {metadata['total_budget']:,.2f}")
    print(f"  非零预算: {metadata['currency']} {metadata['non_zero_budget']:,.2f}")
    print(f"  预算为0: {metadata['zero_count']}个分类")
    print()

    # 生成SQL
    sql_statements = []
    sql_statements.append("START TRANSACTION;")
    sql_statements.append("")

    for rec in records:
        description = rec['description'].replace("'", "''")

        # 预算导入到expense_budgets表
        sql = f"""INSERT INTO expense_budgets (
  family_id,
  budget_year,
  minor_category_id,
  budget_amount,
  currency,
  notes,
  created_at
) VALUES (
  {rec['family_id']},
  {rec['year']},
  {rec['minor_category_id']},
  {rec['budget_amount']},
  '{rec['currency']}',
  '{description}',
  NOW()
);"""

        sql_statements.append(sql)

    sql_statements.append("")
    sql_statements.append("COMMIT;")

    # 保存SQL文件
    sql_filename = f"import_budget_{metadata['year']}_{metadata['currency']}.sql"
    with open(sql_filename, 'w', encoding='utf-8') as f:
        f.write('\n'.join(sql_statements))

    print(f"✅ SQL脚本已生成: {sql_filename}")
    print(f"   包含 {len(records)} 条INSERT语句")
    print()

    # 执行SQL
    print("开始执行预算导入...")

    # 加载数据库配置
    import subprocess

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
            print("\n✅ 预算导入成功！")
            print(f"   已导入 {len(records)} 条预算记录")
            print(f"   总预算: {metadata['currency']} {metadata['total_budget']:,.2f}")
            print(f"   非零预算: {metadata['currency']} {metadata['non_zero_budget']:,.2f}")

            # 生成成功报告
            report_filename = f"import_budget_success_{metadata['year']}_{metadata['currency']}.txt"
            with open(report_filename, 'w', encoding='utf-8') as f:
                f.write(f"预算导入成功报告\n")
                f.write(f"="*60 + "\n")
                f.write(f"导入时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
                f.write(f"Excel文件: {metadata['excel_file']}\n")
                f.write(f"工作表: {metadata['sheet_name']}\n")
                f.write(f"年份: {metadata['year']}\n")
                f.write(f"货币: {metadata['currency']}\n")
                f.write(f"记录数: {metadata['total_records']}\n")
                f.write(f"总预算: {metadata['currency']} {metadata['total_budget']:,.2f}\n")
                f.write(f"非零预算: {metadata['currency']} {metadata['non_zero_budget']:,.2f}\n")

            print(f"\n✅ 成功报告已生成: {report_filename}")
        else:
            print(f"\n❌ 预算导入失败")
            print(f"错误信息: {result.stderr}")
            sys.exit(1)

    except Exception as e:
        print(f"\n❌ 执行导入时出错: {e}")
        sys.exit(1)

def main():
    parser = argparse.ArgumentParser(
        description='Excel预算数据导入工具',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=__doc__
    )

    subparsers = parser.add_subparsers(dest='command', help='命令')

    # 预览命令
    preview_parser = subparsers.add_parser('preview', help='预览Excel预算数据')
    preview_parser.add_argument('--file', required=True, help='Excel文件路径')
    preview_parser.add_argument('--sheet', required=True, help='工作表名称')
    preview_parser.add_argument('--family', type=int, default=1, help='家庭ID (默认: 1)')
    preview_parser.add_argument('--year', type=int, required=True, help='年份')
    preview_parser.add_argument('--currency', required=True, choices=['USD', 'CNY'], help='货币')
    preview_parser.add_argument('--mapping', default=DEFAULT_MAPPING_FILE, help='分类映射文件')
    preview_parser.add_argument('--start-row', type=int, default=DEFAULT_START_ROW, help='起始行索引 (默认: 86)')

    # 导入命令
    import_parser = subparsers.add_parser('import', help='导入预算数据到数据库')
    import_parser.add_argument('--preview-file', required=True, help='预览文件路径')

    args = parser.parse_args()

    if args.command == 'preview':
        preview_data(
            excel_file=args.file,
            sheet_name=args.sheet,
            family_id=args.family,
            year=args.year,
            currency=args.currency,
            mapping_file=args.mapping,
            start_row=args.start_row
        )
    elif args.command == 'import':
        import_data(preview_file=args.preview_file)
    else:
        parser.print_help()
        sys.exit(1)

if __name__ == '__main__':
    main()
