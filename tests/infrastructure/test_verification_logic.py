"""
Unit tests for Docker Compose verification script logic
Tests the verification logic without requiring Docker access
"""

import pytest
import subprocess
import sys
from pathlib import Path
from unittest.mock import Mock, patch

# Add the tests/infrastructure directory to path
sys.path.insert(0, str(Path(__file__).parent))

from test_docker_compose_verification import DockerComposeVerification


class TestDockerComposeVerificationLogic:
    """Test the Docker Compose verification logic"""

    def test_initialization(self):
        """Test that DockerComposeVerification initializes correctly"""
        verifier = DockerComposeVerification()
        assert verifier.compose_file == "docker-compose.yml"
        # Count the actual services in the list (traefik was added later)
        assert len(verifier.required_services) == 19

    @patch('subprocess.run')
    def test_docker_available_success(self, mock_run):
        """Test Docker availability check when Docker is available"""
        verifier = DockerComposeVerification()
        
        # Mock successful docker and docker-compose commands
        mock_run.side_effect = [
            Mock(returncode=0),  # docker --version
            Mock(returncode=0),  # docker-compose --version
        ]
        
        result = verifier.check_docker_available()
        assert result is True
        assert mock_run.call_count == 2

    @patch('subprocess.run')
    def test_docker_available_fails(self, mock_run):
        """Test Docker availability check when Docker is not available"""
        verifier = DockerComposeVerification()
        
        # Mock failed docker command
        mock_run.return_value = Mock(returncode=1)
        
        result = verifier.check_docker_available()
        assert result is False

    @patch('subprocess.run')
    def test_run_command_success(self, mock_run):
        """Test running a command successfully"""
        verifier = DockerComposeVerification()
        
        mock_run.return_value = Mock(
            returncode=0,
            stdout="output",
            stderr=""
        )
        
        code, stdout, stderr = verifier.run_command(["echo", "test"])
        
        assert code == 0
        assert stdout == "output"
        assert stderr == ""

    @patch('subprocess.run')
    def test_run_command_timeout(self, mock_run):
        """Test running a command that times out"""
        verifier = DockerComposeVerification()
        
        mock_run.side_effect = subprocess.TimeoutExpired("cmd", 300)
        
        code, stdout, stderr = verifier.run_command(["sleep", "999"])
        
        assert code == -1
        assert "timed out" in stderr

    @patch('subprocess.run')
    def test_run_command_exception(self, mock_run):
        """Test running a command that raises an exception"""
        verifier = DockerComposeVerification()
        
        mock_run.side_effect = Exception("command failed")
        
        code, stdout, stderr = verifier.run_command(["invalid", "command"])
        
        assert code == -1
        assert "command failed" in stderr

    @patch('subprocess.run')
    def test_stop_existing_containers(self, mock_run):
        """Test stopping existing containers"""
        verifier = DockerComposeVerification()
        
        mock_run.return_value = Mock(returncode=0)
        
        result = verifier.stop_existing_containers()
        
        assert result is True
        mock_run.assert_called_once_with(
            ["docker-compose", "-f", "docker-compose.yml", "down", "-v"],
            capture_output=True,
            text=True,
            timeout=300
        )

    @patch('subprocess.run')
    def test_start_infrastructure_success(self, mock_run):
        """Test starting infrastructure successfully"""
        verifier = DockerComposeVerification()
        
        mock_run.return_value = Mock(returncode=0)
        
        result = verifier.start_infrastructure()
        
        assert result is True
        mock_run.assert_called_once_with(
            ["docker-compose", "-f", "docker-compose.yml", "up", "-d", "--build"],
            capture_output=True,
            text=True,
            timeout=300
        )

    @patch('subprocess.run')
    def test_start_infrastructure_failure(self, mock_run):
        """Test starting infrastructure when it fails"""
        verifier = DockerComposeVerification()
        
        mock_run.return_value = Mock(
            returncode=1,
            stderr="Build failed"
        )
        
        result = verifier.start_infrastructure()
        
        assert result is False

    @patch('subprocess.run')
    def test_stop_infrastructure(self, mock_run):
        """Test stopping infrastructure"""
        verifier = DockerComposeVerification()
        
        mock_run.return_value = Mock(returncode=0)
        
        result = verifier.stop_infrastructure()
        
        assert result is True
        mock_run.assert_called_once_with(
            ["docker-compose", "-f", "docker-compose.yml", "down", "-v"],
            capture_output=True,
            text=True,
            timeout=300
        )

    @patch('subprocess.run')
    def test_verify_cleanup_success(self, mock_run):
        """Test verifying cleanup when all containers are stopped"""
        verifier = DockerComposeVerification()
        
        # Mock empty ps output (no containers running)
        mock_run.return_value = Mock(
            returncode=0,
            stdout="",
            stderr=""
        )
        
        result = verifier.verify_cleanup()
        
        assert result is True

    @patch('subprocess.run')
    def test_verify_cleanup_failure(self, mock_run):
        """Test verifying cleanup when containers are still running"""
        verifier = DockerComposeVerification()
        
        # Mock ps output with running containers
        mock_run.return_value = Mock(
            returncode=0,
            stdout="abc123\n",
            stderr=""
        )
        
        result = verifier.verify_cleanup()
        
        assert result is False

    def test_required_services_list(self):
        """Test that all required services are listed"""
        verifier = DockerComposeVerification()
        
        expected_services = [
            "postgres",
            "redis",
            "zookeeper",
            "kafka",
            "kafka-ui",
            "keycloak",
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
            "analytics-service",
            "traefik"
        ]
        
        assert verifier.required_services == expected_services

    @patch('subprocess.run')
    def test_verify_database_connectivity_mock(self, mock_run):
        """Test database connectivity verification with mocked commands"""
        verifier = DockerComposeVerification()
        
        # Mock successful postgres commands
        mock_run.return_value = Mock(
            returncode=0,
            stdout="""
                           List of databases
       Name    |  Owner   | Encoding | Collate |  Ctype
---------------+----------+----------+---------+-------
 payu_account  | payu     | UTF8     | en_US.UTF-8 | en_US.UTF-8
 payu_auth     | payu     | UTF8     | en_US.UTF-8 | en_US.UTF-8
 payu_transaction| payu   | UTF8     | en_US.UTF-8 | en_US.UTF-8
 payu_wallet   | payu     | UTF8     | en_US.UTF-8 | en_US.UTF-8
 payu_notification| payu   | UTF8     | en_US.UTF-8 | en_US.UTF-8
 payu_billing  | payu     | UTF8     | en_US.UTF-8 | en_US.UTF-8
 payu_kyc     | payu     | UTF8     | en_US.UTF-8 | en_US.UTF-8
 payu_analytics| payu     | UTF8     | en_US.UTF-8 | en_US.UTF-8
 payu_bifast   | payu     | UTF8     | en_US.UTF-8 | en_US.UTF-8
 payu_dukcapil | payu     | UTF8     | en_US.UTF-8 | en_US.UTF-8
 payu_qris     | payu     | UTF8     | en_US.UTF-8 | en_US.UTF-8
""",
            stderr=""
        )
        
        result = verifier.verify_database_connectivity()
        
        assert result is True
        assert mock_run.call_count >= 2

    @patch('subprocess.run')
    def test_verify_kafka_connectivity_mock(self, mock_run):
        """Test Kafka connectivity verification with mocked commands"""
        verifier = DockerComposeVerification()
        
        # Mock successful kafka command
        mock_run.return_value = Mock(
            returncode=0,
            stdout="",
            stderr=""
        )
        
        result = verifier.verify_kafka_connectivity()
        
        assert result is True
        mock_run.assert_called_once_with(
            [
                "docker", "exec", "payu-kafka",
                "kafka-topics", "--bootstrap-server", "localhost:9092", "--list"
            ],
            capture_output=True,
            text=True,
            timeout=300
        )


if __name__ == "__main__":
    pytest.main([__file__, "-v"])
