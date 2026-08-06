import re
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
BACKEND = ROOT / "backend"
COMPOSE = ROOT / "infrastructure/local/podman/podman-compose.yml"
APICAST_CONFIG = ROOT / "infrastructure/local/podman/config/apicast-config.json"
KAFKA_CONFIG = ROOT / "infrastructure/local/podman/config/kafka-server.properties"
INIT_DB = ROOT / "infrastructure/local/podman/config/init-db.sql"
WEB_CONTAINERFILE = ROOT / "frontend/web-app/Containerfile"
MANAGE_SCRIPT = ROOT / "infrastructure/local/podman/containers/manage-podman.sh"
GATEWAY_LOCAL = BACKEND / "gateway-service/src/main/resources/application-local.yaml"


class PodmanComposeParityTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.document = COMPOSE.read_text()

    def service_config(self, service):
        match = re.search(
            rf"(?s)^  {re.escape(service)}:\n(.*?)(?=^  [a-z0-9-]+:|\Z)",
            self.document,
            re.MULTILINE,
        )
        self.assertIsNotNone(match, service)
        return match.group(1)

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

    def test_uses_infinispan_hotrod_dev_and_standard_networking(self):
        self.assertNotIn("podman_networks", self.document)
        self.assertNotIn("redis-native", self.document)
        self.assertNotIn("payu-cache-resp", self.document)
        self.assertNotIn("PAYU_CACHE_REDIS_", self.document)
        self.assertIn(
            "image: quay.io/infinispan/server:16.2.1",
            self.document,
        )
        self.assertRegex(self.document, r"(?m)^  payu-cache:$")
        self.assertIn(
            "PAYU_CACHE_HOTROD_SERVER_LIST: ${PAYU_CACHE_HOTROD_SERVER_LIST:-payu-cache:11222}",
            self.document,
        )
        self.assertIn('PAYU_CACHE_HOTROD_USE_SSL: "false"', self.document)
        self.assertIn(
            "PAYU_CACHE_HOTROD_SNI_HOST_NAME: ${PAYU_CACHE_HOTROD_SNI_HOST_NAME:-payu-cache}",
            self.document,
        )

    def test_uses_openshift_service_dns_names(self):
        for service in (
            "payu-database-rw",
            "payu-cache",
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
            "1be81136da130940742f9836d09dadd6100474d943aad2d607c836811931e29e",
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
        self.assertNotIn("KC_HOSTNAME:", config)
        self.assertIn('KC_HOSTNAME_STRICT: "false"', config)
        self.assertIn('LIQUIBASE_ANALYTICS_ENABLED: "false"', config)

    def test_host_run_contract_exposes_postgres_and_keycloak(self):
        postgres = self.service_config("payu-database-rw")
        self.assertRegex(postgres, r"(?ms)^    ports:\n      - 5432:5432$")

        keycloak = self.service_config("payu-keycloak-service")
        self.assertNotIn("KC_HOSTNAME:", keycloak)
        self.assertIn('KC_HOSTNAME_STRICT: "false"', keycloak)

        profiles = list(BACKEND.glob("*/src/main/resources/application-local.y*"))
        profiles += list(BACKEND.glob("*/src/main/resources/application-dev.y*"))
        self.assertTrue(profiles)
        for profile in profiles:
            document = profile.read_text()
            self.assertNotIn("localhost:8080/realms/payu", document, profile)
            self.assertNotIn("KEYCLOAK_SERVER_URL:http://localhost:8080", document, profile)

    def test_host_run_profiles_reference_bootstrapped_databases(self):
        sql = INIT_DB.read_text()
        databases = set(re.findall(r"(?m)^CREATE DATABASE ([a-z0-9_]+);$", sql))
        databases.add("payu_account")

        profiles = list(BACKEND.glob("*/src/main/resources/application-local.y*"))
        profiles += list(BACKEND.glob("*/src/main/resources/application-dev.y*"))
        for profile in profiles:
            document = profile.read_text()
            referenced = re.findall(r"jdbc:postgresql://localhost:5432/([a-z0-9_]+)", document)
            for database in referenced:
                self.assertIn(database, databases, f"{profile}: {database}")

            if "primary:" in document and "datasource:\n" in document:
                has_standard_url = re.search(r"(?m)^    url: .*jdbc:postgresql", document)
                has_hikari_primary = re.search(
                    r"(?ms)^    primary:\n      hikari:\n        jdbc-url: .*jdbc:postgresql",
                    document,
                )
                self.assertTrue(has_standard_url or has_hikari_primary, profile)

    def test_host_run_profiles_use_compose_development_credentials(self):
        profiles = list(BACKEND.glob("*/src/main/resources/application-local.y*"))
        profiles += list(BACKEND.glob("*/src/main/resources/application-dev.y*"))
        for profile in profiles:
            document = profile.read_text()
            self.assertNotIn("payu_dev_secret_password_1234", document, profile)
            self.assertNotIn("payu-keycloak-dev-client-secret-12345", document, profile)
            self.assertNotIn("admin_dev_pass_1234", document, profile)

    def test_apps_and_api_management_have_distinct_host_ports(self):
        def published_ports(service):
            config = self.service_config(service)
            return set(re.findall(r'(?m)^      - "?(\d+):\d+"?$', config))

        self.assertTrue(published_ports("cms-service"))
        self.assertTrue(published_ports("apicast"))
        self.assertFalse(
            published_ports("cms-service") & published_ports("apicast"),
            "apps and api-management profiles must be runnable together",
        )

    def test_management_script_targets_the_canonical_compose_stack(self):
        script = MANAGE_SCRIPT.read_text()
        self.assertIn('infrastructure/local/podman/podman-compose.yml', script)
        self.assertIn('podman compose', script)
        self.assertIn('payu-database-rw', script)
        self.assertNotIn('podman play', script)
        self.assertNotIn('podman down --all', script)

    def test_local_app_anchor_is_hardened_and_builds_locally(self):
        anchor = re.search(r"(?s)^x-app-defaults: &app-defaults\n(.*?)^services:", self.document, re.MULTILINE)
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

    def test_web_app_uses_standard_internal_port(self):
        containerfile = WEB_CONTAINERFILE.read_text()
        self.assertIn("ENV PORT=8080", containerfile)
        self.assertIn("EXPOSE 8080", containerfile)
        self.assertNotIn("PLAYWRIGHT_BROWSERS_PATH", containerfile)
        self.assertNotIn("/app/e2e ./e2e", containerfile)
        runner = containerfile.split("# Stage 3: Minimal production runner", 1)[1]
        self.assertNotIn("/app/node_modules ./node_modules", runner)
        self.assertRegex(self.document, r"(?s)\n  web-app:.*?ports:\n      - 3001:8080")
        self.assertRegex(
            self.document,
            r"(?s)\n  web-app:.*?healthcheck:.*?http://localhost:8080/api/health",
        )

    def test_gateway_maps_mandatory_runtime_secrets(self):
        gateway = re.search(
            r"(?s)^  gateway-service:\n(.*?)(?=^  [a-z0-9-]+:)",
            self.document,
            re.MULTILINE,
        )
        self.assertIsNotNone(gateway)
        config = gateway.group(1)
        self.assertIn("JWT_SECRET: ${JWT_SECRET:-", config)
        self.assertIn("OIDC_CLIENT_SECRET: ${OIDC_CLIENT_SECRET:-", config)

    def test_gateway_has_a_host_run_local_profile(self):
        self.assertTrue(GATEWAY_LOCAL.is_file())
        profile = GATEWAY_LOCAL.read_text()
        self.assertIn("jdbc:postgresql://localhost:5432/payu_gateway", profile)
        self.assertIn("http://localhost:8099/realms/payu", profile)
        self.assertIn("dummy_secret_for_dev_only", profile)

    def test_fx_and_web_app_match_dev_runtime_contract(self):
        fx = self.service_config("fx-service")
        for name in (
            "SPRING_APPLICATION_NAME: fx-service",
            "SERVICE_VERSION: ${FX_SERVICE_VERSION:-1.8.106}",
            'SERVER_PORT: "8080"',
            "FX_PROVIDER_URL: ${FX_PROVIDER_URL:-}",
            "FX_PROVIDER_SOURCE: ${FX_PROVIDER_SOURCE:-}",
            "FX_PROVIDER_API_KEY: ${FX_PROVIDER_API_KEY:-}",
            "FX_PROVIDER_TIMEOUT: ${FX_PROVIDER_TIMEOUT:-3s}",
            "FX_PROVIDER_MAX_AGE: ${FX_PROVIDER_MAX_AGE:-15m}",
            "FX_DEFAULT_PROVIDER: ${FX_DEFAULT_PROVIDER:-UNCONFIGURED}",
        ):
            self.assertIn(name, fx)

        web = self.service_config("web-app")
        self.assertIn(
            "NEXT_PUBLIC_BASE_URL: ${NEXT_PUBLIC_BASE_URL:-http://localhost:3001}",
            web,
        )

    def test_security_environment_matches_workload_contracts(self):
        shared = re.search(
            r"(?s)^x-local-security-environment: &local-security-environment\n(.*?)(?=^x-app-defaults:)",
            self.document,
            re.MULTILINE,
        )
        self.assertIsNotNone(shared)
        self.assertIn("PAYU_SECURITY_ENCRYPTION_SALT: ${ENCRYPTION_SALT:-", shared.group(1))
        self.assertIn("WEBHOOK_SECURITY_SECRET: ${WEBHOOK_SECRET:-", shared.group(1))
        shared_security_services = (
            "account-service", "analytics-service", "api-portal-service", "auth-service",
            "backoffice-service", "billing-service", "cms-service", "compliance-service",
            "dispute-service", "fx-service", "gateway-service", "integration-service",
            "investment-service", "kyc-service", "lending-service",
            "loan-origination-process", "notification-service", "partner-service",
            "product-catalog-service", "promotion-service", "statement-service",
            "support-service", "transaction-service", "wallet-service",
        )
        for service in shared_security_services:
            config = self.service_config(service)
            self.assertIn("<<: *local-security-environment", config, service)

        for service in ("account-service", "gateway-service", "partner-service"):
            self.assertIn("JWT_SECRET: ${JWT_SECRET:-", self.service_config(service), service)
        self.assertIn(
            "PAYU_CALLBACK_SIGNATURE_SECRET: ${CALLBACK_SIGNATURE_SECRET:-",
            self.service_config("transaction-service"),
        )

    def test_artemis_consumers_have_exact_runtime_contract_and_dependency(self):
        common = ("ARTEMIS_HOST:", "ARTEMIS_USERNAME:", "ARTEMIS_PASSWORD:")
        for service in ("billing-service", "integration-service", "notification-service"):
            config = self.service_config(service)
            for name in common + ("ARTEMIS_URL:", "ARTEMIS_PORT:"):
                self.assertIn(name, config, service)
            self.assertRegex(config, r"(?ms)depends_on:.*?^      artemis:\n        condition: service_healthy")

        kyc = self.service_config("kyc-service")
        for name in common + (
            "ARTEMIS_STOMP_PORT:", "ARTEMIS_HEARTBEAT_SEND_MS:",
            "ARTEMIS_HEARTBEAT_RECEIVE_MS:",
        ):
            self.assertIn(name, kyc)
        self.assertRegex(kyc, r"(?ms)depends_on:.*?^      artemis:\n        condition: service_healthy")

    def test_compose_healthchecks_use_workload_liveness_endpoints(self):
        expected = {
            "product-catalog-service": "/actuator/health/liveness",
            "partner-service": "/actuator/health/liveness",
            "integration-service": "/actuator/health/liveness",
            "cms-service": "/actuator/health/liveness",
            "dispute-service": "/actuator/health/liveness",
            "notification-service": "/q/health/live",
            "api-portal-service": "/q/health/live",
            "gateway-service": "/q/health/live",
            "bi-fast-simulator": "/q/health/live",
            "dukcapil-simulator": "/q/health/live",
            "qris-simulator": "/q/health/live",
        }
        for service, endpoint in expected.items():
            self.assertIn(f"http://localhost:8080{endpoint}", self.service_config(service), service)

    def test_services_wait_for_mandatory_infrastructure(self):
        expected = {
            "auth-service": ("payu-kafka-kafka-bootstrap",),
            "api-portal-service": (
                "payu-database-rw", "payu-kafka-kafka-bootstrap", "payu-keycloak-service",
            ),
            "notification-service": ("artemis",),
        }
        for service, dependencies in expected.items():
            config = self.service_config(service)
            for dependency in dependencies:
                self.assertRegex(
                    config,
                    rf"(?ms)depends_on:.*?^      {re.escape(dependency)}:\n        condition: service_healthy",
                    service,
                )

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
