import pytest
import os
import sys
import requests

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


@pytest.fixture(scope="session", autouse=True)
def check_gateway_available(gateway_url):
    """Skip all tests if the gateway is not reachable.
    
    Gateway is Quarkus-based, so health endpoint is /q/health (not /actuator/health).
    Falls back to /actuator/health for Spring Boot gateways.
    """
    health_paths = ["/q/health", "/actuator/health"]
    last_error = None
    for path in health_paths:
        try:
            resp = requests.get(f"{gateway_url}{path}", timeout=5)
            if resp.status_code < 500:
                return  # Gateway is reachable
            last_error = f"HTTP {resp.status_code}"
        except requests.ConnectionError:
            last_error = "connection refused"
        except requests.Timeout:
            last_error = "timeout"
    
    if last_error in ("connection refused",):
        pytest.skip(
            f"Gateway not reachable at {gateway_url}. "
            "Start services first: make podman-test-up"
        )
    elif last_error == "timeout":
        pytest.skip(f"Gateway timed out at {gateway_url}")
    elif last_error:
        pytest.skip(f"Gateway unhealthy at {gateway_url} ({last_error})")
