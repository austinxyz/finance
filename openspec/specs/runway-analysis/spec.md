## ADDED Requirements

### Requirement: Liquid asset total computation
The system SHALL compute total liquid assets by summing the most recent USD-converted value of each asset account belonging to the family, filtered to liquid asset types (CASH, STOCK, BROKERAGE, CRYPTO, DIGITAL_CURRENCY by default). The user SHALL be able to override which asset types are included via UI toggles.

#### Scenario: Default liquid asset calculation excludes illiquid types
- **WHEN** the runway analysis API is called without explicit type overrides
- **THEN** the system returns a `liquidTotal` that includes only CASH, STOCK, BROKERAGE, CRYPTO, and DIGITAL_CURRENCY account values using each account's latest snapshot

#### Scenario: User includes retirement accounts in liquid total
- **WHEN** the user toggles RETIREMENT as included in the UI
- **THEN** the API is called with `includedTypes` containing RETIREMENT and the returned `liquidTotal` reflects those accounts

#### Scenario: No asset records found
- **WHEN** the family has no asset records of any liquid type
- **THEN** the system returns `liquidTotal: 0` and a warning flag `assetDataMissing: true`

### Requirement: Monthly burn rate from expense history
The system SHALL compute monthly burn rate as the arithmetic mean of total expenses per month over the last N complete months (default N=6, configurable by the user). The system SHALL use actual expense records, not budgets.

#### Scenario: Burn rate computed from 6 months of data
- **WHEN** the user requests runway analysis with default settings
- **THEN** the system averages the total expenses of the 6 most recently completed calendar months and returns this as `monthlyBurn`

#### Scenario: User changes lookback window to 12 months
- **WHEN** the user selects a 12-month lookback in the UI
- **THEN** the API is called with `months=12` and `monthlyBurn` is the average of the last 12 complete months of expenses

#### Scenario: Fewer months of data than requested
- **WHEN** the family has only 3 months of expense records but 6 months are requested
- **THEN** the system uses the available 3 months, returns the actual count as `expenseMonthsUsed: 3`, and includes `expenseDataWarning: true`

### Requirement: Runway calculation and scenario modeling
The system SHALL calculate runway months as `floor(liquidTotal / monthlyBurn)` and return the projected depletion date. The frontend SHALL support three scenarios — base, optimistic (reduced burn), pessimistic (increased burn) — computed client-side by applying a multiplier to the base `monthlyBurn`.

#### Scenario: Base runway calculation
- **WHEN** `liquidTotal` is $640,000 and `monthlyBurn` is $17,000
- **THEN** `runwayMonths` is 37 and `depletionDate` is the current month plus 37 months

#### Scenario: Optimistic scenario (20% spending reduction)
- **WHEN** the user sets the optimistic multiplier to 0.8
- **THEN** the frontend recomputes runway as `floor(640000 / (17000 * 0.8))` = 47 months without an API call

#### Scenario: Pessimistic scenario (20% spending increase)
- **WHEN** the user sets the pessimistic multiplier to 1.2
- **THEN** the frontend recomputes runway as `floor(640000 / (17000 * 1.2))` = 31 months without an API call

#### Scenario: Monthly burn is zero or missing
- **WHEN** the family has no expense records and `monthlyBurn` computes to 0
- **THEN** the system returns `runwayMonths: null` and an error message indicating insufficient expense data

### Requirement: Asset breakdown display
The system SHALL return a breakdown of the liquid total by asset type and by individual account so users understand what is included in the liquid total.

#### Scenario: Asset breakdown returned in API response
- **WHEN** the runway analysis API is called
- **THEN** the response includes `accountBreakdown` listing each included account with its name, type, and USD value

#### Scenario: User sees account-level detail in the UI
- **WHEN** the RunwayAnalysis page loads
- **THEN** the UI displays a table or expandable section listing each liquid account and its contribution to the total

### Requirement: Expense breakdown display
The system SHALL return a breakdown of the average monthly burn by expense major category so users understand what drives their burn rate.

#### Scenario: Expense breakdown returned in API response
- **WHEN** the runway analysis API is called
- **THEN** the response includes `expenseBreakdown` listing each major expense category with its average monthly amount over the lookback period

### Requirement: Runway analysis page navigation
The system SHALL provide a dedicated Runway Analysis page accessible from the main navigation. The page SHALL be accessible to all authenticated users within a family.

#### Scenario: Navigation link appears for authenticated users
- **WHEN** a logged-in user views the main navigation sidebar
- **THEN** a "Runway" or "资金跑道" link is visible and navigates to `/runway`

#### Scenario: Unauthenticated access is blocked
- **WHEN** an unauthenticated user navigates to `/runway`
- **THEN** the route guard redirects them to the login page

### Requirement: Data freshness warning
The system SHALL display a warning when asset data is stale (most recent snapshot older than 30 days) or when expense data covers fewer months than requested.

#### Scenario: Stale asset data warning
- **WHEN** the most recent asset snapshot for any included account is older than 30 days
- **THEN** the UI displays a warning message indicating the data may not reflect current values, including the date of the most recent snapshot

#### Scenario: Insufficient expense data warning
- **WHEN** fewer than 3 months of expense records are available
- **THEN** the UI displays a warning that the burn rate estimate is based on limited data
