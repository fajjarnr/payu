import pytest
from httpx import AsyncClient, ASGITransport
from unittest.mock import AsyncMock, patch, MagicMock
from datetime import datetime, timedelta
from decimal import Decimal

from app.main import app


@pytest.mark.e2e
class TestAnalyticsWorkflowE2E:
    """End-to-end tests for Analytics workflow"""

    @pytest.mark.asyncio
    async def test_get_user_metrics(self, sample_user_id):
        """Test getting user metrics"""

        async with AsyncClient(
            transport=ASGITransport(app=app),
            base_url="http://test"
        ) as client:
            # Mock database query
            with patch('app.services.analytics_service.AnalyticsService.get_user_metrics') as mock_get:
                mock_metrics = MagicMock()
                mock_metrics.user_id = sample_user_id
                mock_metrics.total_transactions = 150
                mock_metrics.total_amount = Decimal("15000000.00")
                mock_metrics.average_transaction = Decimal("100000.00")
                mock_metrics.last_transaction_date = datetime.utcnow()
                mock_metrics.account_age_days = 90
                mock_metrics.kyc_status = "VERIFIED"

                mock_get.return_value = mock_metrics

                response = await client.get(f"/api/v1/analytics/user/{sample_user_id}/metrics")

                assert response.status_code == 200
                data = response.json()
                assert data["user_id"] == sample_user_id
                assert data["total_transactions"] == 150
                assert data["total_amount"] == "15000000.00"
                assert data["average_transaction"] == "100000.00"
                assert data["account_age_days"] == 90
                assert data["kyc_status"] == "VERIFIED"

    @pytest.mark.asyncio
    async def test_get_spending_trends(self, sample_user_id):
        """Test getting spending trends"""

        async with AsyncClient(
            transport=ASGITransport(app=app),
            base_url="http://test"
        ) as client:
            # Mock analytics service
            with patch('app.services.analytics_service.AnalyticsService.get_spending_trends') as mock_get:
                mock_trends = MagicMock()
                mock_trends.period = "30 days"
                mock_trends.total_spending = Decimal("5000000.00")
                mock_trends.month_over_month_change = 15.5
                mock_trends.spending_by_category = [
                    {
                        "category": "FOOD",
                        "amount": "1500000.00",
                        "percentage": 30.0,
                        "transaction_count": 45,
                        "trend": "stable"
                    },
                    {
                        "category": "TRANSPORT",
                        "amount": "1000000.00",
                        "percentage": 20.0,
                        "transaction_count": 30,
                        "trend": "increasing"
                    },
                    {
                        "category": "SHOPPING",
                        "amount": "2500000.00",
                        "percentage": 50.0,
                        "transaction_count": 25,
                        "trend": "stable"
                    }
                ]
                mock_trends.top_merchants = [
                    {
                        "merchant_id": "merchant_001",
                        "total_amount": 500000.0,
                        "transaction_count": 10
                    },
                    {
                        "merchant_id": "merchant_002",
                        "total_amount": 300000.0,
                        "transaction_count": 8
                    }
                ]

                mock_get.return_value = mock_trends

                response = await client.post(
                    "/api/v1/analytics/spending/trends",
                    json={
                        "user_id": sample_user_id,
                        "period_days": 30,
                        "group_by": "category"
                    }
                )

                assert response.status_code == 200
                data = response.json()
                assert data["period"] == "30 days"
                assert data["total_spending"] == "5000000.00"
                assert data["month_over_month_change"] == 15.5
                assert len(data["spending_by_category"]) == 3
                assert data["spending_by_category"][0]["category"] == "FOOD"
                assert data["spending_by_category"][0]["percentage"] == 30.0
                assert len(data["top_merchants"]) == 2

    @pytest.mark.asyncio
    async def test_get_cash_flow_analysis(self, sample_user_id):
        """Test getting cash flow analysis"""

        async with AsyncClient(
            transport=ASGITransport(app=app),
            base_url="http://test"
        ) as client:
            # Mock analytics service
            with patch('app.services.analytics_service.AnalyticsService.get_cash_flow_analysis') as mock_get:
                mock_analysis = MagicMock()
                mock_analysis.period = "30 days"
                mock_analysis.income = Decimal("20000000.00")
                mock_analysis.expenses = Decimal("15000000.00")
                mock_analysis.net_cash_flow = Decimal("5000000.00")
                mock_analysis.income_by_source = [
                    {
                        "source": "SALARY",
                        "amount": "15000000.00"
                    },
                    {
                        "source": "SIDE_HUSTLE",
                        "amount": "5000000.00"
                    }
                ]
                mock_analysis.expenses_by_category = [
                    {
                        "category": "FOOD",
                        "amount": "5000000.00",
                        "percentage": 33.33,
                        "transaction_count": 45,
                        "trend": "stable"
                    },
                    {
                        "category": "SHOPPING",
                        "amount": "10000000.00",
                        "percentage": 66.67,
                        "transaction_count": 25,
                        "trend": "increasing"
                    }
                ]

                mock_get.return_value = mock_analysis

                response = await client.post(
                    "/api/v1/analytics/cashflow",
                    json={
                        "user_id": sample_user_id,
                        "period_days": 30
                    }
                )

                assert response.status_code == 200
                data = response.json()
                assert data["period"] == "30 days"
                assert data["income"] == "20000000.00"
                assert data["expenses"] == "15000000.00"
                assert data["net_cash_flow"] == "5000000.00"
                assert len(data["income_by_source"]) == 2
                assert len(data["expenses_by_category"]) == 2

    @pytest.mark.asyncio
    async def test_get_recommendations(self, sample_user_id):
        """Test getting personalized recommendations"""

        async with AsyncClient(
            transport=ASGITransport(app=app),
            base_url="http://test"
        ) as client:
            # Mock analytics service
            with patch('app.services.analytics_service.AnalyticsService.get_recommendations') as mock_get:
                mock_recommendations = [
                    {
                        "recommendation_id": "rec_001",
                        "recommendation_type": "SPENDING_TREND",
                        "title": "Pengeluaran Anda meningkat",
                        "description": "Pengeluaran Anda naik 15.5% dibanding bulan lalu.",
                        "priority": 2,
                        "action_url": None,
                        "metadata": {"mom_change": 15.5}
                    },
                    {
                        "recommendation_id": "rec_002",
                        "recommendation_type": "SAVINGS_GOAL",
                        "title": "Mulai investasi",
                        "description": "Anda memiliki saldo yang cukup untuk mulai berinvestasi.",
                        "priority": 4,
                        "action_url": "/investments",
                        "metadata": {"total_balance": 15000000.0}
                    },
                    {
                        "recommendation_id": "rec_003",
                        "recommendation_type": "BUDGET_ALERT",
                        "title": "Perhatian: SHOPPING",
                        "description": "Pengeluaran SHOPPING mencapai 66.67% total.",
                        "priority": 4,
                        "action_url": None,
                        "metadata": {"category": "SHOPPING", "trend": "increasing"}
                    }
                ]

                mock_get.return_value = mock_recommendations

                response = await client.get(f"/api/v1/analytics/user/{sample_user_id}/recommendations")

                assert response.status_code == 200
                data = response.json()
                assert data["user_id"] == sample_user_id
                assert len(data["recommendations"]) == 3
                assert data["recommendations"][0]["title"] == "Pengeluaran Anda meningkat"
                assert data["recommendations"][1]["recommendation_type"] == "SAVINGS_GOAL"
                assert data["recommendations"][2]["priority"] == 4

    @pytest.mark.asyncio
    async def test_complete_user_journey_with_analytics(self, sample_user_id):
        """Test complete user journey with analytics integration"""

        async with AsyncClient(
            transport=ASGITransport(app=app),
            base_url="http://test"
        ) as client:
            # Step 1: Get user metrics
            with patch('app.services.analytics_service.AnalyticsService.get_user_metrics') as mock_metrics:
                mock_metrics.return_value = MagicMock(
                    user_id=sample_user_id,
                    total_transactions=200,
                    total_amount=Decimal("20000000.00"),
                    average_transaction=Decimal("100000.00"),
                    last_transaction_date=datetime.utcnow(),
                    account_age_days=90,
                    kyc_status="VERIFIED"
                )

                metrics_response = await client.get(f"/api/v1/analytics/user/{sample_user_id}/metrics")
                assert metrics_response.status_code == 200

            # Step 2: Get spending trends
            with patch('app.services.analytics_service.AnalyticsService.get_spending_trends') as mock_trends:
                mock_trends.return_value = MagicMock(
                    period="30 days",
                    total_spending=Decimal("5000000.00"),
                    month_over_month_change=20.0,
                    spending_by_category=[
                        {
                            "category": "FOOD",
                            "amount": "2000000.00",
                            "percentage": 40.0,
                            "transaction_count": 60,
                            "trend": "increasing"
                        },
                        {
                            "category": "SHOPPING",
                            "amount": "3000000.00",
                            "percentage": 60.0,
                            "transaction_count": 30,
                            "trend": "increasing"
                        }
                    ],
                    top_merchants=[
                        {
                            "merchant_id": "merchant_001",
                            "total_amount": 600000.0,
                            "transaction_count": 15
                        }
                    ]
                )

                trends_response = await client.post(
                    "/api/v1/analytics/spending/trends",
                    json={"user_id": sample_user_id, "period_days": 30, "group_by": "category"}
                )
                assert trends_response.status_code == 200

            # Step 3: Get cash flow
            with patch('app.services.analytics_service.AnalyticsService.get_cash_flow_analysis') as mock_cf:
                mock_cf.return_value = MagicMock(
                    period="30 days",
                    income=Decimal("25000000.00"),
                    expenses=Decimal("20000000.00"),
                    net_cash_flow=Decimal("5000000.00"),
                    income_by_source=[
                        {"source": "SALARY", "amount": "20000000.00"},
                        {"source": "SIDE_HUSTLE", "amount": "5000000.00"}
                    ],
                    expenses_by_category=[
                        {
                            "category": "FOOD",
                            "amount": "8000000.00",
                            "percentage": 40.0,
                            "transaction_count": 80,
                            "trend": "stable"
                        },
                        {
                            "category": "SHOPPING",
                            "amount": "12000000.00",
                            "percentage": 60.0,
                            "transaction_count": 40,
                            "trend": "increasing"
                        }
                    ]
                )

                cf_response = await client.post(
                    "/api/v1/analytics/cashflow",
                    json={"user_id": sample_user_id, "period_days": 30}
                )
                assert cf_response.status_code == 200

            # Step 4: Get recommendations (should trigger due to high spending)
            with patch('app.services.analytics_service.AnalyticsService.get_recommendations') as mock_rec:
                mock_rec.return_value = [
                    {
                        "recommendation_id": "rec_001",
                        "recommendation_type": "SPENDING_TREND",
                        "title": "Pengeluaran SHOPPING tinggi",
                        "description": "Anda menghabiskan 60% pengeluaran untuk SHOPPING.",
                        "priority": 4,
                        "action_url": None,
                        "metadata": {"category": "SHOPPING", "percentage": 60.0}
                    },
                    {
                        "recommendation_id": "rec_002",
                        "recommendation_type": "SAVINGS_GOAL",
                        "title": "Mulai investasi",
                        "description": "Anda memiliki saldo yang cukup.",
                        "priority": 4,
                        "action_url": "/investments",
                        "metadata": {}
                    }
                ]

                rec_response = await client.get(f"/api/v1/analytics/user/{sample_user_id}/recommendations")
                assert rec_response.status_code == 200
                data = rec_response.json()
                assert len(data["recommendations"]) == 2
                assert any(r["recommendation_type"] == "SPENDING_TREND" for r in data["recommendations"])
                assert any(r["recommendation_type"] == "SAVINGS_GOAL" for r in data["recommendations"])
