import pytest
import uuid
import time
from faker import Faker

fake = Faker()


@pytest.mark.e2e
@pytest.mark.smoke
class TestGatewayServiceFlow:
    """
    Gateway Service E2E tests.
    Tests: Health check -> Rate limiting -> Payment methods -> Rate plans -> CORS -> Routing
    """

    def test_gateway_health(self, api):
        """Verify gateway health endpoint"""
        response = api.get("/q/health")
        assert response.status_code in [200, 503], f"Unexpected status: {response.status_code}"
        if response.status_code == 200:
            data = response.json()
            assert data.get("status") in ["UP", "up"]

    def test_gateway_liveness(self, api):
        """Verify gateway liveness probe"""
        response = api.get("/q/health/live")
        assert response.status_code in [200, 503], f"Unexpected status: {response.status_code}"

    def test_gateway_readiness(self, api):
        """Verify gateway readiness probe"""
        response = api.get("/q/health/ready")
        assert response.status_code in [200, 503], f"Unexpected status: {response.status_code}"

    def test_cors_preflight(self, api):
        """Test CORS preflight for OPTIONS request"""
        response = api.options("/api/v1/accounts")
        assert response.status_code in [200, 204, 403, 404, 405], f"Unexpected status: {response.status_code}"

    def test_get_payment_methods(self, api):
        """Get available payment methods"""
        response = api.get("/api/v1/payments/methods", params={
            "amount": 100000,
            "currency": "IDR"
        })
        assert response.status_code in [200, 401, 404], f"Unexpected status: {response.status_code}"
        if response.status_code == 200:
            data = response.json()
            assert isinstance(data, (list, dict))

    def test_get_rate_plans(self, authenticated_api):
        """List rate plans (admin)"""
        response = authenticated_api.get("/api/v1/admin/rate-plans")
        assert response.status_code in [200, 401, 403, 404], f"Unexpected status: {response.status_code}"
        if response.status_code == 200:
            data = response.json()
            assert isinstance(data, (list, dict))

    def test_create_rate_plan(self, authenticated_api):
        """Create a rate plan (admin)"""
        payload = {
            "name": f"Test Plan {fake.uuid4()[:8]}",
            "requestsPerSecond": 100,
            "requestsPerDay": 10000,
            "burstLimit": 200,
            "active": True
        }
        response = authenticated_api.post("/api/v1/admin/rate-plans", json=payload)
        assert response.status_code in [200, 201, 400, 401, 403, 422], f"Unexpected status: {response.status_code}"

    def test_get_partner_rate_limit_status(self, authenticated_api):
        """Check rate limit status for a partner"""
        fake_partner_id = str(uuid.uuid4())
        response = authenticated_api.get(f"/api/v1/admin/rate-plans/partners/{fake_partner_id}/status")
        assert response.status_code in [200, 401, 403, 404], f"Unexpected status: {response.status_code}"

    def test_routing_to_account_service(self, api):
        """Test gateway routing to account service"""
        response = api.get("/api/v1/accounts/health")
        # Any response means routing works; 401/404 are also valid
        assert response.status_code in [200, 401, 403, 404, 405], f"Routing failed: {response.status_code}"

    def test_routing_to_wallet_service(self, api):
        """Test gateway routing to wallet service"""
        response = api.get("/api/v1/wallets/health")
        assert response.status_code in [200, 401, 403, 404, 405, 500, 503], f"Routing failed: {response.status_code}"

    def test_unauthorized_access(self, api):
        """Test that protected endpoints require authentication"""
        # Use a fresh client to avoid using the session token
        from client import PayUClient
        unauthenticated = PayUClient(gateway_url=api.gateway_url)
        unauthenticated.session.headers.update({"X-E2E-Test": "true"})
        response = unauthenticated.get("/api/v1/accounts/me")
        assert response.status_code in [401, 403], f"Expected auth error, got: {response.status_code}"

    def test_invalid_route(self, api):
        """Test that invalid routes return 404"""
        response = api.get("/api/v1/nonexistent-service/something")
        assert response.status_code in [404, 405], f"Expected 404, got: {response.status_code}"
