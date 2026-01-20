import pytest
from unittest.mock import MagicMock
from decimal import Decimal


@pytest.mark.unit
class TestRecommendationEngine:
    """Unit tests for Recommendation engine"""

    @pytest.fixture
    def recommendation_engine():
        from app.ml.recommendation_engine import RecommendationEngine
        return RecommendationEngine()

    def test_generate_recommendations_high_spending(self, recommendation_engine):
        """Test recommendations for high spending trend"""
        user_metrics = MagicMock()
        user_metrics.total_amount = Decimal("20000000.00")
        user_metrics.last_transaction_date = None

        spending_trends = MagicMock()
        spending_trends.month_over_month_change = 25.0
        spending_trends.spending_by_category = [
            MagicMock(category="FOOD", percentage=40.0),
            MagicMock(category="SHOPPING", percentage=35.0)
        ]
        spending_trends.total_spending = Decimal("5000000.00")

        recommendations = recommendation_engine.generate_recommendations(
            user_metrics, spending_trends
        )

        assert len(recommendations) > 0
        assert any(r['recommendation_type'] == 'SPENDING_TREND' for r in recommendations)

    def test_generate_recommendations_inactive_user(self, recommendation_engine):
        """Test recommendations for inactive user"""
        from datetime import datetime, timedelta

        user_metrics = MagicMock()
        user_metrics.total_amount = Decimal("100000.00")
        user_metrics.last_transaction_date = datetime.utcnow() - timedelta(days=40)
        user_metrics.kyc_status = "VERIFIED"

        spending_trends = MagicMock()
        spending_trends.month_over_month_change = 0
        spending_trends.spending_by_category = []

        recommendations = recommendation_engine.generate_recommendations(
            user_metrics, spending_trends
        )

        assert len(recommendations) > 0
        assert any(r['recommendation_type'] == 'NEW_FEATURE' for r in recommendations)

    def test_generate_recommendations_savings_goal(self, recommendation_engine):
        """Test savings goal recommendations"""
        user_metrics = MagicMock()
        user_metrics.total_amount = Decimal("5000000.00")
        user_metrics.last_transaction_date = None
        user_metrics.kyc_status = "VERIFIED"

        spending_trends = MagicMock()
        spending_trends.month_over_month_change = 0
        spending_trends.spending_by_category = []

        recommendations = recommendation_engine.generate_recommendations(
            user_metrics, spending_trends
        )

        assert len(recommendations) > 0
        assert any(r['recommendation_type'] == 'SAVINGS_GOAL' for r in recommendations)


@pytest.mark.unit
class TestAnalyticsService:
    """Unit tests for Analytics service"""

    @pytest.mark.asyncio
    async def test_get_user_metrics(self, mock_analytics_service, sample_user_id):
        """Test getting user metrics"""
        mock_analytics_service.get_user_metrics = MagicMock(return_value=MagicMock(
            user_id=sample_user_id,
            total_transactions=100,
            total_amount=Decimal("10000000.00"),
            average_transaction=Decimal("100000.00"),
            last_transaction_date=None,
            account_age_days=60,
            kyc_status="VERIFIED"
        ))

        metrics = await mock_analytics_service.get_user_metrics(sample_user_id)

        assert metrics.user_id == sample_user_id
        assert metrics.total_transactions == 100
        assert metrics.kyc_status == "VERIFIED"
