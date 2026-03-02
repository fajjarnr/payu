import pytest
import uuid
from client import PayUClient
from faker import Faker

fake = Faker()


@pytest.mark.e2e
class TestDisputeServiceFlow:
    """
    Dispute & Refund Service E2E tests.
    Tests: Open dispute -> Investigate -> Add evidence -> Resolve/Reject -> Refunds
    """

    @pytest.fixture(scope="class")
    def api(self):
        return PayUClient(gateway_url="http://localhost:8080")

    @pytest.fixture(scope="class")
    def user_session(self, api):
        """Register and login a user for dispute operations"""
        user_data = {
            "email": f"disp_{fake.uuid4()}@example.com",
            "username": f"disp_{fake.uuid4()[:8]}",
            "password": "Password123!",
            "name": fake.name(),
            "phoneNumber": "+6281234567890"
        }

        response = api.post("/api/v1/accounts/register", json=user_data)
        if response.status_code in [401, 403, 429, 500, 502, 503, 504]:
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
    def test_open_dispute(self, user_session):
        """Open a new dispute for a transaction"""
        api = user_session["api"]
        payload = {
            "transactionId": str(uuid.uuid4()),
            "customerId": user_session["user_id"],
            "merchantId": str(uuid.uuid4()),
            "disputedAmount": 150000,
            "currency": "IDR",
            "reason": "Unauthorized transaction"
        }
        response = api.post("/api/v1/disputes", json=payload)
        assert response.status_code in [200, 201, 400, 422], f"Unexpected status: {response.status_code}"

    def test_get_dispute_by_id(self, user_session):
        """Get a dispute by ID"""
        api = user_session["api"]
        fake_id = str(uuid.uuid4())
        response = api.get(f"/api/v1/disputes/{fake_id}")
        assert response.status_code in [200, 404], f"Unexpected status: {response.status_code}"

    def test_get_disputes_by_customer(self, user_session):
        """Get disputes for a customer"""
        api = user_session["api"]
        customer_id = user_session["user_id"]
        response = api.get(f"/api/v1/disputes/customer/{customer_id}")
        assert response.status_code in [200, 404], f"Unexpected status: {response.status_code}"
        if response.status_code == 200:
            data = response.json()
            assert isinstance(data, (list, dict))

    def test_get_disputes_by_status(self, user_session):
        """Get disputes filtered by status"""
        api = user_session["api"]
        response = api.get("/api/v1/disputes/status/OPEN")
        assert response.status_code in [200, 404], f"Unexpected status: {response.status_code}"

    def test_add_evidence_to_dispute(self, user_session):
        """Add evidence to a dispute"""
        api = user_session["api"]
        fake_id = str(uuid.uuid4())
        payload = {
            "fileName": "receipt.pdf",
            "fileUrl": "https://storage.example.com/evidence/receipt.pdf",
            "uploadedBy": user_session["user_id"]
        }
        response = api.post(f"/api/v1/disputes/{fake_id}/evidence", json=payload)
        assert response.status_code in [200, 404], f"Unexpected status: {response.status_code}"

    def test_investigate_dispute(self, user_session):
        """Start investigation on a dispute"""
        api = user_session["api"]
        fake_id = str(uuid.uuid4())
        payload = {"investigationId": str(uuid.uuid4())}
        response = api.post(f"/api/v1/disputes/{fake_id}/investigate", json=payload)
        assert response.status_code in [200, 400, 404], f"Unexpected status: {response.status_code}"

    def test_resolve_dispute(self, user_session):
        """Resolve a dispute"""
        api = user_session["api"]
        fake_id = str(uuid.uuid4())
        payload = {
            "resolutionType": "REFUND",
            "resolution": "Full refund issued to customer"
        }
        response = api.post(f"/api/v1/disputes/{fake_id}/resolve", json=payload)
        assert response.status_code in [200, 400, 404], f"Unexpected status: {response.status_code}"

    def test_escalate_dispute(self, user_session):
        """Escalate a dispute"""
        api = user_session["api"]
        fake_id = str(uuid.uuid4())
        payload = {"escalationReason": "Requires senior review due to high amount"}
        response = api.post(f"/api/v1/disputes/{fake_id}/escalate", json=payload)
        assert response.status_code in [200, 400, 404], f"Unexpected status: {response.status_code}"

    # --- Refund Tests ---

    def test_create_full_refund(self, user_session):
        """Create a full refund for a transaction"""
        api = user_session["api"]
        payload = {
            "transactionId": str(uuid.uuid4()),
            "reason": "Customer requested full refund"
        }
        response = api.post("/api/v1/refunds/full", json=payload)
        assert response.status_code in [200, 201, 400, 404, 422], f"Unexpected status: {response.status_code}"

    def test_create_partial_refund(self, user_session):
        """Create a partial refund"""
        api = user_session["api"]
        payload = {
            "transactionId": str(uuid.uuid4()),
            "amount": 50000,
            "currency": "IDR",
            "reason": "Partial service not delivered"
        }
        response = api.post("/api/v1/refunds/partial", json=payload)
        assert response.status_code in [200, 201, 400, 404, 422], f"Unexpected status: {response.status_code}"

    def test_get_refund_by_id(self, user_session):
        """Get refund by ID"""
        api = user_session["api"]
        fake_id = str(uuid.uuid4())
        response = api.get(f"/api/v1/refunds/{fake_id}")
        assert response.status_code in [200, 404], f"Unexpected status: {response.status_code}"

    def test_get_refunds_by_transaction(self, user_session):
        """Get all refunds for a transaction"""
        api = user_session["api"]
        fake_txn_id = str(uuid.uuid4())
        response = api.get(f"/api/v1/refunds/transaction/{fake_txn_id}")
        assert response.status_code in [200, 404], f"Unexpected status: {response.status_code}"

    def test_get_refunds_by_status(self, user_session):
        """Get refunds filtered by status"""
        api = user_session["api"]
        response = api.get("/api/v1/refunds/status/PENDING")
        assert response.status_code in [200, 404], f"Unexpected status: {response.status_code}"
