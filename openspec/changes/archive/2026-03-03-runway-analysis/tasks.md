## 1. Backend — DTO

- [x] 1.1 Create `RunwayAnalysisDTO` with fields: `liquidTotal`, `monthlyBurn`, `runwayMonths`, `depletionDate`, `expenseMonthsUsed`, `assetDataMissing`, `expenseDataWarning`, `latestSnapshotDate`, `accountBreakdown` (List of `{accountName, accountType, usdValue}`), `expenseBreakdown` (Map of major category → average monthly amount)

## 2. Backend — Service

- [x] 2.1 Create `RunwayService` class with `@Service`, `@RequiredArgsCompatibleConstructor`, `@Slf4j`
- [x] 2.2 Inject `AssetAccountRepository` (or equivalent), `AssetRecordRepository`, `ExpenseRecordRepository`, and any needed expense category repository
- [x] 2.3 Implement `getLiquidAssets(Long familyId, List<String> includedTypes)` — queries latest asset record per account filtered by type, sums USD-converted values, detects stale data (>30 days)
- [x] 2.4 Implement `getMonthlyBurn(Long familyId, int months)` — queries expense records for last N complete calendar months, groups by month, averages the totals; returns burn rate and actual months used
- [x] 2.5 Implement `getExpenseBreakdown(Long familyId, int months)` — same data as monthly burn but grouped by major expense category
- [x] 2.6 Implement `calculateRunway(Long familyId, List<String> includedTypes, int months)` — orchestrates above methods, computes `runwayMonths = floor(liquidTotal / monthlyBurn)` and `depletionDate`, returns `RunwayAnalysisDTO`

## 3. Backend — Controller

- [x] 3.1 Create `RunwayController` with `@RestController`, `@RequestMapping("/runway")`, `@CrossOrigin`
- [x] 3.2 Add `GET /analysis` endpoint accepting `@RequestParam` `familyId` (Long), `months` (int, default 6), `includedTypes` (List<String>, default CASH/STOCK/BROKERAGE/CRYPTO/DIGITAL_CURRENCY)
- [x] 3.3 Validate family access using `authHelper.requireFamilyAccess(authHeader, familyId)` before calling service
- [x] 3.4 Return `ResponseEntity` with `Map.of("success", true, "data", dto)` on success and error body on exception

## 4. Backend — Wire Up

- [x] 4.1 Verify `RunwayController` is picked up by Spring component scan (no extra config needed if in same package tree)
- [ ] 4.2 Test endpoint manually via Swagger UI at `http://localhost:8080/api/swagger-ui/index.html` or curl

## 5. Frontend — API Client

- [x] 5.1 Create `frontend/src/api/runway.js` with `getRunwayAnalysis(familyId, months, includedTypes)` that calls `GET /api/runway/analysis` with query params

## 6. Frontend — View Component

- [x] 6.1 Create `frontend/src/views/analysis/RunwayAnalysis.vue` with `<script setup>` and Composition API
- [x] 6.2 On mount, call `getRunwayAnalysis()` using the authenticated user's familyId; show loading state
- [x] 6.3 Display summary cards: Liquid Total, Monthly Burn, Runway Months, Projected Depletion Date — formatted with `formatCurrency`
- [x] 6.4 Add scenario controls: two sliders or inputs for optimistic multiplier (default 0.8) and pessimistic multiplier (default 1.2); compute scenario runway months reactively client-side
- [x] 6.5 Display scenario comparison table or cards: base / optimistic / pessimistic rows with runway months and depletion date
- [x] 6.6 Add Chart.js bar or area chart showing account breakdown (each liquid account as a segment)
- [x] 6.7 Add expense breakdown table: major category + average monthly amount, sorted descending
- [x] 6.8 Show data freshness warning banner if `assetDataMissing` or `expenseDataWarning` flags are true in response
- [x] 6.9 Add lookback window selector (3 / 6 / 12 months) that re-fetches data on change
- [x] 6.10 Add asset type toggle checkboxes (CASH, STOCK, BROKERAGE, CRYPTO, DIGITAL_CURRENCY, RETIREMENT) that re-fetch on change

## 7. Frontend — Routing & Navigation

- [x] 7.1 Add lazy-loaded route to `frontend/src/router/index.js`: path `runway`, name `RunwayAnalysis`, component `() => import('../views/analysis/RunwayAnalysis.vue')`, meta `{ title: '资金跑道' }`
- [x] 7.2 Add nav link in `frontend/src/components/Sidebar.vue` under the Analysis tab section with label "资金跑道" and an appropriate icon (e.g., `Timer` or `TrendingDown` from lucide-vue-next)

## 8. Requirement Documentation

- [x] 8.1 Create `requirement/runway-analysis-requirements.md` documenting the feature requirements, data model assumptions, and UI behavior (mirrors the personal runway-calculation.md pattern but generalized for the system)
