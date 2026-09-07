"""Tests for the reconciling write path.

The database holds one row per (period, minor category, currency). Posting alone
is not enough: a category written under a wrong rule stays in the database
forever once the rule is corrected, because an upsert never removes anything.
Reconciling means the period ends up *equal* to the local aggregate — writes and
deletions both.

The highest-risk behaviour in this change lives here. Manually entered records in
other currencies share the period with ours and are independent rows. Diffing
without filtering on currency would classify every one of them as "remote but not
local" and delete the user's hand-entered data with no way to recover it from
this tool.

Run: python -m unittest discover -s tools/expense-import -t tools/expense-import -p "test_reconcile.py"
"""

from __future__ import annotations

import os
import unittest
from decimal import Decimal
from unittest import mock

from importer.apiclient import (
    ApiError,
    CredentialsError,
    FinanceApiClient,
    credentials_from_env,
)
from importer.sources import Source
from importer.reconcile import RemoteRecord, reconcile
from importer.write import WriteBlocked, apply_plan, plan_write


def remote(rid: int, cid: int, amount: str, currency: str = "USD") -> RemoteRecord:
    return RemoteRecord(id=rid, minor_category_id=cid, amount=Decimal(amount), currency=currency)


class TestPosting(unittest.TestCase):
    def test_new_category_is_posted(self) -> None:
        plan = reconcile(local={68: Decimal("259.24")}, remote=[])

        self.assertEqual(plan.to_post, {68: Decimal("259.24")})
        self.assertEqual(plan.to_delete, [])

    def test_changed_amount_is_posted(self) -> None:
        plan = reconcile(local={68: Decimal("259.24")}, remote=[remote(5, 68, "100.00")])

        self.assertEqual(plan.to_post, {68: Decimal("259.24")})
        self.assertEqual(plan.to_delete, [])

    def test_unchanged_amount_is_not_reposted(self) -> None:
        plan = reconcile(local={68: Decimal("259.24")}, remote=[remote(5, 68, "259.24")])

        self.assertEqual(plan.to_post, {})
        self.assertEqual(plan.to_delete, [])

    def test_empty_local_and_remote_is_a_no_op(self) -> None:
        plan = reconcile(local={}, remote=[])

        self.assertEqual(plan.to_post, {})
        self.assertEqual(plan.to_delete, [])


class TestDeleting(unittest.TestCase):
    """Without this, every corrected rule leaves its mistake behind."""

    def test_category_no_longer_present_locally_is_deleted(self) -> None:
        plan = reconcile(local={68: Decimal("259.24")}, remote=[remote(9, 72, "500.00")])

        self.assertEqual([r.id for r in plan.to_delete], [9])

    def test_corrected_rule_clears_the_stale_row(self) -> None:
        """The scenario the design calls out: a wrong rule wrote 500 last run."""
        plan = reconcile(local={}, remote=[remote(9, 72, "500.00")])

        self.assertEqual([r.id for r in plan.to_delete], [9])
        self.assertEqual(plan.to_post, {})


class TestNonPositiveNet(unittest.TestCase):
    """The backend rejects amounts <= 0, so those categories must be removed."""

    def test_zero_net_is_not_posted(self) -> None:
        plan = reconcile(local={67: Decimal("0")}, remote=[])

        self.assertEqual(plan.to_post, {})

    def test_negative_net_is_not_posted(self) -> None:
        plan = reconcile(local={67: Decimal("-15.00")}, remote=[])

        self.assertEqual(plan.to_post, {})

    def test_zero_net_deletes_the_existing_row(self) -> None:
        """Skipping the post alone would leave last month's figure standing."""
        plan = reconcile(local={67: Decimal("0")}, remote=[remote(3, 67, "80.00")])

        self.assertEqual([r.id for r in plan.to_delete], [3])

    def test_negative_net_deletes_the_existing_row(self) -> None:
        plan = reconcile(local={67: Decimal("-15.00")}, remote=[remote(3, 67, "80.00")])

        self.assertEqual([r.id for r in plan.to_delete], [3])


