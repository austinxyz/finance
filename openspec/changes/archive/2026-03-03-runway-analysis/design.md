## Context

The finance system already stores two key data sources needed for runway analysis:
- **Liquid assets**: `asset_records` table — cash, stock/brokerage, crypto accounts with USD-converted values
- **Monthly expenses**: `expense_records` table — actual recorded spending per month per family

The feature synthesizes these into a runway calculation: `liquid_assets / monthly_burn_rate = months_of_runway`. No schema changes are required.

Current state: users can view their assets and expenses separately but have no single view that answers "how long can I survive without income?"

## Goals / Non-Goals

**Goals:**
- Compute total liquid assets from selected account types (user can toggle which asset types count as liquid)
- Derive monthly burn rate from actual expense records over a configurable lookback window (default 6 months)
- Show runway in months + projected depletion date
- Support three scenarios: base (actual burn), optimistic (user-defined % reduction), pessimistic (user-defined % increase)
- Display a visual burn-down timeline chart

**Non-Goals:**
- Income projection or re-employment modeling
- Tax calculations on asset liquidation
- Real estate or retirement account liquidation scenarios
- Editing/overriding individual expense records from this page

## Decisions

### Decision 1: Pull liquid assets from latest snapshot per account, not historical average
**Why**: Runway calculation should reflect current wealth, not average historical balance. Each asset account stores time-series snapshots; we use the most recent record per account.
**Alternative considered**: Average of last N months. Rejected — a stock account's current value is what's actually available, not its 6-month average.

### Decision 2: Classify liquid asset types server-side with user override via query param
**Liquid by default**: CASH, STOCK, BROKERAGE, CRYPTO, DIGITAL_CURRENCY
**Illiquid by default**: REAL_ESTATE, INSURANCE, RETIREMENT (401k/IRA — penalty applies)
**Why**: Matches the mental model from the runway-calculation example doc where real estate and 401k are explicitly excluded. User can override via toggles in the UI which sends `includedTypes` list to the API.

### Decision 3: Monthly burn = average of last N months of actual expense records
**Why**: Actual recorded expenses are already in the system and are more accurate than budget estimates. Default 6-month window smooths out one-off large expenses.
**Alternative considered**: Use expense budgets. Rejected — budgets are forward-looking targets, not actuals.

### Decision 4: Scenario modeling as client-side multiplier on base burn
The API returns a single `monthlyBurn` value. The frontend applies multipliers (e.g., 0.8× for optimistic, 1.2× for pessimistic) and recomputes runway in real-time without additional API calls.
**Why**: Keeps the API simple; scenarios are a presentation concern.

### Decision 5: New endpoint, no modification to existing APIs
`GET /api/runway/analysis` returns `liquidTotal`, `monthlyBurn`, `runwayMonths`, `accountBreakdown`, `expenseBreakdown`.
**Why**: Clean separation; existing dashboard endpoints are unaffected.

## Risks / Trade-offs

- **Stale asset data** → Users who haven't entered recent asset snapshots will see outdated liquid totals. Mitigation: show the date of the most recent snapshot in the UI with a warning if >30 days old.
- **Expense records sparse or missing** → If few months of expense data exist, the burn rate average is unreliable. Mitigation: show the actual number of months used in the calculation and warn if <3 months of data.
- **Multi-currency assets** → All assets are already stored with USD-converted values in `asset_records`. Use the stored USD value directly; no real-time exchange rate lookup needed.
- **Large expense one-offs inflate burn** → A $24k property tax payment in one month skews the 6-month average. Mitigation: document in UI that the lookback window is configurable; user can extend to 12 months to smooth outliers.
