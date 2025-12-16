#!/usr/bin/env python3
"""
Excel数据导入工具 - 统一入口

支持的命令:
  preview   - 生成预览Excel文件
  check     - 检查哪些记录是新的（不会导入）
  import    - 导入数据到数据库
  clean     - 清理临时目录

工作机制:
  - 为每个年份创建临时目录（如 2024/）
  - 所有中间文件存放在临时目录中
  - 原始Excel保持在import/目录
  - 使用clean命令清理临时目录

使用示例:
  # 生成2024年的预览文件
  python3 import_from_excel.py preview --year 2024

  # 检查新记录
  python3 import_from_excel.py check --year 2024

  # 导入数据
  python3 import_from_excel.py import --year 2024

  # 清理临时目录
  python3 import_from_excel.py clean --year 2024
"""

import sys
import os
import argparse
import subprocess
import shutil

def get_temp_dir(year):
    """获取年份对应的临时目录路径"""
    import_dir = os.path.dirname(os.path.abspath(__file__))
    return os.path.join(import_dir, f'temp_{year}')

def ensure_temp_dir(year):
    """确保临时目录存在"""
    temp_dir = get_temp_dir(year)
    os.makedirs(temp_dir, exist_ok=True)
    return temp_dir

def main():
    parser = argparse.ArgumentParser(
        description='Excel数据导入工具',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
命令说明:
  preview   生成预览Excel文件（包含费用和预算数据）
  check     检查新记录（不执行导入）
  import    导入数据到数据库
  clean     清理临时目录

使用示例:
  # 1. 生成预览
  %(prog)s preview --year 2024

  # 2. 检查新记录
  %(prog)s check --year 2024

  # 3. 导入数据
  %(prog)s import --year 2024

  # 导入指定sheets
  %(prog)s import --year 2024 --sheets 2024-expense-USD 2024-budgets-USD

  # 清理临时目录
  %(prog)s clean --year 2024
        """
    )

    subparsers = parser.add_subparsers(dest='command', help='子命令')

    # preview 子命令
    preview_parser = subparsers.add_parser('preview', help='生成预览Excel文件')
    preview_parser.add_argument('--year', type=int, required=True, help='年份（如2024）')
    preview_parser.add_argument('--family', type=int, default=1, help='家庭ID（默认:1）')
    preview_parser.add_argument('--mapping', type=str, default='config/category_mapping_corrected.json',
                              help='分类映射文件')

    # check 子命令
    check_parser = subparsers.add_parser('check', help='检查新记录')
    check_parser.add_argument('--year', type=int, required=True, help='年份（如2024）')
    check_parser.add_argument('--sheets', nargs='+', help='指定要检查的sheets')

    # import 子命令
    import_parser = subparsers.add_parser('import', help='导入数据到数据库')
    import_parser.add_argument('--year', type=int, required=True, help='年份（如2024）')
    import_parser.add_argument('--sheets', nargs='+', help='指定要导入的sheets')
    import_parser.add_argument('--dry-run', action='store_true', help='检查模式（不实际导入）')

    # clean 子命令
    clean_parser = subparsers.add_parser('clean', help='清理临时目录')
    clean_parser.add_argument('--year', type=int, required=True, help='年份（如2024）')
    clean_parser.add_argument('--force', action='store_true', help='强制删除（不询问）')
    clean_parser.add_argument('--all', action='store_true', help='同时删除原始Excel文件')

    args = parser.parse_args()

    if not args.command:
        parser.print_help()
        sys.exit(1)

    # 调用对应的脚本
    script_dir = os.path.join(os.path.dirname(__file__), 'scripts')

    if args.command == 'preview':
        # 创建临时目录
        temp_dir = ensure_temp_dir(args.year)
        output_file = os.path.join(temp_dir, f'preview_{args.year}.xlsx')

        print(f"📁 临时目录: {temp_dir}")
        print(f"📄 预览文件: {output_file}\n")

        # 调用 scripts/import_all_preview.py
        cmd = [
            'python3', os.path.join(script_dir, 'import_all_preview.py'),
            '--year', str(args.year),
            '--family', str(args.family),
            '--mapping', args.mapping,
            '--output', output_file
        ]
        print(f"执行: {' '.join(cmd)}\n")
        return subprocess.call(cmd)

    elif args.command == 'check':
        # 获取预览文件路径
        temp_dir = get_temp_dir(args.year)
        preview_file = os.path.join(temp_dir, f'preview_{args.year}.xlsx')

        if not os.path.exists(preview_file):
            print(f"❌ 错误: 预览文件不存在: {preview_file}")
            print(f"💡 请先运行: python3 import_from_excel.py preview --year {args.year}")
            return 1

        # 调用 scripts/import_from_preview.py --dry-run
        cmd = [
            'python3', os.path.join(script_dir, 'import_from_preview.py'),
            '--file', preview_file,
            '--dry-run'
        ]
        if args.sheets:
            cmd.extend(['--sheets'] + args.sheets)
        print(f"执行: {' '.join(cmd)}\n")
        return subprocess.call(cmd)

    elif args.command == 'import':
        # 获取预览文件路径
        temp_dir = get_temp_dir(args.year)
        preview_file = os.path.join(temp_dir, f'preview_{args.year}.xlsx')

        if not os.path.exists(preview_file):
            print(f"❌ 错误: 预览文件不存在: {preview_file}")
            print(f"💡 请先运行: python3 import_from_excel.py preview --year {args.year}")
            return 1

        # 调用 scripts/import_from_preview.py
        cmd = [
            'python3', os.path.join(script_dir, 'import_from_preview.py'),
            '--file', preview_file
        ]
        if args.sheets:
            cmd.extend(['--sheets'] + args.sheets)
        if args.dry_run:
            cmd.append('--dry-run')
        print(f"执行: {' '.join(cmd)}\n")
        return subprocess.call(cmd)

    elif args.command == 'clean':
        # 清理临时目录和可选的原始文件
        import_dir = os.path.dirname(os.path.abspath(__file__))
        temp_dir = get_temp_dir(args.year)
        excel_file = os.path.join(import_dir, f'{args.year}.xlsx')

        # 检查临时目录
        temp_exists = os.path.exists(temp_dir)
        excel_exists = os.path.exists(excel_file)

        if not temp_exists and not (args.all and excel_exists):
            print(f"ℹ️  没有可清理的文件")
            if not temp_exists:
                print(f"  • 临时目录不存在: {temp_dir}")
            return 0

        # 显示要删除的内容
        print(f"📋 将要删除的内容:\n")
        total_size = 0

        if temp_exists:
            files = os.listdir(temp_dir)
            if files:
                print(f"📁 临时目录: {temp_dir}")
                for f in files:
                    file_path = os.path.join(temp_dir, f)
                    if os.path.isfile(file_path):
                        size = os.path.getsize(file_path)
                        total_size += size
                        print(f"  • {f} ({size:,} bytes)")
                print()

        if args.all and excel_exists:
            size = os.path.getsize(excel_file)
            total_size += size
            print(f"📄 原始文件: {excel_file}")
            print(f"  • {os.path.basename(excel_file)} ({size:,} bytes)")
            print()

        print(f"💾 总大小: {total_size:,} bytes\n")

        # 询问确认
        if not args.force:
            if args.all and excel_exists:
                print("⚠️  警告: --all 将删除原始Excel文件！")
            response = input(f"确认删除? [y/N]: ")
            if response.lower() not in ['y', 'yes']:
                print("❌ 已取消")
                return 0

        # 删除临时目录
        if temp_exists:
            try:
                shutil.rmtree(temp_dir)
                print(f"✅ 已删除临时目录: {temp_dir}")
            except Exception as e:
                print(f"❌ 删除临时目录失败: {e}")
                return 1

        # 删除原始文件（如果指定了--all）
        if args.all and excel_exists:
            try:
                os.remove(excel_file)
                print(f"✅ 已删除原始文件: {excel_file}")
            except Exception as e:
                print(f"❌ 删除原始文件失败: {e}")
                return 1

        print(f"\n🎉 清理完成!")
        return 0

if __name__ == '__main__':
    sys.exit(main() or 0)
