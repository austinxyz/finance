---
Date: 2026-07-02
Change: runway-trends
Status: REVIEWED
HAS_UI_SURFACE: yes
---

# 资金跑道趋势 (Runway Trends)

新增「资金跑道趋势」页面，放在 分析 → 财务规划 分组下（现有 资金跑道分析、跑道报告 之后的第三项）。
把已保存的多份资金跑道报告（`RunwayReport` 快照）汇总成趋势线，追踪现金流健康度随时间的变化。

设计稿：claude.ai/design 项目 `8f1c7aeb-...`，文件 `Runway Trends.dc.html`，使用已同步的 `finance-ui-tokens` 品牌令牌。

## Goals

- 新增只读趋势页 `/analysis/runway-trend`，导航挂在 财务规划 分组。
- 数据源 = 已保存的 `RunwayReport` 快照（不重算历史）。**逐报告原始点**：每份报告一个点，按 `savedAt` 升序。
- 4 个 KPI 卡：当前现金余额、剩余跑道月数、月度净烧钱率、预计现金耗尽日期。每卡显示相对上一份报告的变化。
- 趋势图：可切换三个指标（剩余跑道月数 / 月度净烧钱 / 现金余额），面积图/折线图，hover tooltip。
- 区间选择：近 6 份 / 近 12 份 / 全部（对报告数量截取，非日历月）。
- 分类支出明细表：取最新报告的 `expenseBreakdown`，含 类别 / 金额 / 占比条 / 较上次报告环比。
- 文案改为逐报告真实语义（不沿用设计稿的「按月」措辞）。

## Non-Goals

- 不按日历月重算历史跑道（选 B 而非 A/重算方案）。
- 不改动现有 `RunwayService`（live 分析）、`RunwayAnalysis.vue`、`RunwayReportList/View`。
- 不新增报告保存能力（沿用现有保存流程作为数据来源）。
- 不做多币种切换；沿用现有 USD 展示口径（见 Open Questions）。
- 不做导出 PDF（`RunwayReportView` 已有，不在本页范围）。

## Constraints

- **数据模型现状**：`RunwayReport` = `{id, familyId, reportName, savedAt, snapshotJson(TEXT)}`。`snapshotJson` 结构（version "1"）：
  ```
  { version, settings{lookbackMonths, optimisticMultiplier, pessimisticMultiplier},
    excludedAccountIds[], expenseAdjustments{},
    snapshot { liquidTotal, monthlyBurn, runwayMonths, depletionDate,
               accountBreakdown[], expenseBreakdown{CODE: usdAmount} } }
  ```
- `listReports` 的 `RunwayReportSummaryDTO` 只含 `id/reportName/savedAt` → 无指标。**必须新增后端端点**在服务端解析快照，避免前端 N+1。
- 快照金额均为 USD（`RunwayService.toUsd` 已折算）。设计稿的 ¥ 仅为 mock。
- `snapshot.monthlyBurn` = 按该报告自身 `lookbackMonths`（用户当时选的，常为 6）求的均值，**非固定近 3 月** → KPI 文案不得写「近 3 月均」。
- 权限：非管理员用 `familyAPI.getDefault()` 取 familyId；后端 `authHelper.requireFamilyAccess`。
- 前端：Composition API + `<script setup>`；Tailwind 工具类；图表用 chart.js + vue-chartjs（项目已有依赖，勿手写 SVG）；金额用 `formatCurrency`/`formatUSD` helper。
- 时区/时间：后端用 `TimeService`；快照为只读历史，不新建时间序列记录。

## Success Criteria

