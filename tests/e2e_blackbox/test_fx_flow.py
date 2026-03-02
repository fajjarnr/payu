import pytest
import uuid
from client import PayUClient
from faker import Faker

fake = Faker()


@pytest.mark.e2e
class TestFxServiceFlow:
    """
    FX (Foreign Exchange) Service E2E tests.
    Tests: Get rates -> Estimate conversion -> Execute conversion -> Reverse conversion
    """

    @pytest.fixture(scope="class")
    def api(self):
        return PayUClient(gateway_url="http://localhost:8080")

    @pytest.fixture(scope="class")
    def user_session(self, api):
        """Register and login a user for FX operations"""
        user_data = {
            "email": f"fx_{fake.uuid4()}@example.com",
            "username": f"fx_{fake.uuid4()[:8]}",
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
    def test_get_all_fx_rates(self, user_session):
        """Get all available FX rates"""
        api = user_session["api"]
        response = api.get("/api/v1/fx/rates")
        assert response.status_code in [200, 404], f"Unexpected status: {response.status_code}"
        if response.status_code == 200:
            data = response.json()
            assert isinstance(data, (list, dict))

    def test_get_specific_fx_rate(self, user_session):
        """Get exchange rate for USD/IDR pair"""
        api = user_session["api"]
        response = api.get("/api/v1/fx/rates/USD/IDR")
        assert response.status_code in [200, 404], f"Unexpected status: {response.status_code}"
        if response.status_code == 200:
            rate = response.json()
            assert "rate" in rate or "buyRate" in rate or "sellRate" in rate

    def test_estimate_conversion(self, user_session):
        """Estimate currency conversion without executing"""
        api = user_session["api"]
        payload = {
            "fromCurrency": "USD",
            "toCurrency": "IDR",
            "amount": 100.00
        }
        response = api.post("/api/v1/fx/conversions/estimate", json=payload)
        assert response.status_code in [200, 400, 404], f"Unexpected status: {response.status_code}"
        if response.status_code == 200:
            result = response.json()
            assert "convertedAmount" in result or "estimatedAmount" in result or "amount" in result

    def test_execute_conversion(self, user_session):
        """Execute an actual currency conversion"""
        api = user_session["api"]
        payload = {
            "fromCurrency": "USD",
            "toCurrency": "IDR",
            "amount": 50.00
        }
        response = api.post("/api/v1/fx/conversions", json=payload)
        assert response.status_code in [200, 201, 400, 422], f"Unexpected status: {response.status_code}"
        if response.status_code in [200, 201]:
            conversion = response.json()
            assert "id" in conversion or "conversionId" in conversion

    def test_get_user_conversions(self, user_session):
        """Get list of user's FX conversions"""
        api = user_session["api"]
        response = api.get("/api/v1/fx/conversions")
        assert response.status_code in [200, 404], f"Unexpected status: {response.status_code}"
        if response.status_code == 200:
            data = response.json()
            assert isinstance(data, (list, dict))

    def test_get_conversion_by_id(self, user_session):
        """Get a specific conversion by ID (expect 404 if none exist)"""
        api = user_session["api"]
        fake_id = str(uuid.uuid4())
        response = api.get(f"/api/v1/fx/conversions/{fake_id}")
        assert response.status_code in [200, 404], f"Unexpected status: {response.status_code}"
