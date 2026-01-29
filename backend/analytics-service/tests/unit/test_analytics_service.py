"""
Unit tests for AnalyticsService.

Tests the core analytics service methods including user metrics,
spending trends, cash flow analysis, and recommendations.
"""
import pytest
from datetime import datetime, timedelta
from decimal import Decimal
from unittest.mock import AsyncMock, MagicMock

from app.services.analytics_service import AnalyticsService
from app.database import UserMetricsEntity
from tests.conftest import create_mock_row


class TestAnalyticsService:
    """Test suite for AnalyticsService."""

    def test_service_initialization(self, analytics_service):
        """Test service initialization."""
        assert analytics_service.db is not None
        assert analytics_service.recommendation_engine is not None

    @pytest.mark.asyncio
    async def test_get_user_metrics_success(
        self,
        analytics_service,
        mock_db_session,
        sample_user_metrics,
        mock_scalar_result
    ):
        """Test successful retrieval of user metrics."""
        # Setup mock response using helper fixture
        mock_db_session.execute.return_value = mock_scalar_result(sample_user_metrics)

        # Execute
        result = await analytics_service.get_user_metrics("user_123")

        # Verify
        assert result is not None
        assert result.user_id == "user_123"
        assert result.total_transactions == 150
        assert result.total_amount == Decimal("15000000.00")
        assert result.kyc_status == "VERIFIED"

    @pytest.mark.asyncio
    async def test_get_user_metrics_not_found(
        self,
        analytics_service,
        mock_db_session,
        mock_scalar_result
    ):
        """Test retrieval when user metrics don't exist."""
        # Setup mock to return None
        mock_db_session.execute.return_value = mock_scalar_result(None)

        # Execute
        result = await analytics_service.get_user_metrics("nonexistent_user")

        # Verify
        assert result is None

    @pytest.mark.asyncio
    async def test_get_spending_trends_by_category(
        self,
        analytics_service,
        mock_db_session,
        mock_query_result
    ):
        """Test spending trends grouped by category."""
        # Mock rows representing spending by category using helper
        mock_rows = [
            create_mock_row(
                category="FOOD",
                total_amount=Decimal("3000000"),
                transaction_count=30
            ),
            create_mock_row(
                category="TRANSPORT",
                total_amount=Decimal("1500000"),
                transaction_count=20
            ),
            create_mock_row(
                category="SHOPPING",
                total_amount=Decimal("5000000"),
                transaction_count=10
            ),
        ]

        mock_db_session.execute.return_value = mock_query_result(mock_rows)

        # Mock helper methods
        analytics_service._calculate_mom_change = AsyncMock(return_value=15.0)
        analytics_service._get_top_merchants = AsyncMock(return_value=[])

        # Execute
        result = await analytics_service.get_spending_trends(
            "user_123", period_days=30, group_by="category"
        )

        # Verify
        assert result is not None
        assert result.period == "30 days"
        assert len(result.spending_by_category) > 0

    @pytest.mark.asyncio
    async def test_get_spending_trends_empty_transactions(
        self,
        analytics_service,
        mock_db_session,
        mock_query_result
    ):
        """Test spending trends when user has no transactions."""
        mock_db_session.execute.return_value = mock_query_result([])

        # Mock helper methods
        analytics_service._calculate_mom_change = AsyncMock(return_value=0.0)
        analytics_service._get_top_merchants = AsyncMock(return_value=[])

        result = await analytics_service.get_spending_trends("user_123", period_days=30)

        assert result is not None

    @pytest.mark.asyncio
    async def test_get_cash_flow_analysis(
        self,
        analytics_service,
        mock_db_session,
        mock_scalar_result,
        mock_execute_sequence
    ):
        """Test cash flow analysis calculation."""
        # Setup results for income and expenses
        income_result = mock_scalar_result(Decimal("10000000.00"))
        expenses_result = mock_scalar_result(Decimal("7000000.00"))

        # Configure execute to return different results in sequence
        mock_execute_sequence(mock_db_session, [income_result, expenses_result])

        # Mock helper methods
        analytics_service._get_income_by_source = AsyncMock(return_value=[])
        analytics_service._get_expenses_by_category = AsyncMock(return_value=[])

        # Execute
        result = await analytics_service.get_cash_flow_analysis("user_123", period_days=30)

        # Verify
        assert result is not None
        assert result.period == "30 days"
        assert result.income == Decimal("10000000.00")
        assert result.expenses == Decimal("7000000.00")
        assert result.net_cash_flow == Decimal("3000000.00")

    @pytest.mark.asyncio
    async def test_get_cash_flow_analysis_no_income(
        self,
        analytics_service,
        mock_db_session,
        mock_scalar_result,
        mock_execute_sequence
    ):
        """Test cash flow analysis with no income."""
        income_result = mock_scalar_result(None)
        expenses_result = mock_scalar_result(Decimal("5000000.00"))

        mock_execute_sequence(mock_db_session, [income_result, expenses_result])

        analytics_service._get_income_by_source = AsyncMock(return_value=[])
        analytics_service._get_expenses_by_category = AsyncMock(return_value=[])

        result = await analytics_service.get_cash_flow_analysis("user_123", period_days=30)

        assert result.income == Decimal("0")
        # net_cash_flow = income - expenses = 0 - 5000000 = -5000000
        assert result.net_cash_flow == Decimal("-5000000")

    @pytest.mark.asyncio
    async def test_get_user_metrics_not_found(self, analytics_service, mock_db_session):
        """Test retrieval when user metrics don't exist."""
        # Setup mock to return None
        mock_result = MagicMock()
        mock_result.scalar_one_or_none.return_value = None
        mock_db_session.execute.return_value = mock_result

        # Execute
        result = await analytics_service.get_user_metrics("nonexistent_user")

        # Verify
        assert result is None

    @pytest.mark.asyncio
    async def test_get_spending_trends_by_category(
        self,
        analytics_service,
        mock_db_session,
        mock_query_result
    ):
        """Test spending trends grouped by category."""
        # Mock rows representing spending by category using helper
        mock_rows = [
            create_mock_row(
                category="FOOD",
                total_amount=Decimal("3000000"),
                transaction_count=30
            ),
            create_mock_row(
                category="TRANSPORT",
                total_amount=Decimal("1500000"),
                transaction_count=20
            ),
            create_mock_row(
                category="SHOPPING",
                total_amount=Decimal("5000000"),
                transaction_count=10
            ),
        ]

        mock_db_session.execute.return_value = mock_query_result(mock_rows)

        # Mock helper methods
        analytics_service._calculate_mom_change = AsyncMock(return_value=15.0)
        analytics_service._get_top_merchants = AsyncMock(return_value=[])

        # Execute
        result = await analytics_service.get_spending_trends("user_123", period_days=30, group_by="category")

        # Verify
        assert result is not None
        assert result.period == "30 days"
        assert len(result.spending_by_category) > 0

    @pytest.mark.asyncio
    async def test_get_spending_trends_empty_transactions(
        self,
        analytics_service,
        mock_db_session,
        mock_query_result
    ):
        """Test spending trends when user has no transactions."""
        mock_db_session.execute.return_value = mock_query_result([])

        # Mock helper methods
        analytics_service._calculate_mom_change = AsyncMock(return_value=0.0)
        analytics_service._get_top_merchants = AsyncMock(return_value=[])

        result = await analytics_service.get_spending_trends("user_123", period_days=30)

        assert result is not None

    @pytest.mark.asyncio
    async def test_get_cash_flow_analysis(self, analytics_service, mock_db_session):
        """Test cash flow analysis calculation."""
        # Setup income result
        mock_income_result = MagicMock()
        mock_income_result.scalar.return_value = Decimal("10000000.00")

        # Setup expenses result
        mock_expenses_result = MagicMock()
        mock_expenses_result.scalar.return_value = Decimal("7000000.00")

        # Configure execute to return different results
        execute_call_count = 0
        async def mock_execute_func(query):
            nonlocal execute_call_count
            execute_call_count += 1
            if execute_call_count == 1:
                return mock_income_result
            else:
                return mock_expenses_result

        mock_db_session.execute.side_effect = mock_execute_func

        # Mock helper methods
        analytics_service._get_income_by_source = AsyncMock(return_value=[])
        analytics_service._get_expenses_by_category = AsyncMock(return_value=[])

        # Execute
        result = await analytics_service.get_cash_flow_analysis("user_123", period_days=30)

        # Verify
        assert result is not None
        assert result.period == "30 days"
        assert result.income == Decimal("10000000.00")
        assert result.expenses == Decimal("7000000.00")
        assert result.net_cash_flow == Decimal("3000000.00")

    @pytest.mark.asyncio
    async def test_get_cash_flow_analysis_no_income(self, analytics_service, mock_db_session):
        """Test cash flow analysis with no income."""
        mock_income_result = MagicMock()
        mock_income_result.scalar.return_value = None

        mock_expenses_result = MagicMock()
        mock_expenses_result.scalar.return_value = Decimal("5000000.00")

        execute_call_count = 0
        async def mock_execute_func(query):
            nonlocal execute_call_count
            execute_call_count += 1
            if execute_call_count == 1:
                return mock_income_result
            else:
                return mock_expenses_result

        mock_db_session.execute.side_effect = mock_execute_func
        analytics_service._get_income_by_source = AsyncMock(return_value=[])
        analytics_service._get_expenses_by_category = AsyncMock(return_value=[])

        result = await analytics_service.get_cash_flow_analysis("user_123", period_days=30)

        assert result.income == Decimal("0")
        # net_cash_flow = income - expenses = 0 - 5000000 = -5000000
        assert result.net_cash_flow == Decimal("-5000000")

    @pytest.mark.asyncio
    async def test_get_recommendations(self, analytics_service, sample_user_metrics):
        """Test getting personalized recommendations."""
        # Mock dependencies
        analytics_service.get_user_metrics = AsyncMock(return_value=sample_user_metrics)
        analytics_service.get_spending_trends = AsyncMock(return_value=MagicMock(
            period="30 days",
            total_spending=Decimal("5000000.00"),
            spending_by_category=[],
            month_over_month_change=10.0,
            top_merchants=[]
        ))
        analytics_service.recommendation_engine.generate_recommendations = MagicMock(
            return_value=[{"recommendation_type": "savings", "description": "Save more!", "priority": 1}]
        )

        result = await analytics_service.get_recommendations("user_123")

        assert result is not None
        assert isinstance(result, list)
        analytics_service.get_user_metrics.assert_called_once_with("user_123")

    @pytest.mark.asyncio
    async def test_calculate_mom_change_with_previous_data(
        self,
        analytics_service,
        mock_db_session,
        mock_scalar_result,
        mock_execute_sequence
    ):
        """Test month-over-month change calculation with previous period data."""
        # Setup mock responses
        current_result = mock_scalar_result(Decimal("6000000.00"))
        previous_result = mock_scalar_result(Decimal("5000000.00"))

        mock_execute_sequence(mock_db_session, [current_result, previous_result])

        # Execute
        result = await analytics_service._calculate_mom_change("user_123", 30)

        # Verify: (6M - 5M) / 5M * 100 = 20%
        assert result == 20.0

    @pytest.mark.asyncio
    async def test_calculate_mom_change_no_previous_data(
        self,
        analytics_service,
        mock_db_session,
        mock_scalar_result,
        mock_execute_sequence
    ):
        """Test month-over-month change when no previous period data exists."""
        current_result = mock_scalar_result(Decimal("5000000.00"))
        previous_result = mock_scalar_result(None)

        mock_execute_sequence(mock_db_session, [current_result, previous_result])

        result = await analytics_service._calculate_mom_change("user_123", 30)

        # Should return 0 when no previous data
        assert result == 0.0

    @pytest.mark.asyncio
    async def test_calculate_mom_change_zero_previous_period(
        self,
        analytics_service,
        mock_db_session,
        mock_scalar_result,
        mock_execute_sequence
    ):
        """Test month-over-month change when previous period is zero."""
        current_result = mock_scalar_result(Decimal("5000000.00"))
        previous_result = mock_scalar_result(Decimal("0"))

        mock_execute_sequence(mock_db_session, [current_result, previous_result])

        result = await analytics_service._calculate_mom_change("user_123", 30)

        assert result == 0.0

    @pytest.mark.asyncio
    async def test_get_top_merchants(
        self,
        analytics_service,
        mock_db_session,
        mock_query_result
    ):
        """Test retrieval of top merchants by spending."""
        # Mock merchant data using helper
        mock_rows = [
            create_mock_row(
                merchant_id="merchant_1",
                total_amount=Decimal("2000000"),
                transaction_count=15
            ),
            create_mock_row(
                merchant_id="merchant_2",
                total_amount=Decimal("1500000"),
                transaction_count=10
            ),
            create_mock_row(
                merchant_id="merchant_3",
                total_amount=Decimal("1000000"),
                transaction_count=8
            ),
        ]

        mock_db_session.execute.return_value = mock_query_result(mock_rows)

        result = await analytics_service._get_top_merchants("user_123", 30)

        assert len(result) == 3
        assert result[0]["merchant_id"] == "merchant_1"
        assert result[0]["total_amount"] == 2000000.0

    @pytest.mark.asyncio
    async def test_get_top_merchants_no_data(
        self,
        analytics_service,
        mock_db_session,
        mock_query_result
    ):
        """Test top merchants when no merchant data exists."""
        mock_db_session.execute.return_value = mock_query_result([])

        result = await analytics_service._get_top_merchants("user_123", 30)

        assert result == []

    @pytest.mark.asyncio
    async def test_get_income_by_source(self, analytics_service):
        """Test income by source breakdown."""
        result = await analytics_service._get_income_by_source("user_123", 30)

        # Currently returns empty list as placeholder
        assert result == []

    @pytest.mark.asyncio
    async def test_get_expenses_by_category(self, analytics_service):
        """Test expenses by category breakdown."""
        # Mock spending trends
        mock_spending_pattern = MagicMock(
            category="FOOD",
            amount=Decimal("1000000"),
            percentage=30.0,
            transaction_count=10
        )

        analytics_service.get_spending_trends = AsyncMock(
            return_value=MagicMock(spending_by_category=[mock_spending_pattern])
        )

        result = await analytics_service._get_expenses_by_category("user_123", 30)

        assert isinstance(result, list)
