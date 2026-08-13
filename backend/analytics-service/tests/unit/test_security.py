"""
QAMVP-014 — analytics security tests: 401 (no/invalid/expired token) and 403
(IDOR: token subject != requested user).

Note: does NOT use the conftest `client` fixture — that fixture overrides
`require_auth` (auth disabled for handler tests). This test exercises the real
dependency chain.
"""

import base64
import json
import time
import sys
from unittest.mock import AsyncMock

import pytest

sys.path.insert(0, "/home/ubuntu/payu/backend/analytics-service/src")

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
