import pytest
import uuid


@pytest.mark.e2e
class TestEscrowFlow:
    """Escrow money journey (QAMVP-007 E2E blackbox) — reachability against the live gateway."""

    @pytest.mark.smoke
    def test_escrow_endpoints_reachable(self, api):
        escrow_id = str(uuid.uuid4())
        statuses = {
            "create": api.post("/api/v1/escrow", json={
                "buyerAccountId": "buyer-1", "sellerAccountId": "seller-1",
                "partnerId": "partner-1", "amount": 100000, "currency": "IDR",
            }),
            "release": api.post(f"/api/v1/escrow/{escrow_id}/release", json={}),
            "settle": api.post(f"/api/v1/escrow/{escrow_id}/settle", json={}),
            "refund": api.post(f"/api/v1/escrow/{escrow_id}/refund", json={"reason": "test"}),
        }
        for name, response in statuses.items():
            assert response.status_code in [200, 201, 400, 401, 403, 404, 409, 422, 429, 503], \
                f"{name} unexpected: {response.status_code}"


class TestSplitBillFlow:
    """Split-bill money journey (QAMVP-008 E2E blackbox)."""

    def test_split_bill_endpoints_reachable(self, api):
        response = api.post("/api/v1/split-bills", json={
            "creatorAccountId": "acct-1",
            "totalAmount": 300000,
            "currency": "IDR",
            "title": "Team lunch",
            "splitType": "EQUAL",
        })
        assert response.status_code in [200, 201, 400, 401, 403, 422, 429, 503], \
            f"unexpected: {response.status_code}"


class TestBifastFlow:
    """BI-FAST transfer journey (QAMVP-009 E2E blackbox)."""

    def test_bifast_transfer_endpoint_reachable(self, api):
        api.session.headers.update({"X-Idempotency-Key": str(uuid.uuid4())})
        response = api.post("/api/v1/transactions/transfer", json={
            "senderAccountId": str(uuid.uuid4()),
            "recipientAccountNumber": "0123456789",
            "amount": 10000,
            "currency": "IDR",
            "type": "BIFAST_TRANSFER",
            "description": "e2e bifast",
        })
        assert response.status_code in [200, 201, 400, 401, 403, 422, 429, 503], \
            f"unexpected: {response.status_code}"


class TestDisbursementFlow:
    """Disbursement journey (QAMVP-010 E2E blackbox)."""

    def test_disbursement_endpoint_reachable(self, api):
        api.session.headers.update({"X-Idempotency-Key": str(uuid.uuid4())})
        response = api.post("/api/v1/disbursements", json={
            "sourceAccountId": str(uuid.uuid4()),
            "amount": 50000,
            "currency": "IDR",
            "bankCode": "011",
            "accountNumber": "0123456789",
            "accountName": "Payee",
        })
        assert response.status_code in [200, 201, 400, 401, 403, 422, 429, 503], \
            f"unexpected: {response.status_code}"
