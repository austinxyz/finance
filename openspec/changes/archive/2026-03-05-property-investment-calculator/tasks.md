# Tasks: property-investment-calculator

## Group 1: Sidebar — 投资 tab

- [x] 1.1 In `frontend/src/components/Sidebar.vue`, import `Building2` from `lucide-vue-next` and add it to the existing imports
- [x] 1.2 Add `{ key: 'investments', label: '投资', icon: Building2 }` to `topLevelTabs` array between `analysis` and `settings`
- [x] 1.3 In the route watcher, add `else if (newPath.startsWith('/investments')) { activeTopTab.value = 'investments' }` before the `else` branch
- [x] 1.4 Add the 投资 menu template block (v-if="activeTopTab === 'investments'") with a 投资工具 section containing a nav link `房产计算器` → `/investments/property-calculator`

## Group 2: Router

- [x] 2.1 In `frontend/src/router/index.js`, add a new top-level route `{ path: '/investments', children: [...] }` (or add under the existing layout route), containing: `{ path: 'property-calculator', name: 'PropertyInvestmentCalculator', component: () => import('../views/investments/PropertyInvestmentCalculator.vue') }`

## Group 3: Financial formula helpers

- [x] 3.1 Create `frontend/src/utils/financialFormulas.js` with pure JS functions:
  - `pmt(annualRate, years, presentValue)` — monthly mortgage payment
  - `cumprinc(annualRate, years, presentValue, startPeriod, endPeriod)` — cumulative principal
  - `cumipmt(annualRate, years, presentValue, startPeriod, endPeriod)` — cumulative interest
  - Each function returns a positive number representing the absolute payment amount

## Group 4: PropertyInvestmentCalculator.vue — inputs and structure

- [x] 4.1 Create `frontend/src/views/investments/PropertyInvestmentCalculator.vue` with `<script setup>` and all 13 input `ref`s pre-filled with default values:
  - purchasePrice=1500000, downPaymentPct=0.25, closingCostPct=0.02
  - annualRate=0.065, monthlyRent=5000, propertyTaxRate=0.0125
  - annualInsurance=1200, monthlyHOA=400, vacancyRate=0.05
  - maintenancePct=0.01, marginalTaxRate=0.47, landValuePct=0.60
  - appreciationRate=0.04
- [x] 4.2 Build the input form UI: two-column grid on md+, single column on mobile. Group inputs with labels showing parameter name (Chinese) and hint text (from spreadsheet notes). Use `type="number"` inputs with `step` attributes.

## Group 5: Computed properties — all 5 sections

- [x] 5.1 Implement Section 1 computed (monthly cash flow):
  - `loanAmount`, `grossRentalIncome`, `mortgagePayment` (using `pmt()`), `monthlyPropertyTax`, `monthlyInsurance`, `monthlyMaintenance`, `trueMonthlyFlow`
  - Add `data-testid="true-monthly-flow"` on the result element
- [x] 5.2 Implement Section 2 computed (core metrics):
  - `cashInvested`, `annualCashFlow`, `annualPrincipalPaydown` (using `cumprinc()`), `cocReturn`
  - Add `data-testid="coc-return"` and `data-testid="cash-invested"` on result elements
- [x] 5.3 Implement Section 3 computed (tax-adjusted):
  - `annualDepreciation`, `firstYearInterest` (using `cumipmt()`), `noi`, `paperLoss`, `taxSavingsBank`, `effectiveYield`
  - Add `data-testid="effective-yield"` on result element
- [x] 5.4 Implement Section 4 computed (appreciation + total ROI):
  - `appreciationGain`, `appreciationROI`, `totalWealthROI`
  - Add `data-testid="total-roi"` on result element

## Group 6: Results dashboard UI

- [x] 6.1 Build Section 1 card — Monthly Cash Flow breakdown table: Gross Income, Mortgage, Property Tax, Insurance, HOA, Maintenance, True Monthly Flow. Color True Monthly Flow red if negative, green if positive.
- [x] 6.2 Build Section 2 card — Core Metrics: Cash Invested, Annual Cash Flow, Annual Principal Paydown, CoC Return. Color CoC Return: red < 0%, yellow 0–4%, green ≥ 8%.
- [x] 6.3 Build Section 3 card — Tax-Adjusted Return: Annual Depreciation, First-Year Interest, NOI, Paper Loss, Tax Savings Bank, Effective Yield. Show suspended-loss disclaimer note when Paper Loss < 0.
- [x] 6.4 Build Section 4 card — Appreciation & Final Verdict: Appreciation Gain, Appreciation ROI, and a prominent 🏁 Total Wealth ROI display. Color Total ROI: red < 8%, yellow 8–14%, green ≥ 15%. Add benchmark label text (低于大盘均值 / 接近大盘 / 目标：超越大盘).

## Group 7: Vitest component tests

- [x] 7.1 Create `frontend/src/views/__tests__/PropertyInvestmentCalculator.test.js` with Vitest + Vue Test Utils. Test `financialFormulas.js` pure functions:
  - `pmt(0.065, 30, 1125000)` ≈ 7136 (within 1)
  - `cumprinc(0.065, 30, 1125000, 1, 12)` produces a positive value < monthlyPayment
  - `cumipmt(0.065, 30, 1125000, 1, 12)` produces a positive value > 0
- [x] 7.2 Mount the component and test computed rendering:
  - Default inputs: `[data-testid="true-monthly-flow"]` text contains a negative number
  - Default inputs: `[data-testid="coc-return"]` text contains "-" (negative CoC)
  - Default inputs: `[data-testid="cash-invested"]` text contains "$412,500" (1.5M × 27.5%)
  - Default inputs: annual depreciation = (1.5M × 0.4) / 27.5 ≈ $21,818
- [x] 7.3 Test reactive update: change `monthlyRent` ref value → assert `trueMonthlyFlow` changes accordingly (use `wrapper.setData` or directly mutate `ref` and call `nextTick`)

## Group 8: Smoke test (optional)

- [ ] 8.1 (optional) Manually navigate to `/investments/property-calculator`, verify 投资 tab is active, all 5 result sections display, change Purchase Price to $2,000,000, confirm Total ROI updates immediately
