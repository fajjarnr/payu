"""
QAMVP-014 / AI-AUTH-001 — KYC security tests: 401 (no/invalid/expired/alg-none
token) and 403 (IDOR: token subject != requested user).

Heavy ML deps (paddleocr/cv2) are stubbed so the API layer is testable
without the model stack, mirroring how API unit tests avoid real inference.
"""

import base64
import json
import os
import sys
import time
from unittest.mock import AsyncMock, MagicMock

os.environ.setdefault("KEYCLOAK_URL", "http://keycloak:8080")

# Stub heavy/optional third-party modules before importing app.main.
for mod in ("cv2", "stomp", "paddle", "paddleocr", "paddle2onnx", "torch", "torchvision"):
    if mod not in sys.modules:
        sys.modules[mod] = MagicMock()

sys.modules.setdefault("paddle.ocr", MagicMock())
sys.modules.setdefault("paddleocr.ocr", MagicMock())

import pytest  # noqa: E402

sys.path.insert(0, "/home/ubuntu/payu/backend/kyc-service/src")

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


@pytest.mark.unit
class TestKycSecurity:
    @pytest.fixture(autouse=True)
    def mock_db(self):
        session = AsyncMock()

        async def override_get_db():
            yield session

        app.dependency_overrides[get_db_session] = override_get_db
        yield
        app.dependency_overrides.clear()

    @pytest.mark.asyncio
    async def test_missing_token_rejected_401(self):
        from httpx import AsyncClient, ASGITransport
        async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
            response = await client.post(
                "/api/v1/kyc/verify/start",
                json={"user_id": "user_123", "verification_type": "FULL_KYC"},
            )
            assert response.status_code == 401

    @pytest.mark.asyncio
    async def test_invalid_token_rejected_401(self):
        from httpx import AsyncClient, ASGITransport
        async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
            response = await client.post(
                "/api/v1/kyc/verify/start",
                json={"user_id": "user_123", "verification_type": "FULL_KYC"},
                headers={"Authorization": "Bearer not-a-jwt"},
            )
            assert response.status_code == 401

    @pytest.mark.asyncio
    async def test_alg_none_forged_token_rejected_401(self):
        from httpx import AsyncClient, ASGITransport
        async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
            response = await client.post(
                "/api/v1/kyc/verify/start",
                json={"user_id": "user_123", "verification_type": "FULL_KYC"},
                headers={"Authorization": f"Bearer {make_alg_none_token('user_123')}"},
            )
            assert response.status_code == 401

    @pytest.mark.asyncio
    async def test_expired_token_rejected_401(self):
        from httpx import AsyncClient, ASGITransport
        async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
            response = await client.post(
                "/api/v1/kyc/verify/start",
                json={"user_id": "user_123", "verification_type": "FULL_KYC"},
                headers={"Authorization": f"Bearer {make_token('user_123', int(time.time()) - 10)}"},
            )
            assert response.status_code == 401

    @pytest.mark.asyncio
    async def test_token_subject_mismatch_rejected_403(self):
        from httpx import AsyncClient, ASGITransport
        async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
            response = await client.post(
                "/api/v1/kyc/verify/start",
                json={"user_id": "user_123", "verification_type": "FULL_KYC"},
                headers={"Authorization": f"Bearer {make_token('attacker', int(time.time()) + 600)}"},
            )
            assert response.status_code == 403
