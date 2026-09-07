---
Date: 2026-09-06
Change: expense-summary
HAS_UI_SURFACE: no
Requirements: docs/superpowers/specs/2026-09-06-expense-summary-requirements.md
---

## Why

每月开支要从 6 家机构（BOA、Chase checking、Chase 信用卡、PayPal、Venmo、Robinhood 信用卡）
逐笔手输，再人工汇总成 32 个小类的月度数字填进 `ExpenseBatchUpdate.vue`。
读侧（年度汇总、月度趋势、预算对比）早已完整，瓶颈全在写入侧的人工搬运。

## What Changes

- 新增 CLI 工具 `tools/expense-import/`：目录驱动的 CSV 导入 → 分类 → 聚合 → 写库。
  Phase A 原型已实现规则引擎与 `chase_checking` 解析器，本 change 在其上补齐。
- CSV 存放在**仓库外** `~/finance-data/<YYYY-MM>/`，期间从目录名推导。
- 补齐其余 5 个解析器；确认无 CSV 导出的来源在配置中标记 `manual`。
- 三道写入闸门，各带显式逃生舱：
  - UNKNOWN 未匹配交易 → 拒写（`--allow-unknown`）
  - FUNDING 有充值但缺对应来源 → 拒写（`--allow-gaps`）
  - 预期来源清单里有缺失 → 拒写（`--allow-missing-sources`）
- **对账式写入**：写入前 `GET /records?period=` 取远端现状，本次无的小类走 `DELETE /records/{id}` 清除。
  没有这一步，修正规则后旧值会永久残留。
- 跨账户去重：信用卡消费只从卡账单计，checking 里的还款计为 TRANSFER；
  PayPal 消费只从 PayPal 账单计，checking 里的充值计为 FUNDING。

无 **BREAKING**。无 schema 变更。

## Capabilities

### New Capabilities

- `expense-import` — 目录驱动的银行 CSV 导入、规则分类、月度聚合、对账式写库

### Modified Capabilities

无。现有开支能力的行为要求不变，本 change 只新增一条写入路径。

## Impact

- **新增**：`tools/expense-import/`（Python 3.12，仅标准库）
  - `importer/parsers/` — 每来源一个解析器 + 表头识别注册表
  - `importer/classify.py` — 规则引擎（`rules.toml`，配置非代码）
  - `importer/reconcile.py` — 对账逻辑（新）
  - `importer/apiclient.py` — 登录换 JWT + 批量写入/删除（新）
  - `rules.toml` / `categories.toml` / `sources.toml`（预期来源清单，新）
- **复用，不修改**：
  - `POST /api/expenses/records/batch` — 幂等 upsert
  - `GET /api/expenses/records?period=` — 对账取现状
  - `DELETE /api/expenses/records/{id}` — 对账清除
  - `expense_categories_major` / `expense_categories_minor` — 分类树
- **后端零改动，前端零改动。** `ExpenseBatchUpdate.vue` 保持可用作人工修正入口。
- **凭据**：`FINANCE_API_BASE` / `FINANCE_USERNAME` / `FINANCE_PASSWORD`，仓库外，不硬编码。

## Out of Scope

- 交易明细持久化（`expense_transactions` 表）— 流水只留在本地 `review.csv`，
  数据库仍只存月度聚合。日后若要在 App 内回答「那笔 259 刀是什么」，需另开 change。
- 银行 API 直连（Plaid / SimpleFIN）— 本 change 只吃手工下载的 CSV。
- 收入导入 — `INCOME` 交易被识别但不写入；`income_categories_*` 留作后续 change。
- 多币种 — 6 家账户均为 USD，`currency` 写死。
- 导入相关的任何 UI — 纯 CLI。
