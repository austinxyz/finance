---
Date: 2026-09-06
Change: expense-summary
Status: REVIEWED
HAS_UI_SURFACE: no
---

# 开支汇总 (Expense Summary)

把「每月逐笔手输开支」换成「把各家 CSV 丢进一个目录，跑一条命令，月度分类开支直接进库」。

现状：读侧（`ExpenseAnalysisController` 的年度汇总、月度趋势、预算对比）已经完整，
但 `expense_records` 里的数字全靠 `ExpenseBatchUpdate.vue` 手填 32 个小类。
本 change 只补写入侧，不动读侧。

已有基础：`tools/expense-import/`（Phase A 原型）已实现规则引擎、`chase_checking` 解析器、
`review.csv` / `batch_preview.json` 输出，13 个测试通过。本 change 在其上补齐目录约定、
其余 5 个解析器、两道写入闸门、以及写库。

## Goals

- **目录驱动**：`~/finance-data/<YYYY-MM>/` 下丢入当月各家 CSV，自动发现并按表头识别机构，
  期间从目录名推导，无需手敲 `--period`。
- **覆盖 6 个来源**：BOA checking、Chase checking（已有解析器）、Chase 信用卡、PayPal、Venmo、
  Robinhood 信用卡。有 CSV 导出的写解析器；确认无 CSV 的（可能是 Robinhood）在配置中标记 `manual`，
  由闸门 3 每月提示手工补录 —— 关键是**每一家都被明确覆盖或明确标记**，不能有沉默的空白。
- **写库**：分类聚合后 POST 现有 `POST /api/expenses/records/batch`，凭据走环境变量，登录换 JWT。
- **闸门 1 — UNKNOWN 拒写**：只要有未匹配规则的交易，默认拒绝写入并列出明细；
  `--allow-unknown` 作为逃生舱（归入「其他/未分类」id=80）。逃生舱是显式命令行开关，
  控制台会明确打印被兜底的笔数与金额 —— 与「规则文件不设兜底规则」并不冲突。
- **闸门 2 — FUNDING 缺口拒写**：Chase checking 里有 PayPal 充值、但当月目录没有 PayPal CSV，
  说明这笔钱花了却没明细。拒绝写入并指名缺哪家。`--allow-gaps` 作为逃生舱。
- **闸门 3 — 来源缺失拒写**：配置声明「本月预期来源清单」，目录里缺任何一家即拒绝写入。
  闸门 2 只能抓在 checking 里留下充值痕迹的 PayPal/Venmo；
  信用卡账单（如 Robinhood）若漏导，**在任何 CSV 里都不留痕**，总额会静默偏小 ——
  这道闸门是唯一能发现它的机制。`--allow-missing-sources` 作为逃生舱。
- **对账式写入（保证真幂等）**：写入前先 `GET /records?period=<期间>` 取远端现状，与本次聚合结果比对：
  - 本次有、远端无或不同 → `POST /records/batch`（现有幂等 upsert）
  - 远端有、本次净额 ≤ 0 或已无该小类 → `DELETE /records/{id}` 清除
  没有这一步，修正规则后「上一次写错的小类」会永久残留在库里。
- **可核对**：每笔交易在 `review.csv` 里都带命中的规则名，没有静默归类。

## Non-Goals

- **不改后端**：不加 controller / service / entity / migration。只调用现有 `batchSave`。
- **不改前端**：`ExpenseBatchUpdate.vue` 保持可用，作为人工修正的入口。
- **不存交易明细**：不建 `expense_transactions` 表。流水只留在本地 `review.csv`，
  数据库仍只存月度聚合。（可审计性 → 见 Open Questions，留给后续 change。）
- **不接银行 API**：不用 Plaid / SimpleFIN，只吃手工下载的 CSV。
- **不再往随手记录入**：本 change 交付后随手记退出流程。
- **不做多币种**：6 家账户均为 USD。`currency` 字段写死 "USD"。
- **不自动下载 CSV**：每月手工从各家网站导出，本工具不碰任何银行登录。
- **不做 UI**：纯 CLI。

## Constraints

- **数据模型现状**：`expense_records` 唯一约束 `(family_id, expense_period, minor_category_id)`，
  服务层按 `(familyId, expensePeriod, minorCategoryId, currency)` 查重后 upsert
  （`ExpenseService.batchSaveExpenseRecords`）。**每个小类每月只有一行**，不存流水。
