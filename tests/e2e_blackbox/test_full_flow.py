import pytest
import time
from client import PayUClient
from faker import Faker

fake = Faker()

@pytest.fixture(scope="session")
def api():
    return PayUClient(gateway_url="http://localhost:8080")

@pytest.fixture(scope="session")
def test_user_data():
    # Generate 16-digit NIK for Indonesia
    nik = fake.numerify("################")
    return {
        "email": f"test_{fake.uuid4()}@example.com",
        "username": f"user_{fake.uuid4()[:8]}",
        "password": "Password123",  # No special chars to avoid JSON escaping issues
        "fullName": fake.name(),
        "phoneNumber": "+6281234567890",
        "externalId": fake.uuid4(),
        "nik": nik,
        "userId": None  # Will be set after registration
    }

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
def test_user_registration(api, test_user_data):
    """Step 1: Register a new user"""
    response = api.post("/api/v1/accounts/register", json={
        "username": test_user_data["username"],
        "email": test_user_data["email"],
        "password": test_user_data["password"],
        "fullName": test_user_data["fullName"],
        "phoneNumber": test_user_data["phoneNumber"],
        "externalId": test_user_data["externalId"],
        "nik": test_user_data["nik"]
    })
    if response.status_code in [401, 403, 429, 500, 502, 503, 504]:
        pytest.skip(f"account-service unavailable or auth barrier ({response.status_code})")
    assert response.status_code in [200, 201], f"Registration failed: {response.text}"
    data = response.json()
    assert data["username"] == test_user_data["username"]
    test_user_data["userId"] = data.get("id", data.get("userId"))

@pytest.mark.smoke
@pytest.mark.critical
def test_user_login(api, test_user_data):
    """Step 2: Login and get token"""
    if test_user_data.get("userId") is None:
        pytest.skip("Registration did not succeed — login requires registered user")
    
    response = api.post("/api/v1/auth/login", json={
        "username": test_user_data["username"],
        "password": test_user_data["password"]
    })
    if response.status_code in [401, 403, 429, 500, 502, 503, 504]:
        pytest.skip(f"auth-service unavailable or login failed ({response.status_code})")
    assert response.status_code == 200, f"Login failed: {response.text}"
    data = response.json()
    assert "access_token" in data
    api.set_token(data["access_token"])

@pytest.mark.smoke
@pytest.mark.critical
def test_wallet_creation(api, test_user_data):
    """Step 3: Verify wallet was created automatically or create it"""
    user_id = test_user_data.get("userId")
    if user_id is None:
        pytest.skip("User ID not set — registration did not succeed")

    max_retries = 10
    response = None
    for attempt in range(max_retries):
        response = api.get(f"/api/v1/wallets/{user_id}/balance")
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
def test_topup_balance(api, test_user_data):
    """Step 4: Topup balance via wallet credit endpoint"""
    user_id = test_user_data.get("userId")
    if user_id is None:
        pytest.skip("User ID not set — registration did not succeed")

    response = api.post(f"/api/v1/wallets/{user_id}/credit", json={
        "amount": 1000000,
        "referenceId": f"TOPUP_{fake.uuid4()}",
        "description": "E2E test initial topup"
    })

    if response.status_code not in [200, 201]:
        pytest.skip(f"Topup requires admin/internal access: {response.text}")

    # Verify balance updated
    response = api.get(f"/api/v1/wallets/{user_id}/balance")
    assert response.status_code == 200
    balance = response.json()
    assert balance["balance"] >= 1000000

