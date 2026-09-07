"""The only module that talks to the network.

Everything else in this package is a pure function, so the write path can be
tested end to end by injecting a fake transport here. That is the whole reason
this boundary exists.

Two rules shape the error handling:

- **Never swallow a failure.** A failed ``get_records`` that returned an empty
  list would look exactly like "the period is empty remotely", and the reconciler
  would then propose deleting nothing while posting everything — or worse,
  with an empty local aggregate, delete nothing it should have. Any non-2xx
  response, or a 200 whose body says ``success: false``, raises.
- **Never send credentials anywhere but the login call**, and never let them
  reach a log line or a traceback via ``repr``.
"""

from __future__ import annotations

import json
import os
import urllib.error
import urllib.request
from dataclasses import dataclass, field
from decimal import Decimal
from typing import Callable, Protocol

from .reconcile import RemoteRecord

DEFAULT_BASE_URL = "http://localhost:8080/api"

ENV_BASE_URL = "FINANCE_API_BASE"
ENV_USERNAME = "FINANCE_USERNAME"
ENV_PASSWORD = "FINANCE_PASSWORD"


class ApiError(RuntimeError):
    """Raised on any unsuccessful API interaction — never returned as empty data."""


class CredentialsError(RuntimeError):
    """Raised when connection details are missing — names the variable to set."""


class Transport(Protocol):
    """Seam for tests: performs one request, returns (status, decoded body)."""

    def __call__(
        self, method: str, url: str, body: dict | None, headers: dict
    ) -> tuple[int, dict]: ...


@dataclass(frozen=True)
class Credentials:
    base_url: str
    username: str
    #: Excluded from repr so it cannot leak into logs or tracebacks.
    password: str = field(repr=False, default="")


def credentials_from_env() -> Credentials:
    """Read connection details from the environment, failing loudly if incomplete."""
    missing = [name for name in (ENV_USERNAME, ENV_PASSWORD) if not os.environ.get(name)]
    if missing:
        raise CredentialsError(
            f"缺少环境变量: {', '.join(missing)}。"
            f"写库需要 {ENV_USERNAME} 与 {ENV_PASSWORD}"
            f"（{ENV_BASE_URL} 可选，默认 {DEFAULT_BASE_URL}）。"
            f"凭据不要写进仓库。"
        )
    return Credentials(
        base_url=os.environ.get(ENV_BASE_URL) or DEFAULT_BASE_URL,
        username=os.environ[ENV_USERNAME],
        password=os.environ[ENV_PASSWORD],
    )


def _urllib_transport(
    method: str, url: str, body: dict | None, headers: dict
) -> tuple[int, dict]:
    data = json.dumps(body).encode("utf-8") if body is not None else None
    request = urllib.request.Request(url, data=data, method=method)
    request.add_header("Content-Type", "application/json")
    for key, value in headers.items():
        request.add_header(key, value)
    try:
        with urllib.request.urlopen(request) as response:  # noqa: S310 - fixed scheme, own server
            raw = response.read().decode("utf-8")
            return response.status, (json.loads(raw) if raw else {})
    except urllib.error.HTTPError as exc:
        raw = exc.read().decode("utf-8", errors="replace")
        try:
            parsed = json.loads(raw) if raw else {}
        except json.JSONDecodeError:
            parsed = {"error": raw[:500]}
        return exc.code, parsed
    except urllib.error.URLError as exc:
        raise ApiError(f"无法连接 {url}: {exc.reason}") from exc


class FinanceApiClient:
    """Thin client over the endpoints the reconciling write needs.

    ``family_id`` is deliberately never sent: the backend derives it from the JWT
    and overwrites whatever the body carries, so including it would only create
    the illusion that the client chooses.
    """

    def __init__(
        self,
        base_url: str,
        username: str,
        password: str,
        transport: Transport | Callable[..., tuple[int, dict]] = _urllib_transport,
    ) -> None:
        self._base = base_url.rstrip("/")
        self._username = username
        self._password = password
        self._transport = transport
        self._token: str | None = None

    @classmethod
    def from_env(cls, transport: Transport | None = None) -> FinanceApiClient:
        creds = credentials_from_env()
        return cls(
            creds.base_url,
            creds.username,
            creds.password,
            transport=transport or _urllib_transport,
        )

    # ---- requests -------------------------------------------------------

    def _headers(self) -> dict[str, str]:
        if self._token is None:
            raise ApiError("尚未登录 —— 调用任何接口前必须先 login()")
        return {"Authorization": f"Bearer {self._token}"}

    def _request(self, method: str, path: str, body: dict | None, headers: dict) -> dict:
        status, payload = self._transport(method, f"{self._base}{path}", body, headers)
        if not 200 <= status < 300:
            raise ApiError(
                f"{method} {path} 返回 HTTP {status}: {payload.get('error') or payload}"
            )
        if payload.get("success") is False:
            raise ApiError(f"{method} {path} 失败: {payload.get('error') or payload}")
        return payload

    # ---- operations -----------------------------------------------------

    def login(self) -> None:
        payload = self._request(
            "POST",
            "/auth/login",
            {"username": self._username, "password": self._password},
            {},
        )
        token = (payload.get("data") or {}).get("token")
        if not token:
            raise ApiError("登录响应中没有 token")
        self._token = token

    def get_records(self, period: str) -> list[RemoteRecord]:
        payload = self._request(
            "GET", f"/expenses/records?period={period}", None, self._headers()
        )
        return [
            RemoteRecord(
                id=int(row["id"]),
                minor_category_id=int(row["minorCategoryId"]),
                amount=Decimal(str(row["amount"])),
                currency=row.get("currency") or "USD",
            )
            for row in payload.get("data") or []
        ]

    def batch_save(self, period: str, records: list[dict]) -> dict:
        return self._request(
            "POST",
            "/expenses/records/batch",
            {"expensePeriod": period, "records": records},
            self._headers(),
        )

    def delete_record(self, record_id: int) -> dict:
        return self._request(
            "DELETE", f"/expenses/records/{record_id}", None, self._headers()
        )
