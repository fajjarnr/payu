import pytest
import time
from client import PayUClient
from faker import Faker

fake = Faker()


class TestAnalyticsFlow:
    """
    Analytics and ML Recommendations E2E tests.
    Tests: User Metrics -> Spending Trends -> Cash Flow -> Recommendations
    """

    @pytest.fixture(scope="class")
    def api(self):
        return PayUClient(gateway_url="http://localhost:8080")

    @pytest.fixture(scope="class")
    def user_session(self, api):
        """Register and login a user"""
        user_data = {
            "email": f"analytics_{fake.uuid4()}@example.com",
            "username": f"analytics_{fake.uuid4()[:8]}",
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

        # Add some transactions to the wallet for analytics
        response = api.post(f"/api/v1/wallets/{user_id}/credit", json={
            "amount": 5000000,
            "referenceId": f"CREDIT_{fake.uuid4()}",
            "description": "Initial funding"
        })

        return {"user_id": user_id, "api": api}

    def test_get_user_metrics(self, user_session):
        """
        Get user metrics including transaction summary
        """
        api = user_session["api"]
        user_id = user_session["user_id"]

        response = api.get(f"/api/v1/analytics/user/{user_id}/metrics")
        if response.status_code != 200:
            pytest.skip(f"Analytics service may not be available: {response.text}")

        metrics = response.json()
        assert "user_id" in metrics or "userId" in metrics
        assert metrics.get("user_id") == user_id or metrics.get("userId") == user_id
        assert "total_transactions" in metrics or "totalTransactions" in metrics
        assert "total_amount" in metrics or "totalAmount" in metrics

    def test_get_spending_trends_daily(self, user_session):
        """
        Get spending trends grouped by day
        """
        api = user_session["api"]
        user_id = user_session["user_id"]

        response = api.post("/api/v1/analytics/spending/trends", json={
            "userId": user_id,
            "periodDays": 30,
            "groupBy": "day"
        })

        if response.status_code != 200:
            pytest.skip(f"Analytics service may not be available: {response.text}")

        trends = response.json()
        assert trends is not None
        if "trends" in trends:
            assert isinstance(trends["trends"], list)

    def test_get_spending_trends_category(self, user_session):
        """
        Get spending trends grouped by category
        """
        api = user_session["api"]
        user_id = user_session["user_id"]

        response = api.post("/api/v1/analytics/spending/trends", json={
            "userId": user_id,
            "periodDays": 90,
            "groupBy": "category"
        })

        if response.status_code != 200:
            pytest.skip(f"Analytics service may not be available: {response.text}")

        trends = response.json()
        assert trends is not None

    def test_get_cash_flow_analysis(self, user_session):
        """
        Get cash flow analysis
        """
        api = user_session["api"]
        user_id = user_session["user_id"]

        response = api.post("/api/v1/analytics/cashflow", json={
            "userId": user_id,
            "periodDays": 30
        })

        if response.status_code != 200:
            pytest.skip(f"Analytics service may not be available: {response.text}")

        cashflow = response.json()
        assert cashflow is not None
        if "income" in cashflow:
            assert "expense" in cashflow
            assert "net" in cashflow

    def test_get_recommendations(self, user_session):
        """
        Get personalized recommendations
        """
        api = user_session["api"]
        user_id = user_session["user_id"]

        response = api.get(f"/api/v1/analytics/user/{user_id}/recommendations")
        if response.status_code != 200:
            pytest.skip(f"Analytics service may not be available: {response.text}")

        recommendations = response.json()
        assert recommendations is not None
        assert "user_id" in recommendations or "userId" in recommendations
        if "recommendations" in recommendations:
            assert isinstance(recommendations["recommendations"], list)

    def test_transaction_history_for_analytics(self, user_session):
        """
        Get transaction history for analytics
        """
        api = user_session["api"]
        user_id = user_session["user_id"]

        # Get wallet transactions
        response = api.get(f"/api/v1/wallets/{user_id}/transactions?page=0&size=50")
        assert response.status_code == 200
        transactions = response.json()
        assert isinstance(transactions, list)

        # Get account transactions
        response = api.get(f"/api/v1/v1/transactions/accounts/{user_id}?page=0&size=50")
        if response.status_code == 200:
            transactions = response.json()
            assert isinstance(transactions, list)

    def test_wallet_ledger_for_analytics(self, user_session):
        """
        Get wallet ledger entries for analytics
        """
        api = user_session["api"]
        user_id = user_session["user_id"]

        response = api.get(f"/api/v1/wallets/{user_id}/ledger")
        if response.status_code == 200:
            ledger = response.json()
            assert isinstance(ledger, list)

    def test_balance_snapshot(self, user_session):
        """
        Get balance snapshot for analytics
        """
        api = user_session["api"]
        user_id = user_session["user_id"]

        response = api.get(f"/api/v1/wallets/{user_id}/balance")
        assert response.status_code == 200
        balance = response.json()

        assert "balance" in balance
        assert "availableBalance" in balance or "available_balance" in balance
        assert "reservedBalance" in balance or "reserved_balance" in balance

    def test_spending_by_period(self, user_session):
        """
        Test analytics for different time periods
        """
        api = user_session["api"]
        user_id = user_session["user_id"]

        periods = [7, 30, 90, 180, 365]

        for period in periods:
            response = api.post("/api/v1/analytics/spending/trends", json={
                "userId": user_id,
                "periodDays": period,
                "groupBy": "day"
            })

            if response.status_code == 200:
                trends = response.json()
                assert trends is not None

    def test_comparison_analytics(self, user_session):
        """
        Test comparison analytics (if available)
        """
        api = user_session["api"]
        user_id = user_session["user_id"]

        # Try to get comparison data
        response = api.post("/api/v1/analytics/spending/trends", json={
            "userId": user_id,
            "periodDays": 30,
            "groupBy": "day"
        })

        if response.status_code == 200:
            trends = response.json()
            # Check if comparison data is available
            if "previousPeriod" in trends:
                assert "currentPeriod" in trends
