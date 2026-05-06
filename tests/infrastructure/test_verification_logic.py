"""
Unit tests for Docker Compose verification script logic
Tests the verification logic without requiring Docker access
"""

import pytest
import subprocess
import sys
from pathlib import Path
from unittest.mock import Mock, patch, MagicMock

# Add the tests/infrastructure directory to path
sys.path.insert(0, str(Path(__file__).parent))

from test_docker_infrastructure import TestDockerComposeVerification


class TestDockerComposeVerificationLogic:
    """Test the Docker Compose verification logic"""

    def test_initialization(self):
        """Test that TestDockerComposeVerification initializes correctly"""
        verifier = TestDockerComposeVerification()
        assert verifier.COMPOSE_FILE == "infrastructure/local/podman/podman-compose.yml"
        # 19 services: postgres, redis-native, infinispan, kafka, kafbat-ui, keycloak, vault,
        # bi-fast-simulator, dukcapil-simulator, qris-simulator,
        # account-service, auth-service, transaction-service, wallet-service,
        # billing-service, notification-service, gateway-service,
        # kyc-service, analytics-service
        assert len(verifier.REQUIRED_SERVICES) == 19

    @patch('subprocess.run')
    def test_run_command_success(self, mock_run):
        """Test running a command successfully"""
        verifier = TestDockerComposeVerification()

        mock_run.return_value = Mock(
            returncode=0,
            stdout="output",
            stderr=""
        )

        result = verifier.run_command(["echo", "test"])

        assert result.returncode == 0
        assert result.stdout == "output"
        assert result.stderr == ""

    @patch('subprocess.run')
    def test_run_command_timeout(self, mock_run):
        """Test running a command that times out"""
        verifier = TestDockerComposeVerification()

        mock_run.side_effect = subprocess.TimeoutExpired("cmd", 300)

        with pytest.raises(subprocess.TimeoutExpired):
            verifier.run_command(["sleep", "999"])

    @patch('subprocess.run')
    def test_run_command_failure(self, mock_run):
        """Test running a command that fails"""
        verifier = TestDockerComposeVerification()

        mock_run.return_value = Mock(
            returncode=1,
            stdout="",
            stderr="command failed"
        )

        result = verifier.run_command(["invalid", "command"])

        assert result.returncode == 1
        assert "command failed" in result.stderr

    def test_required_services_list(self):
        """Test that all required services are listed and zookeeper is excluded"""
        verifier = TestDockerComposeVerification()

        expected_services = [
            "postgres",
            "redis-native",
            "infinispan",
            "kafka",
            "kafbat-ui",
            "keycloak",
            "vault",
            "bi-fast-simulator",
            "dukcapil-simulator",
            "qris-simulator",
            "account-service",
            "auth-service",
            "transaction-service",
            "wallet-service",
            "billing-service",
            "notification-service",
            "gateway-service",
            "kyc-service",
            "analytics-service"
        ]

        assert verifier.REQUIRED_SERVICES == expected_services
        assert "zookeeper" not in verifier.REQUIRED_SERVICES

    def test_compose_file_path_not_root(self):
        """Test that COMPOSE_FILE points to deprecated archive, not project root"""
        verifier = TestDockerComposeVerification()
        assert verifier.COMPOSE_FILE != "docker-compose.yml"
        assert "backend/docs/archive" in verifier.COMPOSE_FILE

    @patch('subprocess.run')
    def test_docker_available_success(self, mock_run):
        """Test Docker availability check when Docker is available"""
        verifier = TestDockerComposeVerification()

        mock_run.return_value = Mock(returncode=0, stdout="Docker version 24.0", stderr="")

        # Call the test method directly — it uses assert internally
        # If Docker is available, no AssertionError is raised
        verifier.test_docker_available()

    @patch('subprocess.run')
    def test_compose_down_cleanup(self, mock_run):
        """Test that compose_down fixture runs docker-compose down -v"""
        verifier = TestDockerComposeVerification()

        mock_run.return_value = Mock(returncode=0, stdout="", stderr="")

        result = verifier.run_command(
            ["docker-compose", "-f", verifier.COMPOSE_FILE, "down", "-v"],
            timeout=120
        )

        assert result.returncode == 0
        mock_run.assert_called_once_with(
            ["docker-compose", "-f", "backend/docs/archive/deprecated-docker/docker-compose.yml", "down", "-v"],
            capture_output=True,
            text=True,
            timeout=120
        )


if __name__ == "__main__":
    pytest.main([__file__, "-v"])
