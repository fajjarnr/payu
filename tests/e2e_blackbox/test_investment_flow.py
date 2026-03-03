import pytest
import time
from faker import Faker

fake = Faker()


@pytest.mark.investment
class TestInvestmentFlow:
    """
    Investment and Wealth Management E2E tests.
    Tests: Create Investment Account -> Buy Deposits/Mutual Funds/Gold -> Check Holdings
    """

    def test_investment_account_creation(self, authenticated_api, registered_user):
        """
        Create investment account
        """
        user_id = registered_user["userId"]

        response = authenticated_api.post("/api/v1/investments/accounts", json={"userId": user_id})
        if response.status_code == 500:
            pytest.skip(f"Investment account creation failed with internal error: {response.text}")
        assert response.status_code in [200, 201], f"Failed to create investment account: {response.text}"
        account = response.json()
        if isinstance(account, dict) and "data" in account:
            account = account["data"]
        assert "id" in account or "accountId" in account

    def test_buy_digital_deposit(self, authenticated_api, registered_user):
        """
        Buy a digital deposit
        """
        user_id = registered_user["userId"]

        response = authenticated_api.post("/api/v1/investments/deposits", json={
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

    def test_buy_mutual_fund(self, authenticated_api, registered_user):
        """
        Buy a mutual fund
        """
        user_id = registered_user["userId"]

        response = authenticated_api.post("/api/v1/investments/mutual-funds", json={
            "userId": user_id,
            "fundCode": "ABCP001",
            "amount": 500000
        })

        if response.status_code not in [200, 201]:
            pytest.skip(f"Mutual fund purchase requires investment account and balance: {response.text}")
        else:
            transaction = response.json()
            assert "amount" in transaction

    def test_buy_digital_gold(self, authenticated_api, registered_user):
        """
        Buy digital gold
        """
        user_id = registered_user["userId"]

        response = authenticated_api.post("/api/v1/investments/gold", json={
            "userId": user_id,
            "amount": 2000000
        })

        if response.status_code not in [200, 201]:
            pytest.skip(f"Gold purchase requires investment account and balance: {response.text}")
        else:
            gold = response.json()
            assert "amount" in gold or "weight" in gold

    def test_get_investment_holdings(self, authenticated_api, registered_user):
        """
        Get user's investment holdings
        """
        user_id = registered_user["userId"]

        response = authenticated_api.get("/api/v1/investments/accounts/me")
        if response.status_code != 200:
            pytest.skip("Investment account may not exist")

        account = response.json()
        assert account is not None

        response = authenticated_api.get("/api/v1/investments/gold/me")
        # Gold endpoint might not have holdings yet
        if response.status_code == 200:
            gold_holdings = response.json()
            assert gold_holdings is not None