- **`batchSave` 已幂等**：命中已有记录则更新 amount/expenseType/description，否则插入。重跑安全。
- **金额必须为正**：`BatchExpenseRecordRequest.ExpenseRecordItem` 有 `@DecimalMin("0.01")`。
  分类内的退款需在聚合阶段冲抵。净额 ≤ 0 的小类**不能提交**（会被校验拒绝），
  但也**不能就此跳过** —— 若远端已存在该小类记录，必须走 DELETE 清除，否则残留旧值。
- **对账所需端点均已存在，无需改后端**：
  `GET /api/expenses/records?period=YYYY-MM`（`getExpenseRecordsByPeriod`，返回含 `id` 的 DTO）
  与 `DELETE /api/expenses/records/{id}`。
- **跨期交易归属**：CSV 常含目录期间之外的交易（如 9 月导出的 Chase 文件含 9 月初记录）。
  一律按**交易日期**归属期间，非目录期间的交易被过滤掉，
  并在控制台报告过滤了几笔 —— 让用户知道它们会在下个月的目录里被处理，而不是丢失了。
- **FUNDING 需声明目标**：规则新增 `funding_target` 字段（如 `"paypal"` / `"venmo"`），
  闸门 2 据此判断「有充值但缺对应来源」。不带 `funding_target` 的 FUNDING 规则加载期报错。
- **凭据变量**：`FINANCE_API_BASE`（默认 `http://localhost:8080/api`）、
  `FINANCE_USERNAME`、`FINANCE_PASSWORD`。从环境变量或仓库外的 env 文件读取，
  缺失时明确报错而非静默跳过写入。
- **familyId 由服务端覆盖**：controller 用 `authHelper.getFamilyIdFromAuth(authHeader)` 覆盖请求体，
  客户端传什么都不生效。脚本只需持有有效 JWT。
- **expenseType 来自小类**：`expense_categories_minor.expense_type`（FIXED_DAILY / LARGE_IRREGULAR），
  由 `categories.toml` 携带，不由规则指定。
- **分类表**：32 个启用小类，挂在 10 个大类下。id 列表见 `tools/expense-import/categories.toml`。
- **运行环境**：Python 3.12，**仅标准库**（`tomllib` 已内置；机器上无 pyyaml，不引入依赖）。
  Windows 控制台需显式 UTF-8。
- **金融数据不进仓库**：CSV 含账号、余额、商户名，存放路径在仓库外（默认 `~/finance-data/`），
  路径可通过配置覆盖。凭据走环境变量，沿用 `backend/.env` 的 gitignore 模式，绝不硬编码。
- **规则文件是配置不是代码**：新增商户只改 `rules.toml`，不改 Python。
  规则加载期校验（正则合法、category 存在、EXPENSE 必须带 category），出错即失败，不静默跳过。
- **规则顺序即优先级**：结构性规则（转账/还款/充值）必须排在商户规则之前，
  否则「Payment to Chase card」可能被商户正则抢走。现有测试已锁住这条。

## Success Criteria

1. 把 2026-08 的各家 CSV 放进 `~/finance-data/2026-08/`，跑一条命令，
   `expense_records` 的 2026-08 数据与 `review.csv` 的聚合结果逐项一致。
   命令结束时打印实际写入/删除的小类清单，可直接与控制台的聚合表对照。
2. 同一命令连跑两次，数据库状态不变（幂等），且不产生重复行。
3. **对账生效**：先用一份错误规则写入，再修正规则重跑 —— 被修正掉的小类在库中被清除，
   不残留旧值。这是 #2 之外单独要验的一条。
4. 目录里含未匹配交易时，命令**拒绝写入**，列出每笔的日期/金额/描述，退出码非 0。
5. 目录里有 PayPal 充值但缺 PayPal CSV 时，命令**拒绝写入**并指名缺失来源。
6. 预期来源清单里的任一家缺失时，命令**拒绝写入**并指名缺哪家
   （覆盖信用卡账单漏导这种"无痕"缺口）。
