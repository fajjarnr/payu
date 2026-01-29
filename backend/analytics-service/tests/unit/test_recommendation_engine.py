"""
Unit tests for RecommendationEngine.

Tests the recommendation generation logic based on user metrics
and spending patterns.
"""

import pytest
from datetime import datetime, timedelta
from decimal import Decimal

from app.ml.recommendation_engine import RecommendationEngine
from app.models.schemas import SpendingTrendResponse, SpendingPattern


@pytest.fixture
def recommendation_engine():
    """Create a RecommendationEngine instance."""
    return RecommendationEngine()


@pytest.fixture
def sample_user_metrics():
    """Create sample user metrics."""
    from app.database import UserMetricsEntity

    return UserMetricsEntity(
        user_id="user_123",
        total_transactions=100,
        total_amount=Decimal("10000000.00"),
        average_transaction=Decimal("100000.00"),
        last_transaction_date=datetime.utcnow() - timedelta(days=5),
        account_age_days=180,
        kyc_status="VERIFIED",
    )


@pytest.fixture
def high_spending_user_metrics():
    """Create high spending user metrics."""
    from app.database import UserMetricsEntity

    return UserMetricsEntity(
        user_id="user_456",
        total_transactions=300,
        total_amount=Decimal("50000000.00"),
        average_transaction=Decimal("166666.67"),
        last_transaction_date=datetime.utcnow() - timedelta(days=1),
        account_age_days=365,
        kyc_status="VERIFIED",
    )


@pytest.fixture
def inactive_user_metrics():
    """Create inactive user metrics."""
    from app.database import UserMetricsEntity

    return UserMetricsEntity(
        user_id="user_789",
        total_transactions=50,
        total_amount=Decimal("5000000.00"),
        average_transaction=Decimal("100000.00"),
        last_transaction_date=datetime.utcnow() - timedelta(days=45),
        account_age_days=400,
        kyc_status="VERIFIED",
    )


@pytest.fixture
def sample_spending_trends():
    """Create sample spending trends."""
    return SpendingTrendResponse(
        period="30 days",
        total_spending=Decimal("8000000.00"),
        spending_by_category=[
            SpendingPattern(
                category="FOOD",
                amount=Decimal("3000000.00"),
                percentage=37.5,
                transaction_count=45,
                trend="up",
            ),
            SpendingPattern(
                category="TRANSPORT",
                amount=Decimal("2000000.00"),
                percentage=25.0,
                transaction_count=30,
                trend="stable",
            ),
            SpendingPattern(
                category="ENTERTAINMENT",
                amount=Decimal("1500000.00"),
                percentage=18.75,
                transaction_count=15,
                trend="up",
            ),
            SpendingPattern(
                category="BILLS",
                amount=Decimal("1500000.00"),
                percentage=18.75,
                transaction_count=10,
                trend="stable",
            ),
        ],
        month_over_month_change=15.5,
        top_merchants=[
            {
                "merchant_id": "merchant_1",
                "total_amount": 1500000.0,
                "transaction_count": 20,
            },
            {
                "merchant_id": "merchant_2",
                "total_amount": 1000000.0,
                "transaction_count": 15,
            },
        ],
    )


@pytest.fixture
def savings_goal_trends():
    """Create spending trends for user with savings goals."""
    return SpendingTrendResponse(
        period="30 days",
        total_spending=Decimal("5000000.00"),
        spending_by_category=[
            SpendingPattern(
                category="NECESSITIES",
                amount=Decimal("4000000.00"),
                percentage=80.0,
                transaction_count=25,
                trend="stable",
            ),
            SpendingPattern(
                category="ENTERTAINMENT",
                amount=Decimal("1000000.00"),
                percentage=20.0,
                transaction_count=5,
                trend="down",
            ),
        ],
        month_over_month_change=-10.0,
        top_merchants=[],
    )


