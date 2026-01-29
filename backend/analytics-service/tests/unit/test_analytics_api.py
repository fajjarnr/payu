"""
Unit tests for Analytics API endpoints.

Tests the FastAPI endpoint handlers in the analytics router.
"""
import pytest
from unittest.mock import AsyncMock, MagicMock, patch
from fastapi import status

from app.api.v1.analytics import (
    get_user_metrics,
    get_spending_trends,
    get_cash_flow_analysis,
    get_recommendations,
    get_robo_advisory,
    calculate_fraud_score,
    get_transaction_fraud_score,
    get_user_high_risk_transactions
)
from app.models.schemas import (
    GetAnalyticsRequest,
    GetSpendingTrendsRequest,
    GetRecommendationsResponse,
    GetRoboAdvisoryRequest,
    GetFraudScoreRequest,
    UserMetricsResponse,
    SpendingTrendResponse,
    CashFlowAnalysis
)


@pytest.fixture
def sample_user_metrics_response():
    """Create sample user metrics response."""
    from app.database import UserMetricsEntity
    from datetime import datetime
    from decimal import Decimal

    return UserMetricsEntity(
        user_id="user_123",
        total_transactions=100,
        total_amount=Decimal("10000000.00"),
        average_transaction=Decimal("100000.00"),
        last_transaction_date=datetime.utcnow(),
        account_age_days=365,
        kyc_status="VERIFIED"
    )


@pytest.fixture
def sample_spending_trends_response():
    """Create sample spending trends response."""
    from app.models.schemas import SpendingPattern
    from decimal import Decimal

    return SpendingTrendResponse(
        period="30 days",
        total_spending=Decimal("8000000.00"),
        spending_by_category=[
            SpendingPattern(
                category="FOOD",
                amount=Decimal("3000000.00"),
                percentage=37.5,
                transaction_count=30,
                trend="stable"
            )
        ],
        month_over_month_change=15.5,
        top_merchants=[
            {"merchant_id": "merchant_1", "total_amount": 1500000.0, "transaction_count": 20}
        ]
    )


@pytest.fixture
def sample_cash_flow_response():
    """Create sample cash flow analysis response."""
    from decimal import Decimal

    return CashFlowAnalysis(
        period="30 days",
        income=Decimal("15000000.00"),
        expenses=Decimal("8000000.00"),
        net_cash_flow=Decimal("7000000.00"),
        income_by_source=[],
        expenses_by_category=[]
    )


@pytest.fixture
def sample_robo_advisory_request():
    """Create sample robo advisory request."""
    return GetRoboAdvisoryRequest(
        user_id="user_123",
        risk_questions=[3, 4, 3, 4, 3],
        monthly_investment_amount=2000000.0
    )


@pytest.fixture
def sample_fraud_score_request():
    """Create sample fraud score request."""
    return GetFraudScoreRequest(
        transaction_id="txn_12345",
        user_id="user_67890",
        amount=500000.0,
        currency="IDR",
        transaction_type="TRANSFER",
        metadata={}
    )


