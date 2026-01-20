import pytest
import asyncio
from unittest.mock import Mock, AsyncMock
from sqlalchemy.ext.asyncio import AsyncSession

@pytest.fixture
def mock_settings():
    """Mock settings for testing"""
    from app.config import Settings
    settings = Settings(
        database_url="postgresql+asyncpg://test:test@localhost:5433/test_kyc",
        kafka_bootstrap_servers="localhost:9092",
        dukcapil_url="http://localhost:8091/api/v1"
    )
    return settings


@pytest.fixture
def mock_db_session():
    """Mock database session"""
    session = AsyncMock(spec=AsyncSession)
    session.commit = AsyncMock()
    session.refresh = AsyncMock()
    session.add = Mock()
    return session


@pytest.fixture
async def mock_kyc_service(mock_db_session):
    """Mock KYC service"""
    from app.services.kyc_service import KycService
    return KycService(mock_db_session)


@pytest.fixture
def sample_ktp_image():
    """Sample KTP image base64"""
    return "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="


@pytest.fixture
def sample_selfie_image():
    """Sample selfie image base64"""
    return "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="


@pytest.fixture
def sample_user_id():
    """Sample user ID"""
    return "user_123456789"


@pytest.fixture
def sample_verification_id():
    """Sample verification ID"""
    return "verify_123456789"
