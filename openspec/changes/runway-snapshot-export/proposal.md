## Why

The runway analysis page recalculates from live data every visit, meaning customizations (excluded accounts, expense adjustments) are lost and past planning scenarios cannot be revisited. Users need to save a specific snapshot — with their adjustments intact — and retrieve it later for comparison or reference.

## What Changes

- Add a **Save Report** button on the runway analysis page that persists the current state (settings, exclusions, adjustments, computed values) to the backend database.
- Reports are named automatically as `runway-YYYY-MM-DD-report`. If multiple reports exist for the same date, a sequence suffix is added (e.g., `runway-2026-03-04-report-2`).
- Add a new **Runway Reports** page that lists all saved reports for the current family, showing report name and saved date.
- Each report in the list is clickable and opens a read-only view of the saved snapshot (same layout as the runway analysis page but displaying historical values).
- Add an **Export PDF** button on both the live analysis page and the report view page to download a formatted PDF for offline review.

## Capabilities

### New Capabilities
- `runway-report-persistence`: Save current runway analysis state to the backend and retrieve it later
- `runway-report-list`: View all saved runway reports for the family
- `runway-report-view`: Display a previously saved runway report in read-only mode
- `runway-pdf-export`: Generate a formatted PDF report of the current runway analysis for offline review

### Modified Capabilities
- `runway-analysis`: UI gains Save Report and Export PDF controls; existing calculation logic is unchanged

## Impact

- Backend: new `runway_reports` table, entity, repository, service, controller
- Frontend: `RunwayAnalysis.vue` gains Save Report and Export PDF buttons; two new pages (list and view); new route entries; new Sidebar nav link
- One new frontend dependency: `jsPDF` (client-side PDF generation, ~300 KB)

## Non-goals

- Restoring saved settings back into the live analysis page (view is read-only)
- Editing or renaming saved reports
- Sharing reports between families
- Excel/CSV export
- Client-side JSON file save/load
