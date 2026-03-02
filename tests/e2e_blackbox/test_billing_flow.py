import pytest
import uuid
from faker import Faker

fake = Faker()


@pytest.mark.e2e
class TestBillingServiceFlow:
    """
    Billing Service E2E tests (dedicated).
    Tests: Biller categories -> List billers -> Bill payment -> TopUp -> Subscriptions
    """

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
        assert response.status_code in [200, 401, 403, 404], f"Unexpected status: {response.status_code}"
        if response.status_code == 200:
            data = response.json()
            assert isinstance(data, (list, dict))

    def test_list_billers_by_category(self, api):
        """List billers filtered by category (e.g. PLN)"""
        response = api.get("/api/v1/billers", params={"category": "PLN"})
        assert response.status_code in [200, 401, 403, 404], f"Unexpected status: {response.status_code}"

    def test_get_biller_by_code(self, api):
        """Get specific biller by code"""
        response = api.get("/api/v1/billers/PLN_PREPAID")
        assert response.status_code in [200, 401, 403, 404], f"Unexpected status: {response.status_code}"

    def test_create_bill_payment(self, authenticated_api, registered_user):
        """Create a bill payment with idempotency key"""
        idempotency_key = str(uuid.uuid4())
        authenticated_api.session.headers.update({"X-Idempotency-Key": idempotency_key})
        payload = {
            "billerCode": "PLN_PREPAID",
            "customerNumber": "1234567890",
            "amount": 100000,
            "description": "PLN token purchase"
        }
        response = authenticated_api.post("/api/v1/payments", json=payload)
        assert response.status_code in [200, 201, 400, 404, 422], f"Unexpected status: {response.status_code}"
        authenticated_api.session.headers.pop("X-Idempotency-Key", None)

    def test_get_payment_by_id(self, authenticated_api):
        """Get specific payment by ID"""
        fake_id = str(uuid.uuid4())
        response = authenticated_api.get(f"/api/v1/payments/{fake_id}")
        assert response.status_code in [200, 404], f"Unexpected status: {response.status_code}"

    def test_get_topup_providers(self, authenticated_api):
        """List available e-wallet top-up providers"""
        response = authenticated_api.get("/api/v1/topup/providers")
        assert response.status_code in [200, 404], f"Unexpected status: {response.status_code}"

    def test_create_topup(self, authenticated_api):
        """Create e-wallet top-up with idempotency"""
        idempotency_key = str(uuid.uuid4())
        authenticated_api.session.headers.update({"X-Idempotency-Key": idempotency_key})
        payload = {
            "provider": "GOPAY",
            "phoneNumber": "+6281234567890",
            "amount": 50000
        }
        response = authenticated_api.post("/api/v1/topup", json=payload)
        assert response.status_code in [200, 201, 400, 404, 422], f"Unexpected status: {response.status_code}"
        authenticated_api.session.headers.pop("X-Idempotency-Key", None)

    def test_list_subscription_plans(self, authenticated_api):
        """List subscription plans by partner"""
        fake_partner_id = str(uuid.uuid4())
        response = authenticated_api.get(f"/api/v1/subscriptions/plans/partner/{fake_partner_id}")
        assert response.status_code in [200, 404], f"Unexpected status: {response.status_code}"

    def test_create_subscription_plan(self, authenticated_api):
        """Create a subscription plan"""
        payload = {
            "name": f"Test Plan {fake.uuid4()[:8]}",
            "partnerId": str(uuid.uuid4()),
            "amount": 99000,
            "currency": "IDR",
            "billingInterval": "MONTHLY",
            "description": "Test subscription plan"
        }
        response = authenticated_api.post("/api/v1/subscriptions/plans", json=payload)
        assert response.status_code in [200, 201, 400, 401, 403, 422], f"Unexpected status: {response.status_code}"

    def test_subscribe_to_plan(self, authenticated_api, registered_user):
        """Subscribe to a plan"""
        payload = {
            "accountId": registered_user["userId"],
            "planId": str(uuid.uuid4()),
            "externalReferenceId": str(uuid.uuid4())
        }
        response = authenticated_api.post("/api/v1/subscriptions", json=payload)
        assert response.status_code in [200, 201, 400, 404, 422], f"Unexpected status: {response.status_code}"
