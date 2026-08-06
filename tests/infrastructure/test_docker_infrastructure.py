"""Read-only smoke checks for an already-running local Podman core stack."""

import os
import subprocess
import time
import unittest
import urllib.request
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
COMPOSE_FILE = ROOT / "infrastructure/local/podman/podman-compose.yml"
RUN_RUNTIME = os.getenv("PAYU_RUN_PODMAN_INTEGRATION") == "1"


class PodmanInfrastructureSmokeTest(unittest.TestCase):
    CORE_CONTAINERS = (
        "payu-database-rw",
        "payu-cache",
        "payu-kafka",
        "payu-artemis",
        "payu-keycloak",
    )

    def run_command(self, *command, timeout=30):
        return subprocess.run(command, capture_output=True, text=True, timeout=timeout)

    def test_compose_provider_available(self):
        result = self.run_command("podman", "compose", "version")
        self.assertEqual(result.returncode, 0, result.stderr)

    @unittest.skipUnless(RUN_RUNTIME, "set PAYU_RUN_PODMAN_INTEGRATION=1 after starting core infra")
    def test_core_containers_are_healthy(self):
        for container in self.CORE_CONTAINERS:
            deadline = time.monotonic() + 180
            while True:
                result = self.run_command(
                    "podman", "inspect", "--format", "{{.State.Health.Status}}", container
                )
                self.assertEqual(result.returncode, 0, f"{container}: {result.stderr}")
                status = result.stdout.strip()
                if status == "healthy" or time.monotonic() >= deadline:
                    break
                time.sleep(2)
            self.assertEqual(status, "healthy", container)

    @unittest.skipUnless(RUN_RUNTIME, "set PAYU_RUN_PODMAN_INTEGRATION=1 after starting core infra")
    def test_core_protocols_are_usable(self):
        checks = (
            (
                "postgres",
                ("podman", "exec", "payu-database-rw", "pg_isready", "-U", "payu", "-d", "payu_account"),
            ),
            (
                "data-grid",
                (
                    "podman", "exec", "payu-cache", "curl", "-fsS",
                    "http://localhost:11222/rest/v2/container/health/status",
                ),
            ),
            (
                "kafka",
                (
                    "podman", "exec", "payu-kafka",
                    "/opt/kafka/bin/kafka-broker-api-versions.sh",
                    "--bootstrap-server", "localhost:9092",
                ),
            ),
        )
        for name, command in checks:
            result = self.run_command(*command, timeout=60)
            self.assertEqual(result.returncode, 0, f"{name}: {result.stderr}")

        with urllib.request.urlopen(
            "http://localhost:8099/realms/payu/.well-known/openid-configuration",
            timeout=10,
        ) as response:
            self.assertEqual(response.status, 200)


if __name__ == "__main__":
    unittest.main()
