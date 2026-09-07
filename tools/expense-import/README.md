# 开支自动导入 — Phase A（离线分类器）

把银行 CSV 导出转成按小类聚合的月度开支，**不写数据库**，只产出核对文件。

## 用法

```bash
cd tools/expense-import
python run.py ~/Downloads/Chase8798_Activity_20260906.csv
```

多个账户一起跑（跨账户去重靠规则，不靠文件顺序）：

```bash
python run.py ~/Downloads/Chase*.csv ~/Downloads/BOA*.csv --period 2026-08
```

产出在 `out/`：

| 文件 | 用途 |
|---|---|
| `review.csv` | 每笔交易 + 命中的规则名。UNKNOWN 排在最前 |
| `batch_preview.json` | 按期间分组，形状对齐 `POST /api/expenses/records/batch` |

控制台会打印分类统计、按小类的聚合金额、以及未匹配清单。

## 每月流程

1. 各家下载 CSV
2. `python run.py <csv...> --period YYYY-MM`
3. 看控制台的 **UNKNOWN 清单** → 在 `rules.toml` 补规则 → 重跑
4. 开 `review.csv` 核对，重点看 `note` 里带"假设""需核对"的行
5. 确认无误后（Phase B）再 POST

规则命中率会逐月提高。前两三个月花时间补 `rules.toml`，之后基本只剩核对。

## 分类语义

| action | 含义 | 计入统计 |
|---|---|---|
| `EXPENSE` | 真实开支 | ✅ |
| `TRANSFER` | 账户间转账、信用卡还款 | ❌ |
| `FUNDING` | PayPal/Venmo 充值 | ❌ |
| `INCOME` | 工资、利息 | ❌ |
| `IGNORE` | 明确忽略 | ❌ |
| `UNKNOWN` | 无规则匹配 | ❌ 需补规则 |

**为什么要区分 TRANSFER 和 FUNDING**：两者都不计，但含义不同。
TRANSFER 的钱没花掉（只是换了个账户）；FUNDING 的钱花掉了，
但明细在 PayPal/Venmo 账单里 —— 只导 checking 会**漏计**这部分。
控制台会单独提示 FUNDING 总额，提醒你补导对应账单。

## 加规则

编辑 `rules.toml`，**按文件顺序匹配，第一条命中即生效**，所以具体规则要放在通用规则上面。

```toml
[[rule]]
name = "costco-gas"          # 会显示在 review.csv 的"命中规则"列
desc = '(?i)costco gas'      # 正则，(?i) 必须在最开头（只能出现一次）
action = "EXPENSE"
category = 69                # 小类 id，见 categories.toml
note = "加油"                 # 会显示在 review.csv 便于核对
```

可用的匹配条件：`desc`（正则）、`type_is`（机构类型码精确匹配）、
`source_is`（限定某个 parser）、`amount_min` / `amount_max`（按金额绝对值）。

改完直接重跑，不用改代码。规则文件写错（正则非法、category 不存在、
EXPENSE 缺 category）会在加载时直接报错，不会静默跳过。

**故意不设兜底规则** —— 静默归到"未分类"比留在 UNKNOWN 更危险。

## 加新银行

写一个 parser，注册即可，管道其余部分不用动：

```python
# importer/parsers/boa_checking.py
@register
class BoaCheckingParser(Parser):
    source = "boa_checking"
    header_signature = ("Date", "Description", "Amount")   # 用于自动识别文件

    def parse_row(self, row, account):
        return Txn(source=self.source, account=account, ...)
```

然后在 `importer/parsers/__init__.py` 里 import 它。

已支持：`chase_checking`。
待补：Chase 信用卡、BOA checking、PayPal、Venmo、Robinhood 信用卡。

## 测试

```bash
python -m unittest discover -s tools/expense-import
```

测试覆盖的是会**静默污染数字**的失败模式：转账被当成开支、
充值被重复计、退款没冲抵、规则顺序错位。

## 重新生成 categories.toml

分类表在数据库里改动后：

```bash
MYSQL="/c/Program Files/MySQL/MySQL Workbench 8.0 CE/mysql.exe"
set -a; source ../../backend/.env; set +a
Q="SELECT mi.id, maj.code, maj.name, mi.name, mi.expense_type \
   FROM expense_categories_minor mi \
   JOIN expense_categories_major maj ON mi.major_category_id=maj.id \
   WHERE mi.is_active=1 ORDER BY maj.sort_order, mi.sort_order;"
"$MYSQL" --default-character-set=utf8mb4 -h "$DB_HOST" -P "$DB_PORT" \
  -u "$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" -e "$Q" > cats.tsv
```

再用 `cats.tsv` 生成 `categories.toml`（格式见文件头注释），完成后删掉 `cats.tsv`。

## Phase B（尚未实现）

- `expense_transactions` 明细表：存原始流水 + 分类结果，保留可审计性
- 导入 API + 前端 review 页
- 聚合写入 `expense_records`（batch 接口已是幂等 upsert，可安全重跑）
