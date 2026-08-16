"""
QAMVP-014 / AI-AUTH-001 — analytics security tests: 401 (no/invalid/expired/
alg-none token) and 403 (IDOR: token subject != requested user).

Note: does NOT use the conftest `client` fixture — that fixture overrides
`require_auth` (auth disabled for handler tests). This test exercises the real
dependency chain.
"""

import base64
import json
import os
import sys
import time
from unittest.mock import AsyncMock

os.environ.setdefault("KEYCLOAK_URL", "http://keycloak:8080")

import pytest  # noqa: E402

sys.path.insert(0, "/home/ubuntu/payu/backend/analytics-service/src")

from jose import jwt  # noqa: E402

from app.main import app  # noqa: E402
from app.database import get_db_session  # noqa: E402
from app import jwt_auth  # noqa: E402


def _int_b64(value: int) -> str:
    size = (value.bit_length() + 7) // 8
    return base64.urlsafe_b64encode(value.to_bytes(size, "big")).rstrip(b"=").decode()


def _rsa_jwk_pair():
    from cryptography.hazmat.primitives.asymmetric import rsa

    key = rsa.generate_private_key(public_exponent=65537, key_size=2048)
    pub = key.public_key().public_numbers()
    priv = key.private_numbers()
    private_jwk = {
        "kty": "RSA",
        "alg": "RS256",
        "kid": "test-key",
        "n": _int_b64(pub.n),
        "e": _int_b64(pub.e),
        "d": _int_b64(priv.d),
        "p": _int_b64(priv.p),
        "q": _int_b64(priv.q),
        "dp": _int_b64(priv.dmp1),
        "dq": _int_b64(priv.dmq1),
        "qi": _int_b64(priv.iqmp),
    }
    public_jwk = {k: private_jwk[k] for k in ("kty", "kid", "alg", "n", "e")}
    return private_jwk, public_jwk


PRIVATE_JWK, PUBLIC_JWK = _rsa_jwk_pair()


def make_token(sub, exp=None):
    payload = {"sub": sub}
    if exp is not None:
        payload["exp"] = exp
    return jwt.encode(payload, PRIVATE_JWK, algorithm="RS256", headers={"kid": "test-key"})


def make_alg_none_token(sub):
    header = base64.urlsafe_b64encode(b'{"alg":"none"}').decode().rstrip("=")
    payload = base64.urlsafe_b64encode(
        json.dumps({"sub": sub}).encode()).decode().rstrip("=")
    return f"{header}.{payload}."


@pytest.fixture(autouse=True)
def mock_jwks(monkeypatch):
    async def fake_fetch():
        return {"keys": [PUBLIC_JWK]}

    jwt_auth._jwks = {"keys": [], "fetched_at": 0.0}
    monkeypatch.setattr(jwt_auth, "_fetch_jwks", fake_fetch)


@pytest.fixture
def unauth_client():
    from fastapi.testclient import TestClient
    from app.api.auth import require_auth

    async def override_db():
        yield AsyncMock()

    previous = app.dependency_overrides.copy()
    # conftest's autouse isolate_app_runtime overrides require_auth (auth
    # disabled); pop it so this test drives the real dependency chain.
    app.dependency_overrides.pop(require_auth, None)
    app.dependency_overrides[get_db_session] = override_db
    client = TestClient(app)
    yield client
    client.close()
    app.dependency_overrides.clear()
    app.dependency_overrides.update(previous)


@pytest.mark.unit
class TestAnalyticsSecurity:
    def test_missing_token_rejected_401(self, unauth_client):
        response = unauth_client.get("/api/v1/analytics/user/user_123/metrics")
        assert response.status_code == 401

    def test_invalid_token_rejected_401(self, unauth_client):
        response = unauth_client.get(
            "/api/v1/analytics/user/user_123/metrics",
            headers={"Authorization": "Bearer not-a-jwt"},
        )
        assert response.status_code == 401

    def test_alg_none_forged_token_rejected_401(self, unauth_client):
        response = unauth_client.get(
            "/api/v1/analytics/user/user_123/metrics",
            headers={"Authorization": f"Bearer {make_alg_none_token('user_123')}"},
        )
        assert response.status_code == 401

    def test_expired_token_rejected_401(self, unauth_client):
        response = unauth_client.get(
            "/api/v1/analytics/user/user_123/metrics",
            headers={"Authorization": f"Bearer {make_token('user_123', int(time.time()) - 10)}"},
        )
        assert response.status_code == 401

    def test_token_subject_mismatch_rejected_403(self, unauth_client):
        response = unauth_client.get(
            "/api/v1/analytics/user/user_123/metrics",
            headers={"Authorization": f"Bearer {make_token('attacker', int(time.time()) + 600)}"},
        )
        assert response.status_code == 403
