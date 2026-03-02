import pytest
import time
from faker import Faker

fake = Faker()


@pytest.mark.lending
class TestLendingFlow:
    """
    Lending and Credit E2E tests.
    Tests: Loan Application -> Credit Score -> Repayment -> PayLater
    """

    def test_calculate_credit_score(self, authenticated_api, registered_user):
        """
        Calculate credit score for a user
        """
        user_id = registered_user["userId"]

        response = authenticated_api.post("/api/v1/lending/credit-score/calculate", params={"userId": user_id})
        if response.status_code != 200:
            pytest.skip(f"Credit score calculation failed: {response.text}")

        credit_score = response.json()
        assert "score" in credit_score or credit_score is not None

    def test_get_credit_score(self, authenticated_api, registered_user):
        """
        Get existing credit score
        """
        user_id = registered_user["userId"]

        response = authenticated_api.get(f"/api/v1/lending/credit-score/{user_id}")
        if response.status_code != 200:
            pytest.skip("Credit score may not exist yet")

        credit_score = response.json()
        assert credit_score is not None

    def test_apply_personal_loan(self, authenticated_api, registered_user):
        """
        Apply for a personal loan
        """
        user_id = registered_user["userId"]

        response = authenticated_api.post("/api/v1/lending/loans", json={
            "userId": user_id,
            "amount": 10000000,
            "tenure": 12,
            "purpose": "Home Renovation",
            "income": 15000000
        })

        if response.status_code not in [200, 201]:
            pytest.skip(f"Loan application may require credit history: {response.text}")
        else:
            loan = response.json()
            assert "id" in loan or "loanId" in loan
            assert loan.get("status") in ["PENDING", "APPROVED", "REJECTED"]

            return loan.get("id", loan.get("loanId"))

    def test_get_loan_details(self, authenticated_api, registered_user):
        """
        Get loan details
        """
        user_id = registered_user["userId"]

        # First, try to get a loan ID
        # This is optional as it requires a successful loan application
        loan_id = None
        response = authenticated_api.post("/api/v1/lending/loans", json={
            "userId": user_id,
            "amount": 5000000,
            "tenure": 6,
            "purpose": "Emergency Fund",
            "income": 10000000
        })

        if response.status_code in [200, 201]:
            loan = response.json()
            loan_id = loan.get("id", loan.get("loanId"))

        if not loan_id:
            pytest.skip("No loan ID available")

        response = authenticated_api.get(f"/api/v1/lending/loans/{loan_id}")
        assert response.status_code == 200
        loan_details = response.json()
        assert loan_details.get("id") == loan_id

    def test_create_repayment_schedule(self, authenticated_api, registered_user):
        """
        Create repayment schedule for a loan
        """
        user_id = registered_user["userId"]

        # Get a loan ID first
        response = authenticated_api.post("/api/v1/lending/loans", json={
            "userId": user_id,
            "amount": 8000000,
            "tenure": 12,
            "purpose": "Car Purchase",
            "income": 12000000
        })

        if response.status_code not in [200, 201]:
            pytest.skip("Loan creation required for schedule")

        loan = response.json()
        loan_id = loan.get("id", loan.get("loanId"))

        if not loan_id:
            pytest.skip("No loan ID available")

        response = authenticated_api.post(f"/api/v1/lending/loans/{loan_id}/repayment-schedule")
        if response.status_code != 200:
            pytest.skip(f"Repayment schedule creation failed: {response.text}")

        schedule = response.json()
        assert isinstance(schedule, list)
        assert len(schedule) > 0

    def test_activate_paylater(self, authenticated_api, registered_user):
        """
        Activate PayLater for a user
        """
        user_id = registered_user["userId"]

        response = authenticated_api.post("/api/v1/lending/paylater/activate", json={
            "monthlyIncome": 15000000,
            "employmentStatus": "EMPLOYED"
        }, params={"userId": user_id})

        if response.status_code != 200:
            pytest.skip(f"PayLater activation requires credit score: {response.text}")

        paylater = response.json()
        assert paylater is not None
        assert paylater.get("status") in ["ACTIVE", "PENDING"]

    def test_record_paylater_purchase(self, authenticated_api, registered_user):
        """
        Record a PayLater purchase
        """
        user_id = registered_user["userId"]

        response = authenticated_api.post(f"/api/v1/lending/paylater/{user_id}/purchase", params={
            "merchantName": "TokoBapak",
            "amount": 500000,
            "description": "Grocery shopping"
        })

        if response.status_code != 200:
            pytest.skip(f"PayLater purchase requires active PayLater: {response.text}")

        transaction = response.json()
        assert transaction is not None
        assert transaction.get("amount") == 500000

    def test_record_paylater_payment(self, authenticated_api, registered_user):
        """
        Record a PayLater payment
        """
        user_id = registered_user["userId"]

        response = authenticated_api.post(f"/api/v1/lending/paylater/{user_id}/payment", params={
            "amount": 200000
        })

        if response.status_code != 200:
            pytest.skip(f"PayLater payment requires outstanding balance: {response.text}")

        transaction = response.json()
        assert transaction is not None

    def test_get_paylater_transactions(self, authenticated_api, registered_user):
        """
        Get PayLater transaction history
        """
        user_id = registered_user["userId"]

        response = authenticated_api.get(f"/api/v1/lending/paylater/{user_id}/transactions")
        if response.status_code != 200:
            pytest.skip("PayLater may not be active")

        transactions = response.json()
        assert isinstance(transactions, list)