class TestAnalyticsAPIEndpoints:
    """Test suite for Analytics API endpoints."""

    @pytest.mark.asyncio
    async def test_get_user_metrics_success(self, mock_db_session, sample_user_metrics_response):
        """Test successful user metrics retrieval."""
        # Mock the service
        with patch('app.api.v1.analytics.AnalyticsService') as mock_service_class:
            mock_service = AsyncMock()
            mock_service.get_user_metrics.return_value = sample_user_metrics_response
            mock_service_class.return_value = mock_service

            # Execute
            result = await get_user_metrics("user_123", mock_db_session)

            # Verify
            assert isinstance(result, UserMetricsResponse)
            assert result.user_id == "user_123"
            assert result.total_transactions == 100
            assert result.kyc_status == "VERIFIED"

    @pytest.mark.asyncio
    async def test_get_user_metrics_not_found(self, mock_db_session):
        """Test user metrics retrieval when user not found."""
        with patch('app.api.v1.analytics.AnalyticsService') as mock_service_class:
            mock_service = AsyncMock()
            mock_service.get_user_metrics.return_value = None
            mock_service_class.return_value = mock_service

            from fastapi import HTTPException

            # Should raise HTTPException
            with pytest.raises(HTTPException) as exc_info:
                await get_user_metrics("nonexistent_user", mock_db_session)

            assert exc_info.value.status_code == status.HTTP_404_NOT_FOUND
            assert exc_info.value.detail["error_code"] == "ANA_VAL_001"

    @pytest.mark.asyncio
    async def test_get_user_metrics_service_error(self, mock_db_session):
        """Test user metrics retrieval when service raises error."""
        with patch('app.api.v1.analytics.AnalyticsService') as mock_service_class:
            mock_service = AsyncMock()
            mock_service.get_user_metrics.side_effect = Exception("Database error")
            mock_service_class.return_value = mock_service

            from fastapi import HTTPException

            with pytest.raises(HTTPException) as exc_info:
                await get_user_metrics("user_123", mock_db_session)

            assert exc_info.value.status_code == status.HTTP_500_INTERNAL_SERVER_ERROR
            assert exc_info.value.detail["error_code"] == "ANA_SYS_001"

    @pytest.mark.asyncio
    async def test_get_spending_trends_success(self, mock_db_session, sample_spending_trends_response):
        """Test successful spending trends retrieval."""
        request = GetSpendingTrendsRequest(
            user_id="user_123",
            period_days=30,
            group_by="category"
        )

        with patch('app.api.v1.analytics.AnalyticsService') as mock_service_class:
            mock_service = AsyncMock()
            mock_service.get_spending_trends.return_value = sample_spending_trends_response
            mock_service_class.return_value = mock_service

            result = await get_spending_trends(request, mock_db_session)

            assert isinstance(result, SpendingTrendResponse)
            assert result.period == "30 days"

    @pytest.mark.asyncio
    async def test_get_spending_trends_service_error(self, mock_db_session):
        """Test spending trends when service raises error."""
        request = GetSpendingTrendsRequest(
            user_id="user_123",
            period_days=30,
            group_by="category"
        )

        with patch('app.api.v1.analytics.AnalyticsService') as mock_service_class:
            mock_service = AsyncMock()
            mock_service.get_spending_trends.side_effect = Exception("Query error")
            mock_service_class.return_value = mock_service

            from fastapi import HTTPException

            with pytest.raises(HTTPException) as exc_info:
                await get_spending_trends(request, mock_db_session)

            assert exc_info.value.status_code == status.HTTP_500_INTERNAL_SERVER_ERROR
            assert exc_info.value.detail["error_code"] == "ANA_SYS_002"

    @pytest.mark.asyncio
    async def test_get_cash_flow_analysis_success(self, mock_db_session, sample_cash_flow_response):
        """Test successful cash flow analysis retrieval."""
        request = GetAnalyticsRequest(
            user_id="user_123",
            period_days=30
        )

        with patch('app.api.v1.analytics.AnalyticsService') as mock_service_class:
            mock_service = AsyncMock()
            mock_service.get_cash_flow_analysis.return_value = sample_cash_flow_response
            mock_service_class.return_value = mock_service

            result = await get_cash_flow_analysis(request, mock_db_session)

            assert isinstance(result, CashFlowAnalysis)
            assert result.period == "30 days"
            assert result.income > 0

    @pytest.mark.asyncio
    async def test_get_cash_flow_analysis_service_error(self, mock_db_session):
        """Test cash flow analysis when service raises error."""
        request = GetAnalyticsRequest(
            user_id="user_123",
            period_days=30
        )

        with patch('app.api.v1.analytics.AnalyticsService') as mock_service_class:
            mock_service = AsyncMock()
            mock_service.get_cash_flow_analysis.side_effect = Exception("Analysis error")
            mock_service_class.return_value = mock_service

            from fastapi import HTTPException

            with pytest.raises(HTTPException) as exc_info:
                await get_cash_flow_analysis(request, mock_db_session)

            assert exc_info.value.status_code == status.HTTP_500_INTERNAL_SERVER_ERROR
            assert exc_info.value.detail["error_code"] == "ANA_SYS_003"

    @pytest.mark.asyncio
    async def test_get_recommendations_success(self, mock_db_session):
        """Test successful recommendations retrieval."""
        with patch('app.api.v1.analytics.AnalyticsService') as mock_service_class:
            mock_service = AsyncMock()
            mock_service.get_recommendations.return_value = [
                {"type": "savings", "message": "Save more!", "priority": "high"}
            ]
            mock_service_class.return_value = mock_service

            result = await get_recommendations("user_123", mock_db_session)

            assert isinstance(result, GetRecommendationsResponse)
            assert result.user_id == "user_123"
            assert len(result.recommendations) > 0

    @pytest.mark.asyncio
    async def test_get_recommendations_service_error(self, mock_db_session):
        """Test recommendations when service raises error."""
        with patch('app.api.v1.analytics.AnalyticsService') as mock_service_class:
            mock_service = AsyncMock()
            mock_service.get_recommendations.side_effect = Exception("Recommendation error")
            mock_service_class.return_value = mock_service

            from fastapi import HTTPException

            with pytest.raises(HTTPException) as exc_info:
                await get_recommendations("user_123", mock_db_session)

            assert exc_info.value.status_code == status.HTTP_500_INTERNAL_SERVER_ERROR
            assert exc_info.value.detail["error_code"] == "ANA_SYS_004"

    @pytest.mark.asyncio
    async def test_get_robo_advisory_success(self, sample_robo_advisory_request):
        """Test successful robo advisory generation."""
        with patch('app.api.v1.analytics.RoboAdvisoryEngine') as mock_engine_class:
            mock_engine = MagicMock()
            mock_engine.generate_robo_advisory.return_value = {
                "risk_profile": "moderate",
                "portfolio_allocation": {},
                "recommendations": [],
                "expected_annual_return": 12.5
            }
            mock_engine_class.return_value = mock_engine

            result = await get_robo_advisory(sample_robo_advisory_request)

            assert result is not None
            mock_engine.generate_robo_advisory.assert_called_once()

    @pytest.mark.asyncio
    async def test_get_robo_advisory_service_error(self, sample_robo_advisory_request):
        """Test robo advisory when service raises error."""
        with patch('app.api.v1.analytics.RoboAdvisoryEngine') as mock_engine_class:
            mock_engine = MagicMock()
            mock_engine.generate_robo_advisory.side_effect = Exception("Advisory error")
            mock_engine_class.return_value = mock_engine

            from fastapi import HTTPException

            with pytest.raises(HTTPException) as exc_info:
                await get_robo_advisory(sample_robo_advisory_request)

            assert exc_info.value.status_code == status.HTTP_500_INTERNAL_SERVER_ERROR
            assert exc_info.value.detail["error_code"] == "ANA_SYS_005"

    @pytest.mark.asyncio
    async def test_calculate_fraud_score_success(self, sample_fraud_score_request):
        """Test successful fraud score calculation."""
        from app.models.schemas import FraudDetectionResult, FraudScore, RiskLevel

        mock_result = FraudDetectionResult(
            fraud_score=FraudScore(
                transaction_id="txn_12345",
                user_id="user_67890",
                risk_score=25,
                risk_level=RiskLevel.LOW,
                risk_factors={},
                is_suspicious=False,
                recommended_action="ALLOW"
            ),
            is_blocked=False,
            requires_review=False,
            rule_triggers=[]
        )

        with patch('app.api.v1.analytics.FraudDetectionEngine') as mock_engine_class:
            mock_engine = AsyncMock()
            mock_engine.calculate_fraud_score.return_value = mock_result
            mock_engine_class.return_value = mock_engine

            result = await calculate_fraud_score(sample_fraud_score_request)

            assert isinstance(result, FraudDetectionResult)
            assert result.fraud_score.transaction_id == "txn_12345"

    @pytest.mark.asyncio
    async def test_calculate_fraud_score_service_error(self, sample_fraud_score_request):
        """Test fraud score calculation when service raises error."""
        with patch('app.api.v1.analytics.FraudDetectionEngine') as mock_engine_class:
            mock_engine = AsyncMock()
            mock_engine.calculate_fraud_score.side_effect = Exception("Fraud detection error")
            mock_engine_class.return_value = mock_engine

            from fastapi import HTTPException

            with pytest.raises(HTTPException) as exc_info:
                await calculate_fraud_score(sample_fraud_score_request)

            assert exc_info.value.status_code == status.HTTP_500_INTERNAL_SERVER_ERROR
            assert exc_info.value.detail["error_code"] == "ANA_SYS_006"

    @pytest.mark.asyncio
    async def test_get_transaction_fraud_score_found(
        self,
        mock_db_session,
        sample_fraud_score_entity,
        mock_scalar_result
    ):
        """Test retrieving existing fraud score for transaction."""
        mock_db_session.execute.return_value = mock_scalar_result(
            sample_fraud_score_entity
        )

        result = await get_transaction_fraud_score("txn_12345", mock_db_session)

        assert result is not None
        assert result.fraud_score["transaction_id"] == "txn_12345"

    @pytest.mark.asyncio
    async def test_get_transaction_fraud_score_not_found(
        self,
        mock_db_session,
        mock_scalar_result
    ):
        """Test retrieving fraud score when transaction not found."""
        mock_db_session.execute.return_value = mock_scalar_result(None)

        from fastapi import HTTPException

        with pytest.raises(HTTPException) as exc_info:
            await get_transaction_fraud_score("nonexistent_txn", mock_db_session)

        assert exc_info.value.status_code == status.HTTP_404_NOT_FOUND
        assert exc_info.value.detail["error_code"] == "ANA_VAL_002"

    @pytest.mark.asyncio
    async def test_get_transaction_fraud_score_service_error(self, mock_db_session):
        """Test fraud score retrieval when service raises error."""
        mock_db_session.execute.side_effect = Exception("Database error")

        from fastapi import HTTPException

        with pytest.raises(HTTPException) as exc_info:
            await get_transaction_fraud_score("txn_12345", mock_db_session)

        assert exc_info.value.status_code == status.HTTP_500_INTERNAL_SERVER_ERROR
        assert exc_info.value.detail["error_code"] == "ANA_SYS_007"

    @pytest.mark.asyncio
    async def test_get_user_high_risk_transactions_success(
        self,
        mock_db_session,
        sample_fraud_score_entity,
        mock_scalars_result
    ):
        """Test retrieving high-risk transactions for user."""
        from datetime import datetime, timedelta
        from app.database import FraudScoreEntity

        mock_entities = [
            FraudScoreEntity(
                score_id=f"score_{i}",
                transaction_id=f"txn_{i}",
                user_id="user_123",
                risk_score=80 + i,
                risk_level="HIGH",
                risk_factors={},
                is_suspicious=True,
                recommended_action="BLOCK",
                is_blocked=True,
                requires_review=True,
                rule_triggers=[],
                scored_at=datetime.utcnow() - timedelta(days=i)
            )
            for i in range(5)
        ]

        mock_db_session.execute.return_value = mock_scalars_result(mock_entities)

        result = await get_user_high_risk_transactions("user_123", mock_db_session)

        assert isinstance(result, list)
        assert len(result) > 0
        assert all("transaction_id" in txn for txn in result)

    @pytest.mark.asyncio
    async def test_get_user_high_risk_transactions_empty(
        self,
        mock_db_session,
        mock_scalars_result
    ):
        """Test retrieving high-risk transactions when none exist."""
        mock_db_session.execute.return_value = mock_scalars_result([])

        result = await get_user_high_risk_transactions("user_123", mock_db_session)

        assert result == []

    @pytest.mark.asyncio
    async def test_get_user_high_risk_transactions_service_error(self, mock_db_session):
        """Test high-risk transactions retrieval when service raises error."""
        mock_db_session.execute.side_effect = Exception("Query error")

        from fastapi import HTTPException

        with pytest.raises(HTTPException) as exc_info:
            await get_user_high_risk_transactions("user_123", mock_db_session)

        assert exc_info.value.status_code == status.HTTP_500_INTERNAL_SERVER_ERROR
        assert exc_info.value.detail["error_code"] == "ANA_SYS_008"


