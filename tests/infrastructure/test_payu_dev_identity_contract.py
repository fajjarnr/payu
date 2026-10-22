"""Executable contracts for the payu-dev identity + backup wiring.

Covers E2E-CLIENTSECRET-001 (ESO-generated Keycloak client secrets are the
single source, synced to payu-sso and converged into the realm by a PostSync
Job; no static duplicates) and CNPG-SNAPSHOT-001 (dev schedules volume
snapshot backups now that the csi-aws-vsc VolumeSnapshotClass exists).
"""

from __future__ import annotations

import subprocess
import unittest
from pathlib import Path

import yaml


REPO_ROOT = Path(__file__).resolve().parents[2]


def load_documents(path: Path) -> list[dict]:
    with path.open(encoding="utf-8") as stream:
        return [doc for doc in yaml.safe_load_all(stream) if isinstance(doc, dict)]


def load_rendered(stdout: str) -> list[dict]:
    return [doc for doc in yaml.safe_load_all(stdout) if isinstance(doc, dict)]


class PayuDevIdentityContractTest(unittest.TestCase):
    def render(self, relative_path: str) -> subprocess.CompletedProcess[str]:
        return subprocess.run(
            ["oc", "kustomize", relative_path],
            cwd=REPO_ROOT,
            capture_output=True,
            text=True,
            timeout=120,
            check=False,
        )

    def test_dev_identity_syncs_eso_client_secrets_to_sso(self) -> None:
        result = self.render("infrastructure/platform/identity/overlays/dev")
        self.assertEqual(0, result.returncode, result.stderr)
        documents = load_rendered(result.stdout)

        sync = next(
            item
            for item in documents
            if item.get("kind") == "ExternalSecret"
            and item.get("metadata", {}).get("name")
            == "payu-keycloak-client-secrets"
        )
        self.assertEqual(
            {"payu-backend-client-secret", "payu-web-client-secret"},
            {entry["secretKey"] for entry in sync["spec"]["data"]},
        )
        self.assertTrue(
            all(
                entry["remoteRef"]["key"] == "payu-keycloak-client-secrets"
                for entry in sync["spec"]["data"]
            )
        )
        store = next(
            item
            for item in documents
            if item.get("kind") == "SecretStore"
            and item.get("metadata", {}).get("name")
            == "payu-dev-kubernetes-secrets"
        )
        self.assertEqual(
            "payu-dev", store["spec"]["provider"]["kubernetes"]["remoteNamespace"]
        )
        role = next(
            item
            for item in documents
            if item.get("kind") == "ClusterRole"
            and item.get("metadata", {}).get("name")
            == "payu-dev-keycloak-secret-reader"
        )
        resource_names = {
            resource_name
            for rule in role["rules"]
            for resource_name in rule.get("resourceNames", [])
        }
        self.assertIn("payu-keycloak-client-secrets", resource_names)

    def test_dev_identity_mounts_no_static_client_secrets(self) -> None:
        result = self.render("infrastructure/platform/identity/overlays/dev")
        self.assertEqual(0, result.returncode, result.stderr)
        static = [
            item["metadata"]["name"]
            for item in load_rendered(result.stdout)
            if item.get("kind") == "Secret"
            and item.get("metadata", {}).get("name")
            == "payu-keycloak-client-secrets"
            and (item.get("data") or item.get("stringData"))
        ]
        self.assertEqual([], static)

    def test_client_secret_sync_job_converges_realm_to_eso(self) -> None:
        job = next(
            doc
            for doc in load_documents(
                REPO_ROOT
                / "infrastructure/platform/identity/overlays/common/keycloak-client-secret-sync-job.yaml"
            )
            if doc.get("kind") == "Job"
        )
        self.assertEqual(
            "PostSync", job["metadata"]["annotations"]["argocd.argoproj.io/hook"]
        )
        pod = job["spec"]["template"]["spec"]
        mounted = {
            volume["secret"]["secretName"]
            for volume in pod["volumes"]
            if "secret" in volume
        }
        self.assertIn("payu-keycloak-admin", mounted)
        self.assertIn("payu-keycloak-client-secrets", mounted)
        script = pod["containers"][0]["command"][-1]
        for needle in (
            "/admin/realms/",
            "/clients/",
            "client-secret",
            "exit 1",
        ):
            self.assertIn(needle, script)

    def test_dev_auth_service_reads_web_client_secret_from_eso(self) -> None:
        result = self.render("infrastructure/workloads/overlays/payu-dev/auth-service")
        self.assertEqual(0, result.returncode, result.stderr)
        deploy = next(
            item
            for item in load_rendered(result.stdout)
            if item.get("kind") == "Deployment"
            and item.get("metadata", {}).get("name") == "auth-service"
        )
        env = next(
            item
            for item in deploy["spec"]["template"]["spec"]["containers"][0]["env"]
            if item["name"] == "PAYU_KEYCLOAK_WEB_CLIENT_SECRET"
        )
        self.assertNotIn("value", env)
        self.assertEqual(
            "payu-keycloak-client-secrets",
            env["valueFrom"]["secretKeyRef"]["name"],
        )
        self.assertEqual(
            "payu-web-client-secret", env["valueFrom"]["secretKeyRef"]["key"]
        )

    def test_dev_data_schedules_volume_snapshot_backups(self) -> None:
        result = self.render("infrastructure/platform/data/overlays/dev")
        self.assertEqual(0, result.returncode, result.stderr)
        backup = next(
            item
            for item in load_rendered(result.stdout)
            if item.get("kind") == "ScheduledBackup"
        )
        self.assertEqual("payu-database", backup["spec"]["cluster"]["name"])
        self.assertEqual("volumeSnapshot", backup["spec"]["method"])



    def test_dev_workload_overlays_are_single_apply_units(self) -> None:
        # OVERLAY-STRUCT-001: one `oc apply -k` of EITHER the root overlay or
        # any per-service overlay must carry the same Deployment env contract.
        # Root-only env would be stripped by a per-service apply and vice
        # versa (outside the documented per-service-only allowlist).
        allowed_svc_only = {
            "MANAGEMENT_ENDPOINT_HEALTH_SHOW_DETAILS",
            "PAYU_KEYCLOAK_WEB_CLIENT_ID",
            "PAYU_KEYCLOAK_WEB_CLIENT_SECRET",
            "OIDC_ISSUER",
            "WS_TICKET_SECRET",
            "SERVICES_ANALYTICS_URL",
            "NEXT_PUBLIC_WS_URL",
        }
        root = self.render("infrastructure/workloads/overlays/payu-dev")
        self.assertEqual(0, root.returncode, root.stderr)
        root_env: dict[str, set[str]] = {}
        for item in load_rendered(root.stdout):
            if item.get("kind") != "Deployment":
                continue
            name = item["metadata"]["name"]
            root_env[name] = {
                entry["name"]
                for entry in item["spec"]["template"]["spec"]["containers"][0].get("env", [])
                if "name" in entry
            }
        overlay = REPO_ROOT / "infrastructure/workloads/overlays/payu-dev"
        checked = 0
        for child in sorted(overlay.iterdir()):
            if not child.is_dir() or not (child / "kustomization.yaml").exists():
                continue
            if child.name == "components":
                continue
            result = self.render(f"infrastructure/workloads/overlays/payu-dev/{child.name}")
            self.assertEqual(0, result.returncode, f"{child.name}: {result.stderr}")
            for item in load_rendered(result.stdout):
                if item.get("kind") != "Deployment":
                    continue
                name = item["metadata"]["name"]
                self.assertIn(name, root_env, f"{name} per-service not in root render")
                svc_names = {
                    entry["name"]
                    for entry in item["spec"]["template"]["spec"]["containers"][0].get("env", [])
                    if "name" in entry
                }
                self.assertEqual(
                    set(), root_env[name] - svc_names, f"{name} root-only env"
                )
                self.assertEqual(
                    set(), svc_names - root_env[name] - allowed_svc_only,
                    f"{name} per-service-only env",
                )
                checked += 1
        self.assertGreaterEqual(checked, 30)

    def test_dev_mesh_has_deny_by_default_authorization(self) -> None:
        # MESH-003: every meshed workload in payu-dev must sit behind a
        # namespace-wide deny-by-default AuthorizationPolicy. The policy is
        # intentionally "empty spec" (deny all unmatched): ALLOW exceptions
        # for the mesh gateway + same-namespace callers live in the same
        # overlay file, and plaintext callers (router direct Routes,
        # Prometheus scrape) are covered by design, not by carve-outs.
        result = self.render("infrastructure/platform/mesh/overlays/payu-dev")
        self.assertEqual(0, result.returncode, result.stderr)
        documents = load_rendered(result.stdout)
        deny = next(
            item
            for item in documents
            if item.get("kind") == "AuthorizationPolicy"
            and item.get("metadata", {}).get("name") == "deny-all"
        )
        self.assertEqual("payu-dev", deny["metadata"]["namespace"])
        self.assertNotIn("selector", deny["spec"])
        self.assertEqual({}, deny["spec"])
        allow = next(
            item
            for item in documents
            if item.get("kind") == "AuthorizationPolicy"
            and item.get("metadata", {}).get("name") == "allow-mesh-and-gateway"
        )
        namespaces = {
            src.get("namespaces", [None])[0]
            for rule in allow["spec"]["rules"]
            for src in rule.get("from", [{}])
            for src in [src.get("source", {})]
        }
        self.assertIn("payu-dev", namespaces)
        self.assertIn("payu-mesh-gateway", namespaces)


    def test_prod_ingress_gateway_sits_behind_apicast(self) -> None:
        # MESH-004: prod mesh gateway is a ClusterIP backend behind APIcast,
        # never a public edge. Contract: (a) Gateway carries the ClusterIP
        # pin annotation, (b) NO OpenShift Route exists in the package (no
        # public hostname, no TLS edge at the router — APIcast owns the edge),
        # (c) prod HTTPRoute attaches to the prod gateway and routes only the
        # public API prefixes to gateway-service, setting X-PayU-Environment.
        result = self.render("infrastructure/platform/mesh/ingress-prod")
        self.assertEqual(0, result.returncode, result.stderr)
        documents = load_rendered(result.stdout)
        kinds = {item.get("kind") for item in documents}
        self.assertNotIn("Route", kinds)
        gateway = next(
            item for item in documents if item.get("kind") == "Gateway"
        )
        self.assertEqual("istio", gateway["spec"]["gatewayClassName"])
        self.assertEqual(
            "ClusterIP",
            gateway["metadata"]["annotations"][
                "networking.istio.io/service-type"
            ],
        )
        route = next(
            item for item in documents if item.get("kind") == "HTTPRoute"
        )
        parents = route["spec"]["parentRefs"]
        self.assertEqual(gateway["metadata"]["name"], parents[0]["name"])
        self.assertEqual(
            gateway["metadata"]["namespace"], parents[0]["namespace"]
        )
        backend = route["spec"]["rules"][0]["backendRefs"][0]
        self.assertEqual("gateway-service", backend["name"])
        filters = route["spec"]["rules"][0].get("filters", [])
        headers = [
            setter["name"]
            for item in filters
            for setter in item.get("requestHeaderModifier", {}).get("set", [])
        ]
        self.assertIn("X-PayU-Environment", headers)


if __name__ == "__main__":
    unittest.main()
