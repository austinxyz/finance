# Excel支出数据导入工具使用说明

## 概述

`import_expenses.py` 是一个通用的Excel支出数据导入工具，可以将Excel表格中的支出数据导入到数据库中。

## 特性

- ✅ 支持自定义Excel文件和工作表
- ✅ 支持指定家庭ID、年份、货币
- ✅ 自动检测1-6月和7-12月数据段
- ✅ 两步导入：先预览，再执行
- ✅ 使用分类映射表自动匹配数据库分类
- ✅ 生成详细的预览报告（JSON + Excel）
- ✅ 事务安全，失败自动回滚

## 前提条件

### 1. 安装依赖

```bash
pip3 install pandas openpyxl
```

### 2. 准备分类映射表

确保 `category_mapping_corrected.json` 文件存在于同一目录。

当前映射表包含以下分类：
- 子女相关：学费、礼金、育儿费用
- 住房相关：保姆、房产税、水电煤物业、生活用品、租房还贷、装修
- 交通相关：日常交通、车保养保险、旅游、中美往来机票
- 保险相关：房屋保险
- 医疗相关：门诊
- 人情相关：亲戚往来
- 娱乐相关：健身美容、数码产品
- 食：饮食
- 衣：服装化妆品

### 3. 数据库配置

确保 `backend/.env` 文件存在并包含数据库连接信息：

```bash
DB_URL=jdbc:mysql://host:port/finance?...
DB_USERNAME=your_username
DB_PASSWORD=your_password
```

## 使用方法

### 步骤1: 预览数据

在导入前，先预览Excel数据，确认数据正确。

```bash
python3 import_expenses.py preview \
  --file 2024.xlsx \
  --sheet "2024总帐 (US)" \
  --family 1 \
  --year 2024 \
  --currency USD
```

**参数说明:**
- `--file`: Excel文件路径（相对或绝对路径）
- `--sheet`: 工作表名称（带引号，因为可能包含空格或特殊字符）
- `--family`: 家庭ID（默认为1，即Austin Family）
- `--year`: 数据年份
- `--currency`: 货币类型（USD或CNY）
- `--mapping`: （可选）分类映射文件路径（默认: category_mapping_corrected.json）

**输出文件:**
- `preview_2024_USD.json` - JSON格式的预览数据
- `preview_2024_USD.xlsx` - Excel格式的预览数据（便于查看）

**预览报告包含:**
- 总记录数和总金额
- 按月统计
- 按分类统计（Top 10）
- 未映射的分类（需要手动添加到映射表）
- 已跳过的分类

### 步骤2: 执行导入

确认预览数据无误后，执行导入：

```bash
python3 import_expenses.py import --preview-file preview_2024_USD.json
```

**执行过程:**
1. 加载预览数据
2. 生成SQL脚本（`import_2024_USD.sql`）
3. 显示导入摘要
4. 询问确认
5. 执行导入到数据库
6. 生成成功报告（`import_success_2024_USD.txt`）

**安全机制:**
- 使用数据库事务，失败自动回滚
- 导入前需要用户确认
- 保留SQL脚本供手动执行

## Excel文件要求

### 工作表结构

1. **第一行**（行0）：月份标题
   - Column I (索引8): 一月/七月
   - Column J (索引9): 二月/八月
   - Column K (索引10): 三月/九月
   - Column L (索引11): 四月/十月
   - Column M (索引12): 五月/十一月
   - Column N (索引13): 六月/腊月

2. **Column C**（索引2）：支出子分类名称

3. **Columns I-N**（索引8-13）：每月的支出金额

### 数据段

脚本自动检测以下两种结构：

**完整年度（1-12月）:**
- 行1-39：1-6月数据
- 行40-46：（跳过，汇总行）
- 行47：7-12月月份标题行
- 行48-85：7-12月数据
- 行86+：（跳过，其他表格）

**半年（仅1-6月或7-12月）:**
- 自动检测月份标题行判断是上半年还是下半年

### 示例结构

```
行号  | Column C (分类)    | Col I  | Col J  | Col K  | ...
------|-------------------|--------|--------|--------|-----
0     | (空)              | 一月   | 二月   | 三月   | ...
1     | 保姆              | (空)   | (空)   | (空)   | ...
2     | 亲戚朋友往来        | 703.5  | 110    | 692.41 | ...
3     | 学费              | 2137.15| 1045   | 1525   | ...
...   | ...               | ...    | ...    | ...    | ...
```

## 分类映射

### 查看当前映射

映射文件: `category_mapping_corrected.json`

