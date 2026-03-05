## ADDED Requirements

### Requirement: Save current runway analysis as a report
The system SHALL allow the user to save the current runway analysis state — including settings, excluded account IDs, expense adjustments, and all computed snapshot values — to the backend database. The save SHALL be triggered by a single button click with no confirmation dialog required. The report name SHALL be auto-generated as `runway-YYYY-MM-DD-report`.

#### Scenario: Save with default settings persists full snapshot
- **WHEN** the user clicks the Save Report button with no accounts excluded and no adjustments
- **THEN** a new record is created in `runway_reports` with the family's ID, a report name of `runway-YYYY-MM-DD-report`, the current timestamp as `saved_at`, and a `snapshot_json` containing version, settings, empty excludedAccountIds array, empty expenseAdjustments object, and a snapshot block with liquidTotal, monthlyBurn, runwayMonths, depletionDate, accountBreakdown, and expenseBreakdown

#### Scenario: Save preserves excluded accounts and adjustments
- **WHEN** the user has unchecked two accounts and set INSURANCE adjustment to +200, then clicks Save Report
- **THEN** the stored `snapshot_json` contains those two account IDs in excludedAccountIds and expenseAdjustments with INSURANCE: 200

#### Scenario: Duplicate date appends sequence suffix
- **WHEN** a report named `runway-2026-03-04-report` already exists for the family and the user saves another report on the same date
- **THEN** the new report is saved as `runway-2026-03-04-report-2`

#### Scenario: Save button is disabled when no data is loaded
- **WHEN** the page has not yet loaded data (data is null)
- **THEN** the Save Report button is disabled and not clickable

### Requirement: List saved runway reports
The system SHALL display a list of all saved runway reports for the current family, showing report name and saved date, ordered by most recent first.

#### Scenario: List shows all saved reports for the family
- **WHEN** the user navigates to the Runway Reports page
- **THEN** all reports saved by the current family are shown, each displaying its report name and saved date, ordered from newest to oldest

#### Scenario: Empty state when no reports exist
- **WHEN** the user navigates to the Runway Reports page and no reports have been saved
- **THEN** an empty state message is displayed (e.g., "No reports saved yet")

#### Scenario: Reports are isolated by family
- **WHEN** the user is logged in as family 1
- **THEN** only reports with family_id = 1 are shown; reports from other families are never returned

### Requirement: View a saved runway report
The system SHALL allow the user to open any saved report and view its historical snapshot values in a read-only display matching the layout of the live runway analysis page.

#### Scenario: Clicking a report opens the historical view
- **WHEN** the user clicks on a report in the list
- **THEN** a new page opens showing the stored snapshot values: liquid total, monthly burn, runway months, depletion date, scenario comparison, account breakdown, and expense breakdown — exactly as they were at save time

#### Scenario: View page does not re-fetch live data
- **WHEN** the report was saved with a liquid total of $450,000 but current asset values have since changed
- **THEN** the view page still shows $450,000 (the stored historical value), not the current live value

#### Scenario: Delete a report removes it from the list
- **WHEN** the user deletes a report from the list or view page
- **THEN** the report is removed from `runway_reports` and no longer appears in the list

### Requirement: Export runway analysis as PDF report
The system SHALL allow the user to export the current runway analysis (or a saved report) as a formatted PDF file for offline review.

#### Scenario: Export PDF from live analysis page
- **WHEN** the user clicks the Export PDF button on the live analysis page
- **THEN** the browser downloads a PDF file named `runway-report-YYYY-MM-DD.pdf` containing title, summary section, scenario comparison table, account breakdown table, and expense breakdown table — reflecting current exclusions and adjustments

#### Scenario: Export PDF from saved report view page
- **WHEN** the user opens a saved report and clicks Export PDF
- **THEN** the browser downloads a PDF reflecting the historical snapshot values stored in that report

#### Scenario: Export PDF button is disabled when no data is loaded
- **WHEN** the page has not yet loaded data (data is null)
- **THEN** the Export PDF button is disabled and not clickable

#### Scenario: PDF filename includes current date
- **WHEN** the user exports on 2026-03-04
- **THEN** the downloaded file is named `runway-report-2026-03-04.pdf`
