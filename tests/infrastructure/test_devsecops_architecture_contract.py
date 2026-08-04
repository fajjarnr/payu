"""Executable contracts for the production DevSecOps architecture."""

from __future__ import annotations

import subprocess
import unittest
from pathlib import Path

import yaml


REPO_ROOT = Path(__file__).resolve().parents[2]


def load_documents(path: Path) -> list[dict]:
    with path.open(encoding="utf-8") as stream:
        return [doc for doc in yaml.safe_load_all(stream) if isinstance(doc, dict)]


class DevSecOpsArchitectureContractTest(unittest.TestCase):
    def render(self, relative_path: str) -> subprocess.CompletedProcess[str]:
        return subprocess.run(
            ["oc", "kustomize", relative_path],
            cwd=REPO_ROOT,
            capture_output=True,
            text=True,
            timeout=120,
            check=False,
        )

    def test_payu_dev_overlay_renders(self) -> None:
        result = self.render("infrastructure/workloads/overlays/payu-dev")
        self.assertEqual(0, result.returncode, result.stderr)

    def test_observability_root_renders(self) -> None:
        result = self.render("infrastructure/platform/observability")
        self.assertEqual(0, result.returncode, result.stderr)

    def test_web_app_uses_internal_port_8080_end_to_end(self) -> None:
        web_app = REPO_ROOT / "infrastructure/workloads/base/web-app"
        deployment = load_documents(web_app / "deployment.yaml")[0]
        service = load_documents(web_app / "service.yaml")[0]
        route = load_documents(web_app / "route.yaml")[0]

        container = deployment["spec"]["template"]["spec"]["containers"][0]
        self.assertEqual(8080, container["ports"][0]["containerPort"])
        self.assertEqual(8080, service["spec"]["ports"][0]["port"])
        self.assertEqual(8080, service["spec"]["ports"][0]["targetPort"])
        self.assertEqual("http", route["spec"]["port"]["targetPort"])

    def test_account_service_runtime_image_is_digest_pinned_red_hat(self) -> None:
        containerfile = (
            REPO_ROOT / "backend/account-service/Containerfile"
        ).read_text(encoding="utf-8")
        self.assertIn(
            "FROM registry.redhat.io/ubi9/openjdk-25-runtime@sha256:",
            containerfile,
        )
        self.assertNotIn("registry.access.redhat.com", containerfile)

    def test_build_pipeline_runs_tests_and_blocks_vulnerable_images(self) -> None:
        pipeline = (
            REPO_ROOT / "infrastructure/platform/cicd/tekton/build-pipeline.yaml"
        ).read_text(encoding="utf-8")

        self.assertNotIn("-DskipTests", pipeline)
        self.assertIn('name: EXIT_CODE\n          value: "1"', pipeline)
        self.assertNotIn("onError: continue", pipeline)

    def test_analytics_ci_is_reproducible_without_external_services(self) -> None:
        workflow = (
            REPO_ROOT / ".github/workflows/analytics-tests.yml"
        ).read_text(encoding="utf-8")
        pyproject = (
            REPO_ROOT / "backend/analytics-service/pyproject.toml"
        ).read_text(encoding="utf-8")

        self.assertIn("SECRET_KEY: ci-test-${{ github.run_id }}-${{ github.sha }}", workflow)
        self.assertIn('ENABLE_TRACING: "false"', workflow)
        self.assertIn('ENABLE_METRICS: "false"', workflow)
        self.assertIn("Faker==30.0.0", workflow)
        self.assertIn(
            "asyncio_default_fixture_loop_scope = \"function\"",
            pyproject,
        )

    def test_mandatory_security_tasks_are_fail_closed(self) -> None:
        tasks = REPO_ROOT / "infrastructure/platform/cicd/tekton/tasks"
        mandatory = (
            "semgrep-task.yaml",
            "trufflehog-task.yaml",
            "trivy-task.yaml",
            "grype-task.yaml",
            "pact-verify-task.yaml",
            "security-scan-task.yaml",
        )

        for filename in mandatory:
            with self.subTest(task=filename):
                content = (tasks / filename).read_text(encoding="utf-8")
                self.assertNotIn("|| true", content)

        security_scan = (tasks / "security-scan-task.yaml").read_text(
            encoding="utf-8"
        )
        self.assertNotIn("failOnError=false", security_scan)
        self.assertNotIn("|| echo", security_scan)
        self.assertNotIn("threshold enforcement disabled", security_scan)
        self.assertIn(
            "$(workspaces.source.path)/scripts/security/spotbugs-filter.xml",
            security_scan,
        )
        self.assertIn(
            "com.github.spotbugs:spotbugs-maven-plugin:4.10.3.0:check",
            security_scan,
        )
        reactor_install = (
            'mvn -f "$(params.MAVEN_POM)" -pl "$(params.MODULE)" -am install'
        )
        module_scan = 'mvn -f "$(params.CONTEXT_DIR)/pom.xml"'
        self.assertIn(reactor_install, security_scan)
        self.assertIn(module_scan, security_scan)
        self.assertLess(security_scan.index(reactor_install), security_scan.index(module_scan))
        self.assertIn('-Dspotbugs.threshold="$SPOTBUGS_THRESHOLD"', security_scan)
        self.assertNotIn("-Dspotbugs.threshold=Low", security_scan)
        self.assertIn("reports/spotbugsXml.xml", security_scan)
        self.assertNotIn("spotbugs-report.xml", security_scan)
        self.assertNotIn("spotbugs-report.html", security_scan)
        self.assertNotIn("org.owasp:dependency-check-maven", security_scan)

        build_pipeline = load_documents(
            REPO_ROOT / "infrastructure/platform/cicd/tekton/build-pipeline.yaml"
        )[0]
        sast_task = next(
            task
            for task in build_pipeline["spec"]["tasks"]
            if task["name"] == "sast-sca-scan"
        )
        sast_params = {param["name"]: param["value"] for param in sast_task["params"]}
        self.assertEqual("$(params.service-path)", sast_params["MODULE"])
        self.assertTrue(
            (REPO_ROOT / "scripts/security/spotbugs-filter.xml").is_file()
        )

        trufflehog = load_documents(tasks / "trufflehog-task.yaml")[0]
        results_default = next(
            param["default"]
            for param in trufflehog["spec"]["params"]
            if param["name"] == "RESULTS"
        )
        self.assertEqual("verified,unknown", results_default)
        excluded_default = next(
            param["default"]
            for param in trufflehog["spec"]["params"]
            if param["name"] == "EXCLUDE_DETECTORS"
        )
        self.assertEqual("JDBC,Postgres", excluded_default)

    def test_tekton_prefers_digest_pinned_red_hat_images(self) -> None:
        tasks = REPO_ROOT / "infrastructure/platform/cicd/tekton/tasks"
        for manifest in tasks.glob("*.yaml"):
            content = manifest.read_text(encoding="utf-8")
            self.assertNotIn("registry.access.redhat.com", content, str(manifest))
            self.assertNotIn("quay.io/openshift/origin-cli", content, str(manifest))
            for line in content.splitlines():
                if "image:" in line and "registry.redhat.io" in line:
                    self.assertIn("@sha256:", line, f"mutable Red Hat image: {manifest}: {line}")

        mandatory_scanners = (
            "gitleaks-task.yaml",
            "semgrep-task.yaml",
            "trufflehog-task.yaml",
            "trivy-task.yaml",
            "syft-task.yaml",
            "grype-task.yaml",
            "zap-baseline-task.yaml",
            "zap-full-owasp-task.yaml",
        )
        for filename in mandatory_scanners:
            for line in (tasks / filename).read_text(encoding="utf-8").splitlines():
                if line.strip().startswith("image:") and "$(params." not in line:
                    self.assertIn(
                        "@sha256:", line, f"mutable scanner image: {filename}: {line}"
                    )

        maven_java21 = load_documents(tasks / "maven-java21-task.yaml")[0]
        image_default = next(
            param["default"] for param in maven_java21["spec"]["params"]
            if param["name"] == "MAVEN_IMAGE"
        )
        self.assertEqual(
            "registry.redhat.io/ubi9/openjdk-25@sha256:f35678fbb52016a6b61ea586ee4413e616a300cdf0969bf0bfc5b7c6791033d6",
            image_default,
        )

        security_scan = load_documents(tasks / "security-scan-task.yaml")[0]
        security_image = next(
            param["default"]
            for param in security_scan["spec"]["params"]
            if param["name"] == "MAVEN_IMAGE"
        )
        self.assertEqual(image_default, security_image)

    def test_every_deployed_tekton_task_image_is_digest_pinned(self) -> None:
        root = REPO_ROOT / "infrastructure/platform/cicd/tekton"
        kustomization = load_documents(root / "kustomization.yaml")[0]
        task_resources = [
            resource for resource in kustomization["resources"]
            if resource.startswith("tasks/")
        ]

        for resource in task_resources:
            for task in load_documents(root / resource):
                if task.get("kind") != "Task":
                    continue
                defaults = {
                    param["name"]: param.get("default")
                    for param in task["spec"].get("params", [])
                }
                for step in task["spec"].get("steps", []):
                    image = step["image"]
                    if image.startswith("$(params."):
                        param_name = image.removeprefix("$(params.").removesuffix(")")
                        image = defaults.get(param_name)
                    self.assertIsInstance(image, str, f"missing image default: {resource}")
                    self.assertIn(
                        "@sha256:", image, f"mutable deployed Task image: {resource}: {image}"
                    )

    def test_python_and_nextjs_builds_run_fail_closed_unit_tests(self) -> None:
        root = REPO_ROOT / "infrastructure/platform/cicd/tekton"
        pipeline = load_documents(root / "build-pipeline.yaml")[0]
        tasks = {task["name"]: task for task in pipeline["spec"]["tasks"]}
        self.assertEqual("pytest", tasks["python-unit-tests"]["taskRef"]["name"])
        self.assertEqual("node-unit-tests", tasks["node-unit-tests"]["taskRef"]["name"])
        self.assertTrue(
            {"python-unit-tests", "node-unit-tests"}
            <= set(tasks["build-push-direct"]["runAfter"])
        )

        pytest_task = load_documents(root / "tasks/pytest-task.yaml")[0]
        self.assertEqual(1, len(pytest_task["spec"]["steps"]))
        self.assertNotIn(
            "MARKERS", {param["name"] for param in pytest_task["spec"]["params"]}
        )
        pytest_script = pytest_task["spec"]["steps"][0]["script"]
        self.assertIn("PIPESTATUS[0]", pytest_script)
        self.assertIn("exit 1", pytest_script)
        self.assertNotIn("--user", pytest_script)

        node_task = load_documents(root / "tasks/node-test-task.yaml")[0]
        node_script = node_task["spec"]["steps"][0]["script"]
        self.assertIn("npm ci", node_script)
        self.assertIn("npm run test -- --run", node_script)

    def test_tekton_results_retains_audit_records_for_one_year(self) -> None:
        root = REPO_ROOT / "infrastructure/platform/cicd/tekton"
        kustomization = load_documents(root / "kustomization.yaml")[0]
        self.assertIn("pipelines.yaml", kustomization["resources"])

        config = load_documents(root / "pipelines.yaml")[0]
        self.assertEqual("TektonConfig", config["kind"])
        retention = config["spec"]["result"]["options"]["configMaps"][
            "tekton-results-config-results-retention-policy"
        ]["data"]
        self.assertEqual("365d", retention["defaultRetention"])
        self.assertFalse(config["spec"]["result"]["route_enabled"])
        self.assertEqual("beta", config["spec"]["pipeline"]["enable-api-fields"])
        self.assertEqual("stable", config["spec"]["trigger"]["enable-api-fields"])

    def test_tekton_chains_generates_a_supported_signing_key(self) -> None:
        config = load_documents(
            REPO_ROOT / "infrastructure/platform/cicd/tekton/pipelines.yaml"
        )[0]
        chain = config["spec"]["chain"]
        self.assertFalse(chain["disabled"])
        self.assertTrue(chain["generateSigningSecret"])
        self.assertEqual("simplesigning", chain["artifacts.oci.format"])
        self.assertEqual("in-toto", chain["artifacts.pipelinerun.format"])

    def test_cluster_config_renders_with_aesgcm_etcd_encryption(self) -> None:
        root = REPO_ROOT / "infrastructure/foundation/cluster-config"
        render = subprocess.run(
            ["oc", "kustomize", str(root)],
            check=False,
            capture_output=True,
            text=True,
        )
        self.assertEqual(0, render.returncode, render.stderr)
        documents = [item for item in yaml.safe_load_all(render.stdout) if item]
        api_server = next(item for item in documents if item["kind"] == "APIServer")
        self.assertEqual("cluster", api_server["metadata"]["name"])
        self.assertEqual("aesgcm", api_server["spec"]["encryption"]["type"])

    def test_non_dev_namespaces_do_not_allow_unrestricted_egress(self) -> None:
        root = REPO_ROOT / "infrastructure/foundation/namespaces/overlays/shared"
        intra = load_documents(root / "allow-intra-namespace.yaml")
        by_namespace = {item["metadata"]["namespace"]: item for item in intra}
        for namespace in ("payu-sit", "payu-uat", "payu-preprod", "payu"):
            with self.subTest(namespace=namespace):
                rules = by_namespace[namespace]["spec"]["egress"]
                self.assertNotIn({}, rules)
                self.assertEqual(
                    {},
                    rules[0]["to"][0]["podSelector"],
                )

        dns = {
            item["metadata"]["namespace"]
            for item in load_documents(root / "allow-dns-egress.yaml")
        }
        routers = {
            item["metadata"]["namespace"]
            for item in load_documents(root / "allow-openshift-router.yaml")
        }
        expected = {"payu-dev", "payu-sit", "payu-uat", "payu-preprod", "payu"}
        self.assertEqual(expected, dns)
        self.assertEqual(expected, routers)

    def test_build_service_account_can_push_only_to_dev(self) -> None:
        bindings = load_documents(
            REPO_ROOT
            / "infrastructure/foundation/namespaces/base/cicd-image-push-rolebindings.yaml"
        )
        image_builders = [
            item for item in bindings
            if item["roleRef"]["name"] == "system:image-builder"
        ]
        self.assertEqual(
            ["payu-dev"],
            [item["metadata"]["namespace"] for item in image_builders],
        )

    def test_contract_gate_rejects_missing_or_failed_pacts(self) -> None:
        deploy_pipeline = load_documents(
            REPO_ROOT / "infrastructure/platform/cicd/tekton/deploy-pipeline.yaml"
        )[0]
        contract_task = next(
            task
            for task in deploy_pipeline["spec"]["tasks"]
            if task["name"] == "contract-test-gate"
        )
        params = {param["name"]: param["value"] for param in contract_task["params"]}
        self.assertEqual("true", params["FAIL_ON_NO_PACTS"])
        self.assertEqual(
            [
                {
                    "input": "$(params.contract-tests-required)",
                    "operator": "in",
                    "values": ["true"],
                }
            ],
            contract_task["when"],
        )

    def test_supply_chain_controls_consume_builder_digest(self) -> None:
        pipeline = load_documents(
            REPO_ROOT / "infrastructure/platform/cicd/tekton/build-pipeline.yaml"
        )[0]
        tasks = {task["name"]: task for task in pipeline["spec"]["tasks"]}
        digest_reference = "$(tasks.resolve-image-digest.results.IMAGE_URL)"

        self.assertIn("resolve-image-digest", tasks)
        for task_name in (
            "trivy-image-scan",
            "rhacs-image-scan",
            "rhacs-image-check",
            "generate-sbom",
        ):
            with self.subTest(task=task_name):
                params = {
                    param["name"]: param["value"]
                    for param in tasks[task_name].get("params", [])
                }
                self.assertEqual(digest_reference, params["IMAGE"])

        buildah = load_documents(
            REPO_ROOT / "infrastructure/platform/cicd/tekton/tasks/buildah-task.yaml"
        )[0]
        self.assertNotIn(
            "IMAGE_URL", {result["name"] for result in buildah["spec"]["results"]}
        )
        self.assertNotIn(
            "IMAGE_DIGEST", {result["name"] for result in buildah["spec"]["results"]}
        )
        build_script = buildah["spec"]["steps"][0]["script"]
        self.assertIn(".tekton-results/image-url", build_script)

        resolver = load_documents(
            REPO_ROOT
            / "infrastructure/platform/cicd/tekton/tasks/resolve-image-digest-task.yaml"
        )[0]
        resolver_script = resolver["spec"]["steps"][0]["script"]
        self.assertIn("@sha256:", resolver_script)

        release = load_documents(
            REPO_ROOT
            / "infrastructure/platform/cicd/tekton/tasks/release-image-task.yaml"
        )[0]
        self.assertEqual(
            {"IMAGE_URL", "IMAGE_DIGEST"},
            {result["name"] for result in release["spec"]["results"]},
        )
        self.assertEqual(
            1001,
            release["spec"]["steps"][0]["securityContext"]["runAsUser"],
        )
        release_run = tasks["release-signed-image"]
        self.assertEqual(["grype-sbom-check"], release_run["runAfter"])
        release_params = {
            param["name"]: param["value"] for param in release_run["params"]
        }
        self.assertEqual(digest_reference, release_params["IMAGE"])

        tekton_config = load_documents(
            REPO_ROOT / "infrastructure/platform/cicd/tekton/pipelines.yaml"
        )[0]
        self.assertEqual("true", tekton_config["spec"]["chain"]["transparency.enabled"])
        self.assertEqual(
            "http://rekor-server.trusted-artifact-signer.svc",
            tekton_config["spec"]["chain"]["transparency.url"],
        )

    def test_trivy_reports_all_findings_and_uses_expiring_exceptions(self) -> None:
        trivy = load_documents(
            REPO_ROOT / "infrastructure/platform/cicd/tekton/tasks/trivy-task.yaml"
        )[0]
        script = trivy["spec"]["steps"][0]["args"][1]
        self.assertEqual(2, script.count("trivy image "))
        self.assertNotIn("--ignore-unfixed", script)
        self.assertEqual(1, script.count("--ignorefile"))

        exceptions_path = REPO_ROOT / "backend/account-service/.trivyignore.yaml"
        self.assertFalse((REPO_ROOT / ".trivyignore.yaml").exists())
        exceptions = load_documents(exceptions_path)[0]
        self.assertGreater(len(exceptions["vulnerabilities"]), 0)
        for exception in exceptions["vulnerabilities"]:
            self.assertIn("Owner platform-security", exception["statement"])
            self.assertGreaterEqual(str(exception["expired_at"]), "2026-08-21")

        pipeline = load_documents(
            REPO_ROOT / "infrastructure/platform/cicd/tekton/build-pipeline.yaml"
        )[0]
        trivy_run = next(
            task for task in pipeline["spec"]["tasks"]
            if task["name"] == "trivy-image-scan"
        )
        self.assertIn(
            {"name": "output", "workspace": "source"},
            trivy_run["workspaces"],
        )
        trivy_params = {
            param["name"]: param["value"] for param in trivy_run["params"]
        }
        self.assertEqual(
            "$(params.service-base-dir)/$(params.service-path)/.trivyignore.yaml",
            trivy_params["IGNORE_FILE"],
        )

    def test_gitleaks_covers_jdbc_credentials_before_trufflehog_excludes_jdbc(self) -> None:
        config = (REPO_ROOT / ".gitleaks.toml").read_text(encoding="utf-8")
        self.assertIn('id = "jdbc-embedded-credentials"', config)
        self.assertIn('id = "jdbc-password-parameter"', config)
        self.assertIn('id = "postgres-uri-credentials"', config)
        self.assertIn("replication-lag-service-monitor", config)

    def test_account_image_patches_os_and_postgresql_cves(self) -> None:
        containerfile = (
            REPO_ROOT / "backend/account-service/Containerfile"
        ).read_text(encoding="utf-8")
        backend_pom = (REPO_ROOT / "backend/pom.xml").read_text(encoding="utf-8")
        self.assertNotIn("microdnf update -y", containerfile)
        self.assertIn("glib2-2.68.4-19.el9_8.2", containerfile)
        self.assertIn("libacl-2.4.0-1.el9_8", containerfile)
        self.assertIn("python3-3.9.25-7.el9_8.2", containerfile)
        self.assertIn("<postgresql.version>42.7.12</postgresql.version>", backend_pom)

    def test_buildah_merges_red_hat_and_openshift_registry_auth(self) -> None:
        buildah = load_documents(
            REPO_ROOT / "infrastructure/platform/cicd/tekton/tasks/buildah-task.yaml"
        )[0]
        script = buildah["spec"]["steps"][0]["script"]
        self.assertIn("cp \"$(workspaces.dockerconfig.path)/.dockerconfigjson\"", script)
        self.assertIn("buildah login", script)
        self.assertIn("--password-stdin", script)

        pipeline_run = load_documents(
            REPO_ROOT
            / "infrastructure/platform/cicd/tekton/pipeline-runs/account-service-pipelinerun.yaml"
        )[0]
        dockerconfig = next(
            workspace for workspace in pipeline_run["spec"]["workspaces"]
            if workspace["name"] == "dockerconfig"
        )
        self.assertEqual(
            "redhat-registry-pull",
            dockerconfig["secret"]["secretName"],
        )

    def test_rhacs_registry_reader_is_least_privilege(self) -> None:
        acs = REPO_ROOT / "infrastructure/platform/security/acs"
        kustomization = load_documents(acs / "kustomization.yaml")[0]
        self.assertIn("registry-reader-rbac.yaml", kustomization["resources"])
        resources = load_documents(acs / "registry-reader-rbac.yaml")
        service_account = next(item for item in resources if item["kind"] == "ServiceAccount")
        role_binding = next(item for item in resources if item["kind"] == "RoleBinding")
        self.assertEqual("payu-dev", service_account["metadata"]["namespace"])
        self.assertEqual("system:image-puller", role_binding["roleRef"]["name"])
        self.assertEqual(
            ["rhacs-registry-reader"],
            [subject["name"] for subject in role_binding["subjects"]],
        )

    def test_grype_can_read_the_generated_sbom_workspace(self) -> None:
        task = load_documents(
            REPO_ROOT / "infrastructure/platform/cicd/tekton/tasks/grype-task.yaml"
        )[0]
        self.assertIn("source", {item["name"] for item in task["spec"]["workspaces"]})
        scan = next(step for step in task["spec"]["steps"] if step["name"] == "scan")
        self.assertEqual(["/grype"], scan["command"])
        self.assertEqual(
            ["$(params.TARGET)", "--fail-on", "$(params.FAIL_ON)"],
            scan["args"],
        )
        self.assertNotIn("script", scan)

        pipeline = load_documents(
            REPO_ROOT / "infrastructure/platform/cicd/tekton/build-pipeline.yaml"
        )[0]
        grype = next(
            item for item in pipeline["spec"]["tasks"]
            if item["name"] == "grype-sbom-check"
        )
        self.assertIn(
            {"name": "source", "workspace": "source"},
            grype["workspaces"],
        )

    def test_argocd_manifests_do_not_contain_secret_material(self) -> None:
        argocd = REPO_ROOT / "infrastructure/platform/cicd/argocd"
        violations: list[str] = []
        for manifest in argocd.rglob("*.yaml"):
            for document in load_documents(manifest):
                if document.get("kind") != "Secret":
                    continue
                if document.get("data") or document.get("stringData"):
                    violations.append(str(manifest.relative_to(REPO_ROOT)))

        self.assertEqual([], violations, f"tracked secret material: {violations}")

    def test_workload_base_does_not_deploy_static_credentials(self) -> None:
        kustomization = load_documents(
            REPO_ROOT / "infrastructure/workloads/base/kustomization.yaml"
        )[0]
        resources = set(kustomization.get("resources", []))
        self.assertNotIn("dev-env-secrets.yaml", resources)
        self.assertNotIn("dev-secrets-patch.yaml", resources)

    def test_dev_keycloak_client_credentials_are_generated_once_and_synced(self) -> None:
        workload_result = self.render("infrastructure/workloads/overlays/payu-dev")
        self.assertEqual(0, workload_result.returncode, workload_result.stderr)
        workload_documents = [
            item
            for item in yaml.safe_load_all(workload_result.stdout)
            if isinstance(item, dict)
        ]
        generator = next(
            item
            for item in workload_documents
            if item.get("kind") == "Password"
            and item.get("metadata", {}).get("name")
            == "payu-keycloak-client-secrets"
        )
        self.assertEqual(
            [
                "payu-backend-client-secret",
                "payu-web-client-secret",
            ],
            generator["spec"]["secretKeys"],
        )
        source = next(
            item
            for item in workload_documents
            if item.get("kind") == "ExternalSecret"
            and item.get("metadata", {}).get("name")
            == "payu-keycloak-client-secrets"
        )
        self.assertEqual("OnChange", source["spec"]["refreshPolicy"])
        self.assertEqual(
            "payu-keycloak-client-secrets",
            source["spec"]["target"]["name"],
        )

        identity_result = self.render("infrastructure/platform/identity/overlays/dev")
        self.assertEqual(0, identity_result.returncode, identity_result.stderr)
        identity_documents = [
            item
            for item in yaml.safe_load_all(identity_result.stdout)
            if isinstance(item, dict)
        ]
        sync = next(
            item
            for item in identity_documents
            if item.get("kind") == "ExternalSecret"
            and item.get("metadata", {}).get("name")
            == "payu-keycloak-client-secrets"
        )
        self.assertEqual(
            {"payu-backend-client-secret", "payu-web-client-secret"},
            {
                entry["secretKey"]
                for entry in sync["spec"]["data"]
            },
        )
        role = next(
            item
            for item in identity_documents
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

    def test_analytics_ci_supplies_required_test_environment_and_dependencies(self) -> None:
        workflow = (
            REPO_ROOT / ".github/workflows/analytics-tests.yml"
        ).read_text(encoding="utf-8")
        self.assertIn(
            "SECRET_KEY: ci-test-${{ github.run_id }}-${{ github.sha }}",
            workflow,
        )
        self.assertIn("Faker==30.0.0", workflow)

        pyproject = (
            REPO_ROOT / "backend/analytics-service/pyproject.toml"
        ).read_text(encoding="utf-8")
        self.assertIn(
            'asyncio_default_fixture_loop_scope = "function"',
            pyproject,
        )

    def test_promoted_workloads_are_environment_isolated(self) -> None:
        environments = {
            "payu-sit": "payu-sit",
            "payu-uat": "payu-uat",
            "payu-preprod": "payu-preprod",
            "payu-prod": "payu",
        }
        for overlay, namespace in environments.items():
            with self.subTest(environment=overlay):
                result = self.render(
                    f"infrastructure/workloads/overlays/{overlay}"
                )
                self.assertEqual(0, result.returncode, result.stderr)
                self.assertNotIn("payu-dev", result.stdout)
                documents = [
                    item
                    for item in yaml.safe_load_all(result.stdout)
                    if isinstance(item, dict)
                ]
                namespace_document = next(
                    item for item in documents if item.get("kind") == "Namespace"
                )
                self.assertEqual(namespace, namespace_document["metadata"]["name"])
                static_secrets = [
                    item["metadata"]["name"]
                    for item in documents
                    if item.get("kind") == "Secret"
                    and (item.get("data") or item.get("stringData"))
                ]
                self.assertEqual([], static_secrets)

    def test_production_identity_has_no_test_users_or_inline_credentials(self) -> None:
        result = self.render("infrastructure/platform/identity/overlays/prod")
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertNotIn("payu-dev", result.stdout)
        documents = [
            item
            for item in yaml.safe_load_all(result.stdout)
            if isinstance(item, dict)
        ]
        realm_import = next(
            item for item in documents
            if item.get("kind") == "KeycloakRealmImport"
        )
        realm = realm_import["spec"]["realm"]
        self.assertEqual([], realm.get("users", []))
        expected_placeholders = {
            "payu-web-app": "PAYU_WEB_CLIENT_SECRET",
            "payu-backend": "PAYU_BACKEND_CLIENT_SECRET",
        }
        for client in realm.get("clients", []):
            client_id = client["clientId"]
            if client_id in expected_placeholders:
                self.assertEqual(
                    f"${{{expected_placeholders[client_id]}}}",
                    client.get("secret"),
                )
            else:
                self.assertNotIn("secret", client)
        placeholders = realm_import["spec"].get("placeholders", {})
        for placeholder in expected_placeholders.values():
            self.assertEqual(
                "payu-keycloak-client-secrets",
                placeholders[placeholder]["secret"]["name"],
            )
        for external_secret in (
            item for item in documents
            if item.get("kind") == "ExternalSecret"
        ):
            remote_keys = [
                entry["remoteRef"]["key"]
                for entry in external_secret["spec"].get("data", [])
            ]
            self.assertTrue(
                all(key.startswith("payu/prod/") for key in remote_keys),
                remote_keys,
            )

    def test_promoted_database_pvc_fits_namespace_limit_range(self) -> None:
        limits = {
            item["metadata"]["namespace"]: item
            for item in load_documents(
                REPO_ROOT / "infrastructure/foundation/namespaces/base/limit-ranges.yaml"
            )
        }
        for environment, namespace in (
            ("sit", "payu-sit"),
            ("uat", "payu-uat"),
            ("preprod", "payu-preprod"),
            ("prod", "payu"),
        ):
            with self.subTest(environment=environment):
                result = self.render(
                    f"infrastructure/platform/data/overlays/{environment}"
                )
                self.assertEqual(0, result.returncode, result.stderr)
                cluster = next(
                    item
                    for item in yaml.safe_load_all(result.stdout)
                    if isinstance(item, dict)
                    and item.get("kind") == "Cluster"
                    and item.get("apiVersion", "").startswith("postgresql.cnpg.io/")
                )
                requested = int(cluster["spec"]["storage"]["size"].removesuffix("Gi"))
                pvc_limit = next(
                    entry
                    for entry in limits[namespace]["spec"]["limits"]
                    if entry["type"] == "PersistentVolumeClaim"
                )
                maximum = int(pvc_limit["max"]["storage"].removesuffix("Gi"))
                self.assertLessEqual(requested, maximum)

    def test_platform_network_policies_allow_kube_api_after_service_dnat(self) -> None:
        policy_path = (
            REPO_ROOT
            / "infrastructure/platform/data/overlays/common/network-policy-egress.yaml"
        )
        for policy in load_documents(policy_path):
            with self.subTest(policy=policy["metadata"]["name"]):
                self.assertTrue(
                    any(
                        rule.get("to") == [{"ipBlock": {"cidr": "10.0.0.0/8"}}]
                        and {"protocol": "TCP", "port": 6443}
                        in rule.get("ports", [])
                        for rule in policy["spec"]["egress"]
                    )
                )

    def test_promoted_datagrid_storage_is_not_smaller_than_memory(self) -> None:
        result = self.render("infrastructure/platform/data/overlays/sit")
        self.assertEqual(0, result.returncode, result.stderr)
        datagrid = next(
            item
            for item in yaml.safe_load_all(result.stdout)
            if isinstance(item, dict) and item.get("kind") == "Infinispan"
        )
        memory = int(datagrid["spec"]["container"]["memory"].removesuffix("Gi"))
        storage = int(
            datagrid["spec"]["service"]["container"]["storage"].removesuffix("Gi")
        )
        self.assertGreaterEqual(storage, memory)

    def test_security_controls_remain_enforced_during_acs_migration(self) -> None:
        policy_root = REPO_ROOT / "infrastructure/platform/security/kyverno/policies"
        kustomization = load_documents(policy_root / "kustomization.yaml")[0]
        resources = set(kustomization.get("resources", []))
        required_until_acs_enforcement_is_proven = {
            "disallow-root-user.yaml",
            "disallow-host-namespaces.yaml",
            "require-approved-registry.yaml",
            "readonly-root-filesystem.yaml",
        }
        self.assertTrue(required_until_acs_enforcement_is_proven <= resources)
        self.assertNotIn("require-cosign-signature.yaml", resources)

    def test_noncompliant_workloads_are_remediated_before_admission_enforcement(self) -> None:
        policies = REPO_ROOT / "infrastructure/platform/security/kyverno/policies"
        for filename in (
            "disallow-root-user.yaml",
            "require-approved-registry.yaml",
            "require-labels.yaml",
        ):
            with self.subTest(policy=filename):
                policy = load_documents(policies / filename)[0]
                self.assertEqual("Audit", policy["spec"]["validationFailureAction"])

        host_namespace = load_documents(policies / "disallow-host-namespaces.yaml")[0]
        self.assertEqual("Enforce", host_namespace["spec"]["validationFailureAction"])

    def test_rhacs_central_lets_the_operator_own_persistence(self) -> None:
        kustomization = load_documents(
            REPO_ROOT / "infrastructure/platform/security/acs/kustomization.yaml"
        )[0]
        self.assertNotIn("rhacs-pvc.yaml", kustomization.get("resources", []))
        self.assertNotIn("rhacs-secrets.yaml", kustomization.get("resources", []))

        central = load_documents(
            REPO_ROOT / "infrastructure/platform/security/acs/rhacs.yaml"
        )[0]
        self.assertFalse(central["spec"]["central"]["exposure"]["route"]["enabled"])
        self.assertNotIn("persistence", central["spec"]["central"])
        scanner_pvc = central["spec"]["scannerV4"]["db"]["persistence"][
            "persistentVolumeClaim"
        ]
        self.assertEqual("gp3-csi", scanner_pvc["storageClassName"])
        self.assertEqual("50Gi", scanner_pvc["size"])

    def test_kyverno_controllers_are_highly_available(self) -> None:
        values_path = REPO_ROOT / "infrastructure/platform/security/kyverno/values.yaml"
        values = load_documents(values_path)[0]
        self.assertNotIn(": latest", values_path.read_text(encoding="utf-8"))
        for controller in (
            "admissionController",
            "backgroundController",
            "cleanupController",
            "reportsController",
        ):
            with self.subTest(controller=controller):
                self.assertGreaterEqual(values[controller]["replicas"], 2)

    def test_kyverno_chart_is_kubernetes_133_compatible(self) -> None:
        kustomization = load_documents(
            REPO_ROOT / "infrastructure/platform/security/kyverno/kustomization.yaml"
        )[0]
        self.assertEqual("3.8.2", kustomization["helmCharts"][0]["version"])

    def test_kyverno_allows_openshift_to_assign_uid(self) -> None:
        values = load_documents(
            REPO_ROOT / "infrastructure/platform/security/kyverno/values.yaml"
        )[0]
        contexts = (
            values["admissionController"]["initContainer"]["securityContext"],
            values["admissionController"]["container"]["securityContext"],
            values["backgroundController"]["securityContext"],
            values["cleanupController"]["securityContext"],
            values["reportsController"]["securityContext"],
            values["crds"]["migration"]["securityContext"],
            values["webhooksCleanup"]["securityContext"],
            values["test"]["securityContext"],
        )
        for context in contexts:
            self.assertIsNone(context["runAsUser"])
            self.assertIsNone(context["runAsGroup"])

    def test_secured_cluster_uses_standard_roxctl_init_bundle(self) -> None:
        secured_cluster = load_documents(
            REPO_ROOT / "infrastructure/platform/security/acs/rhacs.yaml"
        )[1]
        self.assertNotIn("registrationSecret", secured_cluster["spec"])

    def test_rhacs_admission_is_fail_closed_and_operator_native(self) -> None:
        documents = load_documents(
            REPO_ROOT / "infrastructure/platform/security/acs/rhacs.yaml"
        )
        secured_cluster = next(item for item in documents if item["kind"] == "SecuredCluster")
        admission = secured_cluster["spec"]["admissionControl"]
        self.assertEqual("Enabled", admission["enforcement"])
        self.assertEqual("Fail", admission["failurePolicy"])
        self.assertEqual("BreakGlassAnnotation", admission["bypass"])
        self.assertNotIn("listenOnCreates", admission)
        self.assertNotIn("listenOnUpdates", admission)
        self.assertNotIn("listenOnEvents", admission)

    def test_rhacs_ci_tasks_use_short_lived_projected_identity(self) -> None:
        tekton_root = REPO_ROOT / "infrastructure/platform/cicd/tekton"
        task_file = (
            tekton_root / "tasks/rhacs-tasks.yaml"
        ).read_text(encoding="utf-8")
        self.assertNotIn("ROX_ADMIN_PASSWORD", task_file)
        self.assertNotIn("rox-admin-password", task_file)
        self.assertNotIn("ROX_API_TOKEN", task_file)
        self.assertNotIn("roxctl-api-token", task_file)
        self.assertEqual(3, task_file.count("audience: rhacs-ci"))
        self.assertEqual(3, task_file.count("expirationSeconds: 600"))
        self.assertEqual(
            3,
            task_file.count("--token-file /var/run/rhacs-identity/token"),
        )
        rhacs_tasks = load_documents(tekton_root / "tasks/rhacs-tasks.yaml")
        deployment_check = next(
            task for task in rhacs_tasks
            if task["metadata"]["name"] == "rhacs-deployment-check"
        )
        self.assertIn(
            "set -euo pipefail",
            deployment_check["spec"]["steps"][0]["script"],
        )
        self.assertNotIn("INSECURE_SKIP_TLS_VERIFY", task_file)
        self.assertNotIn("--insecure", task_file)
        self.assertNotIn("|| true", task_file)
        self.assertNotIn("Continuing build", task_file)
        self.assertNotIn("Pipeline continues", task_file)
        self.assertIn("--ca /var/run/rhacs-ca/ca.pem", task_file)
        self.assertIn("central.stackrox.svc:443", task_file)

        kustomization = load_documents(tekton_root / "kustomization.yaml")[0]
        self.assertIn("rhacs-central-ca.yaml", kustomization["resources"])
        ca_config = load_documents(tekton_root / "rhacs-central-ca.yaml")[0]
        self.assertEqual("ConfigMap", ca_config["kind"])
        self.assertIn("BEGIN CERTIFICATE", ca_config["data"]["ca.pem"])


    def test_rhacs_blocks_privileged_workloads_outside_dev(self) -> None:
        policies = load_documents(
            REPO_ROOT / "infrastructure/platform/security/acs/security-policies.yaml"
        )
        privileged = next(
            item for item in policies
            if item["metadata"]["name"] == "payu-block-privileged-containers"
        )
        spec = privileged["spec"]
        self.assertFalse(spec["disabled"])
        self.assertEqual(["DEPLOY"], spec["lifecycleStages"])
        self.assertTrue(
            {
                "FAIL_DEPLOYMENT_CREATE_ENFORCEMENT",
                "FAIL_DEPLOYMENT_UPDATE_ENFORCEMENT",
                "SCALE_TO_ZERO_ENFORCEMENT",
            }
            <= set(spec["enforcementActions"])
        )
        self.assertEqual(
            {"payu-sit", "payu-uat", "payu-preprod", "payu"},
            {scope["namespace"] for scope in spec["scope"]},
        )

    def test_compliance_scan_uses_current_cis_and_pci_profiles(self) -> None:
        scan_setting, binding = load_documents(
            REPO_ROOT
            / "infrastructure/platform/security/compliance-operator/cis-scan.yaml"
        )
        self.assertFalse(scan_setting["spec"]["autoApplyRemediations"])
        self.assertEqual("0 3 * * 1", scan_setting["spec"]["schedule"])
        profiles = {profile["name"] for profile in binding["profiles"]}
        self.assertEqual(
            {
                "ocp4-cis-1-9",
                "ocp4-cis-node-1-9",
                "ocp4-pci-dss-4-0",
                "ocp4-pci-dss-node-4-0",
            },
            profiles,
        )

    def test_logging_operators_use_supported_channels_and_namespaces(self) -> None:
        documents = load_documents(
            REPO_ROOT
            / "infrastructure/platform/observability/logging/logging-operator.yaml"
        )
        subscriptions = {
            document["metadata"]["name"]: document
            for document in documents
            if document["kind"] == "Subscription"
        }
        self.assertEqual({"openshift-logging"}, set(subscriptions))
        self.assertEqual("stable-6.6", subscriptions["openshift-logging"]["spec"]["channel"])

    def test_logging_bundle_contains_no_placeholder_storage(self) -> None:
        root = REPO_ROOT / "infrastructure/platform/observability/logging"
        kustomization = load_documents(root / "kustomization.yaml")[0]
        self.assertEqual(
            {"logging-namespace.yaml", "logging-operator.yaml"},
            set(kustomization.get("resources", [])),
        )
        self.assertNotIn("secretGenerator", kustomization)

    def test_chains_is_the_single_image_signing_owner(self) -> None:
        pipeline = load_documents(
            REPO_ROOT / "infrastructure/platform/cicd/tekton/build-pipeline.yaml"
        )[0]
        self.assertNotIn(
            "signing-secrets",
            {workspace["name"] for workspace in pipeline["spec"]["workspaces"]},
        )
        self.assertNotIn(
            "sign-image",
            {task["name"] for task in pipeline["spec"]["tasks"]},
        )

        kustomization = load_documents(
            REPO_ROOT / "infrastructure/platform/cicd/tekton/kustomization.yaml"
        )[0]
        self.assertNotIn("tasks/cosign-task.yaml", kustomization["resources"])

        entrypoints = (
            "triggers/git-webhook-trigger.yaml",
            "pipeline-runs/build-service-example.yaml",
            "pipeline-runs/account-service-pipelinerun.yaml",
        )
        for relative_path in entrypoints:
            with self.subTest(entrypoint=relative_path):
                document = load_documents(
                    REPO_ROOT / "infrastructure/platform/cicd/tekton" / relative_path
                )[0]
                run_spec = document["spec"]["resourcetemplates"][0]["spec"] if document["kind"] == "TriggerTemplate" else document["spec"]
                self.assertNotIn(
                    "signing-secrets",
                    {workspace["name"] for workspace in run_spec["workspaces"]},
                )
                dockerconfigs = [
                    workspace for workspace in run_spec["workspaces"]
                    if workspace["name"] == "dockerconfig"
                ]
                for dockerconfig in dockerconfigs:
                    self.assertEqual(
                        "redhat-registry-pull",
                        dockerconfig["secret"]["secretName"],
                    )

    def test_unauthenticated_webhook_trigger_is_not_deployed(self) -> None:
        root = REPO_ROOT / "infrastructure/platform/cicd/tekton"
        kustomization = load_documents(root / "kustomization.yaml")[0]
        self.assertNotIn(
            "triggers/git-webhook-trigger.yaml",
            kustomization["resources"],
        )
        self.assertNotIn(
            "triggers/tekton-triggers-rbac.yaml",
            kustomization["resources"],
        )

    def test_opencost_is_internal_and_verifies_thanos_tls(self) -> None:
        root = REPO_ROOT / "infrastructure/platform/cost/opencost"
        kustomization = load_documents(root / "kustomization.yaml")[0]
        resources = set(kustomization["resources"])
        self.assertNotIn("route.yaml", resources)
        self.assertIn("service-ca.yaml", resources)

        documents = load_documents(root / "opencost-generated.yaml")
        self.assertFalse(
            any(document.get("kind") == "Secret" for document in documents),
            "generated OpenCost bundle must not contain credential material",
        )
        deployment = next(
            document for document in documents if document.get("kind") == "Deployment"
        )
        container = deployment["spec"]["template"]["spec"]["containers"][0]
        env = {entry["name"]: entry.get("value") for entry in container["env"]}
        self.assertEqual("false", env["INSECURE_SKIP_VERIFY"])
        self.assertEqual("false", env["MCP_SERVER_ENABLED"])
        self.assertEqual("true", env["KUBE_RBAC_PROXY_ENABLED"])
        self.assertNotIn("DB_BEARER_TOKEN", env)
        self.assertEqual("/var/run/configmaps/service-ca/service-ca.crt", env["SSL_CERT_FILE"])
        mounts = {mount["name"]: mount["mountPath"] for mount in container["volumeMounts"]}
        self.assertEqual("/var/configs", mounts["pricing-config"])

        rbac_documents = load_documents(root / "rbac.yaml")
        self.assertFalse(
            any(
                document.get("kind") == "Secret"
                and document.get("type") == "kubernetes.io/service-account-token"
                for document in rbac_documents
            )
        )

    def test_rhtas_production_dependencies_are_ha_and_durable(self) -> None:
        root = REPO_ROOT / "infrastructure/platform/security/rhtas"
        kustomization = load_documents(root / "kustomization.yaml")[0]
        resources = set(kustomization["resources"])
        self.assertIn("postgresql.yaml", resources)
        self.assertIn("redis-ha.yaml", resources)
        self.assertIn("securesign-ha.yaml", resources)

        postgres = load_documents(root / "postgresql.yaml")[0]
        self.assertEqual("Cluster", postgres["kind"])
        self.assertGreaterEqual(postgres["spec"]["instances"], 3)
        self.assertEqual("gp3-csi", postgres["spec"]["storage"]["storageClass"])
        self.assertIn("@sha256:", postgres["spec"]["imageName"])

        credentials = load_documents(root / "credentials-request.yaml")
        self.assertEqual(2, len(credentials))
        by_secret = {
            item["spec"]["secretRef"]["name"]: item
            for item in credentials
        }
        self.assertEqual(
            {"rhtas-aws-credentials", "rhtas-postgres-aws-credentials"},
            set(by_secret),
        )
        rekor_resources = str(
            by_secret["rhtas-aws-credentials"]["spec"]["providerSpec"]["statementEntries"]
        )
        postgres_resources = str(
            by_secret["rhtas-postgres-aws-credentials"]["spec"]["providerSpec"]["statementEntries"]
        )
        self.assertNotIn("backups", rekor_resources)
        self.assertNotIn("rhtas-339712853697", postgres_resources)
        self.assertNotIn("s3:DeleteObject", rekor_resources)

        redis_documents = load_documents(root / "redis-ha.yaml")
        redis = next(item for item in redis_documents if item["kind"] == "StatefulSet")
        self.assertGreaterEqual(redis["spec"]["replicas"], 3)
        image = redis["spec"]["template"]["spec"]["containers"][0]["image"]
        self.assertTrue(image.startswith("registry.redhat.io/"))
        self.assertIn("@sha256:", image)

        securesign = load_documents(root / "securesign-ha.yaml")[0]
        self.assertEqual("Securesign", securesign["kind"])
        for component in ("rekor", "fulcio", "ctlog", "tuf", "tsa"):
            self.assertGreaterEqual(securesign["spec"][component]["replicas"], 3)
        self.assertFalse(securesign["spec"]["trillian"]["database"]["create"])
        self.assertFalse(securesign["spec"]["rekor"]["searchIndex"]["create"])
        self.assertTrue(securesign["spec"]["tuf"]["pvc"]["retain"])

        network_policy = load_documents(root / "network-policies.yaml")[0]
        operator_ingress = next(
            rule
            for rule in network_policy["spec"]["ingress"]
            if any(port.get("port") == 3000 for port in rule.get("ports", []))
        )
        self.assertEqual(
            "openshift-operators",
            operator_ingress["from"][0]["namespaceSelector"]["matchLabels"]
            ["kubernetes.io/metadata.name"],
        )
        self.assertEqual(
            "operator-controller-manager",
            operator_ingress["from"][0]["podSelector"]["matchLabels"]
            ["control-plane"],
        )
        chains_ingress = next(
            rule
            for rule in network_policy["spec"]["ingress"]
            if any(
                source.get("namespaceSelector", {})
                .get("matchLabels", {})
                .get("kubernetes.io/metadata.name")
                == "openshift-pipelines"
                for source in rule.get("from", [])
            )
        )
        self.assertEqual(
            "tekton-chains-controller",
            chains_ingress["from"][0]["podSelector"]["matchLabels"]["app"],
        )
        self.assertEqual([{"protocol": "TCP", "port": 3000}], chains_ingress["ports"])


if __name__ == "__main__":
    unittest.main()
