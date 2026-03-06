## ADDED Requirements

### Requirement: Input parameters with live defaults
The calculator SHALL present 13 editable input fields pre-filled with example values. All fields SHALL be numeric. Changes to any field SHALL trigger immediate recomputation of all output sections with no submit button required.

Default values: Purchase Price $1,500,000 · Down Payment 25% · Closing Cost 2% · Interest Rate 6.5% · Monthly Rent $5,000 · Property Tax Rate 1.25% · Insurance $1,200/yr · HOA $400/mo · Vacancy Rate 5% · Maintenance 1% · Marginal Tax Rate 47% · Land Value 60% · Projected Appreciation 4%.

#### Scenario: Page loads with defaults
- **WHEN** user navigates to `/investments/property-calculator`
- **THEN** all input fields are pre-filled with the default example values
- **AND** all computed results are visible immediately without any user action

#### Scenario: Input change triggers recomputation
- **WHEN** user changes Monthly Rent from $5,000 to $6,000
- **THEN** Gross Rental Income, True Monthly Cash Flow, Annual Cash Flow, CoC Return, Effective Yield, and Total Wealth ROI all update immediately

---

### Requirement: Monthly cash flow calculation
The calculator SHALL compute monthly cash flow components using these formulas:
- Gross Rental Income = Monthly Rent × (1 − Vacancy Rate)
- Mortgage P&I = PMT(annualRate/12, 360, −loanAmount) where loanAmount = purchasePrice × (1 − downPaymentPct)
- Property Tax (monthly) = purchasePrice × propertyTaxRate / 12
- Insurance (monthly) = annualInsurance / 12
- HOA = monthlyHOA (as entered)
- Maintenance (monthly) = purchasePrice × 0.01 / 12
- True Monthly Cash Flow = Gross Rental Income − Mortgage − PropertyTax − Insurance − HOA − Maintenance

#### Scenario: Negative cash flow displayed in red
- **WHEN** computed True Monthly Cash Flow is negative
- **THEN** the value is displayed in red text with a negative sign

#### Scenario: Positive cash flow displayed in green
- **WHEN** computed True Monthly Cash Flow is positive
- **THEN** the value is displayed in green text

#### Scenario: Default inputs produce expected mortgage
- **WHEN** inputs are at defaults (Purchase $1.5M, 25% down, 6.5% rate)
- **THEN** Mortgage P&I ≈ $7,136/mo (within $1 of PMT formula result)

---

### Requirement: Core metrics panel
The calculator SHALL display the following metrics:
- Cash Invested = purchasePrice × (downPaymentPct + closingCostPct)
- Annual Cash Flow = trueMonthlyFlow × 12
- Annual Principal Paydown = CUMPRINC(rate/12, 360, loan, 1, 12, 0) for year 1
- CoC Return = Annual Cash Flow / Cash Invested (displayed as percentage)

CoC Return SHALL be color-coded: red if < 0%, yellow if 0%–4%, green if ≥ 8%.

#### Scenario: CoC Return color coding negative
- **WHEN** Annual Cash Flow is negative
- **THEN** CoC Return is displayed in red

#### Scenario: Default inputs show negative CoC
- **WHEN** inputs are at defaults
- **THEN** CoC Return is negative (Bay Area cash flow negative scenario)

---

### Requirement: Tax-adjusted return calculation
The calculator SHALL compute the tax-adjusted metrics:
- Annual Depreciation = purchasePrice × (1 − landValuePct) / 27.5
- First-Year Interest = CUMIPMT(rate/12, 360, loan, 1, 12, 0) (absolute value)
- NOI = (grossRentalIncome − propertyTaxMonthly − insuranceMonthly − hoa − maintenanceMonthly) × 12
- Paper Loss = NOI − firstYearInterest − annualDepreciation
- Tax Savings Bank = Paper Loss × marginalTaxRate (only shown when Paper Loss < 0; displayed as positive benefit)
- Effective Yield = (Annual Cash Flow − Tax Savings Bank + Annual Principal Paydown) / Cash Invested

The page SHALL display an explanatory note that Tax Savings Bank is a "suspended loss" — not immediately deductible against W2 income unless the user qualifies as a Real Estate Professional.

#### Scenario: Depreciation computed correctly
- **WHEN** Purchase Price = $1,500,000 and Land Value = 60%
- **THEN** Annual Depreciation = $1,500,000 × 0.4 / 27.5 = $21,818 (±$1)

#### Scenario: Tax Savings Bank note displayed
- **WHEN** Paper Loss is negative (i.e., there is a tax-deductible paper loss)
- **THEN** a note is shown explaining this is a suspended passive loss

---

### Requirement: Appreciation and total wealth ROI
The calculator SHALL compute:
- Appreciation Gain = purchasePrice × projectedAppreciationRate
- Appreciation ROI = Appreciation Gain / Cash Invested
- Total Wealth ROI = Effective Yield + Appreciation ROI

Total Wealth ROI SHALL be color-coded against benchmarks:
- Red: < 8% (below S&P 500 long-term average)
- Yellow: 8%–14%
- Green: ≥ 15% (target: beats index with leverage)

#### Scenario: Total ROI benchmark color at 15%
- **WHEN** Total Wealth ROI ≥ 15%
- **THEN** it is displayed in green with label "目标：超越大盘"

#### Scenario: Total ROI benchmark color below 8%
- **WHEN** Total Wealth ROI < 8%
- **THEN** it is displayed in red with label "低于大盘均值"

---

### Requirement: 投资 sidebar tab
The Sidebar SHALL include a new top-level tab 投资 (Investments) inserted between 分析 and 设置.
Navigating to any `/investments/*` route SHALL activate this tab automatically.
The tab SHALL contain a nav link 房产计算器 pointing to `/investments/property-calculator`.

#### Scenario: Tab auto-activates on navigation
- **WHEN** user navigates to `/investments/property-calculator`
- **THEN** the 投资 tab is highlighted as active in the Sidebar

#### Scenario: Tab shows nav link
- **WHEN** 投资 tab is active
- **THEN** a nav link 房产计算器 is visible under 投资工具 section
