"""Parser tests, one class per institution.

Fixtures are **format-faithful but synthetic** — same headers, date formats,
preamble shapes and sign conventions as the real exports, with invented
merchants and amounts. Real statement rows carry account numbers, balances and
merchant history, which must never enter the repository; the format is what
these tests are actually pinning down.

The sign assertion in each class is the one that matters most. There is no rule
like "credit cards are positive": Chase's card and BOA both report purchases as
negative, Robinhood reports them as positive, and Venmo carries an explicit
sign in the text. Getting one wrong turns a month of spending negative.

Run: python -m unittest discover -s tools/expense-import -t tools/expense-import -p "test_parsers.py"
"""

from __future__ import annotations

import tempfile
import unittest
from datetime import date
from decimal import Decimal
from pathlib import Path

from importer.parsers import parser_for

CHASE_CARD = """Transaction Date,Post Date,Description,Category,Type,Amount,Memo
09/03/2026,09/04/2026,SAMPLE RESTAURANT,Food & Drink,Sale,-130.88,
08/28/2026,08/29/2026,SAMPLE GAS #1709,Gas,Sale,-61.12,
08/14/2026,08/14/2026,Payment Thank You-Mobile,,Payment,4028.11,
08/10/2026,08/11/2026,SAMPLE STORE REFUND,Shopping,Return,25.00,
"""

BOA_CHECKING = """Description,,Summary Amt.
Beginning balance as of 07/31/2026,,"34,452.69"
Total credits,,"9,657.72"
Total debits,,"-24,143.07"
Ending balance as of 09/07/2026,,"19,967.34"

Date,Description,Amount,Running Bal.
07/31/2026,Beginning balance as of 07/31/2026,,"34,452.69"
08/03/2026,"Zelle payment to SAMPLE VENDOR; Conf# abc123","-150.00","34,302.69"
08/05/2026,"Zelle payment from SAMPLE PAYER","1,105.22","35,407.91"
08/20/2026,"SAMPLE UTILITY DES:BILL PAY","-259.24","35,148.67"
"""

ROBINHOOD_CARD = """Date,Time,Cardholder,Amount,Points,Balance,Status,Type,Merchant,Description
2026-08-30,"11:45 PM","SAMPLE HOLDER",120.50,361,2380.15,Posted,Purchase,SampleCo,"SAMPLE CO 800-000-0000 CA"
2026-08-30,"11:43 PM","SAMPLE HOLDER",120.50,0,,Declined,Purchase,SampleCo,
2026-08-22,"12:24 PM","OTHER HOLDER",7.40,22,17.63,Posted,Purchase,SampleCafe,"SQ *SAMPLE CAFE Cupertino CA"
2026-08-15,"9:00 AM","SAMPLE HOLDER",-500.00,0,0.00,Posted,Payment,,"PAYMENT - THANK YOU"
"""

PAYPAL = """Date,Time,TimeZone,Name,Type,Status,Currency,Amount,Fees,Total,Exchange Rate,Receipt ID,Balance,Transaction ID,Item Title
08/09/2026,"10:00:00",PDT,Sample Payee,Mobile Payment,Completed,USD,-24.00,0.00,-24.00,,,0.00,ABC123,
08/09/2026,"10:01:00",PDT,,Bank Deposit to PP Account ,Completed,USD,24.00,0.00,24.00,,,24.00,DEF456,
08/25/2026,"11:00:00",PDT,Sample Merchant,Mobile Payment,Completed,USD,-243.15,0.00,-243.15,,,0.00,GHI789,
08/07/2026,"12:00:00",PDT,Sample Friend,Mobile Payment,Completed,USD,51.00,0.00,51.00,,,51.00,JKL012,
08/31/2026,"13:00:00",PDT,Sample Pending,Mobile Payment,Pending,USD,-99.00,0.00,-99.00,,,0.00,MNO345,
"""

VENMO = """Account Statement - (@Sample-User) ,,,,,,,,,,,,,,,,,,,,,
Account Activity,,,,,,,,,,,,,,,,,,,,,
,ID,Datetime,Type,Status,Note,From,To,Amount (total),Amount (tip),Amount (tax),Amount (fee),Tax Rate,Tax Exempt,Funding Source,Destination,Beginning Balance,Ending Balance,Statement Period Venmo Fees,Terminal Location,Year to Date Venmo Fees,Disclaimer
,,,,,,,,,,,,,,,,"$1,121.17",,,,,
,ID001,2026-08-02T05:23:48,Payment,Complete,tennis court,Sample Friend,Sample User,+ $45.00,,0,,0,,,Venmo balance,,,,Venmo,,
,ID002,2026-08-02T18:00:00,Payment,Complete,dinner,Sample User,Other Friend,- $32.00,,0,,0,,,Venmo balance,,,,Venmo,,
,ID003,2026-08-08T12:00:00,Payment,Complete,hotpot,Third Friend,Sample User,+ $51.00,,0,,0,,,Venmo balance,,,,Venmo,,
"""


