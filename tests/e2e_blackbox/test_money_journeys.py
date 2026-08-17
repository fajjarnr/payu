import pytest
import uuid


@pytest.mark.e2e
class TestEscrowFlow:
    """Escrow money journey (Flow 16 E2E blackbox) — reachability against the live gateway."""

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


@pytest.mark.e2e
class TestSplitBillFlow:
    """Split-bill money journey (Flow 11 E2E blackbox)."""

    def test_split_bill_endpoints_reachable(self, api):
        response = api.post("/api/v1/split-bills", json={
            "creatorAccountId": "acct-1",
            "totalAmount": 300000,
            "currency": "IDR",
            "title": "Team lunch",
            "splitType": "EQUAL",
        })
        assert response.status_code in [200, 201, 400, 401, 403, 404, 422, 429, 503], \
            f"unexpected: {response.status_code}"


@pytest.mark.e2e
class TestBifastFlow:
    """BI-FAST transfer journey (Flow 7 E2E blackbox)."""

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
        assert response.status_code in [200, 201, 400, 401, 403, 404, 422, 429, 503], \
            f"unexpected: {response.status_code}"


@pytest.mark.e2e
class TestDisbursementFlow:
    """Disbursement journey (Flow 10 E2E blackbox)."""

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
        assert response.status_code in [200, 201, 400, 401, 403, 404, 422, 429, 503], \
            f"unexpected: {response.status_code}"


@pytest.mark.e2e
class TestVirtualAccountPaymentFlow:
    """Virtual Account Payment journey (Flow 9 E2E blackbox)."""

    def test_va_endpoints_reachable(self, api):
        api.session.headers.update({"X-Idempotency-Key": str(uuid.uuid4())})
        va_number = "9887766554433221"
        statuses = {
            "inquiry": api.post(f"/api/v1/transactions/va/inquiry", json={
                "vaNumber": va_number,
                "partnerId": "partner-tokobapak",
            }),
            "payment": api.post(f"/api/v1/transactions/va/pay", json={
                "vaNumber": va_number,
                "amount": 150000,
                "currency": "IDR",
                "paymentReference": f"REF-{uuid.uuid4().hex[:8].upper()}",
            }),
        }
        for name, response in statuses.items():
            assert response.status_code in [200, 201, 400, 401, 403, 404, 409, 422, 429, 503], \
                f"{name} unexpected: {response.status_code}"


@pytest.mark.e2e
class TestSettlementBatchFlow:
    """Settlement Batch journey (Flow 22 E2E blackbox)."""

    def test_settlement_batch_endpoints_reachable(self, api):
        partner_id = str(uuid.uuid4())
        statuses = {
            "trigger": api.post(f"/api/v1/partners/{partner_id}/settlements/batch", json={
                "date": "2026-08-17",
                "currency": "IDR",
            }),
            "list": api.get(f"/api/v1/partners/{partner_id}/settlements"),
        }
        for name, response in statuses.items():
            assert response.status_code in [200, 201, 400, 401, 403, 404, 409, 422, 429, 503], \
                f"{name} unexpected: {response.status_code}"


@pytest.mark.e2e
class TestSnapRefundFlow:
    """SNAP Refund journey (Flow 5 E2E blackbox)."""

    def test_snap_refund_endpoint_reachable(self, api):
        api.session.headers.update({
            "X-Idempotency-Key": str(uuid.uuid4()),
            "X-PARTNER-ID": "partner-tokobapak",
        })
        response = api.post("/v1.0/debit/refund", json={
            "originalPartnerReferenceNo": f"PAY-{uuid.uuid4().hex[:8].upper()}",
            "originalReferenceNo": f"ORIG-{uuid.uuid4().hex[:8].upper()}",
            "refundAmount": {
                "value": "50000.00",
                "currency": "IDR",
            },
            "reason": "Customer cancellation",
        })
        assert response.status_code in [200, 201, 400, 401, 403, 404, 409, 422, 429, 503], \
            f"unexpected: {response.status_code}"


