# Eval Log — expense-summary

<!-- Appended by evaluator subagent after each N.E EVAL run -->

- group: 1
  attempt: 1
  scores: {spec: 98, runtime: 100, code: 82}
  total: 96
  status: PASS
  findings:
    - "spec: All 4 SHALL statements fully implemented—directory discovery, period derivation, cross-period filtering with counts, header-based source identification, data isolation"
    - "runtime: 17/17 tests pass (test_discover.py); all 4 contract categories green"
    - "code: No CRITICAL or HIGH issues. 5 MEDIUM/LOW issues are refinements: CSV encoding, year validation, unsorted transactions, error message language, source id format. None block functionality."
  fix_tasks: []

- group: 2
  attempt: 1
  scores: {spec: 96, runtime: 100, code: 88}
  total: 96.8
  status: PASS
  findings:
    - "spec: All 4 SHALL statements in contract implemented—unknown gate with folding to category 80, funding gap gate counting only outflows, missing source gate with manual/pending handling, FUNDING rules validated at load time, no catch-all rules enforced"
    - "runtime: 36/36 tests pass (21 from test_gates.py + 15 from test_sources.py); prior HIGH issue (inflows counted as gaps) is fixed with `if not row.txn.is_outflow: continue` check"
    - "code: No CRITICAL or HIGH issues. Pure function design correctly implemented in gates.py (check_unknown, fold_unknown_into_uncategorised, check_funding_gaps, check_missing_sources). Proper immutable dataclasses with frozen=True. Comprehensive validation at load time in sources.py and classify.py. Only MEDIUM/LOW observations: docstring clarity on immutability guarantee, minor internal type hints. All requirements met."
  fix_tasks: []

- group: 3
  attempt: 1
  scores: {spec: 100, runtime: 100, code: 88}
  total: 97.6
  status: PASS
  findings:
    - "spec: All 5 SHALL statements fully implemented and verified. Currency filtering hardcoded (MANAGED_CURRENCY='USD') at reconcile.py:25,64-65. Reconciliation with GET→diff→{POST,DELETE} at apiclient.py & write.py. Idempotence verified by TestIdempotence (test_reconcile.py:829-848). Credentials from env only (apiclient.py:67-81), password field has repr=False. Refund netting with positive-amount filter (write.py:268, reconcile.py:68)."
    - "runtime: All 41 tests pass (test_reconcile.py); includes 6 dedicated currency-isolation tests covering critical D4 scenarios: test_usd_row_is_updated_while_cny_row_survives, test_empty_local_does_not_delete_foreign_currency_rows, test_all_foreign_remote_produces_no_deletions, test_foreign_row_does_not_satisfy_a_local_category, test_deletions_are_all_usd, test_never_deletes_foreign_currency_rows"
    - "code: No CRITICAL or HIGH issues—APPROVE. D4 currency isolation correctly enforced: remote partitioned by currency at reconcile.py:64-65, only managed records used for delete/post decisions (lines 67-75), untouched records explicitly preserved and never fed to apply_plan. Proper error handling (ApiError on non-2xx and success=false), pure functions (reconcile.py, gates.py have no I/O imports), credential safety (password repr=False, never sent except to login). 1 MEDIUM: missing network timeout on urllib (apiclient.py:87, recommend 30s timeout). 2 LOW: partial-failure in apply_plan undocumented but harmless (idempotent), raw response payload in ApiError message (cosmetic, localhost-only)."
  fix_tasks:
    - "3.F1 FIX — Add `timeout=30` argument to `urllib.request.urlopen()` call in apiclient.py:87 and add explicit catch for socket.timeout to route through ApiError"
