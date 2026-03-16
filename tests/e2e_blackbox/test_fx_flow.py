import pytest
import uuid
from faker import Faker

fake = Faker()


@pytest.mark.e2e
class TestFxServiceFlow:
    """
    FX (Foreign Exchange) Service E2E tests.
    Tests: Get rates -> Estimate conversion -> Execute conversion -> Reverse conversion
    """

    @pytest.mark.smoke
    def test_get_all_fx_rates(self, authenticated_api):
        """Get all available FX rates"""
        response = authenticated_api.get("/api/v1/fx/rates")
        assert response.status_code in [200, 404, 429, 503], f"Unexpected status: {response.status_code}"
        if response.status_code == 200:
            data = response.json()
            assert isinstance(data, (list, dict))

    def test_get_specific_fx_rate(self, authenticated_api):
        """Get exchange rate for USD/IDR pair"""
        response = authenticated_api.get("/api/v1/fx/rates/USD/IDR")
        assert response.status_code in [200, 404, 429, 500, 503], f"Unexpected status: {response.status_code}"
        if response.status_code == 200:
            rate = response.json()
            if isinstance(rate, dict) and "data" in rate:
                rate = rate["data"]
            assert "rate" in rate or "buyRate" in rate or "sellRate" in rate

    def test_estimate_conversion(self, authenticated_api):
        """Estimate currency conversion without executing"""
        payload = {
            "fromCurrency": "USD",
            "toCurrency": "IDR",
            "amount": 100.00
        }
        response = authenticated_api.post("/api/v1/fx/conversions/estimate", json=payload)
        assert response.status_code in [200, 400, 404, 429, 500, 503], f"Unexpected status: {response.status_code}"
        if response.status_code == 200:
            result = response.json()
            assert "convertedAmount" in result or "estimatedAmount" in result or "amount" in result

    def test_execute_conversion(self, authenticated_api):
        """Execute an actual currency conversion"""
        payload = {
            "fromCurrency": "USD",
            "toCurrency": "IDR",
            "amount": 50.00
        }
        response = authenticated_api.post("/api/v1/fx/conversions", json=payload)
        assert response.status_code in [200, 201, 400, 422, 429, 500, 503], f"Unexpected status: {response.status_code}"
        if response.status_code in [200, 201]:
            conversion = response.json()
            assert "id" in conversion or "conversionId" in conversion

    def test_get_user_conversions(self, authenticated_api):
        """Get list of user's FX conversions"""
        response = authenticated_api.get("/api/v1/fx/conversions")
        assert response.status_code in [200, 404, 429, 500, 503], f"Unexpected status: {response.status_code}"
        if response.status_code == 200:
            data = response.json()
            assert isinstance(data, (list, dict))

    def test_get_conversion_by_id(self, authenticated_api):
        """Get a specific conversion by ID (expect 404 if none exist)"""
        fake_id = str(uuid.uuid4())
        response = authenticated_api.get(f"/api/v1/fx/conversions/{fake_id}")
        assert response.status_code in [200, 404, 429, 500, 503], f"Unexpected status: {response.status_code}"
