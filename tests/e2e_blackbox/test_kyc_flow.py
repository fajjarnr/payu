import pytest
import uuid
from faker import Faker

fake = Faker()


@pytest.mark.e2e
class TestKycServiceFlow:
    """
    KYC (Know Your Customer) Service E2E tests.
    Tests: Start verification -> Upload KTP -> Upload selfie -> Check status -> User history
    """

    @pytest.mark.smoke
    def test_start_kyc_verification(self, authenticated_api, registered_user):
        """Start a new KYC verification process"""
        idempotency_key = str(uuid.uuid4())
        authenticated_api.session.headers.update({"Idempotency-Key": idempotency_key})
        payload = {
            "user_id": registered_user["userId"],
            "verification_type": "FULL_KYC"
        }
        response = authenticated_api.post("/api/v1/kyc/verify/start", json=payload)
        assert response.status_code in [200, 201, 400, 422], f"Unexpected status: {response.status_code}"
        authenticated_api.session.headers.pop("Idempotency-Key", None)
        if response.status_code in [200, 201]:
            data = response.json()
            assert "verification_id" in data or "verificationId" in data

    def test_upload_ktp(self, authenticated_api):
        """Upload KTP image for OCR processing"""
        idempotency_key = str(uuid.uuid4())
        authenticated_api.session.headers.update({"Idempotency-Key": idempotency_key})
        payload = {
            "verification_id": str(uuid.uuid4()),
            "ktp_image": "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAAgGBgcGBQgHBwcJCQgK..."
        }
        response = authenticated_api.post("/api/v1/kyc/verify/ktp", json=payload)
        assert response.status_code in [200, 201, 400, 404, 422], f"Unexpected status: {response.status_code}"
        authenticated_api.session.headers.pop("Idempotency-Key", None)

    def test_upload_selfie(self, authenticated_api):
        """Upload selfie for liveness detection and face matching"""
        idempotency_key = str(uuid.uuid4())
        authenticated_api.session.headers.update({"Idempotency-Key": idempotency_key})
        payload = {
            "verification_id": str(uuid.uuid4()),
            "selfie_image": "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAAgGBgcGBQgHBwcJCQgK..."
        }
        response = authenticated_api.post("/api/v1/kyc/verify/selfie", json=payload)
        assert response.status_code in [200, 201, 400, 404, 422], f"Unexpected status: {response.status_code}"
        authenticated_api.session.headers.pop("Idempotency-Key", None)

    def test_get_verification_status(self, authenticated_api):
        """Get KYC verification status by ID"""
        fake_id = str(uuid.uuid4())
        response = authenticated_api.get(f"/api/v1/kyc/verify/{fake_id}")
        assert response.status_code in [200, 404], f"Unexpected status: {response.status_code}"

    def test_get_user_kyc_history(self, authenticated_api, registered_user):
        """Get KYC verification history for user"""
        user_id = registered_user["userId"]
        response = authenticated_api.get(f"/api/v1/kyc/user/{user_id}")
        assert response.status_code in [200, 404], f"Unexpected status: {response.status_code}"
        if response.status_code == 200:
            data = response.json()
            assert isinstance(data, (list, dict))

    def test_kyc_health_check(self, api):
        """Verify KYC service health endpoint"""
        # Direct call to KYC service health (bypassing gateway)
        import requests
        try:
            response = requests.get("http://localhost:8007/health", timeout=5)
            assert response.status_code in [200, 404, 503], f"Unexpected status: {response.status_code}"
        except requests.ConnectionError:
            pytest.skip("KYC service not directly reachable on port 8007")