class TestRecommendationEngine:
    """Test suite for RecommendationEngine."""

    def test_engine_initialization(self, recommendation_engine):
        """Test engine initialization."""
        assert recommendation_engine is not None

    def test_generate_recommendations_high_spending(
        self, recommendation_engine, high_spending_user_metrics, sample_spending_trends
    ):
        """Test recommendations for high spending user."""
        recommendations = recommendation_engine.generate_recommendations(
            high_spending_user_metrics, sample_spending_trends
        )

        assert isinstance(recommendations, list)
        assert len(recommendations) <= 5  # Max 5 recommendations returned

        # Check recommendation structure matches actual implementation
        for rec in recommendations:
            assert "recommendation_id" in rec
            assert "recommendation_type" in rec
            assert "title" in rec
            assert "description" in rec
            assert "priority" in rec

    def test_generate_recommendations_inactive_user(
        self, recommendation_engine, inactive_user_metrics, sample_spending_trends
    ):
        """Test recommendations for inactive user."""
        recommendations = recommendation_engine.generate_recommendations(
            inactive_user_metrics, sample_spending_trends
        )

        assert isinstance(recommendations, list)

        # Should include re-engagement recommendations for inactive users (>30 days)
        rec_titles = " ".join([rec.get("title", "") for rec in recommendations])
        assert "aktifkan" in rec_titles.lower() or "kembali" in rec_titles.lower()

    def test_generate_recommendations_savings_goal(
        self, recommendation_engine, sample_user_metrics, savings_goal_trends
    ):
        """Test recommendations for user focused on savings."""
        recommendations = recommendation_engine.generate_recommendations(
            sample_user_metrics, savings_goal_trends
        )

        assert isinstance(recommendations, list)

    def test_generate_recommendations_new_user(self, recommendation_engine):
        """Test recommendations for new user with minimal history."""
        from app.database import UserMetricsEntity

        new_user_metrics = UserMetricsEntity(
            user_id="new_user",
            total_transactions=5,
            total_amount=Decimal("500000.00"),
            average_transaction=Decimal("100000.00"),
            last_transaction_date=datetime.utcnow() - timedelta(days=1),
            account_age_days=7,
            kyc_status="VERIFIED",
        )

        minimal_trends = SpendingTrendResponse(
            period="30 days",
            total_spending=Decimal("500000.00"),
            spending_by_category=[],
            month_over_month_change=0.0,
            top_merchants=[],
        )

        recommendations = recommendation_engine.generate_recommendations(
            new_user_metrics, minimal_trends
        )

        assert isinstance(recommendations, list)

    def test_generate_recommendations_unverified_kyc(self, recommendation_engine):
        """Test recommendations for user with unverified KYC."""
        from app.database import UserMetricsEntity

        unverified_metrics = UserMetricsEntity(
            user_id="unverified_user",
            total_transactions=10,
            total_amount=Decimal("1000000.00"),
            average_transaction=Decimal("100000.00"),
            last_transaction_date=datetime.utcnow() - timedelta(days=5),
            account_age_days=30,
            kyc_status="PENDING",
        )

        minimal_trends = SpendingTrendResponse(
            period="30 days",
            total_spending=Decimal("1000000.00"),
            spending_by_category=[],
            month_over_month_change=0.0,
            top_merchants=[],
        )

        recommendations = recommendation_engine.generate_recommendations(
            unverified_metrics, minimal_trends
        )

        assert isinstance(recommendations, list)

        # Should include KYC verification recommendation
        rec_titles = " ".join([rec.get("title", "") for rec in recommendations])
        assert "kyc" in rec_titles.lower()

    def test_generate_recommendations_with_mom_increase(
        self, recommendation_engine, sample_user_metrics
    ):
        """Test recommendations when spending increased month-over-month."""
        increasing_trends = SpendingTrendResponse(
            period="30 days",
            total_spending=Decimal("12000000.00"),
            spending_by_category=[
                SpendingPattern(
                    category="ENTERTAINMENT",
                    amount=Decimal("5000000.00"),
                    percentage=41.67,
                    transaction_count=25,
                    trend="up",
                )
            ],
            month_over_month_change=35.0,  # 35% increase
            top_merchants=[],
        )

        recommendations = recommendation_engine.generate_recommendations(
            sample_user_metrics, increasing_trends
        )

        assert isinstance(recommendations, list)
        # Should include spending trend alert for >20% mom change
        rec_types = [rec.get("recommendation_type") for rec in recommendations]
        assert "SPENDING_TREND" in rec_types

    def test_generate_recommendations_with_mom_decrease(
        self, recommendation_engine, sample_user_metrics
    ):
        """Test recommendations when spending decreased month-over-month."""
        decreasing_trends = SpendingTrendResponse(
            period="30 days",
            total_spending=Decimal("4000000.00"),
            spending_by_category=[
                SpendingPattern(
                    category="NECESSITIES",
                    amount=Decimal("3000000.00"),
                    percentage=75.0,
                    transaction_count=20,
                    trend="stable",
                )
            ],
            month_over_month_change=-25.0,  # 25% decrease
            top_merchants=[],
        )

        recommendations = recommendation_engine.generate_recommendations(
            sample_user_metrics, decreasing_trends
        )

        assert isinstance(recommendations, list)

    def test_recommendation_priority_ordering(
        self, recommendation_engine, sample_user_metrics, sample_spending_trends
    ):
        """Test that recommendations are ordered by priority."""
        recommendations = recommendation_engine.generate_recommendations(
            sample_user_metrics, sample_spending_trends
        )

        # Check that priorities are in descending order (higher priority first)
        priorities = [rec.get("priority", 0) for rec in recommendations]
        assert priorities == sorted(priorities, reverse=True)

    def test_recommendation_no_more_than_five(
        self, recommendation_engine, sample_user_metrics, sample_spending_trends
    ):
        """Test that no more than 5 recommendations are returned."""
        recommendations = recommendation_engine.generate_recommendations(
            sample_user_metrics, sample_spending_trends
        )

        assert len(recommendations) <= 5

    def test_generate_recommendations_empty_trends(
        self, recommendation_engine, sample_user_metrics
    ):
        """Test recommendations when spending trends are empty."""
        empty_trends = SpendingTrendResponse(
            period="30 days",
            total_spending=Decimal("0"),
            spending_by_category=[],
            month_over_month_change=0.0,
            top_merchants=[],
        )

        recommendations = recommendation_engine.generate_recommendations(
            sample_user_metrics, empty_trends
        )

        # Should still return some recommendations
        assert isinstance(recommendations, list)

    def test_recommendation_description_not_empty(
        self, recommendation_engine, sample_user_metrics, sample_spending_trends
    ):
        """Test that all recommendation descriptions are non-empty."""
        recommendations = recommendation_engine.generate_recommendations(
            sample_user_metrics, sample_spending_trends
        )

        for rec in recommendations:
            description = rec.get("description", "")
            assert len(description.strip()) > 0

    def test_recommendation_for_entertainment_spending(self, recommendation_engine):
        """Test recommendations for high entertainment spending."""
        from app.database import UserMetricsEntity

        metrics = UserMetricsEntity(
            user_id="user_ent",
            total_transactions=50,
            total_amount=Decimal("5000000.00"),
            average_transaction=Decimal("100000.00"),
            last_transaction_date=datetime.utcnow() - timedelta(days=2),
            account_age_days=90,
            kyc_status="VERIFIED",
        )

        entertainment_heavy_trends = SpendingTrendResponse(
            period="30 days",
            total_spending=Decimal("4000000.00"),
            spending_by_category=[
                SpendingPattern(
                    category="ENTERTAINMENT",
                    amount=Decimal("3000000.00"),
                    percentage=75.0,
                    transaction_count=30,
                    trend="up",
                )
            ],
            month_over_month_change=20.0,
            top_merchants=[],
        )

        recommendations = recommendation_engine.generate_recommendations(
            metrics, entertainment_heavy_trends
        )

        assert isinstance(recommendations, list)

        # Should trigger category-specific alert
        rec_descriptions = " ".join(
            [rec.get("description", "") for rec in recommendations]
        )
        assert "entertainment" in rec_descriptions.lower()

    def test_recommendation_for_top_merchant_concentration(self, recommendation_engine):
        """Test recommendations when spending is concentrated on few merchants."""
        from app.database import UserMetricsEntity

        metrics = UserMetricsEntity(
            user_id="user_merchant",
            total_transactions=100,
            total_amount=Decimal("10000000.00"),
            average_transaction=Decimal("100000.00"),
            last_transaction_date=datetime.utcnow() - timedelta(days=1),
            account_age_days=200,
            kyc_status="VERIFIED",
        )

        concentrated_trends = SpendingTrendResponse(
            period="30 days",
            total_spending=Decimal("8000000.00"),
            spending_by_category=[
                SpendingPattern(
                    category="SHOPPING",
                    amount=Decimal("8000000.00"),
                    percentage=100.0,
                    transaction_count=80,
                    trend="stable",
                )
            ],
            month_over_month_change=5.0,
            top_merchants=[
                {
                    "merchant_id": "single_merchant",
                    "total_amount": 7000000.0,
                    "transaction_count": 70,
                }
            ],
        )

        recommendations = recommendation_engine.generate_recommendations(
            metrics, concentrated_trends
        )

        assert isinstance(recommendations, list)
