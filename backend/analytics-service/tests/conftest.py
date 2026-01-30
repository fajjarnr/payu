import pytest
import sys
from typing import Optional
from datetime import datetime, timedelta
from decimal import Decimal

sys.path.insert(0, "/home/ubuntu/payu/backend/analytics-service/src")  # noqa: E402
from unittest.mock import AsyncMock, MagicMock  # noqa: E402
from sqlalchemy.ext.asyncio import AsyncSession  # noqa: E402


# Helper functions (not fixtures) for creating mock objects
def create_mock_row(**kwargs):
    """Create a mock database row"""
    row = MagicMock()
    for key, value in kwargs.items():
        setattr(row, key, value)
    return row


def mock_scalar_result(value: Optional[object]) -> MagicMock:
    """Create a mock result for scalar() queries"""
    result = MagicMock()
    result.scalar_one_or_none = MagicMock(return_value=value)
    result.scalar = MagicMock(return_value=value)
    return result


def mock_query_result(rows: list) -> MagicMock:
    """Create a mock result for queries that return multiple rows"""
    result = MagicMock()
    # Mock the result iteration
    result.__iter__ = lambda self: iter(rows)
    result.all = MagicMock(return_value=rows)
    result.first = MagicMock(return_value=rows[0] if rows else None)
    # Mock column_descriptions for the service
    result.column_descriptions = [{"name": "total_amount", "type": type(None)}]
    return result


def mock_scalars_result(values: list) -> AsyncMock:
    """Create a mock result for scalars() queries"""
    result = AsyncMock()
    # Create a mock scalars() result that has all() and first() methods
    scalars_obj = MagicMock()
    scalars_obj.all = MagicMock(return_value=values)
    scalars_obj.first = MagicMock(return_value=values[0] if values else None)
    result.scalars = MagicMock(return_value=scalars_obj)
    # Also set all() and first() directly on result for backward compatibility
    result.all = MagicMock(return_value=values)
    result.first = MagicMock(return_value=values[0] if values else None)
    return result


# Pytest fixtures
@pytest.fixture
def mock_settings():
    """Mock settings for testing"""
    from app.config import Settings

    settings = Settings(
        database_url="postgresql+asyncpg://test:test@localhost:5433/test_analytics",  # pragma: allowlist secret
        kafka_bootstrap_servers="localhost:9092",
    )
    return settings


@pytest.fixture
def mock_db_session():
    """Mock database session"""
    session = AsyncMock(spec=AsyncSession)
    session.commit = AsyncMock()
    return session


@pytest.fixture
def mock_analytics_service(mock_db_session):
    """Mock Analytics service"""
    from app.services.analytics_service import AnalyticsService

    return AnalyticsService(mock_db_session)


@pytest.fixture
def analytics_service(mock_analytics_service):
    """Alias for mock_analytics_service for compatibility"""
    return mock_analytics_service


@pytest.fixture
def sample_user_id():
    """Sample user ID"""
    return "user_123456789"


@pytest.fixture
def sample_user_metrics():
    """Create sample user metrics for testing"""
    from app.database import UserMetricsEntity

    return UserMetricsEntity(
        user_id="user_123",
        total_transactions=150,
        total_amount=Decimal("15000000.00"),
        average_transaction=Decimal("100000.00"),
        last_transaction_date=datetime.utcnow() - timedelta(days=5),
        account_age_days=180,
        kyc_status="VERIFIED",
    )


@pytest.fixture
def sample_fraud_score_entity():
    """Create sample fraud score entity for testing"""
    from app.database import FraudScoreEntity

    return FraudScoreEntity(
        score_id="score_12345",
        transaction_id="txn_12345",
        user_id="user_67890",
        risk_score=25.5,
        risk_level="LOW",
        risk_factors={"amount": 10.0, "velocity": 5.0, "behavioral": 10.5},
        is_suspicious=False,
        recommended_action="ALLOW",
        is_blocked=False,
        requires_review=False,
        rule_triggers=[],
        scored_at=datetime.utcnow(),
    )


# Additional fixtures for tests that need helper functions as parameters
@pytest.fixture
def mock_scalars_result_fn():
    """Fixture providing the mock_scalars_result helper function for tests"""
    return mock_scalars_result


@pytest.fixture
def mock_scalar_result_fn():
    """Fixture providing the mock_scalar_result helper function for tests"""
    return mock_scalar_result


@pytest.fixture
def mock_execute_sequence():
    """Helper to configure mock execute to return different results in sequence"""
    def _setup_execute_sequence(mock_session, results: list):
        """Setup execute to return different results on each call"""
        mock_session.execute.side_effect = results
    return _setup_execute_sequence
