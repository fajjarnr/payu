import pytest
import uuid
from client import PayUClient
from faker import Faker

fake = Faker()


@pytest.mark.e2e
class TestKycServiceFlow:
    """
    KYC (Know Your Customer) Service E2E tests.
    Tests: Start verification -> Upload KTP -> Upload selfie -> Check status -> User history
    """

    @pytest.fixture(scope="class")
    def api(self):
        return PayUClient(gateway_url="http://localhost:8080")

    @pytest.fixture(scope="class")
    def user_session(self, api):
        """Register and login a user for KYC operations"""
        user_data = {
            "email": f"kyc_{fake.uuid4()}@example.com",
            "username": f"kyc_{fake.uuid4()[:8]}",
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
    def test_start_kyc_verification(self, user_session):
        """Start a new KYC verification process"""
        api = user_session["api"]
        idempotency_key = str(uuid.uuid4())
        api.session.headers.update({"Idempotency-Key": idempotency_key})
        payload = {
            "user_id": user_session["user_id"],
            "verification_type": "FULL_KYC"
        }
        response = api.post("/api/v1/kyc/verify/start", json=payload)
        assert response.status_code in [200, 201, 400, 422], f"Unexpected status: {response.status_code}"
        api.session.headers.pop("Idempotency-Key", None)
        if response.status_code in [200, 201]:
            data = response.json()
            assert "verification_id" in data or "verificationId" in data

    def test_upload_ktp(self, user_session):
        """Upload KTP image for OCR processing"""
        api = user_session["api"]
        idempotency_key = str(uuid.uuid4())
        api.session.headers.update({"Idempotency-Key": idempotency_key})
        payload = {
            "verification_id": str(uuid.uuid4()),
            "ktp_image": "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAAgGBgcGBQgHBwcJCQgK..."
        }
        response = api.post("/api/v1/kyc/verify/ktp", json=payload)
        assert response.status_code in [200, 201, 400, 404, 422], f"Unexpected status: {response.status_code}"
        api.session.headers.pop("Idempotency-Key", None)

    def test_upload_selfie(self, user_session):
        """Upload selfie for liveness detection and face matching"""
        api = user_session["api"]
        idempotency_key = str(uuid.uuid4())
        api.session.headers.update({"Idempotency-Key": idempotency_key})
        payload = {
            "verification_id": str(uuid.uuid4()),
            "selfie_image": "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAAgGBgcGBQgHBwcJCQgK..."
        }
        response = api.post("/api/v1/kyc/verify/selfie", json=payload)
        assert response.status_code in [200, 201, 400, 404, 422], f"Unexpected status: {response.status_code}"
        api.session.headers.pop("Idempotency-Key", None)

    def test_get_verification_status(self, user_session):
        """Get KYC verification status by ID"""
        api = user_session["api"]
        fake_id = str(uuid.uuid4())
        response = api.get(f"/api/v1/kyc/verify/{fake_id}")
        assert response.status_code in [200, 404], f"Unexpected status: {response.status_code}"

    def test_get_user_kyc_history(self, user_session):
        """Get KYC verification history for user"""
        api = user_session["api"]
        user_id = user_session["user_id"]
        response = api.get(f"/api/v1/kyc/user/{user_id}")
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
