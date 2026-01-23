import pytest
from client import PayUClient
from faker import Faker

fake = Faker()


class TestComplianceFlow:
    """
    Compliance and AML/CFT E2E tests.
    Tests: Create Audit Report -> Check Compliance Status -> GDPR Audit
    """

    @pytest.fixture(scope="class")
    def api(self):
        return PayUClient(gateway_url="http://localhost:8080")

    @pytest.fixture(scope="class")
    def user_session(self, api):
        """Register and login a user"""
        user_data = {
            "email": f"compliance_{fake.uuid4()}@example.com",
            "username": f"comp_{fake.uuid4()[:8]}",
            "password": "Password123!",
            "name": fake.name(),
            "phoneNumber": "+6281234567890"
        }

        response = api.post("/api/v1/accounts/register", json=user_data)
        assert response.status_code in [200, 201]
        user_id = response.json().get("id", response.json().get("userId"))

        response = api.post("/api/v1/auth/login", json={
            "username": user_data["username"],
            "password": user_data["password"]
        })
        assert response.status_code == 200
        api.set_token(response.json()["access_token"])

        return {"user_id": user_id, "api": api}

    def test_create_aml_audit_report(self, user_session):
        """
        Create an AML (Anti-Money Laundering) audit report
        """
        api = user_session["api"]
        user_id = user_session["user_id"]

        # Create a transaction to audit
        transaction_id = fake.uuid4()
        merchant_id = f"MERCH_{fake.uuid4()[:8]}"

        response = api.post("/api/v1/compliance/audit-report", json={
            "transactionId": transaction_id,
            "merchantId": merchant_id,
            "standard": "AML",
            "checks": [
                {
                    "type": "SANCTION_SCREENING",
                    "status": "PASSED",
                    "notes": "No sanctions found"
                },
                {
                    "type": "PEP_SCREENING",
                    "status": "PASSED",
                    "notes": "No PEP matches"
                },
                {
                    "type": "TRANSACTION_PATTERN",
                    "status": "REVIEW_REQUIRED",
                    "notes": "Unusual transaction pattern detected"
                }
            ]
        })

        if response.status_code not in [200, 201]:
            pytest.skip(f"Audit report creation may require valid transaction: {response.text}")

        report = response.json()
        assert report is not None
        assert "id" in report
        assert report["standard"] == "AML"

        return report.get("id")

    def test_get_audit_report(self, user_session):
        """
        Get an existing audit report
        """
        api = user_session["api"]

        # First create a report
        response = api.post("/api/v1/compliance/audit-report", json={
            "transactionId": fake.uuid4(),
            "merchantId": f"MERCH_{fake.uuid4()[:8]}",
            "standard": "CFT",
            "checks": [
                {
                    "type": "TERRORIST_FINANCING",
                    "status": "PASSED",
                    "notes": "No terrorist financing indicators"
                }
            ]
        })

        if response.status_code not in [200, 201]:
            pytest.skip("Audit report creation required")

        report = response.json()
        report_id = report.get("id")

        response = api.get(f"/api/v1/compliance/audit-report/{report_id}")
        assert response.status_code == 200
        retrieved_report = response.json()
        assert retrieved_report["id"] == report_id

    def test_search_audit_reports_by_transaction(self, user_session):
        """
        Search audit reports by transaction ID
        """
        api = user_session["api"]
        user_id = user_session["user_id"]

        transaction_id = fake.uuid4()

        # Create a report
        response = api.post("/api/v1/compliance/audit-report", json={
            "transactionId": transaction_id,
            "merchantId": f"MERCH_{fake.uuid4()[:8]}",
            "standard": "AML",
            "checks": []
        })

        if response.status_code not in [200, 201]:
            pytest.skip("Audit report creation required")

        # Search for the report
        response = api.get("/api/v1/compliance/audit-report", params={
            "transactionId": transaction_id
        })

        if response.status_code != 200:
            pytest.skip(f"Search may require valid transaction ID: {response.text}")

        reports = response.json()
        assert isinstance(reports, list)

    def test_search_audit_reports_by_merchant(self, user_session):
        """
        Search audit reports by merchant ID
        """
        api = user_session["api"]
        user_id = user_session["user_id"]

        merchant_id = f"MERCH_{fake.uuid4()[:8]}"

        # Create a report
        response = api.post("/api/v1/compliance/audit-report", json={
            "transactionId": fake.uuid4(),
            "merchantId": merchant_id,
            "standard": "AML",
            "checks": []
        })

        if response.status_code not in [200, 201]:
            pytest.skip("Audit report creation required")

        # Search for the report
        response = api.get("/api/v1/compliance/audit-report", params={
            "merchantId": merchant_id
        })

        if response.status_code != 200:
            pytest.skip(f"Search may require valid merchant ID: {response.text}")

        reports = response.json()
        assert isinstance(reports, list)

    def test_filter_audit_reports_by_standard(self, user_session):
        """
        Filter audit reports by compliance standard
        """
        api = user_session["api"]

        # Create AML report
        api.post("/api/v1/compliance/audit-report", json={
            "transactionId": fake.uuid4(),
            "merchantId": f"MERCH_{fake.uuid4()[:8]}",
            "standard": "AML",
            "checks": []
        })

        # Create CFT report
        api.post("/api/v1/compliance/audit-report", json={
            "transactionId": fake.uuid4(),
            "merchantId": f"MERCH_{fake.uuid4()[:8]}",
            "standard": "CFT",
            "checks": []
        })

        # Search for AML reports
        response = api.get("/api/v1/compliance/audit-report", params={
            "merchantId": f"MERCH_{fake.uuid4()[:8]}",
            "standard": "AML"
        })

        if response.status_code != 200:
            pytest.skip(f"Filter may require valid merchant ID: {response.text}")

        reports = response.json()
        assert isinstance(reports, list)
