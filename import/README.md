# 数据导入工具

Excel数据导入工具，支持费用和预算数据的预览、检查和导入。

## 快速开始（3步）

```bash
cd ~/claude/finance/import

# 1. 生成预览文件
python3 import_from_excel.py preview --year 2024
# 创建 temp_2024/ 目录，生成 temp_2024/preview_2024.xlsx

# 2. 检查新记录
python3 import_from_excel.py check --year 2024
# 显示哪些记录是新的（数据库中不存在）

# 3. 导入数据
python3 import_from_excel.py import --year 2024
# 导入所有新记录到数据库

# 4. 清理临时文件（可选）
python3 import_from_excel.py clean --year 2024
# 删除 temp_2024/ 临时目录
```

**工作原理:**
- 为每个年份创建独立的临时目录（如 `temp_2024/`）
- 所有中间文件（预览、JSON）存放在临时目录
- 临时目录通过 .gitignore 自动排除版本控制
- 原始Excel文件保持在import/目录
- 使用 `clean` 命令一键清理临时目录

## 命令详解

### preview - 生成预览

从原始Excel生成整合的预览文件：

```bash
# 基本用法
python3 import_from_excel.py preview --year 2024

# 指定家庭ID（默认:1）
python3 import_from_excel.py preview --year 2024 --family 1
```

**功能：**
- 自动读取 `{year}.xlsx`
- 创建临时目录 `temp_{year}/`
- 生成4个sheets（费用和预算，USD和CNY）
- 自动分类映射
- 输出到 `temp_{year}/preview_{year}.xlsx`

### check - 检查新记录

检查预览文件中哪些记录是新的：

```bash
# 检查所有sheets
python3 import_from_excel.py check --year 2024

# 只检查指定sheets
python3 import_from_excel.py check --year 2024 --sheets 2024-expense-USD
```

**功能：**
- 自动找到 `temp_{year}/preview_{year}.xlsx`
- 对比预览文件和数据库
- 显示每个sheet的统计（总数、已存在、新记录）
- 不执行实际导入

### import - 导入数据

导入预览文件到数据库：

```bash
# 导入所有sheets
python3 import_from_excel.py import --year 2024

# 只导入指定sheets
python3 import_from_excel.py import --year 2024 --sheets 2024-expense-USD 2024-budgets-USD

# 检查模式（不实际导入）
python3 import_from_excel.py import --year 2024 --dry-run
```

**功能：**
- 自动找到 `temp_{year}/preview_{year}.xlsx`
- 智能去重（自动跳过已存在记录）
- 支持选择性导入
- 支持dry-run模式
- 事务安全（失败自动回滚）

### clean - 清理临时目录

删除年份对应的临时目录和原始文件：

```bash
# 只删除临时目录（交互式）
python3 import_from_excel.py clean --year 2024

# 同时删除原始Excel文件（交互式）
python3 import_from_excel.py clean --year 2024 --all

# 强制删除（不询问）
python3 import_from_excel.py clean --year 2024 --all --force
```

**功能：**
- 显示要删除的内容和总大小
- 默认只删除 `temp_{year}/` 临时目录
- 使用 `--all` 同时删除 `{year}.xlsx` 原始文件
- 使用 `--force` 跳过确认
- 删除前有警告提示（针对原始文件）

## 去重逻辑

工具会自动检测重复记录，通过以下字段组合判断：
- `family_id` - 家庭ID
- `expense_year` - 年份
- `expense_month` - 月份
- `category_id` - 分类ID
- `amount` - 金额

只有全部字段都相同才会被认为是重复记录。

## Excel格式要求

### 原始数据文件 (如 2024.xlsx)

需要包含以下sheets：
- `2024总帐 (US)` - 美国费用数据
- `2024总帐 (CN)` - 中国费用数据
- `2024预算 (US)` - 美国预算数据
- `2024预算 (CN)` - 中国预算数据

每个sheet的列：
- 分类列（如"房产"、"交通"等）
- 1月 ~ 12月的数据列

### 预览文件 (preview_YYYY.xlsx)

工具自动生成，包含4个sheets：
- `YYYY-expense-USD` - 美国费用
- `YYYY-expense-CNY` - 中国费用
- `YYYY-budgets-USD` - 美国预算
- `YYYY-budgets-CNY` - 中国预算

每行包含：
- family_id
- expense_year
- expense_month
- category (原始分类名)
- category_id (数据库分类ID)
- amount
- type (expense/budget)
- currency

## 分类映射

工具使用 `category_mapping_corrected.json` 将Excel中的分类名映射到数据库分类ID。

如果遇到未映射的分类，工具会：
1. 显示警告
2. 跳过该记录
3. 在预览Excel中标记为"未映射"

## 常见问题

**Q: 如何知道哪些记录会被导入？**

A: 使用 `check` 命令或 `import --dry-run` 模式：
```bash
# 方式1：check命令
python3 import_from_excel.py check --year 2024

# 方式2：import的dry-run模式
python3 import_from_excel.py import --year 2024 --dry-run
```

**Q: 导入后发现错误怎么办？**

A: 工具支持事务回滚，如果导入过程出错会自动回滚。如需手动删除，可以在数据库中按年份和家庭ID删除。

**Q: 可以重复运行导入吗？**

A: 可以。工具会自动跳过已存在的记录，只导入新记录。

**Q: 如何只导入某一个月的数据？**

A: 先生成预览Excel，在Excel中删除不需要的行，然后导入。

**Q: 旧的脚本还能用吗？**

A: 可以。`import_from_excel.py` 是统一入口，底层仍调用原有脚本。原有脚本仍可独立使用。

## 目录结构

```
import/
├── 2023.xlsx                              # 原始数据
├── 2024.xlsx                              # 原始数据
├── temp_2024/                             # 临时目录（自动创建，git忽略）⭐
│   ├── preview_2024.xlsx                  # 预览文件
│   ├── preview_2024_USD.json              # 中间JSON
│   ├── preview_2024_CNY.json
│   ├── budget_preview_2024_USD.json
│   └── budget_preview_2024_CNY.json
├── config/                                # 配置文件
│   └── category_mapping_corrected.json    # 分类映射表
├── scripts/                               # 底层实现脚本
│   ├── import_all_preview.py              # 生成预览
│   ├── import_from_preview.py             # 导入数据
│   ├── import_expenses.py                 # 费用导入
│   ├── import_budgets.py                  # 预算导入
│   └── check_new_records.py               # 独立检查工具
├── .gitignore                             # Git忽略规则
├── import_from_excel.py                   # 统一入口 ⭐
└── README.md                              # 本文件

推荐使用 import_from_excel.py 作为主入口。
临时目录（temp_*）已通过 .gitignore 排除版本控制。
```

## 环境要求

```bash
pip3 install pandas openpyxl requests
```

后端API需要运行在 `http://localhost:8080`。
