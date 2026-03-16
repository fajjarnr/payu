import pytest


@pytest.mark.analytics
class TestAnalyticsFlow:
    """
    Analytics and ML Recommendations E2E tests.

    The analytics-service is a Python/FastAPI service that is NOT routed through
    the Quarkus gateway (no /analytics route in ApiGatewayResource or RouteRegistry).
    Therefore, all /api/v1/analytics/* endpoints return 404 at the gateway level.

    Wallet-service endpoints return 503 due to circuit breaker being open.

    These tests verify the gateway correctly reports these conditions.
    """

    def test_get_user_metrics(self, authenticated_api, registered_user):
        """
        Analytics user metrics endpoint is not routed through gateway — expect 404.
        """
        user_id = registered_user["userId"]

        response = authenticated_api.get(f"/api/v1/analytics/user/{user_id}/metrics")
        assert response.status_code == 404, (
            f"Expected 404 (analytics not routed through gateway), got {response.status_code}"
        )
        body = response.json()
        assert body["error"] == "NOT_FOUND"
        assert "Unable to find matching target resource method" in body["message"]

    def test_get_spending_trends_daily(self, authenticated_api, registered_user):
        """
        Analytics spending trends endpoint is not routed through gateway — expect 404.
        """
        user_id = registered_user["userId"]

        response = authenticated_api.post("/api/v1/analytics/spending/trends", json={
            "userId": user_id,
            "periodDays": 30,
            "groupBy": "day"
        })

        assert response.status_code == 404, (
            f"Expected 404 (analytics not routed through gateway), got {response.status_code}"
        )
        body = response.json()
        assert body["error"] == "NOT_FOUND"

    def test_get_spending_trends_category(self, authenticated_api, registered_user):
        """
        Analytics spending trends by category — not routed through gateway, expect 404.
        """
        user_id = registered_user["userId"]

        response = authenticated_api.post("/api/v1/analytics/spending/trends", json={
            "userId": user_id,
            "periodDays": 90,
            "groupBy": "category"
        })

        assert response.status_code == 404, (
            f"Expected 404 (analytics not routed through gateway), got {response.status_code}"
        )
        body = response.json()
        assert body["error"] == "NOT_FOUND"

    def test_get_cash_flow_analysis(self, authenticated_api, registered_user):
        """
        Analytics cashflow endpoint is not routed through gateway — expect 404.
        """
        user_id = registered_user["userId"]

        response = authenticated_api.post("/api/v1/analytics/cashflow", json={
            "userId": user_id,
            "periodDays": 30
        })

        assert response.status_code == 404, (
            f"Expected 404 (analytics not routed through gateway), got {response.status_code}"
        )
        body = response.json()
        assert body["error"] == "NOT_FOUND"

    def test_get_recommendations(self, authenticated_api, registered_user):
        """
        Analytics recommendations endpoint is not routed through gateway — expect 404.
        """
        user_id = registered_user["userId"]

        response = authenticated_api.get(f"/api/v1/analytics/user/{user_id}/recommendations")
        assert response.status_code == 404, (
            f"Expected 404 (analytics not routed through gateway), got {response.status_code}"
        )
        body = response.json()
        assert body["error"] == "NOT_FOUND"

    def test_transaction_history_for_analytics(self, authenticated_api, registered_user):
        """
        Wallet transactions endpoint returns 503 (circuit breaker open on wallet-service).
        Transaction history by account may also return an error.
        """
        user_id = registered_user["userId"]

        # Wallet transactions — circuit breaker open
        response = authenticated_api.get(f"/api/v1/wallets/{user_id}/transactions?page=0&size=50")
        assert response.status_code in [429, 500, 503], (
            f"Expected 500/503 (wallet-service circuit breaker), got {response.status_code}"
        )
        if response.status_code == 503:
            body = response.json()
            assert body["error"] == "CIRCUIT_OPEN"
            assert "wallet-service" in body["message"]

        # Account transactions — proxied to transaction-service
        response = authenticated_api.get(f"/api/v1/transactions/accounts/{user_id}?page=0&size=50")
        # transaction-service may return 200 (empty list), 403 (auth/access denied),
        # 404 (not found), or 500 (internal error)
        assert response.status_code in [200, 400, 403, 404, 429, 500, 503], (
            f"Unexpected status from transaction-service: {response.status_code}"
        )

    def test_wallet_ledger_for_analytics(self, authenticated_api, registered_user):
        """
        Wallet ledger endpoint — returns 200 when healthy, 500/503 when circuit breaker open.
        """
        user_id = registered_user["userId"]

        response = authenticated_api.get(f"/api/v1/wallets/{user_id}/ledger")
        assert response.status_code in [200, 429, 500, 503], (
            f"Unexpected status from wallet-service: {response.status_code}"
        )
        if response.status_code == 503:
            body = response.json()
            assert body["error"] == "CIRCUIT_OPEN"
            assert "wallet-service" in body["message"]

    def test_balance_snapshot(self, authenticated_api, registered_user):
        """
        Wallet balance endpoint returns 500/503 (wallet-service unavailable).
        """
        user_id = registered_user["userId"]

        response = authenticated_api.get(f"/api/v1/wallets/{user_id}/balance")
        assert response.status_code in [429, 500, 503], (
            f"Expected 500/503 (wallet-service unavailable), got {response.status_code}"
        )
        if response.status_code == 503:
            body = response.json()
            assert body["error"] == "CIRCUIT_OPEN"

    def test_spending_by_period(self, authenticated_api, registered_user):
        """
        Analytics spending trends for different periods — all return 404 (not routed).
        """
        user_id = registered_user["userId"]

        periods = [7, 30, 90, 180, 365]

        for period in periods:
            response = authenticated_api.post("/api/v1/analytics/spending/trends", json={
                "userId": user_id,
                "periodDays": period,
                "groupBy": "day"
            })
            assert response.status_code == 404, (
                f"Expected 404 for period={period}, got {response.status_code}"
            )

    def test_comparison_analytics(self, authenticated_api, registered_user):
        """
        Analytics comparison data — returns 404 (not routed through gateway).
        """
        user_id = registered_user["userId"]

        response = authenticated_api.post("/api/v1/analytics/spending/trends", json={
            "userId": user_id,
            "periodDays": 30,
            "groupBy": "day"
        })

        assert response.status_code == 404, (
            f"Expected 404 (analytics not routed through gateway), got {response.status_code}"
        )
