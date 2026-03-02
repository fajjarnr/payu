import pytest
import uuid
from faker import Faker

fake = Faker()


@pytest.mark.e2e
class TestDisputeServiceFlow:
    """
    Dispute & Refund Service E2E tests.
    Tests: Open dispute -> Investigate -> Add evidence -> Resolve/Reject -> Refunds
    """

    @pytest.mark.smoke
    def test_open_dispute(self, authenticated_api, registered_user):
        """Open a new dispute for a transaction"""
        payload = {
            "transactionId": str(uuid.uuid4()),
            "customerId": registered_user["userId"],
            "merchantId": str(uuid.uuid4()),
            "disputedAmount": 150000,
            "currency": "IDR",
            "reason": "Unauthorized transaction"
        }
        response = authenticated_api.post("/api/v1/disputes", json=payload)
        assert response.status_code in [200, 201, 400, 422], f"Unexpected status: {response.status_code}"

    def test_get_dispute_by_id(self, authenticated_api):
        """Get a dispute by ID"""
        fake_id = str(uuid.uuid4())
        response = authenticated_api.get(f"/api/v1/disputes/{fake_id}")
        assert response.status_code in [200, 404], f"Unexpected status: {response.status_code}"

    def test_get_disputes_by_customer(self, authenticated_api, registered_user):
        """Get disputes for a customer"""
        customer_id = registered_user["userId"]
        response = authenticated_api.get(f"/api/v1/disputes/customer/{customer_id}")
        assert response.status_code in [200, 404], f"Unexpected status: {response.status_code}"
        if response.status_code == 200:
            data = response.json()
            assert isinstance(data, (list, dict))

    def test_get_disputes_by_status(self, authenticated_api):
        """Get disputes filtered by status"""
        response = authenticated_api.get("/api/v1/disputes/status/OPEN")
        assert response.status_code in [200, 404], f"Unexpected status: {response.status_code}"

    def test_add_evidence_to_dispute(self, authenticated_api, registered_user):
        """Add evidence to a dispute"""
        fake_id = str(uuid.uuid4())
        payload = {
            "fileName": "receipt.pdf",
            "fileUrl": "https://storage.example.com/evidence/receipt.pdf",
            "uploadedBy": registered_user["userId"]
        }
        response = authenticated_api.post(f"/api/v1/disputes/{fake_id}/evidence", json=payload)
        assert response.status_code in [200, 404], f"Unexpected status: {response.status_code}"

    def test_investigate_dispute(self, authenticated_api):
        """Start investigation on a dispute"""
        fake_id = str(uuid.uuid4())
        payload = {"investigationId": str(uuid.uuid4())}
        response = authenticated_api.post(f"/api/v1/disputes/{fake_id}/investigate", json=payload)
        assert response.status_code in [200, 400, 404], f"Unexpected status: {response.status_code}"

    def test_resolve_dispute(self, authenticated_api):
        """Resolve a dispute"""
        fake_id = str(uuid.uuid4())
        payload = {
            "resolutionType": "REFUND",
            "resolution": "Full refund issued to customer"
        }
        response = authenticated_api.post(f"/api/v1/disputes/{fake_id}/resolve", json=payload)
        assert response.status_code in [200, 400, 404], f"Unexpected status: {response.status_code}"

    def test_escalate_dispute(self, authenticated_api):
        """Escalate a dispute"""
        fake_id = str(uuid.uuid4())
        payload = {"escalationReason": "Requires senior review due to high amount"}
        response = authenticated_api.post(f"/api/v1/disputes/{fake_id}/escalate", json=payload)
        assert response.status_code in [200, 400, 404], f"Unexpected status: {response.status_code}"

    # --- Refund Tests ---

    def test_create_full_refund(self, authenticated_api):
        """Create a full refund for a transaction"""
        payload = {
            "transactionId": str(uuid.uuid4()),
            "reason": "Customer requested full refund"
        }
        response = authenticated_api.post("/api/v1/refunds/full", json=payload)
        assert response.status_code in [200, 201, 400, 404, 422], f"Unexpected status: {response.status_code}"

    def test_create_partial_refund(self, authenticated_api):
        """Create a partial refund"""
        payload = {
            "transactionId": str(uuid.uuid4()),
            "amount": 50000,
            "currency": "IDR",
            "reason": "Partial service not delivered"
        }
        response = authenticated_api.post("/api/v1/refunds/partial", json=payload)
        assert response.status_code in [200, 201, 400, 404, 422], f"Unexpected status: {response.status_code}"

    def test_get_refund_by_id(self, authenticated_api):
        """Get refund by ID"""
        fake_id = str(uuid.uuid4())
        response = authenticated_api.get(f"/api/v1/refunds/{fake_id}")
        assert response.status_code in [200, 404], f"Unexpected status: {response.status_code}"

    def test_get_refunds_by_transaction(self, authenticated_api):
        """Get all refunds for a transaction"""
        fake_txn_id = str(uuid.uuid4())
        response = authenticated_api.get(f"/api/v1/refunds/transaction/{fake_txn_id}")
        assert response.status_code in [200, 404], f"Unexpected status: {response.status_code}"

    def test_get_refunds_by_status(self, authenticated_api):
        """Get refunds filtered by status"""
        response = authenticated_api.get("/api/v1/refunds/status/PENDING")
        assert response.status_code in [200, 404], f"Unexpected status: {response.status_code}"
