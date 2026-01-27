import pytest
import sys

sys.path.insert(0, "/home/ubuntu/payu/backend/kyc-service/src")  # noqa: E402

from unittest.mock import Mock, AsyncMock, patch  # noqa: E402
from sqlalchemy.ext.asyncio import (
    AsyncSession,
    create_async_engine,
    async_sessionmaker,
)  # noqa: E402
from sqlalchemy.pool import StaticPool  # noqa: E402
from app.main import app  # noqa: E402
from app.config import Settings  # noqa: E402
from app.database import Base, get_db_session  # noqa: E402
from httpx import AsyncClient, ASGITransport  # noqa: E402


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
async def test_db_engine():
    """Create test database engine and tables"""
    # Create in-memory database engine with StaticPool for shared connections
    test_engine = create_async_engine(
        "sqlite+aiosqlite:///:memory:",
        echo=False,
        poolclass=StaticPool,
        connect_args={"check_same_thread": False},
    )

    # Create tables
    async with test_engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)

    yield test_engine

    # Cleanup
    await test_engine.dispose()


@pytest.fixture
async def test_session_maker(test_db_engine):
    """Create test session maker"""
    async_session_maker = async_sessionmaker(
        test_db_engine, class_=AsyncSession, expire_on_commit=False
    )

    # Patch the module-level variables
    import app.database

    original_engine = app.database.engine
    original_session_maker = app.database.async_session_maker

    app.database.engine = test_db_engine
    app.database.async_session_maker = async_session_maker

    yield async_session_maker

    # Restore original values
    app.database.engine = original_engine
    app.database.async_session_maker = original_session_maker


@pytest.fixture
async def async_test_client(test_session_maker):
    """Async test client with database session override"""

    # Override get_db_session dependency
    async def override_get_db():
        async with test_session_maker() as session:
            yield session

    app.dependency_overrides[get_db_session] = override_get_db

    # Create AsyncClient with ASGI transport
    async with AsyncClient(
        transport=ASGITransport(app=app), base_url="http://test"
    ) as client:
        yield client

    # Clean up dependency override
    app.dependency_overrides.clear()


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
