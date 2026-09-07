# 开支导入

把各家银行/支付平台的月度 CSV 转成 `expense_records` 的月度分类开支，取代逐笔手输。

## 每月流程

```bash
# 1. 各家导出当月 CSV，丢进 ~/finance-data/2026-08/
# 2. 先只分类，不写库（不需要凭据，不发任何网络请求）
python tools/expense-import/run.py 2026-08

# 3. 看控制台的 UNKNOWN 清单 → 在 rules.toml 补规则 → 重跑，直到干净
# 4. 开 out/review-2026-08.csv 核对，重点看 note 里带"假设""需核对"的行
# 5. 确认无误后写库
python tools/expense-import/run.py 2026-08 --post
```

期间从目录名推导，不用手敲 `--period`。目录下所有 CSV 按表头自动识别机构。

## CSV 放哪

默认 `~/finance-data/<YYYY-MM>/`，**在仓库外**。

CSV 含账号、余额、商户名。放仓库外意味着 `.gitignore` 写错、误用 `git add -f`、
或备份工具扫目录，都不会把它们带进版本库。

覆盖路径：`--root <路径>` 或环境变量 `FINANCE_DATA_ROOT`。

## 三道闸门

默认全部拒写。每道闸门堵的是一种"总额错了但看起来没错"的情况。

| 闸门 | 拦什么 | 逃生舱 |
|---|---|---|
| 未分类 | 有交易没被任何规则匹配 | `--allow-unknown` |
| 充值缺口 | 有 PayPal/Venmo 充值但缺对应账单 | `--allow-gaps` |
| 来源缺失 | `sources.toml` 声明的来源没出现在目录里 | `--allow-missing-sources` |

**为什么要第三道**：前两道只能发现数据里留下痕迹的问题。信用卡账单要是忘了导出，
在任何一份 CSV 里都不留痕 —— 月度总额直接偏小，前两道全部放行。第三道是唯一能发现它的机制。

逃生舱都是显式命令行开关，触发时控制台会明确打印放行了什么、金额多少。

## 对账式写入

`--post` 不是简单地"提交聚合结果"，而是**让该期间的库中状态等于本地聚合结果**：

1. `GET /records?period=` 取远端现状
2. 本次有的小类 → `POST /records/batch`（后端幂等 upsert）
3. 远端有、本次无或净额 ≤ 0 的小类 → `DELETE /records/{id}`

没有第 3 步的话，第一次因规则错误写进去的数字会永久残留 —— 修正规则后重跑
只是"不再提交它"，库里那个错值不会消失。规则会持续演进，这个问题只会越积越多。

**币种隔离**：对账只作用于 `currency = "USD"` 的记录。

`ExpenseBatchUpdate.vue` 有币种选择器，你可能手录过 CNY 记录。后端按
`(family, 期间, 小类, 币种)` 去重 —— 同一小类的 USD 和 CNY 是**两行独立记录**。
不按币种过滤的话，手录的 CNY 记录在 diff 里正好符合"远端有、本次无"，会被全部删掉。
这是本工具风险最高的一处，有 6 个专项测试锁着。

## 凭据

```bash
export FINANCE_USERNAME=...
export FINANCE_PASSWORD=...
export FINANCE_API_BASE=http://localhost:8080/api   # 可选
```

只在 `--post` 时需要。缺失会明确报错指名变量，不会静默跳过写入。
`family_id` 由后端从 JWT 推导，客户端不指定。**凭据不要写进仓库。**

## 分类语义

| action | 含义 | 计入统计 |
|---|---|---|
| `EXPENSE` | 真实开支 | ✅ |
| `TRANSFER` | 账户间转账、信用卡还款 | ❌ 钱没花掉 |
| `FUNDING` | PayPal/Venmo 充值 | ❌ 钱花了，明细在对方账单 |
| `INCOME` | 工资、利息 | ❌ |
| `IGNORE` | 明确忽略 | ❌ |
| `UNKNOWN` | 无规则匹配 | ❌ 需补规则 |

`TRANSFER` 和 `FUNDING` 都不计但含义不同，所以分开：前者钱只是换了个账户，
后者钱花掉了但明细在别处 —— 只导 checking 会**漏计**这部分，闸门 2 就是为它设的。

## 加规则

编辑 `rules.toml`，**按文件顺序匹配，第一条命中即生效**，具体规则放通用规则之前。

```toml
[[rule]]
name = "costco-gas"          # 会显示在 review.csv 的"命中规则"列
desc = '(?i)costco gas'      # 正则，(?i) 必须在最开头且只能出现一次
action = "EXPENSE"
category = 69                # 小类 id，见 categories.toml
note = "加油"                 # 会显示在 review.csv 便于核对
```

匹配条件：`desc`（正则）、`type_is`（机构类型码精确匹配）、`source_is`（限定解析器）、
`amount_min` / `amount_max`（按金额绝对值）。

`FUNDING` 规则必须带 `funding_target`（如 `"paypal"`），否则闸门 2 无从判断缺的是哪家。

改完直接重跑。规则文件写错（正则非法、category 不存在、EXPENSE 缺 category、
FUNDING 缺 funding_target）会在加载时直接报错，不会静默跳过。

**故意不设兜底规则** —— 静默归到"未分类"比留在 UNKNOWN 更危险。

## 来源清单

`sources.toml` 声明"完整的一个月应该有哪几家"。三种显式状态，没有沉默的空白：

| 状态 | 含义 |
|---|---|
| 有 `parser` | 解析器已实现，闸门 3 要求目录里有它的 CSV |
| `manual = true` | 该来源无 CSV 导出，每月提示手工补录 |
| `pending = true` | 已声明、解析器还没写 |

`pending` 存在的理由：清单描述的是"完整的一个月长什么样"，不是"当前实现覆盖了什么"。
没有它，声明一个未实现的来源和拼错来源名无法区分，清单就只能描述现状 ——
而那恰恰让闸门 3 在最需要它的过渡期失效。

解析器落地后必须删掉对应的 `pending` 行（加载器会强制要求，防止标记过期）。

## 加新银行

写一个 parser 注册即可，管道其余部分不动：

```python
# importer/parsers/boa_checking.py
@register
class BoaCheckingParser(Parser):
    source = "boa_checking"
    header_signature = ("Date", "Description", "Amount")   # 用于自动识别文件

    def parse_row(self, row, account):
        return Txn(source=self.source, account=account, ...)
```

然后在 `importer/parsers/__init__.py` 里 import 它，并删掉 `sources.toml` 里的 `pending`。

**符号约定是最容易犯且后果最大的解析错误**：信用卡账单中消费通常为正数，与 checking 相反。
每个 parser 要在 `parse_row` 内归一为「负数 = 支出」，并用真实样本行断言符号。
搞反会让整月开支变成负数。

已支持：`chase_checking`。
待实现：Chase 信用卡、BOA checking、PayPal、Venmo、Robinhood 信用卡。

## 测试

```bash
python -m unittest discover -s tools/expense-import -t tools/expense-import
```

测试覆盖的是会**静默污染数字**的失败模式：转账被当成开支、充值被重复计、
退款没冲抵、规则顺序错位、对账误删他币种记录。

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
