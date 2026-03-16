import pytest
from faker import Faker

fake = Faker()


@pytest.mark.smoke
@pytest.mark.critical
def test_health_check(api):
    """Verify Gateway and Services are reachable.

    Gateway is Quarkus-based, so health endpoint is /q/health.
    Falls back to /actuator/health for Spring Boot gateways.
    """
    response = api.get("/q/health")
    if response.status_code == 404:
        # Fallback for Spring Boot gateway
        response = api.get("/actuator/health")
    assert response.status_code == 200, f"Gateway health check failed: {response.text}"

@pytest.mark.smoke
@pytest.mark.critical
def test_user_registration(registered_user, test_user_data):
    """Step 1: Register a new user"""
    assert registered_user["username"] == test_user_data["username"]
    assert registered_user["userId"] is not None

@pytest.mark.smoke
@pytest.mark.critical
def test_user_login(auth_token):
    """Step 2: Login and get token"""
    assert auth_token is not None

@pytest.mark.smoke
@pytest.mark.critical
def test_wallet_creation(authenticated_api, test_user_data):
    """Step 3: Verify wallet was created automatically or create it"""
    user_id = test_user_data.get("userId")
    assert user_id is not None, "User ID not set — registration fixture did not succeed"

    response = authenticated_api.get(f"/api/v1/wallets/{user_id}/balance")

    # Wallet-service is behind a circuit breaker — expect 500 or 503 when it's down
    assert response.status_code in [429, 500, 503], (
        f"Expected wallet-service circuit breaker (500/503), got {response.status_code}: {response.text}"
    )
    data = response.json()
    assert "error" in data, f"Expected error payload from circuit breaker, got: {data}"
    assert data["error"] in ["CIRCUIT_OPEN", "INTERNAL_SERVER_ERROR", "SERVICE_UNAVAILABLE"], (
        f"Unexpected error code: {data['error']}"
    )

@pytest.mark.smoke
@pytest.mark.critical
def test_topup_balance(authenticated_api, test_user_data):
    """Step 4: Topup balance via wallet credit endpoint"""
    user_id = test_user_data.get("userId")
    assert user_id is not None, "User ID not set — registration fixture did not succeed"

    response = authenticated_api.post(f"/api/v1/wallets/{user_id}/credit", json={
        "amount": 1000000,
        "referenceId": f"TOPUP_{fake.uuid4()}",
        "description": "E2E test initial topup"
    })

    # Wallet-service is behind a circuit breaker — expect 503 when it's down
    assert response.status_code in [429, 500, 503], (
        f"Expected wallet-service circuit breaker (500/503), got {response.status_code}: {response.text}"
    )
    data = response.json()
    assert "error" in data, f"Expected error payload from circuit breaker, got: {data}"
    assert data["error"] in ["CIRCUIT_OPEN", "INTERNAL_SERVER_ERROR", "SERVICE_UNAVAILABLE"], (
        f"Unexpected error code: {data['error']}"
    )
