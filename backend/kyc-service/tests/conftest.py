import pytest
from unittest.mock import Mock, AsyncMock, patch
from sqlalchemy.ext.asyncio import AsyncSession
from app.main import app
from app.config import Settings


# Override settings for testing
@pytest.fixture(scope="session")
def test_settings():
    """Test settings with SQLite in-memory database"""
    original_settings = Settings._settings
    test_settings = Settings(
        database_url="sqlite+aiosqlite:///:memory:",
        kafka_bootstrap_servers="localhost:9092",
        dukcapil_url="http://localhost:8091/api/v1",
        enable_tracing=False,
    )

    with patch("app.config.get_settings", return_value=test_settings):
        yield test_settings

    # Restore original settings
    Settings._settings = original_settings


@pytest.fixture(scope="session")
async def test_db():
    """Create test database tables"""
    from app.database import Base

    # Create in-memory database
    from sqlalchemy.ext.asyncio import create_async_engine, async_sessionmaker

    engine = create_async_engine("sqlite+aiosqlite:///:memory:", echo=False)

    async_session_maker = async_sessionmaker(
        engine, class_=AsyncSession, expire_on_commit=False
    )

    # Create tables
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)

    yield async_session_maker

    # Cleanup
    await engine.dispose()


@pytest.fixture
async def test_client(test_db):
    """Test client with database session"""

    # Override get_db_session to use test database
    async def get_test_db():
        async with test_db() as session:
            yield session

    with patch("app.database.get_db_session", side_effect=get_test_db):
        # Use ASGI transport which will handle lifespan events
        from fastapi.testclient import TestClient

        with TestClient(app) as client:
            yield client


@pytest.fixture
def mock_settings():
    """Mock settings for testing"""
    return Settings(
        database_url="sqlite+aiosqlite:///:memory:",
        kafka_bootstrap_servers="localhost:9092",
        dukcapil_url="http://localhost:8091/api/v1",
        enable_tracing=False,
    )


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
