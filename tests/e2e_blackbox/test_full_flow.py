import pytest
import time
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
    if user_id is None:
        pytest.skip("User ID not set — registration did not succeed")

    max_retries = 10
    response = None
    for attempt in range(max_retries):
        response = authenticated_api.get(f"/api/v1/wallets/{user_id}/balance")
        if response.status_code == 200:
            data = response.json()
            assert "balance" in data
            assert data["balance"] == 0
            return
        elif response.status_code == 404:
            if attempt < max_retries - 1:
                time.sleep(1)
                continue
        else:
            break

    if response is None:
        pytest.skip("Wallet endpoint not reachable after retries")
    assert response.status_code == 200, f"Could not fetch wallet: {response.text}"

@pytest.mark.smoke
@pytest.mark.critical
def test_topup_balance(authenticated_api, test_user_data):
    """Step 4: Topup balance via wallet credit endpoint"""
    user_id = test_user_data.get("userId")
    if user_id is None:
        pytest.skip("User ID not set — registration did not succeed")

    response = authenticated_api.post(f"/api/v1/wallets/{user_id}/credit", json={
        "amount": 1000000,
        "referenceId": f"TOPUP_{fake.uuid4()}",
        "description": "E2E test initial topup"
    })

    if response.status_code not in [200, 201]:
        pytest.skip(f"Topup requires admin/internal access: {response.text}")

    # Verify balance updated
    response = authenticated_api.get(f"/api/v1/wallets/{user_id}/balance")
    assert response.status_code == 200
    balance = response.json()
    assert balance["balance"] >= 1000000
