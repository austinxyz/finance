# Contract — Group 2: Frontend 资金跑道趋势 page

- **Spec**:
  - The system SHALL provide a page at route `/analysis/runway-trend`, lazily loaded, reachable from a navigation item in the 财务规划 group of the sidebar, positioned after 资金跑道分析 and 跑道报告.
  - The page SHALL display four KPI cards computed from the latest trend point (liquidTotal, runwayMonths, monthlyBurn, depletionDate); cash/runway/burn SHALL show change vs the immediately preceding report, colored by direction; fewer than two reports → delta renders `—`.
  - The page SHALL render a trend chart over the selected range supporting three switchable metrics (runway months, monthly net burn, cash balance) using Chart.js, with area/line rendering and a hover tooltip exposing the report's date and exact value.
  - The page SHALL provide a range selector truncating to the most recent N reports (近6份 / 近12份 / 全部); labels SHALL reflect report-count semantics.
  - The page SHALL display a category expense table for the latest report (name+color marker, amount, share bar, change vs previous report; 较上次报告 wording).
  - When the family has no saved reports, the page SHALL show an empty-state prompt linking to 资金跑道分析.
- **Runtime**: `cd frontend && npm run test -- RunwayTrend` → expected: component tests pass — KPI computed values + prior-report deltas, single-report `—`, metric switch, empty state, and token-class assertions.
- **Code**:
  - D2: per-report labels (近N份 / 报告值 / 较上次报告) — do NOT use monthly wording. KPI burn label = 月度净烧钱率（报告值）, never 近3月均.
  - D6/D7: Chart.js (vue-chartjs) for the trend; KPI deltas + range truncation computed in the view; 预计现金耗尽 uses the latest point's stored `depletionDate` (no recompute).
  - Conventions: Composition API `<script setup>`; Tailwind utility classes only — no inline styles / no scoped CSS (the mock's inline styles are reference only); axios interceptor unwraps → use `response.success`/`response.data`; `familyAPI.getDefault()` for familyId; `formatUSD` for money; route in router/index.js, nav in Sidebar.vue.
- **Threshold**: 70
