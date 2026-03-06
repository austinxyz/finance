## Context

The "Brutal Calculator" spreadsheet models US rental property investment returns for high-income W2 earners in high-tax states (California). It includes five calculation sections: monthly cash flow (mortgage amortization, vacancy, HOA, maintenance), core metrics (CoC return, annual principal paydown), tax-adjusted return (MACRS depreciation, suspended passive loss, tax savings bank), appreciation return, and total wealth ROI. All formulas use standard financial math (PMT, CUMPRINC, CUMIPMT). The feature is purely a calculator — no data is persisted, no backend is needed.

## Goals / Non-Goals

**Goals:**
- Implement all 11 input parameters from the spreadsheet with sensible defaults matching the example values
- Replicate all 5 calculation sections exactly as defined in the spreadsheet formulas
- Live-recompute all outputs on every input change (reactive computed properties)
- Add a new 投资 top-level sidebar tab between 分析 and 设置
- Add `data-testid` attributes on key result elements for Vitest component tests

**Non-Goals:**
- Saving/persisting calculator inputs or results to the database
- Comparing multiple properties side-by-side (single property at a time)
- Integration with existing asset records (standalone calculator)
- Backend API changes

## Decisions

### Pure client-side computation
All formulas are deterministic mathematical functions of the inputs. No API calls required. Implemented as Vue `computed` properties that reactively update as the user types.

**Alternatives considered**: A backend `/api/investment/calculate` endpoint — rejected because it adds latency, unnecessary infra complexity, and the formulas are stateless. No data reads from DB.

### Financial formula implementations (JavaScript)

| Formula | Implementation |
|---------|---------------|
| `PMT(rate, nper, pv)` | Standard annuity formula: `rate * pv / (1 - (1+rate)^-nper)` |
| `CUMPRINC(rate, nper, pv, start, end)` | Sum principal portions of each payment in range |
| `CUMIPMT(rate, nper, pv, start, end)` | Sum interest portions of each payment in range |

These are pure JS helper functions, tested independently in Vitest.

### Sidebar: new 投资 tab
Add `{ key: 'investments', label: '投资', icon: Building2 }` to `topLevelTabs` array in `Sidebar.vue`, inserted between `analysis` and `settings`. Route watch updated to activate this tab for `/investments/*` paths.

### Single Vue component
`PropertyInvestmentCalculator.vue` contains the full page: input form (left or top panel) + results dashboard (right or bottom). No sub-components needed for this scope.

### Default values
Pre-filled with the spreadsheet's example values so users see a working demonstration on load:
- Purchase Price: $1,500,000 | Down Payment: 25% | Closing Cost: 2%
- Interest Rate: 6.5% | Monthly Rent: $5,000 | Property Tax: 1.25%
- Insurance: $1,200/yr | HOA: $400/mo | Vacancy: 5% | Maintenance: 1%
- Marginal Tax Rate: 47% | Land Value: 60% | Appreciation: 4%

## Risks / Trade-offs

- **Floating-point precision**: JS `number` is sufficient for dollar amounts displayed to 0-2 decimals. No BigDecimal needed since this is a display calculator, not stored financial data.
- **PMT/CUMPRINC accuracy**: Standard formulas produce results matching Excel to within $0.01 per payment. Acceptable for estimation purposes.
- **No input validation persistence**: If user navigates away, inputs reset to defaults. Acceptable for v1 — saving state is out of scope.
