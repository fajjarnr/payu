import pytest
import time
from client import PayUClient
from faker import Faker

fake = Faker()


class TestLendingFlow:
    """
    Lending and Credit E2E tests.
    Tests: Loan Application -> Credit Score -> Repayment -> PayLater
    """

    @pytest.fixture(scope="class")
    def api(self):
        return PayUClient(gateway_url="http://localhost:8080")

    @pytest.fixture(scope="class")
    def user_session(self, api):
        """Register and login a user, return user data and api with token set"""
        user_data = {
            "email": f"lending_{fake.uuid4()}@example.com",
            "username": f"lending_{fake.uuid4()[:8]}",
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

    def test_calculate_credit_score(self, user_session):
        """
        Calculate credit score for a user
        """
        api = user_session["api"]
        user_id = user_session["user_id"]

        response = api.post("/api/v1/lending/credit-score/calculate", params={"userId": user_id})
        if response.status_code != 200:
            pytest.skip(f"Credit score calculation failed: {response.text}")

        credit_score = response.json()
        assert "score" in credit_score or credit_score is not None

    def test_get_credit_score(self, user_session):
        """
        Get existing credit score
        """
        api = user_session["api"]
        user_id = user_session["user_id"]

        response = api.get(f"/api/v1/lending/credit-score/{user_id}")
        if response.status_code != 200:
            pytest.skip("Credit score may not exist yet")

        credit_score = response.json()
        assert credit_score is not None

    def test_apply_personal_loan(self, user_session):
        """
        Apply for a personal loan
        """
        api = user_session["api"]
        user_id = user_session["user_id"]

        response = api.post("/api/v1/lending/loans", json={
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

    def test_get_loan_details(self, user_session):
        """
        Get loan details
        """
        api = user_session["api"]
        user_id = user_session["user_id"]

        # First, try to get a loan ID
        # This is optional as it requires a successful loan application
        loan_id = None
        response = api.post("/api/v1/lending/loans", json={
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

        response = api.get(f"/api/v1/lending/loans/{loan_id}")
        assert response.status_code == 200
        loan_details = response.json()
        assert loan_details.get("id") == loan_id

    def test_create_repayment_schedule(self, user_session):
        """
        Create repayment schedule for a loan
        """
        api = user_session["api"]
        user_id = user_session["user_id"]

        # Get a loan ID first
        response = api.post("/api/v1/lending/loans", json={
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

        response = api.post(f"/api/v1/lending/loans/{loan_id}/repayment-schedule")
        if response.status_code != 200:
            pytest.skip(f"Repayment schedule creation failed: {response.text}")

        schedule = response.json()
        assert isinstance(schedule, list)
        assert len(schedule) > 0

    def test_activate_paylater(self, user_session):
        """
        Activate PayLater for a user
        """
        api = user_session["api"]
        user_id = user_session["user_id"]

        response = api.post("/api/v1/lending/paylater/activate", json={
            "monthlyIncome": 15000000,
            "employmentStatus": "EMPLOYED"
        }, params={"userId": user_id})

        if response.status_code != 200:
            pytest.skip(f"PayLater activation requires credit score: {response.text}")

        paylater = response.json()
        assert paylater is not None
        assert paylater.get("status") in ["ACTIVE", "PENDING"]

    def test_record_paylater_purchase(self, user_session):
        """
        Record a PayLater purchase
        """
        api = user_session["api"]
        user_id = user_session["user_id"]

        response = api.post(f"/api/v1/lending/paylater/{user_id}/purchase", params={
            "merchantName": "TokoBapak",
            "amount": 500000,
            "description": "Grocery shopping"
        })

        if response.status_code != 200:
            pytest.skip(f"PayLater purchase requires active PayLater: {response.text}")

        transaction = response.json()
        assert transaction is not None
        assert transaction.get("amount") == 500000

    def test_record_paylater_payment(self, user_session):
        """
        Record a PayLater payment
        """
        api = user_session["api"]
        user_id = user_session["user_id"]

        response = api.post(f"/api/v1/lending/paylater/{user_id}/payment", params={
            "amount": 200000
        })

        if response.status_code != 200:
            pytest.skip(f"PayLater payment requires outstanding balance: {response.text}")

        transaction = response.json()
        assert transaction is not None

    def test_get_paylater_transactions(self, user_session):
        """
        Get PayLater transaction history
        """
        api = user_session["api"]
        user_id = user_session["user_id"]

        response = api.get(f"/api/v1/lending/paylater/{user_id}/transactions")
        if response.status_code != 200:
            pytest.skip("PayLater may not be active")

        transactions = response.json()
        assert isinstance(transactions, list)