class TestCurrencyIsolation(unittest.TestCase):
    """Highest-risk behaviour: never touch rows this tool did not write.

    ExpenseBatchUpdate.vue has a currency selector. Records entered there in CNY
    occupy the same period and the same categories as ours, as independent rows.
    """

    def test_usd_row_is_updated_while_cny_row_survives(self) -> None:
        plan = reconcile(
            local={67: Decimal("120.00")},
            remote=[remote(1, 67, "80.00", "USD"), remote(2, 67, "500.00", "CNY")],
        )

        self.assertEqual(plan.to_post, {67: Decimal("120.00")})
        self.assertEqual(plan.to_delete, [])
        self.assertEqual([r.id for r in plan.untouched], [2])

    def test_empty_local_does_not_delete_foreign_currency_rows(self) -> None:
        plan = reconcile(local={}, remote=[remote(2, 67, "500.00", "CNY")])

        self.assertEqual(plan.to_delete, [])
        self.assertEqual([r.id for r in plan.untouched], [2])

    def test_all_foreign_remote_produces_no_deletions(self) -> None:
        plan = reconcile(
            local={},
            remote=[
                remote(2, 67, "500.00", "CNY"),
                remote(3, 68, "300.00", "EUR"),
                remote(4, 72, "900.00", "JPY"),
            ],
        )

        self.assertEqual(plan.to_delete, [])
        self.assertEqual(len(plan.untouched), 3)

    def test_foreign_row_does_not_satisfy_a_local_category(self) -> None:
        """A CNY row must not be mistaken for the USD row we are maintaining."""
        plan = reconcile(local={67: Decimal("120.00")}, remote=[remote(2, 67, "120.00", "CNY")])

        self.assertEqual(plan.to_post, {67: Decimal("120.00")})
        self.assertEqual(plan.to_delete, [])

    def test_deletions_are_all_usd(self) -> None:
        plan = reconcile(
            local={},
            remote=[remote(1, 67, "80.00", "USD"), remote(2, 68, "500.00", "CNY")],
        )

        self.assertTrue(all(r.currency == "USD" for r in plan.to_delete))
        self.assertEqual([r.id for r in plan.to_delete], [1])


class TestIdempotence(unittest.TestCase):
    def test_applying_a_plan_twice_changes_nothing_the_second_time(self) -> None:
        local = {68: Decimal("259.24")}
        first = reconcile(local=local, remote=[])

        # Simulate the backend state after applying the first plan.
        after_first = [remote(1, 68, str(first.to_post[68]))]
        second = reconcile(local=local, remote=after_first)

        self.assertEqual(second.to_post, {})
        self.assertEqual(second.to_delete, [])

    def test_repeated_reconcile_never_duplicates_a_category(self) -> None:
        local = {68: Decimal("259.24"), 67: Decimal("100.00")}
        state = [remote(1, 68, "259.24"), remote(2, 67, "100.00")]

        plan = reconcile(local=local, remote=state)

        self.assertEqual(plan.to_post, {})
        self.assertEqual(plan.to_delete, [])


class FakeTransport:
    """Records requests instead of making them."""

    def __init__(self, responses: list[tuple[int, dict]]) -> None:
        self._responses = list(responses)
        self.calls: list[tuple[str, str, dict | None, dict]] = []

    def __call__(self, method: str, url: str, body: dict | None, headers: dict):
        self.calls.append((method, url, body, dict(headers)))
        if not self._responses:
            raise AssertionError(f"unexpected extra request: {method} {url}")
        return self._responses.pop(0)


class TestCredentials(unittest.TestCase):
    def test_missing_password_is_reported_by_name(self) -> None:
        with mock.patch.dict(
            os.environ,
            {"FINANCE_API_BASE": "http://x/api", "FINANCE_USERNAME": "u"},
            clear=True,
        ):
            with self.assertRaises(CredentialsError) as ctx:
                credentials_from_env()

        self.assertIn("FINANCE_PASSWORD", str(ctx.exception))

    def test_missing_username_is_reported_by_name(self) -> None:
        with mock.patch.dict(
            os.environ,
            {"FINANCE_API_BASE": "http://x/api", "FINANCE_PASSWORD": "p"},
            clear=True,
        ):
            with self.assertRaises(CredentialsError) as ctx:
                credentials_from_env()

        self.assertIn("FINANCE_USERNAME", str(ctx.exception))

    def test_api_base_has_a_default(self) -> None:
        with mock.patch.dict(
            os.environ, {"FINANCE_USERNAME": "u", "FINANCE_PASSWORD": "p"}, clear=True
        ):
            creds = credentials_from_env()

        self.assertTrue(creds.base_url)

    def test_credentials_never_appear_in_repr(self) -> None:
        with mock.patch.dict(
            os.environ, {"FINANCE_USERNAME": "u", "FINANCE_PASSWORD": "hunter2"}, clear=True
        ):
            creds = credentials_from_env()

        self.assertNotIn("hunter2", repr(creds))