- 页面在 财务规划 分组可见并可导航，路由 `/analysis/runway-trend` 正常懒加载。
- 后端 `GET /runway/reports/trend?familyId=` 返回按 `savedAt` 升序的点数组，每点含 `savedAt, reportName, liquidTotal, monthlyBurn, runwayMonths, depletionDate, expenseBreakdown`。
- 有 ≥2 份报告时：KPI 显示最新值 + 相对上一份的变化（涨跌色）；图表三指标可切换并正确渲染；分类表显示最新报告分类 + 较上次环比。
- 有 1 份报告：显示点/值，变化处显示「—」，不报错。
- 0 份报告：空状态提示「先去 资金跑道分析 保存报告」，带跳转链接。
- 区间选择 近6/近12/全部 正确截取最近 N 份报告。
- 后端单测覆盖：快照解析、空/单/多报告、坏 JSON 容错；覆盖率 ≥ 80%。
- 端点不返回原始 `snapshotJson` 全文（只返回抽取后的指标 + 最新 expenseBreakdown），避免泄露与体积膨胀。

## User Stories

1. 作为用户，我每月在 资金跑道分析 保存一份报告后，能在 资金跑道趋势 页看到跑道月数随时间的走势，判断在变好还是变差。
2. 作为用户，我能切换查看 现金余额 / 月度净烧钱 / 剩余跑道 三条趋势，并 hover 看某份报告的精确值与日期。
3. 作为用户，我能一眼看到当前现金、剩余跑道、烧钱率、预计耗尽日期，以及相对上一份报告的变化。
4. 作为用户，我能看到最新报告的分类支出构成（占比条）及各类较上次报告的增减。
5. 作为新用户（还没保存过报告），我看到清晰的空状态引导，而不是报错或空白。

## Resolved Decisions

- **分类颜色/名称**：`ExpenseCategoryMajor` 已有 `color`、`icon`、`name`、`sortOrder` 字段 → 后端 trend 端点按 code join，返回富化分类 `{code, name, color, amount}`。前端不需手写调色板。
- **币种展示**：沿用 USD（与 `RunwayReportView` 一致）。设计稿 ¥ 仅 mock。
- **预计现金耗尽 KPI**：用最新报告快照里的 `depletionDate`（保存当时算的），不重算。
- **删除报告**：本页不提供删除入口（在 跑道报告 页管理）。

## Open Questions

- **区间「全部」上限**：报告很多时是否封顶（如最多 24/50 份）以控图表密度？默认全部，若过密再定。
- **同一时间多份报告**：逐报告原始点都画（x 轴按 savedAt 时间戳）；标签用日期还是「日期+序号」？默认日期，重叠再说。

## Referenced Capabilities

- `RunwayReport` 模型 / `RunwayReportRepository`（`findByFamilyId...`）— 数据来源。
- `RunwayReportService` — 扩展新增 `getTrend(familyId)`（解析快照）。
- `RunwayReportController` `/runway/reports` — 新增 `GET /trend` 子路由。
- `ExpenseCategoryMajor` / `ExpenseCategoryMajorRepository` — 分类 code→名称映射。
- 前端 `api/runway.js` — 新增 `getRunwayTrend(familyId)`。
- `frontend/src/router/index.js` — 新增 `analysis/runway-trend` 路由。
- `frontend/src/components/Sidebar.vue` — 财务规划 分组新增导航项。
- `finance-ui-tokens`（claude.ai/design）— 品牌令牌，绿色 primary。
- chart.js / vue-chartjs — 趋势图渲染（项目已有）。
- `formatCurrency` / `formatUSD` helper — 金额格式化。

## Design System

设计源：claude.ai/design 项目 `8f1c7aeb-bc33-4593-97ec-c14ad6ae8394`，文件 `Runway Trends.dc.html`（已定稿，作为唯一视觉真源）。
样式令牌：`finance-ui-tokens`（同步项目 `95a54834-da84-4c49-8176-159ab329b9fb`）— shadcn 令牌，绿色 primary `142 76% 36%`，中性底色，radius `0.5rem`，浅/深色。
未走 awesome-design-md 重新 mock：已有定稿保真度更高，实现阶段直接把 `.dc.html` 布局翻译成 Vue + Tailwind。
