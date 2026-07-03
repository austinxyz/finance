# Tasks — runway-trends

## 1. Backend — trend aggregation endpoint

### Contract
- **Spec**:
  - The system SHALL expose a read-only endpoint `GET /runway/reports/trend?familyId=` that loads all `RunwayReport` rows for the family, parses each `snapshotJson` server-side, and returns a list of trend points ordered ascending by `savedAt`. Each point SHALL contain `savedAt`, `reportName`, `liquidTotal`, `monthlyBurn`, `runwayMonths`, and `depletionDate`. The response SHALL NOT include the raw `snapshotJson`. The endpoint SHALL enforce family access via the caller's auth token.
  - The trend response SHALL include the latest report's category expense breakdown, with each category enriched from `ExpenseCategoryMajor` by code into `{code, name, color, amount}`. Categories SHALL be sorted by amount descending.
- **Runtime**: `cd backend && mvn test -Dtest="RunwayReportServiceTest,RunwayReportTrendControllerTest"` → expected: all service + controller tests pass; covers multi/single/zero reports, corrupt-JSON skip, category enrichment, HTTP 200 envelope, and auth rejection.
- **Code**:
  - D3: parse `snapshotJson` server-side with `ObjectMapper.readTree`; per-report `try/catch` skips corrupt rows without failing the request; response returns extracted points only — NEVER the raw `snapshotJson`. Wrap in `Map` `{success, data}`.
  - D5: snapshot amounts are already USD (converted at save time) — do NOT inject `ExchangeRateService`/`ExchangeRateRepository`; multi-currency conversion is intentionally N/A for this read path.
  - D4: enrich latest report categories by joining `ExpenseCategoryMajor` (name/color); unknown code → fallback name + default color, not dropped.
  - D8: constructor injection of `RunwayReportRepository`, `ExpenseCategoryMajorRepository`, `ObjectMapper`; no static calls, no `new` in business logic (Mockito-stubbable). New `RunwayTrendDTO` (record). Enforce `authHelper.requireFamilyAccess`. No `SELECT *`.
- **Threshold**: 80

- [x] 1.0 CONTRACT — write openspec/changes/runway-trends/contracts/group-1.md with the ### Contract block above; confirm Spec, Runtime, Code all non-empty
- [x] 1.1 RED — Invoke superpowers:test-driven-development. Write failing `RunwayReportServiceTest.getTrend_*`: (a) 3 valid reports → 3 points ascending by savedAt with liquidTotal/monthlyBurn/runwayMonths/depletionDate; (b) zero reports → empty points; (c) one corrupt snapshotJson among valid → corrupt skipped, others returned
- [x] 1.2 GREEN — add `RunwayTrendDTO` (record: points[] + latest categories) and `RunwayReportService.getTrend(familyId)` parsing snapshots via ObjectMapper; minimal impl to pass 1.1
- [x] 1.3 RED — failing test: latest report's `expenseBreakdown` enriched from `ExpenseCategoryMajor` into `{code,name,color,amount}` sorted amount desc; unknown code → fallback name + default color
- [x] 1.4 GREEN — implement category enrichment in `getTrend` (join ExpenseCategoryMajorRepository); pass 1.3
- [x] 1.5 RED — failing `RunwayReportTrendControllerTest` (@WebMvcTest / MockMvc): `GET /runway/reports/trend?familyId=` returns 200 `{success:true,data:{points,categories}}` and does NOT contain `snapshotJson`; unauthorized family → rejected
- [x] 1.6 GREEN — add `@GetMapping("/trend")` to `RunwayReportController` calling service + `authHelper.requireFamilyAccess`; wrap in success/data Map; pass 1.5
- [x] 1.E EVAL — spawn evaluator subagent (haiku); reads contracts/group-1.md + spec + design + group diff; invokes superpowers:requesting-code-review (CRITICAL/HIGH = BLOCK); scores Spec/Runtime/Code; total ≥ 80 → PASS; < 80 → append FIX tasks + retry (max 3 attempts, plateau < 5pt = escalate)

## 2. Frontend — 资金跑道趋势 page

### Contract
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
  - Conventions: Composition API `<script setup>`; **Tailwind utility classes only — no inline styles / no scoped CSS** (the mock's inline styles are reference only); axios interceptor unwraps → use `response.success`/`response.data`; `familyAPI.getDefault()` for familyId; `formatUSD` for money; route in router/index.js, nav in Sidebar.vue.
- **Threshold**: 70

- [x] 2.0 CONTRACT — write openspec/changes/runway-trends/contracts/group-2.md with the ### Contract block above
- [x] 2.1 MOCK — open docs/superpowers/specs/mocks/2026-07-02-runway-trends-mocks.html; note finance-ui tokens (primary green, border, muted) and verbatim strings: 财务规划 · 资金跑道趋势, 当前现金余额, 剩余跑道, 月度净烧钱率（报告值）, 预计现金耗尽, 近6份/近12份/全部, 剩余跑道月数/月度净烧钱/现金余额, 分类支出明细, 较上次报告, 新增
- [x] 2.2 RED — Invoke superpowers:test-driven-development. Write failing vitest for `RunwayTrend.vue`: KPI computed from latest point + delta vs previous; single report → deltas `—`; empty reports → empty-state with link; metric toggle switches series. Assert token classes via `wrapper.classes()` (e.g. `/bg-primary/`, `/text-muted-foreground/`)
- [x] 2.3 GREEN — add `runwayAPI.getRunwayTrend(familyId)` in api/runway.js; implement `views/analysis/RunwayTrend.vue` (KPI cards, Chart.js trend with metric tabs + range pills, category table, empty/single states) using Tailwind utilities; pass 2.2
- [x] 2.4 GREEN — add route `analysis/runway-trend` (lazy) in router/index.js and nav item 资金跑道趋势 under 财务规划 in Sidebar.vue (after 跑道报告)
- [x] 2.5 VISUAL DIFF — bring up dev stack (`./backend/start.sh`; `cd frontend && npm run dev`); navigate to /analysis/runway-trend; eyeball against the mock; fix token/color/text drift
- [ ] 2.E EVAL — spawn evaluator subagent (haiku); reads contracts/group-2.md + spec + design + group diff; invokes superpowers:requesting-code-review (CRITICAL/HIGH = BLOCK); scores Spec/Runtime/Code; total ≥ 70 → PASS; < 70 → append FIX tasks + retry (max 3 attempts, plateau < 5pt = escalate)

## 3. Verification + ship

- [ ] 3.1 Run backend test suite — `cd backend && mvn test` — ensure no regressions
- [ ] 3.2 Run frontend test suite — `cd frontend && npm run test` — ensure no regressions
- [ ] 3.3 Manual smoke test (optional) — save ≥2 runway reports, open /analysis/runway-trend, verify KPI deltas, metric switch, range pills, category table, and empty state (fresh family)
- [ ] 3.4 Run superpowers:verification-before-completion — run test suites; `grep -r console.log frontend/src` for the new view; confirm no inline styles / scoped CSS in RunwayTrend.vue
