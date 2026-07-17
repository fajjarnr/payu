import unittest
from pathlib import Path
from xml.etree import ElementTree


ROOT = Path(__file__).resolve().parents[2]
BACKEND = ROOT / "backend"
MAVEN = {"m": "http://maven.apache.org/POM/4.0.0"}
EXPECTED_SERVICES = {
    "account-service", "auth-service", "backoffice-service", "billing-service",
    "cms-service", "compliance-service", "dispute-service", "fx-service",
    "gateway-service", "integration-service", "investment-service", "lending-service",
    "notification-service", "partner-service", "product-catalog-service",
    "promotion-service", "statement-service", "support-service", "transaction-service",
    "wallet-service",
}


class ArchUnitDependencyGovernanceTest(unittest.TestCase):
    def test_service_archunit_dependencies_use_parent_version_and_test_scope(self):
        violations = []
        discovered_services = set()
        parent = ElementTree.parse(BACKEND / "pom.xml").getroot()
        current_version = parent.findtext("m:properties/m:archunit.version", namespaces=MAVEN)

        for pom in sorted(BACKEND.glob("*/pom.xml")):
            root = ElementTree.parse(pom).getroot()
            inherits_backend_parent = (
                root.findtext("m:parent/m:groupId", namespaces=MAVEN) == "id.payu"
            )
            local_version = root.findtext("m:properties/m:archunit.version", namespaces=MAVEN)
            for dependency in root.findall(".//m:dependencies/m:dependency", MAVEN):
                if dependency.findtext("m:groupId", namespaces=MAVEN) != "com.tngtech.archunit":
                    continue

                discovered_services.add(pom.parent.name)
                version = dependency.findtext("m:version", namespaces=MAVEN)
                scope = dependency.findtext("m:scope", namespaces=MAVEN)
                if version != "${archunit.version}":
                    violations.append(f"{pom.relative_to(ROOT)}: version={version!r}")
                if scope != "test":
                    violations.append(f"{pom.relative_to(ROOT)}: scope={scope!r}")
                if not inherits_backend_parent and local_version != current_version:
                    violations.append(
                        f"{pom.relative_to(ROOT)}: archunit.version={local_version!r}"
                    )

        self.assertGreater(len(discovered_services), 0, "No service ArchUnit dependencies discovered")
        self.assertEqual(EXPECTED_SERVICES, discovered_services)
        self.assertEqual([], violations, "\n".join(violations))


if __name__ == "__main__":
    unittest.main()
