import re
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
COMPOSE = ROOT / "infrastructure/local/podman/podman-compose.yml"
APICAST_CONFIG = ROOT / "infrastructure/local/podman/config/apicast-config.json"
KAFKA_CONFIG = ROOT / "infrastructure/local/podman/config/kafka-server.properties"
INIT_DB = ROOT / "infrastructure/local/podman/config/init-db.sql"


class PodmanComposeParityTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.document = COMPOSE.read_text()

    def test_contains_openshift_workloads(self):
        for service in (
            "lending-rules",
            "loan-origination-process",
            "biller-simulator",
            "va-simulator",
        ):
            self.assertRegex(self.document, rf"(?m)^  {service}:$")

    def test_each_created_database_has_an_explicit_local_grant(self):
        sql = INIT_DB.read_text()
        created = set(re.findall(r"(?m)^CREATE DATABASE ([a-z0-9_]+);$", sql))
        granted = set(
            re.findall(r"(?m)^GRANT ALL PRIVILEGES ON DATABASE ([a-z0-9_]+) TO payu;$", sql)
        )
        self.assertEqual(created | {"payu_account"}, granted)

    def test_uses_data_grid_resp_and_standard_networking(self):
        self.assertNotIn("podman_networks", self.document)
        self.assertNotIn("redis-native", self.document)
        self.assertIn(
            "registry.redhat.io/datagrid/datagrid-8-rhel9@sha256:"
            "66e76900551564dcb58ed37fb978fa27101849316dba1024a594bf027b25adc6",
            self.document,
        )
        self.assertRegex(self.document, r"(?m)^  payu-cache-resp:$")
        self.assertIn("PAYU_CACHE_REDIS_HOST: payu-cache-resp", self.document)
        self.assertIn('PAYU_CACHE_REDIS_PORT: "11222"', self.document)
        self.assertIn("PAYU_CACHE_REDIS_USERNAME: developer", self.document)

    def test_uses_openshift_service_dns_names(self):
        for service in (
            "payu-database-rw",
            "payu-cache-resp",
            "payu-kafka-kafka-bootstrap",
            "artemis",
            "payu-keycloak-service",
        ):
            self.assertRegex(self.document, rf"(?m)^  {service}:$")
        for local_only_host in ("postgres:5432", "infinispan:11222", "kafka:29092", "keycloak:8080"):
            self.assertNotIn(local_only_host, self.document)

    def test_matches_core_openshift_versions_and_ports(self):
        self.assertIn("image: docker.io/library/postgres:16.8", self.document)
        self.assertIn(
            "registry.redhat.io/amq-streams/kafka-41-rhel9@sha256:"
            "cf93e2ca48fa3596cfead6f01791f672108a160491088d4b02e8e203c4ae76ff",
            self.document,
        )
        self.assertTrue(KAFKA_CONFIG.is_file())
        self.assertIn("process.roles=broker,controller", KAFKA_CONFIG.read_text())
        self.assertIn("LOG_DIR: /tmp/kafka-logs", self.document)
        self.assertIn(
            "registry.redhat.io/amq7/amq-broker-rhel9@sha256:"
            "ec6a178d76a1521ea68ef312b43db6b952802d02431ad81f1b9bd107015c621d",
            self.document,
        )
        self.assertIn(
            "registry.redhat.io/rhbk/keycloak-rhel9@sha256:"
            "4224e2d27ea23af7b50e15ba6df41fc35c2a550ab1e108dd8debed9f7d23fb45",
            self.document,
        )
        self.assertRegex(self.document, r"(?s)  compliance-service:.*?ports:\n      - 8087:8080")
        self.assertRegex(self.document, r"(?s)  lending-service:.*?ports:\n      - 8010:8080")

    def test_keycloak_uses_current_bootstrap_and_hostname_options(self):
        keycloak = re.search(
            r"(?s)^  payu-keycloak-service:\n(.*?)(?=^  apicast:)",
            self.document,
            re.MULTILINE,
        )
        self.assertIsNotNone(keycloak)
        config = keycloak.group(1)
        self.assertNotIn("KEYCLOAK_ADMIN:", config)
        self.assertNotIn("KEYCLOAK_ADMIN_PASSWORD:", config)
        self.assertNotIn("KC_HOSTNAME_URL:", config)
        self.assertNotIn("KC_HOSTNAME_ADMIN_URL:", config)
        self.assertIn("KC_BOOTSTRAP_ADMIN_USERNAME:", config)
        self.assertIn("KC_BOOTSTRAP_ADMIN_PASSWORD:", config)
        self.assertIn("KC_HOSTNAME: http://payu-keycloak-service:8080", config)
        self.assertIn('LIQUIBASE_ANALYTICS_ENABLED: "false"', config)

    def test_local_app_anchor_is_hardened_and_builds_locally(self):
        anchor = re.search(r"(?s)^x-app-defaults: &app-defaults\n(.*?)\nservices:", self.document)
        self.assertIsNotNone(anchor)
        defaults = anchor.group(1)
        for expected in (
            "pull_policy: build",
            'user: "1001"',
            "read_only: true",
            "cap_drop:",
            "- ALL",
            "tmpfs:",
            "- /tmp",
        ):
            self.assertIn(expected, defaults)

    def test_external_anchor_pulls_always_and_heavy_tools_are_profiled(self):
        self.assertRegex(self.document, r"(?s)x-infra-defaults: &infra-defaults\n.*?pull_policy: always")
        for name in ("sonarqube", "trivy", "zap", "gitleaks", "nuclei", "k6", "syft", "grype"):
            self.assertRegex(self.document, rf"(?s)  {name}:.*?profiles:\n      - devsecops")

    def test_red_hat_apicast_is_available_for_local_api_management(self):
        self.assertIn(
            "registry.redhat.io/3scale-amp2/apicast-gateway-rhel8@sha256:"
            "f4cda75503b72c59922e3bdb59ad0d02cf1ff7d64f19455b4fa411dc2519ad55",
            self.document,
        )
        self.assertRegex(
            self.document,
            r"(?ms)^  apicast:.*?profiles:\n      - api-management.*?THREESCALE_CONFIG_FILE:",
        )
        self.assertIn("APICAST_CONFIGURATION_LOADER: lazy", self.document)
        self.assertIn('APICAST_CONFIGURATION_CACHE: "0"', self.document)
        self.assertTrue(APICAST_CONFIG.is_file())
        self.assertIn('"api_backend": "http://gateway-service:8080"', APICAST_CONFIG.read_text())


if __name__ == "__main__":
    unittest.main()
