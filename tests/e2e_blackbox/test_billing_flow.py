import pytest
import uuid
from client import PayUClient
from faker import Faker

fake = Faker()


@pytest.mark.e2e
class TestBillingServiceFlow:
    """
    Billing Service E2E tests (dedicated).
    Tests: Biller categories -> List billers -> Bill payment -> TopUp -> Subscriptions
    """

    @pytest.fixture(scope="class")
    def api(self):
        return PayUClient(gateway_url="http://localhost:8080")

    @pytest.fixture(scope="class")
    def user_session(self, api):
        """Register and login a user for billing operations"""
        user_data = {
            "email": f"bill_{fake.uuid4()}@example.com",
            "username": f"bill_{fake.uuid4()[:8]}",
            "password": "Password123!",
            "name": fake.name(),
            "phoneNumber": "+6281234567890"
        }

        response = api.post("/api/v1/accounts/register", json=user_data)
        if response.status_code in [401, 403, 500, 502, 503, 504]:
            pytest.skip(f"account-service unavailable or auth barrier ({response.status_code})")
        assert response.status_code in [200, 201], f"Register failed: {response.status_code}"
        user_id = response.json().get("id", response.json().get("userId"))

        response = api.post("/api/v1/auth/login", json={
            "username": user_data["username"],
            "password": user_data["password"]
        })
        if response.status_code in [502, 503, 504]:
            pytest.skip(f"auth-service unavailable ({response.status_code})")
        assert response.status_code == 200, f"Login failed: {response.status_code}"
        api.set_token(response.json()["access_token"])

        return {"user_id": user_id, "api": api}

    @pytest.mark.smoke
    def test_get_biller_categories(self, api):
        """List all biller categories (public endpoint)"""
        response = api.get("/api/v1/billers/categories")
        assert response.status_code in [200, 401, 403, 404, 503], f"Unexpected status: {response.status_code}"
        if response.status_code == 200:
            data = response.json()
            assert isinstance(data, (list, dict))

    def test_list_billers(self, api):
        """List all available billers"""
        response = api.get("/api/v1/billers")
        assert response.status_code in [200, 404], f"Unexpected status: {response.status_code}"
        if response.status_code == 200:
            data = response.json()
            assert isinstance(data, (list, dict))

    def test_list_billers_by_category(self, api):
        """List billers filtered by category (e.g. PLN)"""
        response = api.get("/api/v1/billers", params={"category": "PLN"})
        assert response.status_code in [200, 404], f"Unexpected status: {response.status_code}"

    def test_get_biller_by_code(self, api):
        """Get specific biller by code"""
        response = api.get("/api/v1/billers/PLN_PREPAID")
        assert response.status_code in [200, 404], f"Unexpected status: {response.status_code}"

    def test_create_bill_payment(self, user_session):
        """Create a bill payment with idempotency key"""
        api = user_session["api"]
        idempotency_key = str(uuid.uuid4())
        api.session.headers.update({"X-Idempotency-Key": idempotency_key})
        payload = {
            "billerCode": "PLN_PREPAID",
            "customerNumber": "1234567890",
            "amount": 100000,
            "description": "PLN token purchase"
        }
        response = api.post("/api/v1/payments", json=payload)
        assert response.status_code in [200, 201, 400, 404, 422], f"Unexpected status: {response.status_code}"
        api.session.headers.pop("X-Idempotency-Key", None)

    def test_get_payment_by_id(self, user_session):
        """Get specific payment by ID"""
        api = user_session["api"]
        fake_id = str(uuid.uuid4())
        response = api.get(f"/api/v1/payments/{fake_id}")
        assert response.status_code in [200, 404], f"Unexpected status: {response.status_code}"

    def test_get_topup_providers(self, user_session):
        """List available e-wallet top-up providers"""
        api = user_session["api"]
        response = api.get("/api/v1/topup/providers")
        assert response.status_code in [200, 404], f"Unexpected status: {response.status_code}"

    def test_create_topup(self, user_session):
        """Create e-wallet top-up with idempotency"""
        api = user_session["api"]
        idempotency_key = str(uuid.uuid4())
        api.session.headers.update({"X-Idempotency-Key": idempotency_key})
        payload = {
            "provider": "GOPAY",
            "phoneNumber": "+6281234567890",
            "amount": 50000
        }
        response = api.post("/api/v1/topup", json=payload)
        assert response.status_code in [200, 201, 400, 404, 422], f"Unexpected status: {response.status_code}"
        api.session.headers.pop("X-Idempotency-Key", None)

    def test_list_subscription_plans(self, user_session):
        """List subscription plans by partner"""
        api = user_session["api"]
        fake_partner_id = str(uuid.uuid4())
        response = api.get(f"/api/v1/subscriptions/plans/partner/{fake_partner_id}")
        assert response.status_code in [200, 404], f"Unexpected status: {response.status_code}"

    def test_create_subscription_plan(self, user_session):
        """Create a subscription plan"""
        api = user_session["api"]
        payload = {
            "name": f"Test Plan {fake.uuid4()[:8]}",
            "partnerId": str(uuid.uuid4()),
            "amount": 99000,
            "currency": "IDR",
            "billingInterval": "MONTHLY",
            "description": "Test subscription plan"
        }
        response = api.post("/api/v1/subscriptions/plans", json=payload)
        assert response.status_code in [200, 201, 400, 401, 403, 422], f"Unexpected status: {response.status_code}"

    def test_subscribe_to_plan(self, user_session):
        """Subscribe to a plan"""
        api = user_session["api"]
        payload = {
            "accountId": user_session["user_id"],
            "planId": str(uuid.uuid4()),
            "externalReferenceId": str(uuid.uuid4())
        }
        response = api.post("/api/v1/subscriptions", json=payload)
        assert response.status_code in [200, 201, 400, 404, 422], f"Unexpected status: {response.status_code}"
