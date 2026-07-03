# Contract — Group 1: Backend trend aggregation endpoint

- **Spec**:
  - The system SHALL expose a read-only endpoint `GET /runway/reports/trend?familyId=` that loads all `RunwayReport` rows for the family, parses each `snapshotJson` server-side, and returns a list of trend points ordered ascending by `savedAt`. Each point SHALL contain `savedAt`, `reportName`, `liquidTotal`, `monthlyBurn`, `runwayMonths`, and `depletionDate`. The response SHALL NOT include the raw `snapshotJson`. The endpoint SHALL enforce family access via the caller's auth token.
  - The trend response SHALL include the latest report's category expense breakdown, with each category enriched from `ExpenseCategoryMajor` by code into `{code, name, color, amount}`. Categories SHALL be sorted by amount descending.
- **Runtime**: `cd backend && mvn test -Dtest="RunwayReportServiceTest,RunwayReportTrendControllerTest"` → expected: all service + controller tests pass; covers multi/single/zero reports, corrupt-JSON skip, category enrichment, HTTP 200 envelope, and auth rejection.
- **Code**:
  - D3: parse `snapshotJson` server-side with `ObjectMapper.readTree`; per-report `try/catch` skips corrupt rows without failing the request; response returns extracted points only — NEVER the raw `snapshotJson`. Wrap in `Map` `{success, data}`.
  - D5: snapshot amounts are already USD (converted at save time) — do NOT inject `ExchangeRateService`/`ExchangeRateRepository`; multi-currency conversion is intentionally N/A for this read path.
  - D4: enrich latest report categories by joining `ExpenseCategoryMajor` (name/color); unknown code → fallback name + default color, not dropped.
  - D8: constructor injection of `RunwayReportRepository`, `ExpenseCategoryMajorRepository`, `ObjectMapper`; no static calls, no `new` in business logic (Mockito-stubbable). New `RunwayTrendDTO` (record). Enforce `authHelper.requireFamilyAccess`. No `SELECT *`.
- **Threshold**: 80