def write(content: str, name: str = "sample.csv") -> Path:
    tmp = Path(tempfile.mkdtemp())
    path = tmp / name
    path.write_text(content, encoding="utf-8")
    return path


class TestChaseCard(unittest.TestCase):
    """Chase's card export reports purchases as NEGATIVE — same as its checking."""

    def setUp(self) -> None:
        self.txns = parser_for(write(CHASE_CARD)).parse(write(CHASE_CARD))

    def test_parser_is_selected_by_header(self) -> None:
        self.assertEqual(parser_for(write(CHASE_CARD)).source, "chase_card")

    def test_purchase_amount_is_negative(self) -> None:
        purchase = next(t for t in self.txns if "RESTAURANT" in t.description)
        self.assertEqual(purchase.amount, Decimal("-130.88"))
        self.assertTrue(purchase.is_outflow)

    def test_payment_to_the_card_is_positive(self) -> None:
        """The mirror of checking's LOAN_PMT; a rule marks it TRANSFER."""
        payment = next(t for t in self.txns if "Payment Thank You" in t.description)
        self.assertEqual(payment.amount, Decimal("4028.11"))

    def test_refund_is_positive(self) -> None:
        refund = next(t for t in self.txns if "REFUND" in t.description)
        self.assertEqual(refund.amount, Decimal("25.00"))

    def test_transaction_date_is_used_not_post_date(self) -> None:
        """Post date can fall in the next period and would misattribute the month."""
        purchase = next(t for t in self.txns if "RESTAURANT" in t.description)
        self.assertEqual(purchase.txn_date, date(2026, 9, 3))

    def test_institution_category_is_kept_as_raw_type(self) -> None:
        """Chase's own category is a useful signal for writing rules."""
        purchase = next(t for t in self.txns if "GAS" in t.description)
        self.assertEqual(purchase.raw_type, "Gas")


class TestBoaChecking(unittest.TestCase):
    """BOA's export opens with a five-line balance summary before the real header."""

    def setUp(self) -> None:
        self.txns = parser_for(write(BOA_CHECKING)).parse(write(BOA_CHECKING))

    def test_parser_is_selected_despite_the_preamble(self) -> None:
        self.assertEqual(parser_for(write(BOA_CHECKING)).source, "boa_checking")

    def test_summary_rows_are_not_transactions(self) -> None:
        descriptions = [t.description for t in self.txns]
        self.assertNotIn("Total credits", descriptions)
        self.assertNotIn("Beginning balance as of 07/31/2026", descriptions)

    def test_debit_is_negative(self) -> None:
        debit = next(t for t in self.txns if "UTILITY" in t.description)
        self.assertEqual(debit.amount, Decimal("-259.24"))

    def test_credit_is_positive(self) -> None:
        credit = next(t for t in self.txns if "SAMPLE PAYER" in t.description)
        self.assertEqual(credit.amount, Decimal("1105.22"))

    def test_thousands_separator_is_parsed(self) -> None:
        credit = next(t for t in self.txns if "SAMPLE PAYER" in t.description)
        self.assertEqual(credit.amount, Decimal("1105.22"))

    def test_rows_without_an_amount_are_skipped(self) -> None:
        """The opening balance row has a running balance but no amount."""
        self.assertEqual(len(self.txns), 3)


class TestRobinhoodCard(unittest.TestCase):
    """Robinhood reports purchases as POSITIVE — the opposite of both Chase exports."""

    def setUp(self) -> None:
        self.txns = parser_for(write(ROBINHOOD_CARD)).parse(write(ROBINHOOD_CARD))

    def test_parser_is_selected_by_header(self) -> None:
        self.assertEqual(parser_for(write(ROBINHOOD_CARD)).source, "robinhood_card")

    def test_purchase_is_flipped_to_negative(self) -> None:
        purchase = next(t for t in self.txns if "SAMPLE CO" in t.description)
        self.assertEqual(purchase.amount, Decimal("-120.50"))
        self.assertTrue(purchase.is_outflow)

    def test_declined_rows_are_excluded(self) -> None:
        """A declined retry carries the same amount as the posted charge.

        Keeping it would double the purchase — the single most damaging thing
        this parser could get wrong.
        """
        self.assertEqual(len(self.txns), 3)
        self.assertEqual(
            sum(1 for t in self.txns if t.amount == Decimal("-120.50")),
            1,
            "the declined retry must not survive alongside the posted charge",
        )

    def test_payment_to_the_card_is_positive_after_flip(self) -> None:
        """Robinhood reports the payment negative; flipping lands it positive,
        matching how Chase's card reports the same event."""
        payment = next(t for t in self.txns if "PAYMENT - THANK YOU" in t.description)
        self.assertEqual(payment.amount, Decimal("500.00"))

    def test_iso_date_is_parsed(self) -> None:
        purchase = next(t for t in self.txns if "SAMPLE CAFE" in t.description)
        self.assertEqual(purchase.txn_date, date(2026, 8, 22))

    def test_other_cardholders_are_included(self) -> None:
        """Household spending on an authorised card is still household spending."""
        self.assertTrue(any("SAMPLE CAFE" in t.description for t in self.txns))


