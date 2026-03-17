import pytest
from faker import Faker

fake = Faker()


@pytest.mark.backoffice
def test_backoffice_flow(api):
    """
    Backoffice service is behind an IP whitelist filter in the gateway.
    Requests from non-whitelisted IPs (including test runners) receive 403 IP_NOT_ALLOWED.
    This test verifies the IP whitelist is enforced correctly.
    """
    kyc_data = {
        "userId": f"user_{fake.uuid4()}",
        "fullName": fake.name(),
        "documentType": "KTP",
        "documentNumber": str(fake.random_number(digits=16)),
        "documentUrl": "http://example.com/doc.jpg",
        "address": fake.address(),
        "phoneNumber": fake.phone_number()
    }

    resp = api.post("/api/v1/backoffice/kyc-reviews", json=kyc_data)

    # Backoffice endpoints are protected by IP whitelist.
    # Non-whitelisted IPs get 403 with IP_NOT_ALLOWED error.
    assert resp.status_code == 403, (
        f"Expected 403 (IP whitelist), got {resp.status_code}: {resp.text}"
    )
    body = resp.json()
    assert body["error"] == "IP_NOT_ALLOWED", (
        f"Expected IP_NOT_ALLOWED error, got: {body}"
    )
    # Use resilient pattern matching for the message — any rewording of the
    # IP-whitelist message should still match
    message = body.get("message", "").lower()
    assert any(keyword in message for keyword in ["ip", "whitelist", "not authorized", "not allowed", "blocked"]), (
        f"Expected IP whitelist-related message, got: {body['message']}"
    )
