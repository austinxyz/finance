## Why

When facing a potential job transition or income disruption, users need to quickly answer: "How long can my liquid assets sustain my current lifestyle?" The finance system already captures monthly expenses and liquid assets but has no way to synthesize this into a concrete runway estimate. This feature closes that gap by turning existing data into actionable financial clarity.

## What Changes

- New **Runway Analysis** page accessible from the main navigation
- Backend API that computes runway months by dividing total liquid assets by projected monthly burn
- Liquid assets pulled from existing asset records (cash, stocks/brokerage, crypto — user can toggle which accounts to include)
- Monthly burn rate derived from the last N months of actual expense records (configurable lookback, default 6 months)
- Scenario modeling: user can adjust a monthly expense multiplier to model optimistic/pessimistic spending
- Visual timeline showing when liquid assets run out under each scenario
- Summary metrics: liquid total, monthly burn, runway months, projected depletion date

## Capabilities

### New Capabilities
- `runway-analysis`: Runway calculation page — shows liquid asset total, projected monthly burn from expense history, and months of runway with scenario modeling (base/optimistic/pessimistic)

### Modified Capabilities

## Impact

- **Backend**: New `RunwayController` + `RunwayService`; reads from existing `AssetRecord` and `ExpenseRecord` repositories — no schema changes
- **Frontend**: New `RunwayAnalysis.vue` view; new route `/runway`; reuses existing `formatCurrency` helper and Chart.js
- **APIs**: New endpoint `GET /api/runway/analysis?familyId=&months=6`
- **No breaking changes** — purely additive
