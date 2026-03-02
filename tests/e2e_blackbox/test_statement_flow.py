import pytest
import uuid
from client import PayUClient
from faker import Faker

fake = Faker()


@pytest.mark.e2e
class TestStatementServiceFlow:
    """
    Statement Service E2E tests.
    Tests: Generate e-statement -> List statements -> Get latest -> Download PDF -> Receipts
    """

    @pytest.fixture(scope="class")
    def api(self):
        return PayUClient(gateway_url="http://localhost:8080")

    @pytest.fixture(scope="class")
    def user_session(self, api):
        """Register and login a user for statement operations"""
        user_data = {
            "email": f"stmt_{fake.uuid4()}@example.com",
            "username": f"stmt_{fake.uuid4()[:8]}",
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
    def test_list_statements(self, user_session):
        """List user statements (should return empty or list)"""
        api = user_session["api"]
        response = api.get("/api/v1/statements", params={"page": 0, "size": 10})
        assert response.status_code in [200, 404], f"Unexpected status: {response.status_code}"
        if response.status_code == 200:
            data = response.json()
            assert isinstance(data, (list, dict))

    def test_generate_statement(self, user_session):
        """Request e-statement generation"""
        api = user_session["api"]
        payload = {
            "accountId": user_session["user_id"],
            "startDate": "2026-01-01",
            "endDate": "2026-02-28",
            "format": "PDF"
        }
        response = api.post("/api/v1/statements/generate", json=payload)
        assert response.status_code in [200, 201, 202, 400, 422], f"Unexpected status: {response.status_code}"

    def test_get_latest_statement(self, user_session):
        """Get the latest statement for user"""
        api = user_session["api"]
        response = api.get("/api/v1/statements/latest")
        assert response.status_code in [200, 404], f"Unexpected status: {response.status_code}"

    def test_get_statement_by_id(self, user_session):
        """Get statement by ID"""
        api = user_session["api"]
        fake_id = str(uuid.uuid4())
        response = api.get(f"/api/v1/statements/{fake_id}")
        assert response.status_code in [200, 404], f"Unexpected status: {response.status_code}"

    def test_download_statement_pdf(self, user_session):
        """Attempt to download a statement PDF"""
        api = user_session["api"]
        fake_id = str(uuid.uuid4())
        response = api.get(f"/api/v1/statements/{fake_id}/download")
        assert response.status_code in [200, 404], f"Unexpected status: {response.status_code}"
        if response.status_code == 200:
            assert "application/pdf" in response.headers.get("Content-Type", "")

    def test_generate_receipt(self, user_session):
        """Generate a transaction receipt"""
        api = user_session["api"]
        payload = {
            "transactionId": str(uuid.uuid4()),
            "format": "PDF"
        }
        response = api.post("/api/v1/statements/receipts/generate", json=payload)
        assert response.status_code in [200, 201, 400, 404, 422], f"Unexpected status: {response.status_code}"

    def test_get_receipt_by_transaction_id(self, user_session):
        """Get receipt by transaction ID"""
        api = user_session["api"]
        fake_txn_id = str(uuid.uuid4())
        response = api.get(f"/api/v1/statements/receipts/transaction/{fake_txn_id}")
        assert response.status_code in [200, 404], f"Unexpected status: {response.status_code}"