格式:
```json
{
  "Excel分类名": {
    "db_name": "数据库分类名",
    "db_id": 分类ID,
    "confidence": "high/medium/low"
  }
}
```

### 添加新映射

如果预览时发现未映射的分类，编辑映射文件添加：

```json
{
  "新分类名": {
    "db_name": "对应的数据库分类名",
    "db_id": 对应的分类ID,
    "confidence": "high"
  }
}
```

**查找数据库分类ID:**

```sql
SELECT id, name FROM expense_categories_minor WHERE name LIKE '%关键词%';
```

### 排除分类

如果某些分类不应导入（如投资类），设置为低置信度并添加排除标记：

```json
{
  "基金": {
    "db_name": "未分类",
    "db_id": 80,
    "confidence": "low",
    "note": "排除（投资类）"
  }
}
```

## 常见场景

### 场景1: 导入2024年美元支出

```bash
# 预览
python3 import_expenses.py preview \
  --file 2024.xlsx \
  --sheet "2024总帐 (US)" \
  --family 1 \
  --year 2024 \
  --currency USD

# 查看预览
open preview_2024_USD.xlsx

# 导入
python3 import_expenses.py import --preview-file preview_2024_USD.json
```

### 场景2: 导入2024年人民币支出

```bash
# 预览
python3 import_expenses.py preview \
  --file 2024.xlsx \
  --sheet "2024总帐（中国）" \
  --family 1 \
  --year 2024 \
  --currency CNY

# 导入
python3 import_expenses.py import --preview-file preview_2024_CNY.json
```

### 场景3: 导入其他家庭的支出

```bash
# 假设家庭ID为2
python3 import_expenses.py preview \
  --file family2_2024.xlsx \
  --sheet "支出明细" \
  --family 2 \
  --year 2024 \
  --currency USD
```

## 故障排查

### 问题1: 找不到映射文件

**错误**: `❌ 错误: 映射文件不存在`

**解决**:
```bash
# 确保在import目录下
cd ~/claude/finance/import

# 检查文件存在
ls -la category_mapping_corrected.json
```

### 问题2: 未映射的分类

**错误**: 预览报告显示"未映射的分类"

**解决**:
1. 编辑 `category_mapping_corrected.json`
2. 添加新分类的映射
3. 重新运行预览命令

### 问题3: 数据库连接失败

**错误**: `❌ 错误: 找不到数据库配置文件`

**解决**:
```bash
# 检查.env文件
cat ../backend/.env

# 确保包含以下配置
# DB_URL=jdbc:mysql://...
# DB_USERNAME=...
# DB_PASSWORD=...
```

### 问题4: Excel读取失败

**错误**: `❌ 错误: 无法读取Excel文件`

**解决**:
1. 检查文件路径是否正确
2. 检查工作表名称是否正确（区分大小写）
3. 确保Excel文件未被其他程序打开

## 高级用法

### 使用自定义映射文件

```bash
python3 import_expenses.py preview \
  --file 2024.xlsx \
  --sheet "支出" \
  --family 1 \
  --year 2024 \
  --currency USD \
  --mapping my_custom_mapping.json
```

### 仅生成SQL不执行

```bash
# 步骤1: 预览
python3 import_expenses.py preview --file ... --sheet ... --year ... --currency ...

# 步骤2: 导入（选择 no）
python3 import_expenses.py import --preview-file preview_2024_USD.json
# 输入 no 取消执行

# 步骤3: 手动执行SQL
mysql -h host -P port -u user -p database < import_2024_USD.sql
```

## 文件清单

执行完整导入流程后，会生成以下文件：

| 文件 | 说明 |
|------|------|
| `category_mapping_corrected.json` | 分类映射表（必需） |
| `preview_{year}_{currency}.json` | 预览数据（JSON） |
| `preview_{year}_{currency}.xlsx` | 预览数据（Excel） |
| `import_{year}_{currency}.sql` | SQL导入脚本 |
| `import_success_{year}_{currency}.txt` | 导入成功报告 |

## 最佳实践

1. ✅ **总是先预览**: 确保数据正确再导入
2. ✅ **检查Excel预览**: 使用Excel打开预览文件更直观
3. ✅ **备份数据库**: 大批量导入前先备份
4. ✅ **检查重复**: 确保不会导入重复数据
5. ✅ **保留映射表**: `category_mapping_corrected.json` 是关键文件
6. ✅ **记录变更**: 如果修改映射表，记录修改原因

## 技术支持

如遇问题，请检查：
1. Python版本（需要3.6+）
2. 依赖包是否已安装
3. 数据库连接是否正常
4. Excel文件格式是否符合要求
5. 分类映射是否完整
