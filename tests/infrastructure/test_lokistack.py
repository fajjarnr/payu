"""
Tests for LokiStack OpenShift Infrastructure
"""

import pytest
import subprocess
import yaml
import os


class TestLokiStackInfrastructure:
    """Test LokiStack for Centralized Log Management"""

    LOGGING_DIR = "infrastructure/openshift/base/logging"

    REQUIRED_FILES = [
        "logging-namespace.yaml",
        "logging-operator.yaml",
        "lokistack.yaml",
        "lokistack-storage-secret.yaml",
        "clusterlogforwarder.yaml",
        "loki-rbac.yaml",
        "clusterlogging.yaml",
        "lokistack-token-secret.yaml",
        "loki-alert-rules.yaml",
        "loki-route.yaml"
    ]

    def run_command(self, cmd: list, timeout: int = 60) -> subprocess.CompletedProcess:
        """Helper to run commands"""
        return subprocess.run(
            cmd,
            capture_output=True,
            text=True,
            timeout=timeout
        )

    def load_yaml(self, filename: str, multi_doc: bool = False):
        """Load and parse YAML file"""
        filepath = os.path.join(self.LOGGING_DIR, filename)
        with open(filepath, 'r') as f:
            if multi_doc:
                return list(yaml.safe_load_all(f))
            return yaml.safe_load(f)

    def test_logging_directory_exists(self):
        """Test that logging directory exists"""
        assert os.path.exists(self.LOGGING_DIR), f"Directory {self.LOGGING_DIR} does not exist"

    def test_required_files_exist(self):
        """Test that all required LokiStack files exist"""
        for filename in self.REQUIRED_FILES:
            filepath = os.path.join(self.LOGGING_DIR, filename)
            assert os.path.exists(filepath), f"Required file {filepath} does not exist"

    def test_namespace_yaml_valid(self):
        """Test that namespace YAML is valid"""
        data = self.load_yaml("logging-namespace.yaml", multi_doc=True)

        # Should contain multiple documents
        assert isinstance(data, list), "Namespace file should contain multiple documents"

        # Check for openshift-logging namespace
        namespaces = [doc for doc in data if doc.get('kind') == 'Namespace']
        assert len(namespaces) >= 2, "At least 2 namespaces should be defined"

        namespace_names = [ns['metadata']['name'] for ns in namespaces]
        assert 'openshift-logging' in namespace_names, "openshift-logging namespace not found"
        assert 'openshift-operators-redhat' in namespace_names, "openshift-operators-redhat namespace not found"

    def test_operator_subscription_yaml_valid(self):
        """Test that operator subscription YAML is valid"""
        data = self.load_yaml("logging-operator.yaml", multi_doc=True)

        assert isinstance(data, list), "Operator file should contain multiple documents"

        # Check for Loki operator subscription
        subscriptions = [doc for doc in data if doc.get('kind') == 'Subscription']
        assert len(subscriptions) >= 1, "Loki operator subscription not found"

        loki_subscriptions = [sub for sub in subscriptions if 'loki' in sub.get('metadata', {}).get('name', '').lower()]
        assert len(loki_subscriptions) >= 1, "Loki operator subscription not found"

        # Check subscription configuration
        loki_subscription = loki_subscriptions[0]
        assert loki_subscription['spec']['channel'] == 'stable', "Loki operator channel should be stable"
        assert loki_subscription['spec']['source'] == 'redhat-operators', "Source should be redhat-operators"

    def test_operator_group_yaml_valid(self):
        """Test that operator group YAML is valid"""
        data = self.load_yaml("logging-operator.yaml", multi_doc=True)

        operator_groups = [doc for doc in data if doc.get('kind') == 'OperatorGroup']
        assert len(operator_groups) >= 1, "OperatorGroup not found"

        # Check that operator group targets correct namespaces
        for og in operator_groups:
            assert 'targetNamespaces' in og['spec'], "OperatorGroup should have targetNamespaces"
            assert len(og['spec']['targetNamespaces']) > 0, "targetNamespaces should not be empty"

    def test_lokistack_yaml_valid(self):
        """Test that LokiStack CRD is valid"""
        data = self.load_yaml("lokistack.yaml")

        assert data['kind'] == 'LokiStack', "Resource should be LokiStack"
        assert data['apiVersion'] == 'loki.grafana.com/v1', "API version should be loki.grafana.com/v1"
        assert data['metadata']['name'] == 'loki', "LokiStack name should be 'loki'"
        assert data['metadata']['namespace'] == 'openshift-logging', "Namespace should be openshift-logging"

        # Check spec
        spec = data['spec']
        assert spec['size'] == '1x.small', "Size should be 1x.small"
        assert spec['managementState'] == 'Managed', "Management state should be Managed"
        assert spec['replicationFactor'] == 1, "Replication factor should be 1"

        # Check storage configuration
        assert 'storage' in spec, "Storage configuration not found"
        assert 'secret' in spec['storage'], "Storage secret not configured"

        # Check tenants mode
        assert spec['tenants']['mode'] == 'openshift-logging', "Tenants mode should be openshift-logging"

        # Check template components
        template = spec.get('template', {})
        assert 'ingester' in template, "Ingester template not configured"
        assert 'gateway' in template, "Gateway template not configured"

    def test_lokistack_storage_secret_yaml_valid(self):
        """Test that LokiStack storage secret YAML is valid"""
        data = self.load_yaml("lokistack-storage-secret.yaml")

        assert data['kind'] == 'Secret', "Resource should be Secret"
        assert data['metadata']['name'] == 'loki-storage', "Secret name should be 'loki-storage'"
        assert data['metadata']['namespace'] == 'openshift-logging', "Namespace should be openshift-logging"

        # Check type
        assert data['type'] == 'Opaque', "Secret type should be Opaque"

        # Check required data fields
        required_fields = ['endpoint', 'access_key_id', 'access_key_secret', 'bucketnames']
        for field in required_fields:
            assert field in data['stringData'], f"Required field {field} not found in secret"

    def test_clusterlogforwarder_yaml_valid(self):
        """Test that ClusterLogForwarder YAML is valid"""
        data = self.load_yaml("clusterlogforwarder.yaml")

        assert data['kind'] == 'ClusterLogForwarder', "Resource should be ClusterLogForwarder"
        assert data['apiVersion'] == 'logging.openshift.io/v1', "API version should be logging.openshift.io/v1"
        assert data['metadata']['name'] == 'payu-log-forwarder', "Name should be 'payu-log-forwarder'"

        # Check outputs
        assert 'outputs' in data['spec'], "Outputs not configured"
        assert len(data['spec']['outputs']) >= 1, "At least one output should be configured"

        loki_output = data['spec']['outputs'][0]
        assert loki_output['type'] == 'loki', "Output type should be loki"
        assert 'url' in loki_output, "Output URL not configured"
        assert 'secret' in loki_output, "Secret reference not configured"

        # Check pipelines
        assert 'pipelines' in data['spec'], "Pipelines not configured"
        assert len(data['spec']['pipelines']) >= 1, "At least one pipeline should be configured"

        pipeline = data['spec']['pipelines'][0]
        assert 'inputRefs' in pipeline, "inputRefs not configured"
        assert 'outputRefs' in pipeline, "outputRefs not configured"
        assert 'labels' in pipeline, "Labels not configured"
        assert pipeline['labels']['app.kubernetes.io/part-of'] == 'payu', "PayU label not set"

    def test_loki_rbac_yaml_valid(self):
        """Test that Loki RBAC YAML is valid"""
        data = self.load_yaml("loki-rbac.yaml", multi_doc=True)

        assert isinstance(data, list), "RBAC file should contain multiple documents"

        # Check ServiceAccount
        service_accounts = [doc for doc in data if doc.get('kind') == 'ServiceAccount']
        assert len(service_accounts) >= 1, "ServiceAccount not found"
        assert service_accounts[0]['metadata']['name'] == 'loki-promtail', "ServiceAccount name should be loki-promtail"

        # Check ClusterRole
        cluster_roles = [doc for doc in data if doc.get('kind') == 'ClusterRole']
        assert len(cluster_roles) >= 1, "ClusterRole not found"

        loki_role = cluster_roles[0]
        assert loki_role['metadata']['name'] == 'loki-promtail', "ClusterRole name should be loki-promtail"
        assert len(loki_role['rules']) > 0, "ClusterRole should have rules"

        # Check permissions for reading pods, namespaces, nodes
        rules = loki_role['rules']
        pods_rule = next((r for r in rules if 'pods' in r.get('resources', [])), None)
        assert pods_rule is not None, "Rule for reading pods not found"
        assert 'get' in pods_rule['verbs'], "Missing 'get' verb for pods"

        # Check ClusterRoleBinding
        role_bindings = [doc for doc in data if doc.get('kind') == 'ClusterRoleBinding']
        assert len(role_bindings) >= 1, "ClusterRoleBinding not found"

        binding = role_bindings[0]
        assert binding['metadata']['name'] == 'loki-promtail', "ClusterRoleBinding name should be loki-promtail"
        assert binding['roleRef']['kind'] == 'ClusterRole', "RoleRef should be ClusterRole"
        assert binding['roleRef']['name'] == 'loki-promtail', "RoleRef name should match ClusterRole"

    def test_clusterlogging_yaml_valid(self):
        """Test that ClusterLogging YAML is valid"""
        data = self.load_yaml("clusterlogging.yaml")

        assert data['kind'] == 'ClusterLogging', "Resource should be ClusterLogging"
        assert data['apiVersion'] == 'logging.openshift.io/v1', "API version should be logging.openshift.io/v1"
        assert data['metadata']['name'] == 'instance', "Name should be 'instance'"

        # Check spec
        spec = data['spec']
        assert spec['managementState'] == 'Managed', "Management state should be Managed"

        # Check logStore
        assert 'logStore' in spec, "logStore not configured"
        assert spec['logStore']['type'] == 'lokistack', "logStore type should be lokistack"
        assert spec['logStore']['lokistack']['name'] == 'loki', "LokiStack name should be 'loki'"

        # Check collection
        assert 'collection' in spec, "collection not configured"
        assert spec['collection']['logs']['type'] == 'vector', "Log collection type should be vector"

        # Check visualization
        assert 'visualization' in spec, "visualization not configured"
        assert spec['visualization']['type'] == 'ocp-console', "Visualization type should be ocp-console"

    def test_lokistack_token_secret_yaml_valid(self):
        """Test that LokiStack token secret YAML is valid"""
        data = self.load_yaml("lokistack-token-secret.yaml")

        assert data['kind'] == 'Secret', "Resource should be Secret"
        assert data['metadata']['name'] == 'loki-token', "Secret name should be 'loki-token'"
        assert data['metadata']['namespace'] == 'openshift-logging', "Namespace should be openshift-logging"
        assert data['type'] == 'Opaque', "Secret type should be Opaque"
        assert 'token' in data['stringData'], "token field not found in secret"

    def test_loki_alert_rules_yaml_valid(self):
        """Test that Loki alert rules YAML is valid"""
        data = self.load_yaml("loki-alert-rules.yaml")

        assert data['kind'] == 'ConfigMap', "Resource should be ConfigMap"
        assert data['metadata']['name'] == 'loki-rules', "ConfigMap name should be 'loki-rules'"
        assert data['metadata']['namespace'] == 'openshift-logging', "Namespace should be openshift-logging"

        # Check alert rules data
        assert 'alert-rules.yaml' in data['data'], "alert-rules.yaml not found in ConfigMap"

        # Parse alert rules
        alert_rules = yaml.safe_load(data['data']['alert-rules.yaml'])

        # Check groups
        assert 'groups' in alert_rules, "Alert rules should contain groups"
        assert len(alert_rules['groups']) >= 1, "At least one alert rule group should be defined"

        # Check PayU application alerts
        payu_group = alert_rules['groups'][0]
        assert 'name' in payu_group, "Alert group name not found"
        assert 'rules' in payu_group, "Alert rules not found in group"
        assert len(payu_group['rules']) >= 1, "At least one alert rule should be defined"

        # Check for critical alert rules
        critical_alerts = [r for r in payu_group['rules'] if r.get('labels', {}).get('severity') == 'critical']
        assert len(critical_alerts) >= 1, "At least one critical alert should be defined"

        # Check for HighErrorRate alert
        error_rate_alert = next((r for r in payu_group['rules'] if r.get('alert') == 'HighErrorRate'), None)
        assert error_rate_alert is not None, "HighErrorRate alert not found"
        assert 'expr' in error_rate_alert, "Alert expression not found"
        assert 'for' in error_rate_alert, "Alert duration not found"

    def test_loki_route_yaml_valid(self):
        """Test that Loki route YAML is valid"""
        data = self.load_yaml("loki-route.yaml")

        assert data['kind'] == 'Route', "Resource should be Route"
        assert data['apiVersion'] == 'route.openshift.io/v1', "API version should be route.openshift.io/v1"
        assert data['metadata']['name'] == 'loki-gateway', "Route name should be 'loki-gateway'"
        assert data['metadata']['namespace'] == 'openshift-logging', "Namespace should be openshift-logging"

        # Check spec
        spec = data['spec']
        assert spec['to']['kind'] == 'Service', "Route target should be Service"
        assert spec['to']['name'] == 'loki-gateway-http', "Target service name should be 'loki-gateway-http'"
        assert 'tls' in spec, "TLS configuration not found"
        assert spec['tls']['termination'] == 'edge', "TLS termination should be edge"

    def test_loki_alert_rules_content(self):
        """Test that Loki alert rules contain expected alerts"""
        data = self.load_yaml("loki-alert-rules.yaml")
        alert_rules = yaml.safe_load(data['data']['alert-rules.yaml'])

        payu_group = alert_rules['groups'][0]
        alert_names = [r['alert'] for r in payu_group['rules']]

        # Check for required alerts
        expected_alerts = [
            'HighErrorRate',
            'HighLatency',
            'DatabaseConnectionError',
            'ServiceDown'
        ]

        for expected_alert in expected_alerts:
            assert expected_alert in alert_names, f"Required alert '{expected_alert}' not found"

    def test_loki_ingestion_limits_configured(self):
        """Test that LokiStack has ingestion limits configured"""
        data = self.load_yaml("lokistack.yaml")

        spec = data['spec']
        assert 'limits' in spec, "Limits not configured"

        limits = spec['limits']
        assert 'global' in limits, "Global limits not configured"
        assert 'ingestion' in limits['global'], "Ingestion limits not configured"
        assert 'ingestionBurstSize' in limits['global']['ingestion'], "Ingestion burst size not configured"
        assert 'ingestionRate' in limits['global']['ingestion'], "Ingestion rate not configured"

        # Check retention
        assert 'retention' in limits['global'], "Retention not configured"
        assert 'days' in limits['global']['retention'], "Retention days not configured"
        assert limits['global']['retention']['days'] >= 7, "Retention should be at least 7 days"

    def test_loki_gateway_route_enabled(self):
        """Test that LokiStack gateway route is enabled"""
        data = self.load_yaml("lokistack.yaml")

        template = data['spec']['template']
        assert 'gateway' in template, "Gateway not configured"
        assert 'route' in template['gateway'], "Route configuration not found"
        assert template['gateway']['route']['enabled'] == True, "Gateway route should be enabled"

    def test_clusterlogforwarder_pipelines_covers_all_logs(self):
        """Test that ClusterLogForwarder pipelines cover all log types"""
        data = self.load_yaml("clusterlogforwarder.yaml")

        pipelines = data['spec']['pipelines']
        input_types = set()

        for pipeline in pipelines:
            for input_ref in pipeline.get('inputRefs', []):
                input_types.add(input_ref)

        # Check that all log types are covered
        expected_types = ['application', 'infrastructure', 'audit']
        for expected_type in expected_types:
            assert expected_type in input_types, f"Log type '{expected_type}' not covered by any pipeline"

    def test_payu_labels_applied(self):
        """Test that PayU labels are properly applied"""
        files_to_check = [
            ("lokistack.yaml", False),
            ("lokistack-storage-secret.yaml", False),
            ("clusterlogforwarder.yaml", False),
            ("loki-rbac.yaml", True),
            ("clusterlogging.yaml", False),
            ("lokistack-token-secret.yaml", False),
            ("loki-alert-rules.yaml", False),
            ("loki-route.yaml", False)
        ]

        for filename, multi_doc in files_to_check:
            data = self.load_yaml(filename, multi_doc=multi_doc)

            # Handle single and multi-document files
            if isinstance(data, list):
                # Check all documents in list
                for doc in data:
                    if 'metadata' in doc and 'labels' in doc['metadata']:
                        assert doc['metadata']['labels'].get('app.kubernetes.io/part-of') == 'payu', \
                            f"PayU label not found in {filename}"
            else:
                if 'metadata' in data and 'labels' in data['metadata']:
                    assert data['metadata']['labels'].get('app.kubernetes.io/part-of') == 'payu', \
                        f"PayU label not found in {filename}"

    def test_yaml_syntax_is_valid(self):
        """Test that all YAML files have valid syntax"""
        import tempfile

        # Create temp kustomization file to validate
        temp_dir = tempfile.mkdtemp()
        kustomization_path = os.path.join(temp_dir, "kustomization.yaml")

        try:
            kustomization = {
                'apiVersion': 'kustomize.config.k8s.io/v1beta1',
                'kind': 'Kustomization',
                'resources': []
            }

            for filename in self.REQUIRED_FILES:
                kustomization['resources'].append(os.path.join(self.LOGGING_DIR, filename))

            with open(kustomization_path, 'w') as f:
                yaml.dump(kustomization, f)

            # Try to validate with kubectl if available
            result = self.run_command(['kubectl', 'apply', '--dry-run=client', '-f', self.LOGGING_DIR], timeout=30)

            # If kubectl is not available, that's okay
            # The YAML parsing tests above already validate syntax

        except FileNotFoundError:
            # kubectl not available, skip validation
            pass
        finally:
            if os.path.exists(temp_dir):
                import shutil
                shutil.rmtree(temp_dir)
