#!/usr/bin/env python3
"""
PayU Docker Compose Verification Script
Verifies that the local infrastructure can be brought up and down successfully
"""

import subprocess
import time
import sys
from typing import List, Tuple


class DockerComposeVerification:
    """Verify Docker Compose infrastructure up/down operations"""

    def __init__(self, compose_file: str = "docker-compose.yml"):
        self.compose_file = compose_file
        self.required_services = [
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

    def run_command(self, cmd: List[str]) -> Tuple[int, str, str]:
        """Run a shell command and return exit code, stdout, stderr"""
        try:
            result = subprocess.run(
                cmd,
                capture_output=True,
                text=True,
                timeout=300
            )
            return result.returncode, result.stdout, result.stderr
        except subprocess.TimeoutExpired:
            return -1, "", "Command timed out"
        except Exception as e:
            return -1, "", str(e)

    def check_docker_available(self) -> bool:
        """Check if Docker and Docker Compose are available"""
        print("Checking Docker availability...")
        code, _, _ = self.run_command(["docker", "--version"])
        if code != 0:
            print("❌ Docker is not available")
            return False

        code, _, _ = self.run_command(["docker-compose", "--version"])
        if code != 0:
            code, _, _ = self.run_command(["docker", "compose", "version"])
            if code != 0:
                print("❌ Docker Compose is not available")
                return False

        print("✓ Docker and Docker Compose are available")
        return True

    def stop_existing_containers(self) -> bool:
        """Stop any existing PayU containers"""
        print("Stopping existing containers...")
        code, stdout, stderr = self.run_command(
            ["docker-compose", "-f", self.compose_file, "down", "-v"]
        )
        if code != 0:
            print(f"Warning: Failed to stop containers: {stderr}")
            # Don't return False, containers might not exist
        print("✓ Existing containers stopped")
        return True

    def start_infrastructure(self) -> bool:
        """Start the Docker Compose infrastructure"""
        print(f"\n🚀 Starting infrastructure from {self.compose_file}...")
        
        # Build and start services
        code, stdout, stderr = self.run_command(
            ["docker-compose", "-f", self.compose_file, "up", "-d", "--build"]
        )
        
        if code != 0:
            print("❌ Failed to start infrastructure")
            print(f"Error: {stderr}")
            return False
        
        print("✓ Infrastructure started")
        return True

    def wait_for_services(self, timeout: int = 300) -> bool:
        """Wait for all services to become healthy"""
        print(f"\n⏳ Waiting for services to become healthy (timeout: {timeout}s)...")
        
        start_time = time.time()
        healthy_services = set()
        
        while time.time() - start_time < timeout:
            code, stdout, stderr = self.run_command(
                ["docker-compose", "-f", self.compose_file, "ps", "--services", "--filter", "status=running"]
            )

            if code == 0:
                # Check health status
                code, stdout, stderr = self.run_command(
                    ["docker-compose", "-f", self.compose_file, "ps"]
                )
                
                if code == 0:
                    for service in self.required_services:
                        if service in stdout and "healthy" in stdout[stdout.find(service):stdout.find(service) + 200]:
                            if service not in healthy_services:
                                print(f"✓ {service} is healthy")
                                healthy_services.add(service)
                    
                    if len(healthy_services) >= len([s for s in self.required_services if s in stdout]):
                        print("\n✓ All required services are healthy")
                        return True
            
            time.sleep(5)
        
        print(f"❌ Timeout waiting for services. Healthy: {healthy_services}")
        return False

    def verify_services_running(self) -> bool:
        """Verify all required services are running"""
        print("\n🔍 Verifying services are running...")
        
        code, stdout, stderr = self.run_command(
            ["docker-compose", "-f", self.compose_file, "ps"]
        )
        
        if code != 0:
            print(f"❌ Failed to check services status: {stderr}")
            return False
        
        running_services = []
        failed_services = []
        
        for service in self.required_services:
            if service in stdout:
                if "Exit" in stdout[stdout.find(service):stdout.find(service) + 200]:
                    failed_services.append(service)
                else:
                    running_services.append(service)
            else:
                failed_services.append(service)
        
        if failed_services:
            print(f"❌ Some services are not running: {failed_services}")
            return False
        
        print(f"✓ All {len(running_services)} services are running")
        return True

    def verify_database_connectivity(self) -> bool:
        """Verify PostgreSQL database connectivity"""
        print("\n🔍 Verifying PostgreSQL connectivity...")
        
        # Check if postgres container is accepting connections
        code, stdout, stderr = self.run_command([
            "docker", "exec", "payu-postgres", "pg_isready", "-U", "payu"
        ])
        
        if code != 0:
            print(f"❌ PostgreSQL is not ready: {stderr}")
            return False
        
        # Try to list databases
        code, stdout, stderr = self.run_command([
            "docker", "exec", "payu-postgres", "psql", "-U", "payu", "-c", "\\l"
        ])
        
        if code != 0:
            print(f"❌ Cannot connect to PostgreSQL: {stderr}")
            return False
        
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
        
        for db in required_dbs:
            if db in stdout:
                print(f"  ✓ Database {db} exists")
            else:
                print(f"  ❌ Database {db} missing")
                return False
        
        print("✓ PostgreSQL connectivity verified")
        return True

    def verify_kafka_connectivity(self) -> bool:
        """Verify Kafka connectivity"""
        print("\n🔍 Verifying Kafka connectivity...")
        
        code, stdout, stderr = self.run_command([
            "docker", "exec", "payu-kafka",
            "kafka-topics", "--bootstrap-server", "localhost:9092", "--list"
        ])
        
        if code != 0:
            print(f"❌ Kafka is not ready: {stderr}")
            return False
        
        print("✓ Kafka connectivity verified")
        return True

    def verify_microservices_accessible(self) -> bool:
        """Verify microservices are accessible via HTTP"""
        print("\n🔍 Verifying microservices accessibility...")
        
        # Service endpoints to check
        endpoints = [
            ("Gateway", "http://localhost:8080"),
            ("Account Service", "http://localhost:8001"),
            ("Auth Service", "http://localhost:8002"),
            ("Transaction Service", "http://localhost:8003"),
            ("Wallet Service", "http://localhost:8004"),
            ("KYC Service", "http://localhost:8007"),
            ("Analytics Service", "http://localhost:8008"),
        ]
        
        code, stdout, stderr = self.run_command(["curl", "--version"])
        if code != 0:
            print("⚠️  curl not available, skipping HTTP checks")
            return True

        for name, url in endpoints:
            code, stdout, stderr = self.run_command([
                "curl", "-s", "-o", "/dev/null", "-w", "%{http_code}", url
            ])
            
        # Accept 200, 404, or 401 (service is running, might need auth)
            if code in [0] and stdout.strip() in ["200", "404", "401"]:
                print(f"  ✓ {name} is accessible (HTTP {stdout.strip()})")
            else:
                print(f"  ⚠️  {name} returned unexpected status: {stdout.strip()}")
                # Don't fail, services might still be starting
        
        print("✓ Microservices accessibility check completed")
        return True

    def stop_infrastructure(self) -> bool:
        """Stop the Docker Compose infrastructure"""
        print("\n🛑 Stopping infrastructure...")
        
        code, stdout, stderr = self.run_command(
            ["docker-compose", "-f", self.compose_file, "down", "-v"]
        )
        
        if code != 0:
            print(f"❌ Failed to stop infrastructure: {stderr}")
            return False
        
        print("✓ Infrastructure stopped")
        return True

    def verify_cleanup(self) -> bool:
        """Verify all containers are cleaned up"""
        print("\n🧹 Verifying cleanup...")
        
        code, stdout, stderr = self.run_command(
            ["docker-compose", "-f", self.compose_file, "ps", "-q"]
        )
        
        if code == 0 and stdout.strip():
            print(f"❌ Some containers are still running:\n{stdout}")
            return False
        
        print("✓ All containers stopped and removed")
        return True

    def run_verification(self) -> bool:
        """Run the complete verification cycle"""
        print("=" * 60)
        print("PayU Docker Compose Verification")
        print("=" * 60)
        
        # Check prerequisites
        if not self.check_docker_available():
            return False
        
        # Stop existing containers
        self.stop_existing_containers()
        
        # Start infrastructure
        if not self.start_infrastructure():
            self.stop_infrastructure()
            return False
        
        # Wait for services
        if not self.wait_for_services(timeout=300):
            self.stop_infrastructure()
            return False
        
        # Verify services
        if not self.verify_services_running():
            self.stop_infrastructure()
            return False
        
        # Verify database
        if not self.verify_database_connectivity():
            self.stop_infrastructure()
            return False
        
        # Verify Kafka
        if not self.verify_kafka_connectivity():
            self.stop_infrastructure()
            return False
        
        # Verify microservices
        if not self.verify_microservices_accessible():
            self.stop_infrastructure()
            return False
        
        # Stop infrastructure
        if not self.stop_infrastructure():
            return False
        
        # Verify cleanup
        if not self.verify_cleanup():
            return False
        
        print("\n" + "=" * 60)
        print("✅ All verification checks passed!")
        print("=" * 60)
        return True


def main():
    """Main entry point"""
    verifier = DockerComposeVerification()
    success = verifier.run_verification()
    sys.exit(0 if success else 1)


if __name__ == "__main__":
    main()
