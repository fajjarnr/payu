import pytest
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))


def pytest_configure(config):
    """Configure pytest with custom markers"""
    config.addinivalue_line(
        "markers", "smoke: marks tests as smoke tests (fast, critical path)"
    )
    config.addinivalue_line(
        "markers", "critical: marks tests as critical for production"
    )
    config.addinivalue_line(
        "markers", "integration: marks tests as integration tests (cross-service)"
    )
    config.addinivalue_line(
        "markers", "e2e: marks tests as end-to-end tests"
    )
    config.addinivalue_line(
        "markers", "slow: marks tests as slow (deselect with '-m \"not slow\"')"
    )


@pytest.fixture(scope="session")
def gateway_url():
    """Get the gateway URL from environment or use default"""
    return os.getenv("GATEWAY_URL", "http://localhost:8080")


@pytest.fixture(scope="session")
def test_timeout():
    """Get test timeout from environment or use default"""
    return int(os.getenv("TEST_TIMEOUT", "30"))
