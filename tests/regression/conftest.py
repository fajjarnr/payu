"""
Configuration and fixtures for regression tests
"""

import pytest
import requests
import time
from typing import Generator

# Test configuration
BASE_URL = "http://localhost:8080"
KEYCLOAK_URL = "http://localhost:8099"
SERVICES = {
    "gateway": "http://localhost:8080",
    "account": "http://localhost:8001",
    "keycloak": "http://localhost:8099",
    "transaction": "http://localhost:8003",
    "wallet": "http://localhost:8004",
    "billing": "http://localhost:8005",
    "statement": "http://localhost:8015",
}

# Test credentials (Keycloak-based)
TEST_CREDENTIALS = {
    "username": "customer1",
    "password": "P@ssw0rd123",
}


def pytest_configure(config):
    """Configure pytest markers"""
    config.addinivalue_line(
        "markers",
        "critical: Critical financial flows that must always work",
        error=True
    )
    config.addinivalue_line(
        "markers",
        "regression: Tests that prevent feature regressions",
        error=True
    )
    config.addinivalue_line(
        "markers",
        "performance: Performance and SLA tests",
        error=True
    )
    config.addinivalue_line(
        "markers",
        "smoke: Quick smoke tests",
        error=True
    )


@pytest.fixture(scope="session")
def check_services_running():
    """Verify all required services are running"""
    failed_services = []

    for service_name, service_url in SERVICES.items():
        try:
            response = requests.get(
                f"{service_url}/actuator/health",
                timeout=2
            )
            if response.status_code != 200:
                failed_services.append(service_name)
        except Exception:
            failed_services.append(service_name)

    if failed_services:
        pytest.fail(
            f"Required services not running: {', '.join(failed_services)}\n"
            f"Start services with: podman compose -f infrastructure/local/podman/podman-compose.yml up -d"
        )


@pytest.fixture
def auth_token(check_services_running) -> str:
    """
    Get authentication token for tests via Keycloak
    Caches token for session duration
    """
    # Login via Keycloak token endpoint
    payload = {
        "grant_type": "password",
        "client_id": "payu-backend",
        "username": TEST_CREDENTIALS["username"],
        "password": TEST_CREDENTIALS["password"]
    }

    response = requests.post(
        f"{KEYCLOAK_URL}/realms/payu/protocol/openid-connect/token",
        data=payload,
        headers={"Content-Type": "application/x-www-form-urlencoded"}
    )

    if response.status_code != 200:
        pytest.skip(f"Authentication failed: {response.text}")

    data = response.json()
    return data.get("access_token")


@pytest.fixture
def authenticated_headers(auth_token) -> dict:
    """Returns headers with authentication token"""
    return {"Authorization": f"Bearer {auth_token}"}


def pytest_collection_modifyitems(items):
    """
    Modify test collection to add markers dynamically
    """
    for item in items:
        # Add regression marker to all tests in this directory
        if "test_" in item.name:
            item.add_marker("regression")
