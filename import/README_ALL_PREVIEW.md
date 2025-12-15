# 整合预览工具使用说明

## 概述

`import_all_preview.py` 是一个整合的预览工具，可以一次性生成某一年的所有预览数据（费用+预算，USD+CNY），并整合到一个Excel文件中方便查看。

## 功能

输入年份后，脚本会：
1. 自动读取 `{year}.xlsx` 文件
2. 生成4个预览sheets：
   - `{year}-expense-USD`: 美国总账的费用预览
   - `{year}-expense-CNY`: 中国总账的费用预览
   - `{year}-budgets-USD`: 美国总账的预算预览
   - `{year}-budgets-CNY`: 中国总账的预算预览
3. 输出一个整合的Excel文件：`preview_{year}.xlsx`

## 使用方法

### 基本用法

```bash
# 生成2024年的预览文件
python3 import_all_preview.py --year 2024
```

### 高级用法

```bash
# 指定输出文件名
python3 import_all_preview.py --year 2024 --output my_preview.xlsx

# 指定家庭ID
python3 import_all_preview.py --year 2024 --family 1

# 指定自定义的分类映射文件
python3 import_all_preview.py --year 2024 --mapping custom_mapping.json
```

## 输出文件

### 整合的Excel预览文件

文件名：`preview_{year}.xlsx`

包含4个sheets，每个sheet都有格式化的表格（带颜色、冻结首行、自动列宽）：

| Sheet名称 | 内容 | 来源 |
|----------|------|------|
| {year}-expense-USD | 美国总账的费用预览 | {year}总帐 (US) |
| {year}-expense-CNY | 中国总账的费用预览 | {year}总帐（中国） |
| {year}-budgets-USD | 美国总账的预算预览 | {year}总帐 (US) |
| {year}-budgets-CNY | 中国总账的预算预览 | {year}总帐（中国） |

**注意**：脚本会自动清理中间生成的JSON和单独的Excel文件，只保留最终的整合预览文件。

## 工作流程

```bash
# 步骤1: 生成预览文件
python3 import_all_preview.py --year 2024

# 步骤2: 打开Excel文件检查数据
open preview_2024.xlsx

# 步骤3: 如需导入数据，重新运行对应的preview命令生成JSON文件
python3 import_expenses.py preview --file 2024.xlsx --sheet "2024总帐 (US)" --family 1 --year 2024 --currency USD
python3 import_expenses.py import --preview-file preview_2024_USD.json
```

### 为什么需要重新生成JSON？

`import_all_preview.py` 脚本专注于生成易读的预览Excel文件，会自动清理中间JSON文件以保持目录整洁。如果确认数据无误需要导入，请使用单独的preview命令重新生成JSON文件。

## 依赖要求

需要安装以下Python包：
```bash
pip install pandas openpyxl
```

## 注意事项

1. **Excel文件命名规范**：输入文件必须命名为 `{year}.xlsx`（如 `2024.xlsx`）
2. **Sheet名称规范**：
   - 美国总账: `{year}总帐 (US)`（英文括号）
   - 中国总账: `{year}总帐（中国）`（中文括号）
3. **跳过失败的任务**：如果某个sheet不存在或处理失败，脚本会跳过该任务，继续处理其他sheets
4. **分类映射**：默认使用 `category_mapping_corrected.json`，确保该文件存在

## 示例输出

```
================================================================================
✅ 预览文件生成成功!
================================================================================
📁 文件路径: /Users/yanzxu/claude/finance/import/preview_2024.xlsx
📊 包含sheets: 4

Sheet详情:
  • 2024-expense-USD: 108 条记录
  • 2024-expense-CNY: 25 条记录
  • 2024-budgets-USD: 22 条记录
  • 2024-budgets-CNY: 20 条记录

🧹 清理中间文件...
  ✓ 已删除: preview_2024_USD.json
  ✓ 已删除: preview_2024_CNY.json
  ✓ 已删除: budget_preview_2024_USD.json
  ✓ 已删除: budget_preview_2024_CNY.json
  ✓ 已删除: preview_2024_USD.xlsx
  ✓ 已删除: preview_2024_CNY.xlsx
  ✓ 已删除: budget_preview_2024_USD.xlsx
  ✓ 已删除: budget_preview_2024_CNY.xlsx
  共清理 8 个中间文件

💡 下一步:
  1. 打开 preview_2024.xlsx 检查预览数据
  2. 确认数据无误即可

注意: 如需导入数据，请重新运行对应的preview命令生成JSON文件
```

## 与单独工具的对比

### 传统方式（需要4个命令）

```bash
python3 import_expenses.py preview --file 2024.xlsx --sheet "2024总帐 (US)" --family 1 --year 2024 --currency USD
python3 import_expenses.py preview --file 2024.xlsx --sheet "2024总帐（中国）" --family 1 --year 2024 --currency CNY
python3 import_budgets.py preview --file 2024.xlsx --sheet "2024总帐 (US)" --family 1 --year 2024 --currency USD
python3 import_budgets.py preview --file 2024.xlsx --sheet "2024总帐（中国）" --family 1 --year 2024 --currency CNY
```

### 整合方式（只需1个命令）

```bash
python3 import_all_preview.py --year 2024
```

## 优势

1. ✅ **一键生成**：只需一个命令即可生成所有预览
2. ✅ **整合视图**：所有预览数据在一个Excel文件中，方便对比查看
3. ✅ **格式化输出**：自动格式化的Excel表格，提升阅读体验
4. ✅ **容错处理**：某个sheet失败不影响其他sheets的生成
5. ✅ **自动清理**：自动删除中间文件，保持目录整洁
6. ✅ **专注预览**：专为数据检查优化，如需导入再单独生成JSON
