## 1. Backend — Entity and DTO

- [x] 1.1 Create `RunwayReport` JPA entity in `model/` with fields: `id` (BIGINT PK), `familyId` (BIGINT), `reportName` (VARCHAR 255), `savedAt` (LocalDateTime), `snapshotJson` (TEXT annotated with `@Column(columnDefinition = "TEXT")`)
- [x] 1.2 Create `RunwayReportSummaryDTO` with fields: `id`, `reportName`, `savedAt` — used for list responses
- [x] 1.3 Create `RunwayReportDetailDTO` with fields: `id`, `reportName`, `savedAt`, `snapshotJson` — used for single-report response
- [x] 1.4 Create `SaveRunwayReportRequest` DTO with fields: `familyId` (Long), `snapshotJson` (String)

## 2. Backend — Repository

- [x] 2.1 Create `RunwayReportRepository` extending `JpaRepository<RunwayReport, Long>`; add query `findByFamilyIdOrderBySavedAtDesc(Long familyId)` and `countByFamilyIdAndReportNameStartingWith(Long familyId, String prefix)` for duplicate-name detection

## 3. Backend — Service

- [x] 3.1 Create `RunwayReportService` with `@Service`, `@RequiredArgsConstructor`, `@Slf4j`; inject `RunwayReportRepository`
- [x] 3.2 Implement `saveReport(Long familyId, String snapshotJson)`: generate report name `runway-YYYY-MM-DD-report` using today's date; if a report with that name already exists for the family, append `-2`, `-3`, etc.; set `savedAt` to `TimeService.getCurrentTimestamp()`; save and return `RunwayReportSummaryDTO`
- [x] 3.3 Implement `listReports(Long familyId)`: return list of `RunwayReportSummaryDTO` ordered by `savedAt` descending
- [x] 3.4 Implement `getReport(Long id, Long familyId)`: fetch by ID; verify `familyId` matches (throw 403 if not); return `RunwayReportDetailDTO`
- [x] 3.5 Implement `deleteReport(Long id, Long familyId)`: fetch by ID; verify `familyId` matches; delete

## 4. Backend — Controller

- [x] 4.1 Create `RunwayReportController` with `@RestController`, `@RequestMapping("/api/runway/reports")`
- [x] 4.2 `POST /api/runway/reports` — accepts `SaveRunwayReportRequest` body and `Authorization` header; call `AuthHelper` to verify family access; call `runwayReportService.saveReport()`; return `{ success: true, data: RunwayReportSummaryDTO }`
- [x] 4.3 `GET /api/runway/reports?familyId=X` — return `{ success: true, data: List<RunwayReportSummaryDTO> }`
- [x] 4.4 `GET /api/runway/reports/{id}?familyId=X` — return `{ success: true, data: RunwayReportDetailDTO }`
- [x] 4.5 `DELETE /api/runway/reports/{id}` — accepts `Authorization` header; verify family ownership; delete; return `{ success: true }`

## 5. Frontend — API Client

- [x] 5.1 Add runway report API functions to `frontend/src/api/runway.js`: `saveRunwayReport(familyId, snapshotJson)`, `listRunwayReports(familyId)`, `getRunwayReport(id, familyId)`, `deleteRunwayReport(id)`

## 6. Frontend — RunwayAnalysis.vue Updates

- [x] 6.1 Add `saveReport()` function that builds the snapshot JSON (version, settings, excludedAccountIds from `excludedAccounts` Set, expenseAdjustments, computed snapshot values) and calls `saveRunwayReport()`; show success toast on completion
- [x] 6.2 Add Save Report button to the header area (next to the Refresh button); disable it when `data` is null or save is in progress
- [x] 6.3 Add Export PDF button to the header area; implement `exportPDF()` using jsPDF (install `npm install jspdf`); PDF sections: header (title, date, familyId), summary grid, scenario comparison table, account breakdown table, expense breakdown table, settings footer; filename: `runway-report-YYYY-MM-DD.pdf`; disable button when `data` is null

## 7. Frontend — RunwayReportList.vue (new page)

- [x] 7.1 Create `frontend/src/views/analysis/RunwayReportList.vue`; on mount, fetch family ID via `familyAPI.getDefault()` then call `listRunwayReports(familyId)`
- [x] 7.2 Render a table with columns: Report Name, Saved Date, Actions (View button, Delete button); order by newest first
- [x] 7.3 Implement delete: call `deleteRunwayReport(id)`, remove from local list without full reload
- [x] 7.4 Show empty state ("No reports saved yet") when list is empty; show loading spinner while fetching

## 8. Frontend — RunwayReportView.vue (new page)

- [x] 8.1 Create `frontend/src/views/analysis/RunwayReportView.vue`; accepts route param `id`; on mount, fetch family ID then call `getRunwayReport(id, familyId)`; parse `snapshotJson` from the response
- [x] 8.2 Render the report in read-only layout matching RunwayAnalysis.vue: summary cards (liquid total, monthly burn, runway months, depletion date), scenario comparison table, account breakdown table with excluded accounts greyed out, expense breakdown table with adjustments shown
- [x] 8.3 Add Export PDF button that calls the same `exportPDF()` logic using stored snapshot values
- [x] 8.4 Add a "← Back to Reports" navigation link

## 9. Frontend — Router and Sidebar

- [x] 9.1 Add routes to `frontend/src/router/index.js`: `{ path: '/analysis/runway-reports', component: RunwayReportList }` and `{ path: '/analysis/runway-reports/:id', component: RunwayReportView }` under the authenticated parent route
- [x] 9.2 Add "Runway Reports" nav link to `frontend/src/components/Sidebar.vue` under the Analysis section

## 10. Tests

- [x] 10.1 Write service unit test for `saveReport()`: verify report name generation, duplicate suffix logic, and correct fields set on the saved entity
- [x] 10.2 Write service unit test for `getReport()`: verify family ownership check throws 403 for mismatched familyId
- [x] 10.3 Write controller test for `POST /api/runway/reports`: verify HTTP 200, response shape `{ success, data }`, and auth header requirement
- [x] 10.4 Write controller test for `GET /api/runway/reports`: verify HTTP 200 and list shape

## 11. Smoke Test

- [ ] 11.1 (optional) Manually verify: open runway page, exclude one account, set an expense adjustment, click Save Report, navigate to Runway Reports page, confirm new entry appears with today's date, click View, confirm stored values match what was saved, click Export PDF, verify PDF opens with all sections present
