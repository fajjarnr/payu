"""
Tests for Docker Compose infrastructure verification
"""

import pytest
import subprocess
import time


class TestDockerComposeVerification:
    """Test Docker Compose up/down operations"""

    COMPOSE_FILE = "docker-compose.yml"
    REQUIRED_SERVICES = [
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

    def run_command(self, cmd: list, timeout: int = 300) -> subprocess.CompletedProcess:
        """Helper to run commands"""
        return subprocess.run(
            cmd,
            capture_output=True,
            text=True,
            timeout=timeout
        )

    @pytest.fixture(scope="class", autouse=True)
    def compose_down(self):
        """Ensure docker-compose is down after tests"""
        yield
        try:
            self.run_command(["docker-compose", "-f", self.COMPOSE_FILE, "down", "-v"], timeout=120)
        except Exception:
            pass

    def test_docker_available(self):
        """Test that Docker is available"""
        result = self.run_command(["docker", "--version"])
        assert result.returncode == 0, "Docker is not installed or not accessible"

        result = self.run_command(["docker-compose", "--version"])
        if result.returncode != 0:
            result = self.run_command(["docker", "compose", "version"])
        assert result.returncode == 0, "Docker Compose is not installed or not accessible"

    def test_compose_up(self):
        """Test that docker-compose up works"""
        result = self.run_command(
            ["docker-compose", "-f", self.COMPOSE_FILE, "up", "-d"],
            timeout=600
        )
        assert result.returncode == 0, f"Failed to start docker-compose: {result.stderr}"

    def test_services_running(self, compose_up):
        """Test that all required services are running"""
        result = self.run_command(
            ["docker-compose", "-f", self.COMPOSE_FILE, "ps"]
        )
        assert result.returncode == 0, "Failed to get services status"

        stdout = result.stdout
        for service in self.REQUIRED_SERVICES:
            assert service in stdout, f"Service {service} not found in running services"

    def test_postgresql_ready(self):
        """Test that PostgreSQL is ready to accept connections"""
        max_retries = 30
        for i in range(max_retries):
            result = self.run_command([
                "docker", "exec", "payu-postgres",
                "pg_isready", "-U", "payu"
            ])
            if result.returncode == 0:
                break
            time.sleep(2)
        else:
            pytest.fail("PostgreSQL did not become ready")

    def test_databases_created(self):
        """Test that all required databases are created"""
        result = self.run_command([
            "docker", "exec", "payu-postgres",
            "psql", "-U", "payu", "-c", "\\l"
        ])
        assert result.returncode == 0, "Failed to list databases"

        required_dbs = [
            "payu_account",
            "payu_auth",
            "payu_transaction",
            "payu_wallet",
            "payu_notification",
            "payu_billing",
            "payu_kyc",
            "payu_analytics",
            "payu_bifast",
            "payu_dukcapil",
            "payu_qris"
        ]

        stdout = result.stdout
        for db in required_dbs:
            assert db in stdout, f"Database {db} not found"

    def test_redis_ready(self):
        """Test that Redis is ready"""
        max_retries = 30
        for i in range(max_retries):
            result = self.run_command([
                "docker", "exec", "payu-redis",
                "redis-cli", "ping"
            ])
            if result.returncode == 0 and "PONG" in result.stdout:
                break
            time.sleep(2)
        else:
            pytest.fail("Redis did not become ready")

    def test_kafka_ready(self):
        """Test that Kafka is ready"""
        max_retries = 60
        for i in range(max_retries):
            result = self.run_command([
                "docker", "exec", "payu-kafka",
                "kafka-topics", "--bootstrap-server", "localhost:9092", "--list"
            ])
            if result.returncode == 0:
                break
            time.sleep(2)
        else:
            pytest.fail("Kafka did not become ready")

    def test_keycloak_ready(self):
        """Test that Keycloak is ready"""
        max_retries = 60
        for i in range(max_retries):
            result = self.run_command([
                "docker", "exec", "payu-keycloak",
                "curl", "-s", "-o", "/dev/null", "-w", "%{http_code}",
                "http://localhost:8080/health"
            ])
            if result.returncode == 0 and result.stdout.strip() in ["200", "204"]:
                break
            time.sleep(2)
        else:
            pytest.fail("Keycloak did not become ready")

    def test_gateway_accessible(self):
        """Test that Gateway is accessible"""
        max_retries = 60
        for i in range(max_retries):
            result = self.run_command([
                "curl", "-s", "-o", "/dev/null", "-w", "%{http_code}",
                "http://localhost:8080"
            ])
            if result.returncode == 0 and result.stdout.strip() in ["200", "404", "401"]:
                break
            time.sleep(2)
        else:
            pytest.fail(f"Gateway did not become accessible (status: {result.stdout.strip()})")

    def test_account_service_accessible(self):
        """Test that Account Service is accessible"""
        max_retries = 60
        for i in range(max_retries):
            result = self.run_command([
                "curl", "-s", "-o", "/dev/null", "-w", "%{http_code}",
                "http://localhost:8001"
            ])
            if result.returncode == 0 and result.stdout.strip() in ["200", "404", "401"]:
                break
            time.sleep(2)
        else:
            pytest.fail(f"Account Service did not become accessible (status: {result.stdout.strip()})")

    def test_compose_down(self):
        """Test that docker-compose down works"""
        result = self.run_command(
            ["docker-compose", "-f", self.COMPOSE_FILE, "down", "-v"],
            timeout=120
        )
        assert result.returncode == 0, f"Failed to stop docker-compose: {result.stderr}"

        # Verify no containers are running
        result = self.run_command(
            ["docker-compose", "-f", self.COMPOSE_FILE, "ps", "-q"]
        )
        assert result.stdout.strip() == "", "Some containers are still running after down"

    @pytest.fixture
    def compose_up(self):
        """Fixture to ensure compose is up for tests"""
        try:
            self.run_command(
                ["docker-compose", "-f", self.COMPOSE_FILE, "up", "-d"],
                timeout=600
            )
            yield
        finally:
            self.run_command(
                ["docker-compose", "-f", self.COMPOSE_FILE, "down", "-v"],
                timeout=120
            )
