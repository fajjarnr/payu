"""WEB-OS-008 — /ws/analytics/{account_id} route + BFF ticket auth (TDD: written before implementation).

Ticket contract (signed by the web-app BFF, verified here):
  ticket = base64url(json{"sub": str, "account_id": str, "exp": int}) + "." + hex(hmac_sha256(secret, payload_b64))
The route accepts either a `ticket` (browser-safe: page JS cannot read the
httpOnly session cookie) or the existing Keycloak `token` query param.
The push payload is the existing DashboardEvent dump:
  {"event_type": "WALLET_BALANCE_CHANGED", "user_id", "timestamp", "data"}
"""
import asyncio
import base64
import hashlib
import hmac
import json
import sys
import time

import pytest
from fastapi.testclient import TestClient
from starlette.websockets import WebSocketDisconnect

sys.path.insert(0, "/home/ubuntu/payu/backend/analytics-service/src")  # noqa: E402

SECRET = "unit-test-ws-secret"
ACCOUNT = "acc-750e8400-0001"
OTHER_ACCOUNT = "acc-99999999-9999"


def _b64url(raw: bytes) -> str:
    return base64.urlsafe_b64encode(raw).rstrip(b"=").decode()


def make_ticket(secret: str = SECRET, account_id: str = ACCOUNT, ttl: int = 60) -> str:
    payload = _b64url(
        json.dumps(
            {"sub": "user-sub-1", "account_id": account_id, "exp": int(time.time()) + ttl},
            separators=(",", ":"),
        ).encode()
    )
    sig = hmac.new(secret.encode(), payload.encode(), hashlib.sha256).hexdigest()
    return f"{payload}.{sig}"


@pytest.fixture
def ticket_secret(monkeypatch):
    """Place the shared ticket secret on the (already-instantiated) settings."""
    from app.config import get_settings

    monkeypatch.setattr(get_settings(), "ws_ticket_secret", SECRET, raising=False)


@pytest.fixture
def ws_client(mock_kafka_consumer):
    from app.main import app

    with TestClient(app) as c:
        yield c


def _expect_reject(client: TestClient, url: str) -> None:
    with pytest.raises(WebSocketDisconnect) as exc:
        with client.websocket_connect(url):
            pass
    assert exc.value.code == 4001


def test_analytics_ws_rejects_without_auth(ws_client):
    _expect_reject(ws_client, f"/ws/analytics/{ACCOUNT}")


def test_analytics_ws_rejects_account_mismatch(ws_client, ticket_secret):
    _expect_reject(ws_client, f"/ws/analytics/{ACCOUNT}?ticket={make_ticket(account_id=OTHER_ACCOUNT)}")


def test_analytics_ws_rejects_expired_ticket(ws_client, ticket_secret):
    _expect_reject(ws_client, f"/ws/analytics/{ACCOUNT}?ticket={make_ticket(ttl=-10)}")


def test_analytics_ws_rejects_tampered_signature(ws_client, ticket_secret):
    payload, sig = make_ticket().split(".")
    _expect_reject(ws_client, f"/ws/analytics/{ACCOUNT}?ticket={payload}.{'0' * len(sig)}")


def test_analytics_ws_rejects_wrong_secret(ws_client, ticket_secret):
    _expect_reject(ws_client, f"/ws/analytics/{ACCOUNT}?ticket={make_ticket(secret='other-secret')}")


def test_analytics_ws_accepts_valid_ticket_and_pings(ws_client, ticket_secret, reset_manager):
    with ws_client.websocket_connect(f"/ws/analytics/{ACCOUNT}?ticket={make_ticket()}") as ws:
        hello = ws.receive_json()
        assert hello["type"] == "connection_established"
        assert "WALLET_BALANCE_CHANGED" in hello["subscribed_events"]

        ws.send_json({"type": "ping"})
        assert ws.receive_json()["type"] == "pong"


def test_analytics_ws_accepts_keycloak_token(ws_client, monkeypatch, reset_manager):
    async def _ok(token):
        return {"sub": ACCOUNT}

    monkeypatch.setattr("app.jwt_auth.verify_jwt", _ok)
    with ws_client.websocket_connect(f"/ws/analytics/{ACCOUNT}?token=fake.jwt.token") as ws:
        hello = ws.receive_json()
        assert hello["type"] == "connection_established"


def test_analytics_ws_rejects_token_subject_mismatch(ws_client, monkeypatch):
    async def _ok(token):
        return {"sub": OTHER_ACCOUNT}

    monkeypatch.setattr("app.jwt_auth.verify_jwt", _ok)
    _expect_reject(ws_client, f"/ws/analytics/{ACCOUNT}?token=fake.jwt.token")


def test_analytics_ws_receives_wallet_balance_changed(ws_client, ticket_secret, reset_manager):
    """Frontend contract: connection receives the DashboardEvent dump verbatim."""
    from app.websocket.connection_manager import manager

    with ws_client.websocket_connect(f"/ws/analytics/{ACCOUNT}?ticket={make_ticket()}") as ws:
        ws.receive_json()  # connection_established

        event = {
            "event_type": "WALLET_BALANCE_CHANGED",
            "user_id": ACCOUNT,
            "timestamp": "2026-10-13T00:00:00Z",
            "data": {"balance": "9970000.0000", "change_amount": "-30000.0000"},
        }
        asyncio.get_event_loop().run_until_complete(
            manager.broadcast_to_user(event, ACCOUNT, "WALLET_BALANCE_CHANGED")
        )
        msg = ws.receive_json()
        assert msg["event_type"] == "WALLET_BALANCE_CHANGED"
        assert msg["data"]["balance"] == "9970000.0000"


@pytest.mark.asyncio
async def test_validate_ws_ticket_roundtrip(ticket_secret):
    from app.api.v1.websocket import _validate_ws_ticket

    assert await _validate_ws_ticket(make_ticket(), ACCOUNT) is True
    assert await _validate_ws_ticket(make_ticket(account_id=OTHER_ACCOUNT), ACCOUNT) is False
    assert await _validate_ws_ticket(make_ticket(ttl=-10), ACCOUNT) is False

    payload, sig = make_ticket().split(".")
    assert await _validate_ws_ticket(f"{payload}.{'0' * len(sig)}", ACCOUNT) is False
    assert await _validate_ws_ticket("garbage", ACCOUNT) is False