class TestApiClient(unittest.TestCase):
    def test_login_stores_token_and_later_calls_carry_it(self) -> None:
        transport = FakeTransport([
            (200, {"success": True, "data": {"token": "jwt-abc"}}),
            (200, {"success": True, "data": []}),
        ])
        client = FinanceApiClient("http://x/api", "u", "p", transport=transport)

        client.login()
        client.get_records("2026-08")

        _, _, _, headers = transport.calls[1]
        self.assertEqual(headers["Authorization"], "Bearer jwt-abc")

    def test_batch_save_omits_family_id(self) -> None:
        """The backend derives it from the JWT; sending one would be ignored."""
        transport = FakeTransport([
            (200, {"success": True, "data": {"token": "t"}}),
            (200, {"success": True, "data": []}),
        ])
        client = FinanceApiClient("http://x/api", "u", "p", transport=transport)
        client.login()

        client.batch_save("2026-08", [{"minorCategoryId": 68, "amount": 259.24}])

        _, _, body, _ = transport.calls[1]
        self.assertNotIn("familyId", body)
        self.assertEqual(body["expensePeriod"], "2026-08")

    def test_failed_login_raises_and_makes_no_further_calls(self) -> None:
        transport = FakeTransport([(401, {"success": False, "error": "bad credentials"})])
        client = FinanceApiClient("http://x/api", "u", "p", transport=transport)

        with self.assertRaises(ApiError):
            client.login()

        self.assertEqual(len(transport.calls), 1)

    def test_error_message_names_the_reason(self) -> None:
        """The backend reports failures under "message", not "error".

        Reading only "error" printed the raw response dict and buried the one
        line that says what actually went wrong.
        """
        transport = FakeTransport([
            (401, {"success": False, "message": "用户名或密码错误", "data": None})
        ])
        client = FinanceApiClient("http://x/api", "u", "p", transport=transport)

        with self.assertRaises(ApiError) as ctx:
            client.login()

        self.assertIn("用户名或密码错误", str(ctx.exception))
        self.assertNotIn("'success'", str(ctx.exception))

    def test_error_key_is_still_honoured(self) -> None:
        transport = FakeTransport([(500, {"success": False, "error": "boom"})])
        client = FinanceApiClient("http://x/api", "u", "p", transport=transport)

        with self.assertRaises(ApiError) as ctx:
            client.login()

        self.assertIn("boom", str(ctx.exception))

    def test_calls_before_login_are_refused(self) -> None:
        transport = FakeTransport([])
        client = FinanceApiClient("http://x/api", "u", "p", transport=transport)

        with self.assertRaises(ApiError):
            client.get_records("2026-08")

        self.assertEqual(transport.calls, [])

    def test_non_2xx_response_raises_rather_than_returning_empty(self) -> None:
        """Swallowing this would look like 'no remote records' and delete nothing."""
        transport = FakeTransport([
            (200, {"success": True, "data": {"token": "t"}}),
            (500, {"success": False, "error": "boom"}),
        ])
        client = FinanceApiClient("http://x/api", "u", "p", transport=transport)
        client.login()

        with self.assertRaises(ApiError):
            client.get_records("2026-08")

    def test_success_false_body_raises_even_on_http_200(self) -> None:
        transport = FakeTransport([
            (200, {"success": True, "data": {"token": "t"}}),
            (200, {"success": False, "error": "period invalid"}),
        ])
        client = FinanceApiClient("http://x/api", "u", "p", transport=transport)
        client.login()

        with self.assertRaises(ApiError):
            client.get_records("2026-08")

    def test_get_records_maps_response_to_remote_records(self) -> None:
        transport = FakeTransport([
            (200, {"success": True, "data": {"token": "t"}}),
            (200, {"success": True, "data": [
                {"id": 5, "minorCategoryId": 68, "amount": "259.24", "currency": "USD"},
                {"id": 6, "minorCategoryId": 67, "amount": "500.00", "currency": "CNY"},
            ]}),
        ])
        client = FinanceApiClient("http://x/api", "u", "p", transport=transport)
        client.login()

        records = client.get_records("2026-08")

        self.assertEqual([r.id for r in records], [5, 6])
        self.assertEqual(records[0].amount, Decimal("259.24"))
        self.assertEqual(records[1].currency, "CNY")


