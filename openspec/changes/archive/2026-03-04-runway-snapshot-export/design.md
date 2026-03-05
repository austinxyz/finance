## Context

The runway analysis page computes a live snapshot from current DB data on every load. Users can customize it (exclude accounts, adjust expense categories), but those customizations exist only in component state and are lost on navigation or refresh. There is no way to compare today's runway against a scenario planned last month.

This change adds backend persistence for runway snapshots — saved as JSON blobs in a new `runway_reports` table — with a list page and a read-only view page on the frontend.

## Goals / Non-Goals

**Goals:**
- Save the full current analysis state (settings, exclusions, adjustments, computed values) to the backend with one click
- List all saved reports for the family
- Display any saved report in a read-only view matching the live analysis layout
- Generate a formatted PDF report for offline review

**Non-Goals:**
- Restoring saved settings into the live analysis page (view is always read-only)
- Editing or renaming saved reports after creation
- Sharing reports between families
- Excel/CSV export
- Client-side JSON file save/load

## Decisions

**Decision 1 — Store snapshot as a JSON blob in a new `runway_reports` table**
The snapshot is a derived view of existing data with user-specific adjustments. Rather than normalizing each field into columns, storing the full snapshot as a single JSON TEXT column keeps the schema simple, avoids migration complexity, and allows the frontend to receive the exact same shape it already knows how to render.

Schema:
```sql
CREATE TABLE runway_reports (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  family_id   BIGINT       NOT NULL,
  report_name VARCHAR(255) NOT NULL,
  saved_at    DATETIME     NOT NULL,
  snapshot_json TEXT       NOT NULL,
  INDEX idx_runway_reports_family_id (family_id)
);
```

*Alternative considered*: Normalize settings and breakdown into separate columns/tables. Rejected — adds join complexity with no query benefit since we always read the full report.

**Decision 2 — Snapshot JSON format stored in `snapshot_json`**
```json
{
  "version": "1",
  "settings": {
    "lookbackMonths": 6,
    "optimisticMultiplier": 0.8,
    "pessimisticMultiplier": 1.2
  },
  "excludedAccountIds": [3, 7],
  "expenseAdjustments": { "INSURANCE": 200, "FOOD": -50 },
  "snapshot": {
    "liquidTotal": 450000,
    "monthlyBurn": 4200,
    "runwayMonths": 107,
    "depletionDate": "2035-02",
    "accountBreakdown": [...],
    "expenseBreakdown": {...}
  }
}
```
This captures the full page state at save time. The `snapshot` block contains already-computed effective values (reflecting exclusions and adjustments) so the view page can render without re-fetching.

**Decision 3 — Report name auto-generated as `runway-YYYY-MM-DD-report`**
Name is generated server-side at save time. If a report with that name already exists for the family, append a numeric suffix (`runway-2026-03-04-report-2`, etc.). Users cannot rename reports.

**Decision 4 — Report view page renders stored snapshot values, no re-fetch**
Unlike the live analysis page (which always re-fetches from DB), the view page renders the stored JSON directly. This intentionally shows the historical state — the values as they were on the day the report was saved.
*Alternative considered*: Re-fetch fresh DB values and apply saved settings on top. Rejected — a report should be a fixed historical record, not a live recalculation.

**Decision 5 — PDF generation using jsPDF, client-side only**
Available on both the live analysis page and the report view page. Built programmatically (text + lines), not screenshot-based, producing searchable text and smaller file sizes.
*Alternative considered*: `html2canvas` + jsPDF screenshot. Rejected — blurry output, large file size, captures UI chrome rather than a clean report.

PDF report sections (in order):
1. **Header**: "Runway Analysis Report" title, report date, family ID
2. **Summary**: Liquid Total, Monthly Burn, Runway Months, Depletion Date (2×2 grid)
3. **Scenario Comparison**: Base / Optimistic / Pessimistic columns (multiplier, monthly burn, runway months, depletion date)
4. **Account Breakdown**: included accounts (name, type, USD value)
5. **Expense Breakdown**: category, base amount, adjustment, effective amount
6. **Settings footer**: lookback months, multipliers

PDF filename: `runway-report-YYYY-MM-DD.pdf`

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/runway/reports` | Save current snapshot |
| GET | `/api/runway/reports?familyId=X` | List all reports for family |
| GET | `/api/runway/reports/{id}` | Get full report by ID |
| DELETE | `/api/runway/reports/{id}` | Delete a report |

Request body for POST:
```json
{ "familyId": 1, "snapshotJson": "{...}" }
```

List response item shape:
```json
{ "id": 12, "reportName": "runway-2026-03-04-report", "savedAt": "2026-03-04T10:30:00" }
```

## Risks / Trade-offs

- [Risk] `snapshot_json` TEXT column can grow large for families with many accounts → Mitigation: Runway snapshots are small (< 10 KB); TEXT supports up to 64 KB which is more than sufficient.
- [Risk] Account IDs or category codes in the stored JSON may become stale if accounts/categories are deleted → Mitigation: The view page renders stored values directly; no account lookup needed, so stale IDs have no effect.
- [Risk] Version field in JSON may diverge if the snapshot format changes → Mitigation: `version` field is stored; view page can display a banner for older versions if needed in future.

## Open Questions

- None. Architecture is straightforward: standard Spring Boot CRUD + Vue list/view pages.
