# 快速开始 - Excel支出数据导入

## 5分钟快速导入

### 步骤1: 预览数据（2分钟）

```bash
cd ~/claude/finance/import

python3 import_expenses.py preview \
  --file 2024.xlsx \
  --sheet "2024总帐 (US)" \
  --family 1 \
  --year 2024 \
  --currency USD
```

**输出:**
- ✅ `preview_2024_USD.json` - 预览数据
- ✅ `preview_2024_USD.xlsx` - Excel预览（便于查看）

**检查预览:**
```bash
# 在Excel中打开预览
open preview_2024_USD.xlsx

# 或查看摘要
head -50 preview_2024_USD.json
```

### 步骤2: 执行导入（3分钟）

```bash
python3 import_expenses.py import --preview-file preview_2024_USD.json
```

**确认导入:**
- 查看导入摘要
- 输入 `yes` 确认
- 等待导入完成

**完成！** ✅

---

## 导入其他文件

### 导入人民币支出

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

### 导入2025年数据

```bash
# 预览
python3 import_expenses.py preview \
  --file 2025.xlsx \
  --sheet "2025总帐 (US)" \
  --family 1 \
  --year 2025 \
  --currency USD

# 导入
python3 import_expenses.py import --preview-file preview_2025_USD.json
```

---

## 常见问题

### Q: 如何查看帮助?

```bash
python3 import_expenses.py --help
python3 import_expenses.py preview --help
python3 import_expenses.py import --help
```

### Q: 预览时发现未映射的分类怎么办?

1. 打开 `category_mapping_corrected.json`
2. 添加新分类映射
3. 重新运行预览

### Q: 可以取消导入吗?

可以！在导入确认时输入 `no`。SQL脚本已保存，可以稍后手动执行。

### Q: 导入失败怎么办?

导入使用事务，失败会自动回滚，不会污染数据库。

---

## 文件说明

| 文件 | 作用 | 必需? |
|------|------|-------|
| `import_expenses.py` | 导入工具脚本 | ✅ 必需 |
| `category_mapping_corrected.json` | 分类映射表 | ✅ 必需 |
| `backend/.env` | 数据库配置 | ✅ 必需 |
| `*.xlsx` | Excel数据文件 | ✅ 必需 |
| `preview_*.json` | 预览数据（生成） | 自动生成 |
| `preview_*.xlsx` | Excel预览（生成） | 自动生成 |
| `import_*.sql` | SQL脚本（生成） | 自动生成 |

---

## 完整示例

```bash
# 进入导入目录
cd ~/claude/finance/import

# 步骤1: 预览2024年美元支出
python3 import_expenses.py preview \
  --file 2024.xlsx \
  --sheet "2024总帐 (US)" \
  --family 1 \
  --year 2024 \
  --currency USD

# 步骤2: 查看Excel预览
open preview_2024_USD.xlsx

# 步骤3: 确认无误后导入
python3 import_expenses.py import --preview-file preview_2024_USD.json

# 步骤4: 输入 yes 确认
# yes

# 完成！查看成功报告
cat import_success_2024_USD.txt
```

---

更多详细信息请查看 `README_IMPORT_TOOL.md`