class TestPayPal(unittest.TestCase):
    """Amounts are signed like a bank export; deposits mirror the checking top-up."""

    def setUp(self) -> None:
        self.txns = parser_for(write(PAYPAL)).parse(write(PAYPAL))

    def test_parser_is_selected_by_header(self) -> None:
        self.assertEqual(parser_for(write(PAYPAL)).source, "paypal")

    def test_outbound_payment_is_negative(self) -> None:
        payment = next(t for t in self.txns if "Sample Merchant" in t.description)
        self.assertEqual(payment.amount, Decimal("-243.15"))

    def test_inbound_payment_is_positive(self) -> None:
        inbound = next(t for t in self.txns if "Sample Friend" in t.description)
        self.assertEqual(inbound.amount, Decimal("51.00"))

    def test_bank_deposit_is_exposed_for_a_transfer_rule(self) -> None:
        """The mirror of the checking top-up; both sides must be excluded."""
        deposit = next(t for t in self.txns if "Bank Deposit" in t.raw_type)
        self.assertEqual(deposit.amount, Decimal("24.00"))

    def test_pending_rows_are_excluded(self) -> None:
        """A pending charge may still reverse; it reappears once it settles."""
        self.assertNotIn("Sample Pending", [t.description for t in self.txns])
        self.assertEqual(len(self.txns), 4)

    def test_name_and_type_both_reach_the_description(self) -> None:
        """Rules match on description, and PayPal's payee name is the signal."""
        payment = next(t for t in self.txns if "Sample Merchant" in t.description)
        self.assertIn("Sample Merchant", payment.description)


class TestVenmo(unittest.TestCase):
    """Two preamble lines, a leading empty column, and a signed text amount."""

    def setUp(self) -> None:
        self.txns = parser_for(write(VENMO)).parse(write(VENMO))

    def test_parser_is_selected_despite_the_preamble(self) -> None:
        self.assertEqual(parser_for(write(VENMO)).source, "venmo")

    def test_outbound_is_negative(self) -> None:
        outbound = next(t for t in self.txns if "dinner" in t.description)
        self.assertEqual(outbound.amount, Decimal("-32.00"))

    def test_inbound_is_positive(self) -> None:
        """Reimbursements net against the category rather than counting as income."""
        inbound = next(t for t in self.txns if "tennis" in t.description)
        self.assertEqual(inbound.amount, Decimal("45.00"))

    def test_note_is_the_description(self) -> None:
        """The note is the only thing that says what the money was for."""
        descriptions = [t.description for t in self.txns]
        self.assertTrue(any("hotpot" in d for d in descriptions))

    def test_balance_only_rows_are_skipped(self) -> None:
        self.assertEqual(len(self.txns), 3)

    def test_iso_datetime_is_parsed(self) -> None:
        txn = next(t for t in self.txns if "tennis" in t.description)
        self.assertEqual(txn.txn_date, date(2026, 8, 2))


class TestParserSelectionIsUnambiguous(unittest.TestCase):
    """Each fixture must match exactly one parser — overlapping signatures silently
    route a file to the wrong institution, and sign conventions differ."""

    def test_chase_card_does_not_match_chase_checking(self) -> None:
        self.assertEqual(parser_for(write(CHASE_CARD)).source, "chase_card")

    def test_chase_checking_still_matches_itself(self) -> None:
        checking = (
            "Details,Posting Date,Description,Amount,Type,Balance,Check or Slip #\n"
            'DEBIT,08/26/2026,"SAMPLE UTILITY",-259.24,ACH_DEBIT,6030.78,,\n'
        )
        self.assertEqual(parser_for(write(checking)).source, "chase_checking")


if __name__ == "__main__":
    unittest.main()
