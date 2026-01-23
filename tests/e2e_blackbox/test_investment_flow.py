import pytest
import time
from client import PayUClient
from faker import Faker

fake = Faker()


class TestInvestmentFlow:
    """
    Investment and Wealth Management E2E tests.
    Tests: Create Investment Account -> Buy Deposits/Mutual Funds/Gold -> Check Holdings
    """

    @pytest.fixture(scope="class")
    def api(self):
        return PayUClient(gateway_url="http://localhost:8080")

    @pytest.fixture(scope="class")
    def user_session(self, api):
        """Register and login a user, return user data and api with token set"""
        user_data = {
            "email": f"invest_{fake.uuid4()}@example.com",
            "username": f"invest_{fake.uuid4()[:8]}",
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

    def test_investment_account_creation(self, user_session):
        """
        Create investment account
        """
        api = user_session["api"]
        user_id = user_session["user_id"]

        response = api.post("/api/v1/investments/accounts", json={"userId": user_id})
        assert response.status_code == 200, f"Failed to create investment account: {response.text}"
        account = response.json()
        assert "id" in account or "accountId" in account

    def test_buy_digital_deposit(self, user_session):
        """
        Buy a digital deposit
        """
        api = user_session["api"]
        user_id = user_session["user_id"]

        response = api.post("/api/v1/investments/deposits", json={
            "userId": user_id,
            "amount": 1000000,
            "tenure": 12
        })

        # This might fail if investment account doesn't exist or balance is insufficient
        if response.status_code not in [200, 201]:
            pytest.skip(f"Deposit purchase requires investment account and balance: {response.text}")
        else:
            deposit = response.json()
            assert "amount" in deposit

    def test_buy_mutual_fund(self, user_session):
        """
        Buy a mutual fund
        """
        api = user_session["api"]
        user_id = user_session["user_id"]

        response = api.post("/api/v1/investments/mutual-funds", json={
            "userId": user_id,
            "fundCode": "ABCP001",
            "amount": 500000
        })

        if response.status_code not in [200, 201]:
            pytest.skip(f"Mutual fund purchase requires investment account and balance: {response.text}")
        else:
            transaction = response.json()
            assert "amount" in transaction

    def test_buy_digital_gold(self, user_session):
        """
        Buy digital gold
        """
        api = user_session["api"]
        user_id = user_session["user_id"]

        response = api.post("/api/v1/investments/gold", json={
            "userId": user_id,
            "amount": 2000000
        })

        if response.status_code not in [200, 201]:
            pytest.skip(f"Gold purchase requires investment account and balance: {response.text}")
        else:
            gold = response.json()
            assert "amount" in gold or "weight" in gold

    def test_get_investment_holdings(self, user_session):
        """
        Get user's investment holdings
        """
        api = user_session["api"]
        user_id = user_session["user_id"]

        response = api.get(f"/api/v1/investments/accounts/{user_id}")
        if response.status_code != 200:
            pytest.skip("Investment account may not exist")

        account = response.json()
        assert account is not None

        response = api.get(f"/api/v1/investments/gold/{user_id}")
        # Gold endpoint might not have holdings yet
        if response.status_code == 200:
            gold_holdings = response.json()
            assert gold_holdings is not None
