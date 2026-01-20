import pytest
from unittest.mock import Mock, AsyncMock
from sqlalchemy.ext.asyncio import AsyncSession

@pytest.fixture
def mock_settings():
    """Mock settings for testing"""
    from app.config import Settings
    settings = Settings(
        database_url="postgresql+asyncpg://test:test@localhost:5433/test_analytics",
        kafka_bootstrap_servers="localhost:9092"
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
def sample_user_id():
    """Sample user ID"""
    return "user_123456789"
