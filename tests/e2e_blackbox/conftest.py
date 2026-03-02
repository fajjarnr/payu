import pytest
import os
import sys
import requests
from faker import Faker

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from client import PayUClient

fake = Faker()


def pytest_configure(config):
    """Configure pytest with custom markers"""
    config.addinivalue_line(
        "markers", "smoke: marks tests as smoke tests (fast, critical path)"
    )
    config.addinivalue_line(
        "markers", "critical: marks tests as critical for production"
    )
    config.addinivalue_line(
        "markers", "integration: marks tests as integration tests (cross-service)"
    )
    config.addinivalue_line(
        "markers", "e2e: marks tests as end-to-end tests"
    )
    config.addinivalue_line(
        "markers", "slow: marks tests as slow (deselect with '-m \"not slow\"')"
    )


@pytest.fixture(scope="session")
def gateway_url():
    """Get the gateway URL from environment or use default"""
    return os.getenv("GATEWAY_URL", "http://localhost:8080")


@pytest.fixture(scope="session")
def test_timeout():
    """Get test timeout from environment or use default"""
    return int(os.getenv("TEST_TIMEOUT", "30"))


@pytest.fixture(scope="session", autouse=True)
def check_gateway_available(gateway_url):
    """Skip all tests if the gateway is not reachable.
    
    Gateway is Quarkus-based, so health endpoint is /q/health (not /actuator/health).
    Falls back to /actuator/health for Spring Boot gateways.
    """
    health_paths = ["/q/health", "/actuator/health"]
    last_error = None
    for path in health_paths:
        try:
            resp = requests.get(f"{gateway_url}{path}", timeout=5)
            if resp.status_code < 500:
                return  # Gateway is reachable
            last_error = f"HTTP {resp.status_code}"
        except requests.ConnectionError:
            last_error = "connection refused"
        except requests.Timeout:
            last_error = "timeout"
    
    if last_error in ("connection refused",):
        pytest.skip(
            f"Gateway not reachable at {gateway_url}. "
            "Start services first: make podman-test-up"
        )
    elif last_error == "timeout":
        pytest.skip(f"Gateway timed out at {gateway_url}")
    elif last_error:
        pytest.skip(f"Gateway unhealthy at {gateway_url} ({last_error})")


@pytest.fixture(scope="session")
def api(gateway_url):
    """Session-scoped PayUClient instance with E2E test header for rate limit bypass."""
    client = PayUClient(gateway_url=gateway_url)
    client.session.headers.update({"X-E2E-Test": "true"})
    return client


@pytest.fixture(scope="session")
def test_user_data():
    """Session-scoped dict with faker-generated user data."""
    nik = fake.numerify("################")
    return {
        "email": f"test_{fake.uuid4()}@example.com",
        "username": f"user_{fake.uuid4()[:8]}",
        "password": "Password123",
        "fullName": fake.name(),
        "phoneNumber": "+6281234567890",
        "externalId": fake.uuid4(),
        "nik": nik,
        "userId": None,
    }


@pytest.fixture(scope="session")
def registered_user(api, test_user_data):
    """Session-scoped fixture that registers a user and returns user data.
    
    Skips if registration fails due to service unavailability.
    """
    response = api.post("/api/v1/accounts/register", json={
        "username": test_user_data["username"],
        "email": test_user_data["email"],
        "password": test_user_data["password"],
        "fullName": test_user_data["fullName"],
        "phoneNumber": test_user_data["phoneNumber"],
        "externalId": test_user_data["externalId"],
        "nik": test_user_data["nik"],
    })
    if response.status_code in [401, 403, 429, 500, 502, 503, 504]:
        pytest.skip(f"account-service unavailable or auth barrier ({response.status_code})")
    assert response.status_code in [200, 201], f"Registration failed: {response.text}"
    data = response.json()
    test_user_data["userId"] = data.get("id", data.get("userId"))
    return test_user_data


@pytest.fixture(scope="session")
def auth_token(api, registered_user):
    """Session-scoped fixture that logs in the registered user and returns auth token.
    
    Also sets the token on the api client.
    """
    response = api.post("/api/v1/auth/login", json={
        "username": registered_user["username"],
        "password": registered_user["password"],
    })
    if response.status_code in [401, 403, 429, 500, 502, 503, 504]:
        pytest.skip(f"auth-service unavailable or login failed ({response.status_code})")
    assert response.status_code == 200, f"Login failed: {response.text}"
    data = response.json()
    assert "access_token" in data
    token = data["access_token"]
    api.set_token(token)
    return token


@pytest.fixture(scope="session")
def authenticated_api(api, auth_token):
    """Session-scoped fixture that returns the api client with auth token set."""
    return api
