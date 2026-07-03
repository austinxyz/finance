## Context

资金跑道相关能力现状：`RunwayService.calculateRunway()` 计算**单点**（当前）跑道；`RunwayReport` 表存用户手动保存的快照（`snapshotJson` TEXT，含 `liquidTotal/monthlyBurn/runwayMonths/depletionDate/expenseBreakdown` 等）+ `savedAt`。缺一个把历史快照汇总成趋势的视图。需求文档（REVIEWED）已定：数据源为已保存快照（不重算），逐报告原始点。设计源为 claude.ai/design `Runway Trends.dc.html`（`finance-ui-tokens`）。

约束：家庭数据隔离（`authHelper.requireFamilyAccess`）；前端 Composition API + Tailwind + Chart.js；快照金额在保存时已折算为 USD。

## Goals / Non-Goals

**Goals:**
- 只读趋势端点：服务端解析快照，返回按 `savedAt` 升序的指标点数组 + 最新报告富化分类。
- 趋势页：KPI（含较上次报告变化）、可切换三指标趋势图、分类明细表、空/单报告态。
- 复用既有表与快照结构，零 schema 变更。

**Non-Goals:**
- 不重算历史跑道（不按日历月从时间序列还原）。
- 不改 `runway-analysis` / `runway-snapshot-export` 既有需求与写入路径。
- 不做多币种切换、PDF 导出、报告删除入口。

## Decisions

**D1. 数据源 = 聚合已保存快照（B），而非按月重算（A）。**
用户显式选择。B 复用现有快照，零 schema 变更，实现小。代价：趋势密度取决于用户保存频率。已否决 A（需 as-of-date 资产查询 + 新端点，更重）。

**D2. 逐报告原始点，而非按月归籽。**
用户显式选择。每份报告 = 一个点，按 `savedAt` 升序。X 轴按报告时间戳，非日历月。文案随之改为逐报告语义（近N份 / 报告值 / 较上次报告），避免误导。

**D3. 新增后端端点 `GET /runway/reports/trend?familyId=`，服务端解析快照。**
挂在既有 `RunwayReportController`（`@RequestMapping("/runway/reports")`）下新增 `@GetMapping("/trend")`。`RunwayReportService.getTrend(familyId)` 加载该家庭全部报告，逐个 `objectMapper.readTree(snapshotJson)` 抽取指标，构造 `RunwayTrendDTO`。
- 备选（否决）：前端 `listReports` + 逐 id `getReport` → N+1，且 `RunwayReportSummaryDTO` 无指标。
- 端点**不回传原始 `snapshotJson`**（体积 + 泄露）：只回抽取后的点 + 最新分类。
- 坏 JSON 单份跳过（try/catch per report），不整体失败。

**D4. 分类明细服务端富化。**
最新报告的 `snapshot.expenseBreakdown`（`{code: usdAmount}`）按 code join `ExpenseCategoryMajor`，输出 `{code, name, color, amount}`，按 amount 降序。`ExpenseCategoryMajor` 已有 `name`/`color`/`sortOrder` 字段，无需手写调色板。未知 code → 回退名称 + 默认色，不丢弃。

**D5. 货币：不做转换。**
快照金额保存时已是 USD（`RunwayService.toUsd` 在保存路径完成）。趋势端点直接读取，**不调用汇率服务**。前端沿用 `formatUSD`（与 `RunwayReportView` 一致）。说明：本能力不按记录读取原始币种金额，故不涉及 `ExchangeRateService`；若未来需按原币展示，须改为按记录调用 `ExchangeRateService.getExchangeRate(currency, date)`，禁止直接用 `ExchangeRateRepository`。

**D6. 图表用 Chart.js + vue-chartjs（项目标准依赖），非手写 SVG。**
设计稿的手写 SVG 仅作视觉参考；实现用 Chart.js 折线/面积 + 原生 tooltip，风格用 `finance-ui-tokens` 令牌（绿色 primary）。

**D7. KPI 派生与 depletionDate 取值在前端。**
端点回传原始点；前端算 KPI 值 + 较上次差值 + 涨跌色 + 区间截取。`预计现金耗尽` 直接用最新点快照里的 `depletionDate`（保存当时算的），不重算。

**D8. 依赖注入，便于 Mockito。**
`RunwayReportService` 通过构造注入 `RunwayReportRepository`、`ExpenseCategoryMajorRepository`、`ObjectMapper`（Spring 提供），无静态调用、业务逻辑内无 `new`，单测可 stub。

## Risks / Trade-offs

- [趋势稀疏/不规则] 保存频率低时点少、间距不均 → 文案明确「逐报告」，空/单报告态友好引导，不假装月度连续。
- [快照结构漂移] `snapshotJson` 结构以后变更 → 解析用宽松取字段 + per-report try/catch 跳过坏数据；只依赖稳定字段（liquidTotal/monthlyBurn/runwayMonths/depletionDate/expenseBreakdown）。
- [monthlyBurn 语义] 快照 burn 是该报告自身 lookback 的均值（常 6 月），非固定近 3 月 → KPI 文案用「月度净烧钱率（报告值）」，不写「近3月均」。
- [分类 code 缺失] 未知 code → 回退名/默认色，不崩。
- [大量报告图表过密] 暂全部；`全部` 若过密再定封顶（Open Question）。

## Migration Plan

无 schema 变更、无数据迁移。纯新增只读端点 + 新前端页/路由/导航项。部署 = 常规后端 + 前端构建。回滚 = 移除路由/导航项与端点即可，无残留状态。

## Open Questions

- `全部` 区间是否封顶（如最多 24/50 份）以控图表密度？默认全部，过密再定。
- 同一时间多份报告的 X 轴标签：日期 vs 日期+序号？默认日期，重叠再定。
