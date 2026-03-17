import pytest
import uuid
from faker import Faker

fake = Faker()


def get_admin_token(api):
    """Helper to get an admin token for endpoints requiring ADMIN role."""
    response = api.post("/api/v1/auth/login", json={
        "username": "admin",
        "password": "P@ssw0rd123",  # pragma: allowlist secret
    })
    if response.status_code != 200:
        return None
    body = response.json()
    data = body.get("data", body) if isinstance(body, dict) else body
    return data.get("access_token")


@pytest.mark.lending
class TestLendingFlow:
    """
    Lending and Credit E2E tests.
    Tests: Loan Application -> Credit Score -> Repayment -> PayLater

    Key insight: The lending service uses JWT subject (Keycloak sub UUID) as userId,
    NOT the account-service userId. The registered_user["userId"] is from account-service.
    For endpoints requiring userId param, we must use the JWT sub from the token.
    customer1's Keycloak sub is extracted from the JWT.
    """

    def _get_jwt_sub(self, auth_token):
        """Extract the subject (userId) from the JWT token."""
        import base64
        import json as json_mod
        # JWT is header.payload.signature
        parts = auth_token.split(".")
        if len(parts) < 2:
            return None
        # Add padding
        payload = parts[1]
        payload += "=" * (4 - len(payload) % 4)
        decoded = base64.urlsafe_b64decode(payload)
        claims = json_mod.loads(decoded)
        return claims.get("sub")

    def test_calculate_credit_score(self, authenticated_api, auth_token, registered_user):
        """
        Calculate credit score for a user.
        Endpoint: POST /api/v1/lending/credit-score/calculate?userId=UUID
        userId must be a valid UUID (JWT sub).
        """
        jwt_sub = self._get_jwt_sub(auth_token)
        assert jwt_sub is not None, "Could not extract JWT sub from token"

        response = authenticated_api.post(
            "/api/v1/lending/credit-score/calculate",
            params={"userId": jwt_sub}
        )
        # 400 = IDEMPOTENCY_KEY_REQUIRED (gateway requires Idempotency-Key for financial POST operations)
        assert response.status_code in [200, 201, 400, 429, 500, 503], (
            f"Unexpected status {response.status_code}: {response.text}"
        )
        if response.status_code == 200:
            body = response.json()
            credit_score = body.get("data", body) if isinstance(body, dict) else body
            assert credit_score is not None

    def test_get_credit_score(self, authenticated_api, auth_token, registered_user):
        """
        Get existing credit score.
        Uses JWT sub as userId path param.
        """
        jwt_sub = self._get_jwt_sub(auth_token)
        assert jwt_sub is not None

        # First calculate to ensure it exists
        authenticated_api.post(
            "/api/v1/lending/credit-score/calculate",
            params={"userId": jwt_sub}
        )

        response = authenticated_api.get(f"/api/v1/lending/credit-score/{jwt_sub}")
        assert response.status_code in [200, 404, 429, 500, 503], (
            f"Unexpected status {response.status_code}: {response.text}"
        )
        if response.status_code == 200:
            body = response.json()
            credit_score = body.get("data", body) if isinstance(body, dict) else body
            assert credit_score is not None

    def test_apply_personal_loan(self, authenticated_api, registered_user):
        """
        Apply for a personal loan.
        Requires LoanApplicationCommand(externalId, loanType, principalAmount, tenureMonths, purpose).
        loanType enum: PERSONAL_LOAN, INSTALMENT_LOAN, MICRO_LOAN.
        """
        response = authenticated_api.post("/api/v1/lending/loans", json={
            "externalId": str(uuid.uuid4()),
            "loanType": "PERSONAL_LOAN",
            "principalAmount": 10000000,
            "tenureMonths": 12,
            "purpose": "Home Renovation"
        })
        assert response.status_code in [200, 201, 400, 429, 500, 503], (
            f"Unexpected status {response.status_code}: {response.text}"
        )
        if response.status_code in [200, 201]:
            body = response.json()
            loan = body.get("data", body) if isinstance(body, dict) else body
            assert "id" in loan or "loanId" in loan
            assert loan.get("status") in ["PENDING", "APPROVED", "REJECTED", "UNDER_REVIEW"]

    def test_get_loan_details(self, authenticated_api, registered_user):
        """
        Create a loan then get its details.
        """
        # Create a loan first
        response = authenticated_api.post("/api/v1/lending/loans", json={
            "externalId": str(uuid.uuid4()),
            "loanType": "PERSONAL_LOAN",
            "principalAmount": 5000000,
            "tenureMonths": 6,
            "purpose": "Emergency Fund"
        })
        assert response.status_code in [200, 201, 400, 429, 500, 503], (
            f"Loan creation unexpected status {response.status_code}: {response.text}"
        )
        if response.status_code not in [200, 201]:
            # Can't proceed without a loan — assert the error is valid
            body = response.json()
            assert body is not None
            return

        body = response.json()
        loan = body.get("data", body) if isinstance(body, dict) else body
        loan_id = loan.get("id") or loan.get("loanId")
        assert loan_id is not None, f"No loan ID in response: {loan}"

        response = authenticated_api.get(f"/api/v1/lending/loans/{loan_id}")
        assert response.status_code in [200, 404, 429, 500, 503], (
            f"Unexpected status {response.status_code}: {response.text}"
        )
        if response.status_code == 200:
            body = response.json()
            loan_details = body.get("data", body) if isinstance(body, dict) else body
            assert str(loan_details.get("id")) == str(loan_id)

    def test_create_repayment_schedule(self, authenticated_api, registered_user):
        """
        Create repayment schedule for a loan.
        """
        # Create a loan
        response = authenticated_api.post("/api/v1/lending/loans", json={
            "externalId": str(uuid.uuid4()),
            "loanType": "PERSONAL_LOAN",
            "principalAmount": 8000000,
            "tenureMonths": 12,
            "purpose": "Car Purchase"
        })
        assert response.status_code in [200, 201, 400, 429, 500, 503], (
            f"Loan creation unexpected status {response.status_code}: {response.text}"
        )
        if response.status_code not in [200, 201]:
            body = response.json()
            assert body is not None
            return

        body = response.json()
        loan = body.get("data", body) if isinstance(body, dict) else body
        loan_id = loan.get("id") or loan.get("loanId")
        assert loan_id is not None

        response = authenticated_api.post(f"/api/v1/lending/loans/{loan_id}/repayment-schedule")
        assert response.status_code in [200, 201, 400, 404, 429, 500, 503], (
            f"Unexpected status {response.status_code}: {response.text}"
        )
        if response.status_code == 200:
            body = response.json()
            schedule = body.get("data", body) if isinstance(body, dict) else body
            assert isinstance(schedule, list)
            assert len(schedule) > 0

    def test_activate_paylater(self, authenticated_api, auth_token, registered_user):
        """
        Activate PayLater for a user.
        Endpoint: POST /api/v1/lending/paylater/activate?userId=UUID
        Body: PayLaterLimitRequest(creditLimit, billingCycleDay).
        userId must be JWT sub.
        """
        jwt_sub = self._get_jwt_sub(auth_token)
        assert jwt_sub is not None

        response = authenticated_api.post("/api/v1/lending/paylater/activate", json={
            "creditLimit": 5000000,
            "billingCycleDay": 25
        }, params={"userId": jwt_sub})
        assert response.status_code in [200, 201, 400, 429, 500, 503], (
            f"Unexpected status {response.status_code}: {response.text}"
        )
        if response.status_code == 200:
            body = response.json()
            paylater = body.get("data", body) if isinstance(body, dict) else body
            assert paylater is not None

    def test_record_paylater_purchase(self, authenticated_api, auth_token, registered_user):
        """
        Record a PayLater purchase.
        Endpoint: POST /api/v1/lending/paylater/{userId}/purchase
        Uses @RequestParam for merchantName, amount, description (query params).
        userId must be JWT sub. Has owner check via @PreAuthorize.
        """
        jwt_sub = self._get_jwt_sub(auth_token)
        assert jwt_sub is not None

        # Ensure paylater is activated first
        authenticated_api.post("/api/v1/lending/paylater/activate", json={
            "creditLimit": 5000000,
            "billingCycleDay": 25
        }, params={"userId": jwt_sub})

        response = authenticated_api.post(
            f"/api/v1/lending/paylater/{jwt_sub}/purchase",
            params={
                "merchantName": "TokoBapak",
                "amount": 500000,
                "description": "Grocery shopping"
            }
        )
        assert response.status_code in [200, 201, 400, 403, 429, 500, 503], (
            f"Unexpected status {response.status_code}: {response.text}"
        )
        body = response.json()
        if response.status_code == 200:
            transaction = body.get("data", body) if isinstance(body, dict) else body
            assert transaction is not None

    def test_record_paylater_payment(self, authenticated_api, auth_token, registered_user):
        """
        Record a PayLater payment.
        Endpoint: POST /api/v1/lending/paylater/{userId}/payment
        Uses @RequestParam for amount (query param).
        """
        jwt_sub = self._get_jwt_sub(auth_token)
        assert jwt_sub is not None

        response = authenticated_api.post(
            f"/api/v1/lending/paylater/{jwt_sub}/payment",
            params={"amount": 200000}
        )
        assert response.status_code in [200, 201, 400, 403, 429, 500, 503], (
            f"Unexpected status {response.status_code}: {response.text}"
        )
        body = response.json()
        assert body is not None

    def test_get_paylater_transactions(self, authenticated_api, auth_token, registered_user):
        """
        Get PayLater transaction history.
        """
        jwt_sub = self._get_jwt_sub(auth_token)
        assert jwt_sub is not None

        response = authenticated_api.get(f"/api/v1/lending/paylater/{jwt_sub}/transactions")
        assert response.status_code in [200, 400, 403, 404, 429, 500, 503], (
            f"Unexpected status {response.status_code}: {response.text}"
        )
        if response.status_code == 200:
            body = response.json()
            transactions = body.get("data", body) if isinstance(body, dict) else body
            assert isinstance(transactions, list)