class RecordingClient:
    """Stands in for FinanceApiClient, recording what the write path asked for."""

    def __init__(self, existing: list[RemoteRecord] | None = None) -> None:
        self.existing = list(existing or [])
        self.logged_in = False
        self.posted: list[tuple[str, list[dict]]] = []
        self.deleted: list[int] = []

    def login(self) -> None:
        self.logged_in = True

    def get_records(self, period: str) -> list[RemoteRecord]:
        return list(self.existing)

    def batch_save(self, period: str, records: list[dict]) -> dict:
        self.posted.append((period, records))
        return {"success": True}

    def delete_record(self, record_id: int) -> dict:
        self.deleted.append(record_id)
        return {"success": True}


def expense_row(cid: int, amount: str):
    from datetime import date

    from importer.models import Action, Classified, Txn

    return Classified(
        txn=Txn(
            source="chase_checking",
            account="ChaseXXXX",
            txn_date=date(2026, 8, 15),
            description="X",
            amount=Decimal(amount),
        ),
        action=Action.EXPENSE,
        minor_category_id=cid,
        rule="r",
    )


def unknown_row(amount: str = "-45.20"):
    from datetime import date

    from importer.models import Action, Classified, Txn

    return Classified(
        txn=Txn(
            source="chase_checking",
            account="ChaseXXXX",
            txn_date=date(2026, 8, 15),
            description="MYSTERY",
            amount=Decimal(amount),
        ),
        action=Action.UNKNOWN,
    )


def funding_row(target: str, amount: str = "-161.59"):
    from datetime import date

    from importer.models import Action, Classified, Txn

    return Classified(
        txn=Txn(
            source="chase_checking",
            account="ChaseXXXX",
            txn_date=date(2026, 8, 15),
            description="PAYPAL INST XFER",
            amount=Decimal(amount),
        ),
        action=Action.FUNDING,
        funding_target=target,
        rule="f",
    )


class TestGateWiring(unittest.TestCase):
    """Every gate must actually stop the write, and every flag must release it."""

    def test_unknown_blocks_the_write(self) -> None:
        with self.assertRaises(WriteBlocked) as ctx:
            plan_write(
                rows=[expense_row(68, "-259.24"), unknown_row()],
                declared_sources=[],
                present_source_ids={"chase_checking"},
            )
        self.assertIn("未分类", str(ctx.exception))

    def test_allow_unknown_releases_and_folds(self) -> None:
        prepared = plan_write(
            rows=[unknown_row("-45.20")],
            declared_sources=[],
            present_source_ids={"chase_checking"},
            allow_unknown=True,
        )
        self.assertEqual(prepared.totals[80], Decimal("45.20"))

    def test_funding_gap_blocks_the_write(self) -> None:
        with self.assertRaises(WriteBlocked) as ctx:
            plan_write(
                rows=[expense_row(68, "-259.24"), funding_row("paypal")],
                declared_sources=[],
                present_source_ids={"chase_checking"},
            )
        self.assertIn("paypal", str(ctx.exception))

    def test_allow_gaps_releases_the_funding_gate(self) -> None:
        prepared = plan_write(
            rows=[expense_row(68, "-259.24"), funding_row("paypal")],
            declared_sources=[],
            present_source_ids={"chase_checking"},
            allow_gaps=True,
        )
        self.assertEqual(prepared.totals, {68: Decimal("259.24")})

    def test_missing_source_blocks_the_write(self) -> None:
        with self.assertRaises(WriteBlocked) as ctx:
            plan_write(
                rows=[expense_row(68, "-259.24")],
                declared_sources=[Source(id="chase_card", parser="chase_checking")],
                present_source_ids={"chase_checking"},
            )
        self.assertIn("chase_card", str(ctx.exception))

    def test_allow_missing_sources_releases_the_source_gate(self) -> None:
        prepared = plan_write(
            rows=[expense_row(68, "-259.24")],
            declared_sources=[Source(id="chase_card", parser="chase_checking")],
            present_source_ids={"chase_checking"},
            allow_missing_sources=True,
        )
        self.assertEqual(prepared.totals, {68: Decimal("259.24")})

    def test_refunds_are_netted_within_a_category(self) -> None:
        prepared = plan_write(
            rows=[expense_row(67, "-100.00"), expense_row(67, "25.00")],
            declared_sources=[],
            present_source_ids={"chase_checking"},
        )
        self.assertEqual(prepared.totals[67], Decimal("75.00"))