class TestAnalyticsAPIValidation:
    """Test suite for API input validation."""

    @pytest.mark.asyncio
    async def test_get_spending_trends_different_group_by(self, mock_db_session):
        """Test spending trends with different group_by options."""
        with patch('app.api.v1.analytics.AnalyticsService') as mock_service_class:
            mock_service = AsyncMock()
            mock_service.get_spending_trends.return_value = SpendingTrendResponse(
                period="30 days",
                total_spending=0,
                spending_by_category=[],
                month_over_month_change=0,
                top_merchants=[]
            )
            mock_service_class.return_value = mock_service

            # Test different group_by values
            for group_by in ["category", "merchant", "day"]:
                request = GetSpendingTrendsRequest(
                    user_id="user_123",
                    period_days=30,
                    group_by=group_by
                )
                result = await get_spending_trends(request, mock_db_session)
                assert result is not None

    @pytest.mark.asyncio
    async def test_get_spending_trends_custom_period(self, mock_db_session):
        """Test spending trends with custom period."""
        with patch('app.api.v1.analytics.AnalyticsService') as mock_service_class:
            mock_service = AsyncMock()
            mock_service.get_spending_trends.return_value = SpendingTrendResponse(
                period="60 days",
                total_spending=0,
                spending_by_category=[],
                month_over_month_change=0,
                top_merchants=[]
            )
            mock_service_class.return_value = mock_service

            request = GetSpendingTrendsRequest(
                user_id="user_123",
                period_days=60,
                group_by="category"
            )
            result = await get_spending_trends(request, mock_db_session)
            assert result is not None

    @pytest.mark.asyncio
    async def test_get_cash_flow_analysis_custom_period(self, mock_db_session):
        """Test cash flow analysis with custom period."""
        with patch('app.api.v1.analytics.AnalyticsService') as mock_service_class:
            mock_service = AsyncMock()
            mock_service.get_cash_flow_analysis.return_value = CashFlowAnalysis(
                period="90 days",
                income=0,
                expenses=0,
                net_cash_flow=0,
                income_by_source=[],
                expenses_by_category=[]
            )
            mock_service_class.return_value = mock_service

            request = GetAnalyticsRequest(
                user_id="user_123",
                period_days=90
            )
            result = await get_cash_flow_analysis(request, mock_db_session)
            assert result is not None

    @pytest.mark.asyncio
    async def test_robo_advisory_various_risk_profiles(self):
        """Test robo advisory with various risk profiles."""
        risk_profiles = [
            [1, 1, 1, 1, 1],  # Conservative
            [3, 3, 3, 3, 3],  # Moderate
            [5, 5, 5, 5, 5],  # Aggressive
        ]

        with patch('app.api.v1.analytics.RoboAdvisoryEngine') as mock_engine_class:
            mock_engine = MagicMock()
            mock_engine.generate_robo_advisory.return_value = {
                "risk_profile": "moderate",
                "portfolio_allocation": {},
                "recommendations": [],
                "expected_annual_return": 10.0
            }
            mock_engine_class.return_value = mock_engine

            for risk_questions in risk_profiles:
                request = GetRoboAdvisoryRequest(
                    user_id="user_123",
                    risk_questions=risk_questions,
                    monthly_investment_amount=1000000.0
                )
                result = await get_robo_advisory(request)
                assert result is not None

    @pytest.mark.asyncio
    async def test_fraud_score_different_transaction_types(self):
        """Test fraud score calculation for different transaction types."""
        transaction_types = ["TRANSFER", "QRIS", "PAYMENT", "WITHDRAWAL"]

        with patch('app.api.v1.analytics.FraudDetectionEngine') as mock_engine_class:
            from app.models.schemas import FraudDetectionResult, FraudScore, RiskLevel

            mock_result = FraudDetectionResult(
                fraud_score=FraudScore(
                    transaction_id="txn_123",
                    user_id="user_123",
                    risk_score=20,
                    risk_level=RiskLevel.LOW,
                    risk_factors={},
                    is_suspicious=False,
                    recommended_action="ALLOW"
                ),
                is_blocked=False,
                requires_review=False,
                rule_triggers=[]
            )

            mock_engine = AsyncMock()
            mock_engine.calculate_fraud_score.return_value = mock_result
            mock_engine_class.return_value = mock_engine

            for txn_type in transaction_types:
                request = GetFraudScoreRequest(
                    transaction_id=f"txn_{txn_type}",
                    user_id="user_123",
                    amount=500000.0,
                    currency="IDR",
                    transaction_type=txn_type,
                    metadata={}
                )
                result = await calculate_fraud_score(request)
                assert result is not None
