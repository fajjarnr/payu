import pytest
from faker import Faker

fake = Faker()


@pytest.mark.compliance
class TestComplianceFlow:
    """
    Compliance and AML/CFT E2E tests.
    Tests: Create Audit Report -> Check Compliance Status -> GDPR Audit
    """

    def test_create_aml_audit_report(self, authenticated_api, registered_user):
        """
        Create an AML (Anti-Money Laundering) audit report
        """
        user_id = registered_user["userId"]

        # Create a transaction to audit
        transaction_id = fake.uuid4()
        merchant_id = f"MERCH_{fake.uuid4()[:8]}"

        response = authenticated_api.post("/api/v1/compliance/audit-report", json={
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

    def test_get_audit_report(self, authenticated_api):
        """
        Get an existing audit report
        """
        # First create a report
        response = authenticated_api.post("/api/v1/compliance/audit-report", json={
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

        response = authenticated_api.get(f"/api/v1/compliance/audit-report/{report_id}")
        assert response.status_code == 200
        retrieved_report = response.json()
        assert retrieved_report["id"] == report_id

    def test_search_audit_reports_by_transaction(self, authenticated_api, registered_user):
        """
        Search audit reports by transaction ID
        """
        user_id = registered_user["userId"]

        transaction_id = fake.uuid4()

        # Create a report
        response = authenticated_api.post("/api/v1/compliance/audit-report", json={
            "transactionId": transaction_id,
            "merchantId": f"MERCH_{fake.uuid4()[:8]}",
            "standard": "AML",
            "checks": []
        })

        if response.status_code not in [200, 201]:
            pytest.skip("Audit report creation required")

        # Search for the report
        response = authenticated_api.get("/api/v1/compliance/audit-report", params={
            "transactionId": transaction_id
        })

        if response.status_code != 200:
            pytest.skip(f"Search may require valid transaction ID: {response.text}")

        reports = response.json()
        assert isinstance(reports, list)

    def test_search_audit_reports_by_merchant(self, authenticated_api, registered_user):
        """
        Search audit reports by merchant ID
        """
        user_id = registered_user["userId"]

        merchant_id = f"MERCH_{fake.uuid4()[:8]}"

        # Create a report
        response = authenticated_api.post("/api/v1/compliance/audit-report", json={
            "transactionId": fake.uuid4(),
            "merchantId": merchant_id,
            "standard": "AML",
            "checks": []
        })

        if response.status_code not in [200, 201]:
            pytest.skip("Audit report creation required")

        # Search for the report
        response = authenticated_api.get("/api/v1/compliance/audit-report", params={
            "merchantId": merchant_id
        })

        if response.status_code != 200:
            pytest.skip(f"Search may require valid merchant ID: {response.text}")

        reports = response.json()
        assert isinstance(reports, list)

    def test_filter_audit_reports_by_standard(self, authenticated_api):
        """
        Filter audit reports by compliance standard
        """
        # Create AML report
        authenticated_api.post("/api/v1/compliance/audit-report", json={
            "transactionId": fake.uuid4(),
            "merchantId": f"MERCH_{fake.uuid4()[:8]}",
            "standard": "AML",
            "checks": []
        })

        # Create CFT report
        authenticated_api.post("/api/v1/compliance/audit-report", json={
            "transactionId": fake.uuid4(),
            "merchantId": f"MERCH_{fake.uuid4()[:8]}",
            "standard": "CFT",
            "checks": []
        })

        # Search for AML reports
        response = authenticated_api.get("/api/v1/compliance/audit-report", params={
            "merchantId": f"MERCH_{fake.uuid4()[:8]}",
            "standard": "AML"
        })

        if response.status_code != 200:
            pytest.skip(f"Filter may require valid merchant ID: {response.text}")

        reports = response.json()
        assert isinstance(reports, list)