class TestApplyPlan(unittest.TestCase):
    def test_posts_and_deletes_through_the_client(self) -> None:
        client = RecordingClient(existing=[remote(9, 72, "500.00")])
        plan = reconcile(local={68: Decimal("259.24")}, remote=client.get_records("2026-08"))

        result = apply_plan(client, "2026-08", plan, expense_type_for=lambda cid: "FIXED_DAILY")

        self.assertTrue(client.logged_in)
        self.assertEqual(client.deleted, [9])
        [(period, records)] = client.posted
        self.assertEqual(period, "2026-08")
        self.assertEqual(records[0]["minorCategoryId"], 68)

    def test_posted_records_carry_the_expense_type(self) -> None:
        """The backend rejects a record without it (@NotBlank + pattern).

        A fake client accepts anything, so only an explicit assertion catches
        this before the first real write returns 400.
        """
        client = RecordingClient()
        plan = reconcile(local={68: Decimal("259.24")}, remote=[])

        apply_plan(client, "2026-08", plan, expense_type_for=lambda cid: "FIXED_DAILY")

        [(_, records)] = client.posted
        self.assertEqual(records[0]["expenseType"], "FIXED_DAILY")

    def test_expense_type_comes_from_the_category_not_a_constant(self) -> None:
        client = RecordingClient()
        plan = reconcile(
            local={68: Decimal("259.24"), 88: Decimal("1200.00")}, remote=[]
        )

        apply_plan(
            client,
            "2026-08",
            plan,
            expense_type_for=lambda cid: "LARGE_IRREGULAR" if cid == 88 else "FIXED_DAILY",
        )

        [(_, records)] = client.posted
        by_cid = {r["minorCategoryId"]: r["expenseType"] for r in records}
        self.assertEqual(by_cid[68], "FIXED_DAILY")
        self.assertEqual(by_cid[88], "LARGE_IRREGULAR")

    def test_second_run_makes_no_calls(self) -> None:
        client = RecordingClient(existing=[remote(1, 68, "259.24")])
        plan = reconcile(local={68: Decimal("259.24")}, remote=client.get_records("2026-08"))

        apply_plan(client, "2026-08", plan, expense_type_for=lambda cid: "FIXED_DAILY")

        self.assertEqual(client.posted, [])
        self.assertEqual(client.deleted, [])

    def test_never_deletes_foreign_currency_rows(self) -> None:
        client = RecordingClient(existing=[remote(2, 67, "500.00", "CNY")])
        plan = reconcile(local={}, remote=client.get_records("2026-08"))

        apply_plan(client, "2026-08", plan, expense_type_for=lambda cid: "FIXED_DAILY")

        self.assertEqual(client.deleted, [])

    def test_reports_what_it_changed(self) -> None:
        client = RecordingClient(existing=[remote(9, 72, "500.00")])
        plan = reconcile(local={68: Decimal("259.24")}, remote=client.get_records("2026-08"))

        result = apply_plan(client, "2026-08", plan, expense_type_for=lambda cid: "FIXED_DAILY")

        self.assertEqual(result.posted_categories, [68])
        self.assertEqual(result.deleted_ids, [9])


if __name__ == "__main__":
    unittest.main()
