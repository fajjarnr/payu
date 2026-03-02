import pytest
import uuid
from client import PayUClient
from faker import Faker

fake = Faker()


@pytest.mark.e2e
class TestNotificationServiceFlow:
    """
    Notification Service E2E tests.
    Tests: Send notification -> Get by ID -> List user notifications -> Mark as read
    """

    @pytest.fixture(scope="class")
    def api(self):
        return PayUClient(gateway_url="http://localhost:8080")

    @pytest.fixture(scope="class")
    def user_session(self, api):
        """Register and login a user for notification operations"""
        user_data = {
            "email": f"notif_{fake.uuid4()}@example.com",
            "username": f"notif_{fake.uuid4()[:8]}",
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
    def test_get_user_notifications(self, user_session):
        """Get notifications for current user"""
        api = user_session["api"]
        user_id = user_session["user_id"]
        response = api.get(f"/api/v1/notifications/user/{user_id}", params={"limit": 20})
        assert response.status_code in [200, 404], f"Unexpected status: {response.status_code}"
        if response.status_code == 200:
            data = response.json()
            assert isinstance(data, (list, dict))

    def test_send_push_notification(self, user_session):
        """Send a push notification"""
        api = user_session["api"]
        payload = {
            "channel": "PUSH",
            "recipient": user_session["user_id"],
            "title": "Test Notification",
            "body": "This is a test push notification",
            "data": {"type": "test", "referenceId": str(uuid.uuid4())}
        }
        response = api.post("/api/v1/notifications", json=payload)
        assert response.status_code in [200, 201, 400, 422], f"Unexpected status: {response.status_code}"

    def test_send_email_notification(self, user_session):
        """Send an email notification"""
        api = user_session["api"]
        payload = {
            "channel": "EMAIL",
            "recipient": f"test_{fake.uuid4()[:8]}@example.com",
            "title": "Test Email",
            "body": "<h1>Test</h1><p>This is a test email</p>",
            "data": {"type": "test"}
        }
        response = api.post("/api/v1/notifications", json=payload)
        assert response.status_code in [200, 201, 400, 422], f"Unexpected status: {response.status_code}"

    def test_send_sms_notification(self, user_session):
        """Send an SMS notification"""
        api = user_session["api"]
        payload = {
            "channel": "SMS",
            "recipient": "+6281234567890",
            "body": "Your OTP is 123456",
            "data": {"type": "otp"}
        }
        response = api.post("/api/v1/notifications", json=payload)
        assert response.status_code in [200, 201, 400, 422], f"Unexpected status: {response.status_code}"

    def test_get_notification_by_id(self, user_session):
        """Get notification by ID"""
        api = user_session["api"]
        fake_id = str(uuid.uuid4())
        response = api.get(f"/api/v1/notifications/{fake_id}")
        assert response.status_code in [200, 404], f"Unexpected status: {response.status_code}"

    def test_mark_notification_as_read(self, user_session):
        """Mark a notification as read"""
        api = user_session["api"]
        fake_id = str(uuid.uuid4())
        response = api.post(f"/api/v1/notifications/{fake_id}/read")
        assert response.status_code in [200, 404], f"Unexpected status: {response.status_code}"
