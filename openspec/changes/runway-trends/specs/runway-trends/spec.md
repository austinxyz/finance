## ADDED Requirements

### Requirement: Runway trend aggregation endpoint

The system SHALL expose a read-only endpoint `GET /runway/reports/trend?familyId=` that loads all `RunwayReport` rows for the family, parses each `snapshotJson` server-side, and returns a list of trend points ordered ascending by `savedAt`. Each point SHALL contain `savedAt`, `reportName`, `liquidTotal`, `monthlyBurn`, `runwayMonths`, and `depletionDate`. The response SHALL NOT include the raw `snapshotJson`. The endpoint SHALL enforce family access via the caller's auth token.

#### Scenario: Family with multiple saved reports
- **WHEN** an authenticated user requests the trend for their family and 3 reports exist
- **THEN** the response contains 3 points ordered by `savedAt` ascending, each with `liquidTotal`, `monthlyBurn`, `runwayMonths`, and `depletionDate` extracted from that report's snapshot

#### Scenario: Family with no saved reports
- **WHEN** an authenticated user requests the trend and no reports exist
- **THEN** the response is a success envelope with an empty points list (no error)

#### Scenario: Access denied for another family
- **WHEN** a non-admin user requests the trend for a `familyId` that is not theirs
- **THEN** the request is rejected by the family-access check and no data is returned

#### Scenario: Corrupt snapshot is skipped
- **WHEN** one stored report has malformed `snapshotJson` but others are valid
- **THEN** the endpoint returns the valid points and omits the corrupt one without failing the whole request

### Requirement: Latest report category breakdown enrichment

The trend response SHALL include the latest report's category expense breakdown, with each category enriched from `ExpenseCategoryMajor` by code into `{code, name, color, amount}`. Categories SHALL be sorted by amount descending.

#### Scenario: Categories enriched from database
- **WHEN** the latest report's snapshot `expenseBreakdown` contains category codes
- **THEN** each returned category has its `name` and `color` populated from the matching `ExpenseCategoryMajor` record and the list is sorted by amount descending

#### Scenario: Unknown category code
- **WHEN** a snapshot category code has no matching `ExpenseCategoryMajor`
- **THEN** the category is still returned with a fallback name and a default color, not dropped

### Requirement: Runway trend page navigation

The system SHALL provide a page at route `/analysis/runway-trend`, lazily loaded, reachable from a navigation item in the 财务规划 group of the sidebar, positioned after 资金跑道分析 and 跑道报告.

#### Scenario: Navigate to trend page
- **WHEN** a user clicks the 资金跑道趋势 item under 财务规划
- **THEN** the router navigates to `/analysis/runway-trend` and the page component loads

### Requirement: KPI cards with prior-report deltas

The page SHALL display four KPI cards computed from the latest trend point: current cash balance (`liquidTotal`), remaining runway months (`runwayMonths`), monthly net burn (`monthlyBurn`), and projected depletion date (`depletionDate`). For cash, runway, and burn, each card SHALL show the change relative to the immediately preceding report, colored by direction. When fewer than two reports exist, the delta SHALL render as `—`.

#### Scenario: Deltas shown with two or more reports
- **WHEN** at least two reports exist
- **THEN** each of the cash, runway, and burn cards shows the latest value and a signed change versus the previous report, with increase/decrease coloring

#### Scenario: Single report hides deltas
- **WHEN** exactly one report exists
- **THEN** the KPI values render from that report and every delta field shows `—`

### Requirement: Switchable trend chart

The page SHALL render a trend chart over the selected range of reports, supporting three switchable metrics — remaining runway months, monthly net burn, and cash balance — using Chart.js, with area/line rendering and a hover tooltip exposing the report's date and exact value.

#### Scenario: Switch metric
- **WHEN** the user selects a different metric tab (runway / burn / cash)
- **THEN** the chart re-renders showing that metric's series across the selected reports

#### Scenario: Hover reveals point detail
- **WHEN** the user hovers a data point
- **THEN** a tooltip shows that report's date and the metric's exact value

### Requirement: Report-count range selection

The page SHALL provide a range selector that truncates to the most recent N reports (近6份 / 近12份 / 全部), not calendar months. Labels SHALL reflect report-count semantics, not monthly semantics.

#### Scenario: Limit to recent reports
- **WHEN** 20 reports exist and the user selects 近12份
- **THEN** the chart and derived views use only the 12 most recent reports by `savedAt`

### Requirement: Category detail table

The page SHALL display a category expense table for the latest report showing category name (with color marker), amount, share-of-total bar, and change versus the previous report's same category. Labels SHALL use per-report wording (较上次报告), not monthly wording.

#### Scenario: Category row rendering
- **WHEN** the latest report has category breakdown data
- **THEN** each row shows name, amount, a share bar proportional to amount, and a delta versus the previous report (新增 when absent previously)

### Requirement: Empty state guidance

When the family has no saved reports, the page SHALL show an empty-state prompt directing the user to save a report from 资金跑道分析, with a navigation link, instead of an error or blank page.

#### Scenario: No reports empty state
- **WHEN** the family has zero saved reports
- **THEN** the page shows guidance text and a link to 资金跑道分析, and does not error
