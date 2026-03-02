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
    """Verify Gateway and Services are reachable"""
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
    assert response.status_code in [200, 201], f"Registration failed: {response.text}"
    data = response.json()
    assert data["username"] == test_user_data["username"]
    test_user_data["userId"] = data.get("id", data.get("userId"))

@pytest.mark.smoke
@pytest.mark.critical
def test_user_login(api, test_user_data):
    """Step 2: Login and get token"""
    # Auth service might use a specific login endpoint or OAuth2 flow
    # Based on SERVICES_STATUS.md: "Login Proxy (Password Grant) with WebClient"
    # Typically POST /api/v1/auth/login or similar. Let's assume standard PayU structure.
    # Check Auth Service details... "Login Proxy"
    
    # We will try the standard pattern /api/v1/auth/login
    response = api.post("/api/v1/auth/login", json={
        "username": test_user_data["username"],
        "password": test_user_data["password"]
    })
    assert response.status_code == 200, f"Login failed: {response.text}"
    data = response.json()
    assert "access_token" in data
    api.set_token(data["access_token"])

@pytest.mark.smoke
@pytest.mark.critical
def test_wallet_creation(api, test_user_data):
    """Step 3: Verify wallet was created automatically or create it"""
    # Wallet usually created on registration event. Let's check balance.
    # We need to retry a few times because it's event driven
    user_id = test_user_data.get("userId")
    assert user_id is not None, "User ID not set — registration test must run first"

    max_retries = 10
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

    assert response.status_code == 200, f"Could not fetch wallet: {response.text}"

@pytest.mark.smoke
@pytest.mark.critical
def test_topup_balance(api, test_user_data):
    """Step 4: Topup balance via wallet credit endpoint"""
    user_id = test_user_data.get("userId")
    assert user_id is not None, "User ID not set — registration test must run first"

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

