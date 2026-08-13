"""
QAMVP-014 — KYC security tests: 401 (no/invalid/expired token) and 403 (IDOR:
token subject != requested user).

Heavy ML deps (paddleocr/cv2) are stubbed so the API layer is testable
without the model stack, mirroring how API unit tests avoid real inference.
"""

import base64
import json
import sys
import time
import types
from unittest.mock import MagicMock

# Stub heavy/optional third-party modules before importing app.main.
for mod in ("cv2", "stomp", "paddle", "paddleocr", "paddle2onnx", "torch", "torchvision"):
    if mod not in sys.modules:
        sys.modules[mod] = MagicMock()

sys.modules.setdefault("paddle.ocr", MagicMock())
sys.modules.setdefault("paddleocr.ocr", MagicMock())

import pytest  # noqa: E402

sys.path.insert(0, "/home/ubuntu/payu/backend/kyc-service/src")

from app.main import app  # noqa: E402
from app.database import get_db_session  # noqa: E402


def make_token(sub, exp=None):
    header = base64.urlsafe_b64encode(b'{"alg":"none"}').decode().rstrip("=")
    payload = {"sub": sub}
    if exp is not None:
        payload["exp"] = exp
    payload_b64 = base64.urlsafe_b64encode(
        json.dumps(payload).encode()).decode().rstrip("=")
    return f"{header}.{payload_b64}.sig"


@pytest.mark.unit
class TestKycSecurity:
    @pytest.fixture(autouse=True)
    def mock_db(self):
        from unittest.mock import AsyncMock
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
