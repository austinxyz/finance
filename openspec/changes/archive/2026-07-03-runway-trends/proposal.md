---
Date: 2026-07-02
Change: runway-trends
HAS_UI_SURFACE: yes
Requirements: docs/superpowers/specs/2026-07-02-runway-trends-requirements.md
---

## Why

用户已能保存单份资金跑道报告（`RunwayReport` 快照），但无法看到跑道健康度随时间的走势。新增「资金跑道趋势」页把历史快照汇总成趋势线，帮助判断现金流在变好还是变差。

## What Changes

- 新增只读页 `/analysis/runway-trend`，挂在 分析 → 财务规划 导航分组（现有 资金跑道分析、跑道报告 之后第三项）。
- 新增后端只读端点 `GET /runway/reports/trend?familyId=`，服务端解析每份 `RunwayReport.snapshotJson`，返回按 `savedAt` 升序的指标点数组，并用 `ExpenseCategoryMajor` 富化最新报告的分类明细（name/color）。避免前端 N+1。
- 数据源 = 已保存快照（**不重算历史**）。逐报告原始点：每份报告一个点。
- 前端渲染 4 个 KPI 卡（当前现金余额 / 剩余跑道月数 / 月度净烧钱率 / 预计现金耗尽），趋势图（切换 剩余跑道/月度净烧钱/现金余额，面积/折线，hover），分类支出明细表。
- 文案按逐报告真实语义（近N份 / 报告值 / 较上次报告）。
- **无 schema 变更**：复用 `runway_reports` 表与既有快照结构。

## Capabilities

### New Capabilities

- `runway-trends` — 资金跑道趋势页与其只读趋势聚合端点。

### Modified Capabilities

无。不改动 `runway-analysis`（live 分析）与 `runway-snapshot-export`（保存/查看）的既有需求，仅只读消费其已保存快照。

## Impact

- 后端：`RunwayReportController`（+`GET /trend` 子路由）、`RunwayReportService`（+`getTrend`）、新增 `RunwayTrendDTO`；`RunwayReportRepository`、`ExpenseCategoryMajorRepository`（读）。
- 前端：`api/runway.js`（+`getRunwayTrend`）、新增 `views/analysis/RunwayTrend.vue`、`router/index.js`（+路由）、`Sidebar.vue`（+导航项）。
- 图表：Chart.js / vue-chartjs（已有依赖）。
- 样式令牌：`finance-ui-tokens`（claude.ai/design），设计源 `Runway Trends.dc.html`。
- 权限：`familyAPI.getDefault()` + `authHelper.requireFamilyAccess`，家庭数据隔离。

## Out of Scope

- 不按日历月重算历史跑道（选逐报告聚合，非重算方案）。
- 不新增报告保存/删除能力（沿用 `runway-snapshot-export`）。
- 不做多币种切换（沿用 USD 展示口径）与 PDF 导出。
