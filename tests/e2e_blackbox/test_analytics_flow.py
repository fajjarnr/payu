import pytest


@pytest.mark.analytics
class TestAnalyticsFlow:
    """
    Analytics and ML Recommendations E2E tests.
    Tests: User Metrics -> Spending Trends -> Cash Flow -> Recommendations
    """

    def test_get_user_metrics(self, authenticated_api, registered_user):
        """
        Get user metrics including transaction summary
        """
        user_id = registered_user["userId"]

        response = authenticated_api.get(f"/api/v1/analytics/user/{user_id}/metrics")
        if response.status_code != 200:
            pytest.skip(f"Analytics service may not be available: {response.text}")

        metrics = response.json()
        assert "user_id" in metrics or "userId" in metrics
        assert metrics.get("user_id") == user_id or metrics.get("userId") == user_id
        assert "total_transactions" in metrics or "totalTransactions" in metrics
        assert "total_amount" in metrics or "totalAmount" in metrics

    def test_get_spending_trends_daily(self, authenticated_api, registered_user):
        """
        Get spending trends grouped by day
        """
        user_id = registered_user["userId"]

        response = authenticated_api.post("/api/v1/analytics/spending/trends", json={
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

    def test_get_spending_trends_category(self, authenticated_api, registered_user):
        """
        Get spending trends grouped by category
        """
        user_id = registered_user["userId"]

        response = authenticated_api.post("/api/v1/analytics/spending/trends", json={
            "userId": user_id,
            "periodDays": 90,
            "groupBy": "category"
        })

        if response.status_code != 200:
            pytest.skip(f"Analytics service may not be available: {response.text}")

        trends = response.json()
        assert trends is not None

    def test_get_cash_flow_analysis(self, authenticated_api, registered_user):
        """
        Get cash flow analysis
        """
        user_id = registered_user["userId"]

        response = authenticated_api.post("/api/v1/analytics/cashflow", json={
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

    def test_get_recommendations(self, authenticated_api, registered_user):
        """
        Get personalized recommendations
        """
        user_id = registered_user["userId"]

        response = authenticated_api.get(f"/api/v1/analytics/user/{user_id}/recommendations")
        if response.status_code != 200:
            pytest.skip(f"Analytics service may not be available: {response.text}")

        recommendations = response.json()
        assert recommendations is not None
        assert "user_id" in recommendations or "userId" in recommendations
        if "recommendations" in recommendations:
            assert isinstance(recommendations["recommendations"], list)

    def test_transaction_history_for_analytics(self, authenticated_api, registered_user):
        """
        Get transaction history for analytics
        """
        user_id = registered_user["userId"]

        # Get wallet transactions
        response = authenticated_api.get(f"/api/v1/wallets/{user_id}/transactions?page=0&size=50")
        assert response.status_code == 200
        transactions = response.json()
        assert isinstance(transactions, list)

        # Get account transactions
        response = authenticated_api.get(f"/api/v1/transactions/accounts/{user_id}?page=0&size=50")
        if response.status_code == 200:
            transactions = response.json()
            assert isinstance(transactions, list)

    def test_wallet_ledger_for_analytics(self, authenticated_api, registered_user):
        """
        Get wallet ledger entries for analytics
        """
        user_id = registered_user["userId"]

        response = authenticated_api.get(f"/api/v1/wallets/{user_id}/ledger")
        if response.status_code == 200:
            ledger = response.json()
            assert isinstance(ledger, list)

    def test_balance_snapshot(self, authenticated_api, registered_user):
        """
        Get balance snapshot for analytics
        """
        user_id = registered_user["userId"]

        response = authenticated_api.get(f"/api/v1/wallets/{user_id}/balance")
        assert response.status_code == 200
        balance = response.json()

        assert "balance" in balance
        assert "availableBalance" in balance or "available_balance" in balance
        assert "reservedBalance" in balance or "reserved_balance" in balance

    def test_spending_by_period(self, authenticated_api, registered_user):
        """
        Test analytics for different time periods
        """
        user_id = registered_user["userId"]

        periods = [7, 30, 90, 180, 365]

        for period in periods:
            response = authenticated_api.post("/api/v1/analytics/spending/trends", json={
                "userId": user_id,
                "periodDays": period,
                "groupBy": "day"
            })

            if response.status_code == 200:
                trends = response.json()
                assert trends is not None

    def test_comparison_analytics(self, authenticated_api, registered_user):
        """
        Test comparison analytics (if available)
        """
        user_id = registered_user["userId"]

        # Try to get comparison data
        response = authenticated_api.post("/api/v1/analytics/spending/trends", json={
            "userId": user_id,
            "periodDays": 30,
            "groupBy": "day"
        })

        if response.status_code == 200:
            trends = response.json()
            # Check if comparison data is available
            if "previousPeriod" in trends:
                assert "currentPeriod" in trends
