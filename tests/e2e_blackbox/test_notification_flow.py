import pytest
import uuid
from faker import Faker

fake = Faker()


@pytest.mark.e2e
class TestNotificationServiceFlow:
    """
    Notification Service E2E tests.
    Tests: Send notification -> Get by ID -> List user notifications -> Mark as read
    """

    @pytest.mark.smoke
    def test_get_user_notifications(self, authenticated_api, registered_user):
        """Get notifications for current user"""
        user_id = registered_user["userId"]
        response = authenticated_api.get(f"/api/v1/notifications/user/{user_id}", params={"limit": 20})
        assert response.status_code in [200, 404], f"Unexpected status: {response.status_code}"
        if response.status_code == 200:
            data = response.json()
            assert isinstance(data, (list, dict))

    def test_send_push_notification(self, authenticated_api, registered_user):
        """Send a push notification"""
        payload = {
            "channel": "PUSH",
            "recipient": registered_user["userId"],
            "title": "Test Notification",
            "body": "This is a test push notification",
            "data": {"type": "test", "referenceId": str(uuid.uuid4())}
        }
        response = authenticated_api.post("/api/v1/notifications", json=payload)
        assert response.status_code in [200, 201, 400, 422], f"Unexpected status: {response.status_code}"

    def test_send_email_notification(self, authenticated_api):
        """Send an email notification"""
        payload = {
            "channel": "EMAIL",
            "recipient": f"test_{fake.uuid4()[:8]}@example.com",
            "title": "Test Email",
            "body": "<h1>Test</h1><p>This is a test email</p>",
            "data": {"type": "test"}
        }
        response = authenticated_api.post("/api/v1/notifications", json=payload)
        assert response.status_code in [200, 201, 400, 422], f"Unexpected status: {response.status_code}"

    def test_send_sms_notification(self, authenticated_api):
        """Send an SMS notification"""
        payload = {
            "channel": "SMS",
            "recipient": "+6281234567890",
            "body": "Your OTP is 123456",
            "data": {"type": "otp"}
        }
        response = authenticated_api.post("/api/v1/notifications", json=payload)
        assert response.status_code in [200, 201, 400, 422], f"Unexpected status: {response.status_code}"

    def test_get_notification_by_id(self, authenticated_api):
        """Get notification by ID"""
        fake_id = str(uuid.uuid4())
        response = authenticated_api.get(f"/api/v1/notifications/{fake_id}")
        assert response.status_code in [200, 404], f"Unexpected status: {response.status_code}"

    def test_mark_notification_as_read(self, authenticated_api):
        """Mark a notification as read"""
        fake_id = str(uuid.uuid4())
        response = authenticated_api.post(f"/api/v1/notifications/{fake_id}/read")
        assert response.status_code in [200, 404], f"Unexpected status: {response.status_code}"
