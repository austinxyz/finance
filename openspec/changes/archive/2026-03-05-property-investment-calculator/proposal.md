## Why

High-income W2 earners in the Bay Area often evaluate rental property investments without a rigorous, tax-aware model. The existing "Brutal Calculator" spreadsheet captures this analysis precisely — covering mortgage amortization, vacancy, depreciation tax shields, and leveraged appreciation — but lives outside the app. Bringing it in as a native, interactive tool makes it accessible, shareable within the family, and consistent with the app's financial planning suite.

## What Changes

- Add a new **投资 (Investments)** top-level tab to the Sidebar, between 分析 and 设置
- Add a new page **Property Investment Calculator** (`/investments/property-calculator`) under 投资
- The page contains an interactive input form (11 parameters) and a live-computed results dashboard with 5 sections: monthly cash flow, core metrics, tax-adjusted return, appreciation return, and total wealth ROI
- All calculations are pure client-side JavaScript — no backend changes required

## Capabilities

### New Capabilities
- `property-investment-calculator`: Interactive rental property ROI calculator with full tax-adjusted analysis (mortgage P&I, depreciation, suspended passive loss, CoC return, total wealth ROI)

### Modified Capabilities
*(none — no existing spec behavior changes)*

## Impact

- **Frontend only**: new Vue component `PropertyInvestmentCalculator.vue`, router route, Sidebar tab + nav link
- **No backend changes**: zero API calls, zero DB schema changes
- **No existing features touched** except Sidebar (adding one tab) and router (adding one route)
