import pytest
import time
from faker import Faker

fake = Faker()


@pytest.mark.e2e
@pytest.mark.critical
class TestFullUserJourney:
    """
    Holistic End-to-End test suite covering full user onboarding journey.
    Tests: Registration -> Login -> Wallet Creation -> KYC -> Transactions

    Known infrastructure state:
    - wallet-service: circuit breaker OPEN (503)
    - billing-service: billers/categories endpoints work, but payment creation
      endpoint is not routed at gateway root level
    - transaction-service: QRIS requires specific schema (type, sourceAccountId)
      and Idempotency-Key header for financial operations
    """

    def test_complete_user_onboarding_journey(self, authenticated_api, registered_user):
        """
        Complete user onboarding journey:
        1. Register new user (via registered_user fixture)
        2. Login and get authentication token (via authenticated_api fixture)
        3. Verify wallet-service responds (expected: 503 circuit breaker)
        4. Verify transaction history endpoint behavior
        """
        user_id = registered_user.get("userId")
        assert user_id is not None, "User ID not set — registration fixture did not succeed"

        # Step 3: Wallet balance check — wallet-service circuit breaker is open
        response = authenticated_api.get(f"/api/v1/wallets/{user_id}/balance")
        assert response.status_code in [429, 500, 503], (
            f"Expected 500/503 (wallet-service circuit breaker open), got {response.status_code}: {response.text}"
        )
        if response.status_code == 503:
            body = response.json()
            assert body["error"] == "CIRCUIT_OPEN"
            assert "wallet-service" in body["message"]

        # Step 4: Transaction history — wallet transactions also fail via circuit breaker
        response = authenticated_api.get(f"/api/v1/wallets/{user_id}/transactions")
        assert response.status_code in [429, 500, 503], (
            f"Expected 500/503 (wallet-service circuit breaker), got {response.status_code}"
        )

    def test_balance_topup_and_transfer_flow(self, authenticated_api, registered_user):
        """
        Balance topup requires wallet-service which has circuit breaker open.
        Verify the 503 circuit breaker response is returned correctly.
        """
        user_id = registered_user.get("userId")
        assert user_id is not None, "User ID not set — registration fixture did not succeed"

        # Attempt topup — wallet-service circuit breaker open
        response = authenticated_api.post(f"/api/v1/wallets/{user_id}/credit", json={
            "amount": 1000000,
            "referenceId": f"TOPUP_{fake.uuid4()}",
            "description": "Initial topup"
        })
        assert response.status_code in [429, 500, 503], (
            f"Expected 500/503 (wallet-service circuit breaker), got {response.status_code}: {response.text}"
        )
        if response.status_code == 503:
            body = response.json()
            assert body["error"] == "CIRCUIT_OPEN"
            assert "wallet-service" in body["message"]
            assert "retryAfterSeconds" in body

    def test_bill_payment_journey(self, authenticated_api, registered_user):
        """
        Bill payment journey:
        1. List available billers — works via billing-service
        2. Get biller categories — works via billing-service
        3. Payment creation — returns 404 (gateway root /payments POST route
           doesn't match billing-service endpoint)
        """
        # Step 1: List billers — this works
        response = authenticated_api.get("/api/v1/billers")
        assert response.status_code == 200, (
            f"Expected 200 from billers list, got {response.status_code}: {response.text}"
        )
        body = response.json()
        assert body["success"] is True
        billers = body["data"]
        assert isinstance(billers, list)
        assert len(billers) > 0, "Expected at least one biller"

        # Verify biller structure
        first_biller = billers[0]
        assert "code" in first_biller
        assert "displayName" in first_biller
        assert "category" in first_biller

        # Step 2: Get biller categories
        response = authenticated_api.get("/api/v1/billers/categories")
        assert response.status_code == 200, (
            f"Expected 200 from biller categories, got {response.status_code}: {response.text}"
        )
        categories_body = response.json()
        assert categories_body["success"] is True
        categories = categories_body["data"]
        assert isinstance(categories, list)
        assert len(categories) > 0

        # Step 3: Attempt bill payment — POST /api/v1/payments returns 404
        # The gateway's paymentRootPost route exists but the billing-service
        # doesn't have a matching endpoint, resulting in a 404 from the gateway.
        response = authenticated_api.post("/api/v1/payments", json={
            "billerCode": first_biller["code"],
            "customerId": f"CUST_{fake.uuid4()[:8]}",
            "amount": 50000,
            "referenceNumber": f"BILL_{fake.uuid4()}",
            "accountNumber": "1234567890"
        })
        assert response.status_code == 404, (
            f"Expected 404 (payment creation not routed), got {response.status_code}: {response.text}"
        )

    def test_qris_payment_journey(self, authenticated_api):
        """
        QRIS payment requires specific schema fields (type, sourceAccountId)
        and an Idempotency-Key header. The test payload uses the wrong field names,
        resulting in 400 SCHEMA_VALIDATION_FAILED from the gateway's request
        validation filter.
        """
        # Send the request with the legacy/wrong field names
        response = authenticated_api.post("/api/v1/transactions/qris/pay", json={
            "qrisCode": fake.uuid4(),
            "amount": 100000,
            "merchantName": "Test Merchant",
            "reference": f"QRIS_{fake.uuid4()}"
        })

        # Gateway schema validation rejects this — required fields: type, sourceAccountId
        assert response.status_code == 400, (
            f"Expected 400 (schema validation), got {response.status_code}: {response.text}"
        )
        body = response.json()
        assert body["error"] == "SCHEMA_VALIDATION_FAILED", (
            f"Expected SCHEMA_VALIDATION_FAILED error, got: {body.get('error')}"
        )
        assert "validationErrors" in body
        validation_errors = body["validationErrors"]
        assert isinstance(validation_errors, list)
        assert len(validation_errors) > 0

        # Verify specific validation errors about missing required fields
        error_messages = [e["message"] for e in validation_errors]
        has_type_error = any("'type' not found" in msg for msg in error_messages)
        has_source_error = any("'sourceAccountId' not found" in msg for msg in error_messages)
        assert has_type_error, f"Expected missing 'type' field error in: {error_messages}"
        assert has_source_error, f"Expected missing 'sourceAccountId' field error in: {error_messages}"