7. CSV 含目录期间之外的交易时，这些交易被过滤且**在控制台报告笔数**，不静默丢弃。
8. `review.csv` 中每一笔 EXPENSE 都能追溯到一条具名规则；规则文件内无兜底规则。
9. 已实现的每个解析器都有基于真实样本行的测试；
   转账/还款/充值的排除逻辑各有测试覆盖。
   （目标 6 家；若 Robinhood 确认无 CSV 导出，则为 5 家 + 该来源在配置中标记为
   `manual`，由闸门 3 提示用户手工补录，而非当作已覆盖。）
10. 跨账户不重复计账：信用卡消费只从卡账单计一次，checking 里的还款记为 TRANSFER；
    PayPal 消费只从 PayPal 账单计一次，checking 里的充值记为 FUNDING。

## User Stories

**每月记账（主流程）**
> 我从 6 家网站导出当月 CSV，丢进 `~/finance-data/2026-08/`。
> 跑 `python run.py 2026-08`，先看控制台的分类统计和未匹配清单。
> 有未匹配就去 `rules.toml` 补规则，重跑，直到干净。
> 然后跑 `python run.py 2026-08 --post`，数字进库。
> 打开分析页，8 月的分类开支已经在那了。

**补规则**
> 控制台列出 `SQ *BLUE BOTTLE COFFEE -45.20` 未匹配。
> 我在 `rules.toml` 的「饮食」段加一条 `desc = '(?i)blue bottle'`，`category = 67`。
> 重跑，命中，`review.csv` 的「命中规则」列显示我刚加的规则名。

**核对可疑分类**
> `review.csv` 里 Amazon 那几笔备注写着「假设为生活用品，需逐笔核对」。
> 我看了一下其中一笔是买显示器，应该归数码产品。
> 加一条更具体的规则放在 amazon 通用规则**之前**，重跑。

**发现漏计**
> 命令报「PayPal 充值 3 笔共 $161.59，但目录里没有 PayPal 的 CSV」。
> 我回 PayPal 导出当月账单丢进去，重跑，这 3 笔变成 TRANSFER（充值的对侧），
> PayPal 账单里的真实消费被正确分类。

## Open Questions

**阻塞实现（须在 `/opsx:apply` 前解决）**

- **5 份样本 CSV 待提供**：除 Chase checking 外，其余 5 家格式未知。
  需各导出一份任意月份的文件，确定表头、日期格式、金额符号约定
  （信用卡消费通常为正数，与 checking 相反 —— 符号搞反会让整月开支变成负数）。
  解析器任务按来源切分，样本到一份就能做一个，不必等齐。
- **Robinhood 信用卡是否有 CSV 导出？** 可能只有 PDF 账单。
  确认无 CSV → 在配置中标记为 `manual`，闸门 3 每月提示手工补录（见 Success Criteria #9）。
- **Venmo 个人转账 vs 消费**：Venmo 既有朋友间转账（可能属「人情」，也可能只是代付后被还款），
  也有商户消费。需看样本才能定策略 —— 可能要按对方是个人还是商户区分，
  也可能需要「代付-还款」配对抵消。这是本 change 里规则设计最不确定的一块。

**不阻塞实现**

- **交易明细可审计性**：本 change 不存流水，`review.csv` 是唯一明细留存。
  若日后想在 App 里回答「那笔 259 刀是什么」，需后续 change 加 `expense_transactions` 表。
- **收入是否也要导入？** 本 change 只处理开支；`INCOME` 分类的交易被识别但不写入。
  项目有 `income_categories_major/minor` 表，可作为后续 change。

**评审中已定，不再是开放问题**

- ~~信用卡账单周期 vs 交易日期~~ → 已定为**交易日期**归属，跨期交易过滤并报告（见 Constraints）。

## Referenced Capabilities

**新增**
- `expense-import` — 目录驱动的 CSV 导入、分类、聚合、写库（本 change 的主体）

**复用（不修改）**
- `POST /api/expenses/records/batch`（`ExpenseController.batchSaveExpenseRecords`）— 幂等 upsert 写入
- `GET /api/expenses/records?period=YYYY-MM`（`getExpenseRecordsByPeriod`）— 对账取远端现状
- `DELETE /api/expenses/records/{id}` — 对账清除已失效的小类记录
- `ExpenseService.batchSaveExpenseRecords` — 查重与 major/minor 关联
- `AuthHelper.getFamilyIdFromAuth` — familyId 服务端推导
- `expense_categories_major` / `expense_categories_minor` — 分类树来源
- `ExpenseAnalysisController` — 读侧，本 change 交付后自动受益，无需改动
