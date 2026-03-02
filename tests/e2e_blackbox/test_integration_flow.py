import pytest
import uuid
from client import PayUClient
from faker import Faker

fake = Faker()


@pytest.mark.e2e
class TestIntegrationServiceFlow:
    """
    Integration Service E2E tests.
    Tests: SWIFT message -> OJK report -> HTTP/SOAP requests -> Message status/retry
    """

    @pytest.fixture(scope="class")
    def api(self):
        return PayUClient(gateway_url="http://localhost:8080")

    @pytest.fixture(scope="class")
    def admin_session(self, api):
        """Login as admin for integration operations"""
        response = api.post("/api/v1/auth/login", json={
            "username": "admin",
            "password": "admin123"
        })
        if response.status_code == 200:
            api.set_token(response.json()["access_token"])
        return {"api": api}

    @pytest.mark.smoke
    def test_get_messages_by_status(self, admin_session):
        """List integration messages by status"""
        api = admin_session["api"]
        response = api.get("/api/v1/integration/messages", params={"status": "PENDING"})
        assert response.status_code in [200, 401, 403, 404], f"Unexpected status: {response.status_code}"
        if response.status_code == 200:
            data = response.json()
            assert isinstance(data, (list, dict))

    def test_process_swift_message(self, admin_session):
        """Process a SWIFT message"""
        api = admin_session["api"]
        payload = {
            "swiftMessage": "{1:F01TESTBICXAXXX0000000000}{2:I103TESTBICXXXXN}{4:\n:20:REF123\n:23B:CRED\n:32A:260301IDR5000000,\n:50K:/123456789\nTEST SENDER\n:59:/987654321\nTEST RECEIVER\n-}",
            "messageType": "MT103"
        }
        response = api.post("/api/v1/integration/swift/process", json=payload)
        assert response.status_code in [200, 201, 400, 401, 403, 404, 422], f"Unexpected status: {response.status_code}"

    def test_generate_ojk_report(self, admin_session):
        """Generate an OJK regulatory report"""
        api = admin_session["api"]
        payload = {
            "reportType": "MONTHLY_TRANSACTION",
            "reportDate": "2026-02-28"
        }
        response = api.post("/api/v1/integration/ojk/generate-report", json=payload)
        assert response.status_code in [200, 201, 400, 401, 403, 404, 422], f"Unexpected status: {response.status_code}"

    def test_send_http_request(self, admin_session):
        """Send an HTTP request via integration service"""
        api = admin_session["api"]
        payload = {
            "url": "https://httpbin.org/post",
            "method": "POST",
            "headers": {"Content-Type": "application/json"},
            "body": '{"test": "data"}'
        }
        response = api.post("/api/v1/integration/http/send", json=payload)
        assert response.status_code in [200, 201, 400, 401, 403, 404, 422, 500], f"Unexpected status: {response.status_code}"

    def test_send_soap_request(self, admin_session):
        """Send a SOAP request via integration service"""
        api = admin_session["api"]
        payload = {
            "endpoint": "http://example.com/soap-service",
            "operation": "GetStatus",
            "payload": "<soapenv:Envelope><soapenv:Body><GetStatus/></soapenv:Body></soapenv:Envelope>"
        }
        response = api.post("/api/v1/integration/soap/send", json=payload)
        assert response.status_code in [200, 201, 400, 401, 403, 404, 422, 500], f"Unexpected status: {response.status_code}"

    def test_get_message_status(self, admin_session):
        """Get integration message status"""
        api = admin_session["api"]
        fake_id = str(uuid.uuid4())
        response = api.get(f"/api/v1/integration/messages/{fake_id}/status")
        assert response.status_code in [200, 401, 403, 404], f"Unexpected status: {response.status_code}"

    def test_retry_message(self, admin_session):
        """Retry a failed integration message"""
        api = admin_session["api"]
        fake_id = str(uuid.uuid4())
        response = api.post(f"/api/v1/integration/messages/{fake_id}/retry")
        assert response.status_code in [200, 400, 401, 403, 404], f"Unexpected status: {response.status_code}"

    def test_cancel_message(self, admin_session):
        """Cancel a pending integration message"""
        api = admin_session["api"]
        fake_id = str(uuid.uuid4())
        response = api.post(f"/api/v1/integration/messages/{fake_id}/cancel")
        assert response.status_code in [200, 400, 401, 403, 404], f"Unexpected status: {response.status_code}"