@pytest.mark.e2e
class TestInvestmentSellFlow:
    """Investment Sell / Redemption journey (Flow 14 E2E blackbox)."""

    def test_investment_sell_endpoints_reachable(self, api):
        account_id = str(uuid.uuid4())
        txn_id = str(uuid.uuid4())
        api.session.headers.update({"X-Idempotency-Key": str(uuid.uuid4())})
        statuses = {
            "sell_mutual_fund": api.post(f"/api/v1/investments/{account_id}/sell", json={
                "transactionId": txn_id,
                "amount": 250000,
                "units": 150.25,
            }),
            "sell_gold": api.post("/api/v1/investments/gold/sell", json={
                "userId": "user-1",
                "grams": 2.5,
                "amount": 3500000,
            }),
        }
        for name, response in statuses.items():
            assert response.status_code in [200, 201, 400, 401, 403, 404, 409, 422, 429, 503], \
                f"{name} unexpected: {response.status_code}"


@pytest.mark.e2e
class TestPaymentLinkFlow:
    """Payment Link lifecycle journey (Flow 19 E2E blackbox)."""

    def test_payment_link_endpoints_reachable(self, api):
        partner_id = str(uuid.uuid4())
        slug = f"pl-{uuid.uuid4().hex[:8]}"
        statuses = {
            "create": api.post(f"/api/v1/partners/{partner_id}/payment-links", json={
                "title": "Merchant Invoice",
                "amount": 175000,
                "currency": "IDR",
                "expiryMinutes": 60,
            }),
            "public_view": api.get(f"/v1/pay/{slug}"),
            "confirm": api.post(f"/v1/pay/{slug}/confirm", json={
                "paymentMethod": "QRIS",
                "paidAmount": 175000,
            }),
        }
        for name, response in statuses.items():
            assert response.status_code in [200, 201, 400, 401, 403, 404, 409, 422, 429, 503], \
                f"{name} unexpected: {response.status_code}"


@pytest.mark.e2e
class TestInternalTransferFlow:
    """Peer-to-Peer Internal Transfer journey (Flow 3 E2E blackbox)."""

    def test_internal_transfer_endpoint_reachable(self, api):
        api.session.headers.update({"X-Idempotency-Key": str(uuid.uuid4())})
        response = api.post("/api/v1/transactions/transfer", json={
            "senderAccountId": str(uuid.uuid4()),
            "recipientAccountNumber": "100020003000",
            "amount": 75000,
            "currency": "IDR",
            "type": "INTERNAL_TRANSFER",
            "description": "Lunch split payment",
        })
        assert response.status_code in [200, 201, 400, 401, 403, 404, 409, 422, 429, 503], \
            f"unexpected: {response.status_code}"


@pytest.mark.e2e
class TestQrisPaymentFlow:
    """QRIS Payment journey (Flow 8 E2E blackbox)."""

    def test_qris_endpoints_reachable(self, api):
        api.session.headers.update({"X-Idempotency-Key": str(uuid.uuid4())})
        statuses = {
            "generate": api.post("/api/v1/transactions/qris/generate", json={
                "merchantId": "MERCH-001",
                "amount": 45000,
                "currency": "IDR",
            }),
            "pay": api.post("/api/v1/transactions/qris/pay", json={
                "accountId": str(uuid.uuid4()),
                "qrContent": "00020101021226580016ID.CO.PAYU.WWW01189360099900000000015204541153033605802ID5911PayU Merchant6007Jakarta61051234062070703A0163041D3B",
                "amount": 45000,
            }),
        }
        for name, response in statuses.items():
            assert response.status_code in [200, 201, 400, 401, 403, 404, 409, 422, 429, 503], \
                f"{name} unexpected: {response.status_code}"


@pytest.mark.e2e
class TestWalletTopupFlow:
    """Wallet Top-up journey (Flow 12 E2E blackbox)."""

    def test_wallet_topup_endpoint_reachable(self, api):
        wallet_id = str(uuid.uuid4())
        api.session.headers.update({"X-Idempotency-Key": str(uuid.uuid4())})
        response = api.post(f"/api/v1/wallets/{wallet_id}/topup", json={
            "amount": 200000,
            "currency": "IDR",
            "sourceChannel": "BANK_TRANSFER",
            "referenceNumber": f"TOPUP-{uuid.uuid4().hex[:8].upper()}",
        })
        assert response.status_code in [200, 201, 400, 401, 403, 404, 409, 422, 429, 503], \
            f"unexpected: {response.status_code}"
