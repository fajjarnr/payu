"""
Tests for Production Monitoring & Alerting (LokiStack/Prometheus) infrastructure
"""

import pytest
import subprocess
import time
import requests
import json


class TestMonitoringInfrastructure:
    """Test Monitoring and Alerting Infrastructure"""

    COMPOSE_FILE = "docker-compose.yml"
    REQUIRED_MONITORING_SERVICES = [
        "prometheus",
        "loki",
        "promtail",
        "grafana",
        "alertmanager"
    ]

    MONITORING_PORTS = {
        "prometheus": 9090,
        "loki": 3100,
        "grafana": 3000,
        "alertmanager": 9093
    }

    def run_command(self, cmd: list, timeout: int = 300) -> subprocess.CompletedProcess:
        """Helper to run commands"""
        return subprocess.run(
            cmd,
            capture_output=True,
            text=True,
            timeout=timeout
        )

    @pytest.fixture(scope="class", autouse=True)
    def compose_up(self):
        """Ensure docker-compose is up for tests"""
        try:
            self.run_command(
                ["docker-compose", "-f", self.COMPOSE_FILE, "up", "-d"],
                timeout=600
            )
            # Wait for services to start
            time.sleep(30)
            yield
        finally:
            try:
                self.run_command(
                    ["docker-compose", "-f", self.COMPOSE_FILE, "down", "-v"],
                    timeout=120
                )
            except Exception:
                pass

    def test_prometheus_container_running(self, compose_up):
        """Test that Prometheus container is running"""
        result = self.run_command(["docker", "ps", "--filter", "name=payu-prometheus"])
        assert result.returncode == 0, "Failed to check Prometheus status"
        assert "payu-prometheus" in result.stdout, "Prometheus container not running"

    def test_loki_container_running(self, compose_up):
        """Test that Loki container is running"""
        result = self.run_command(["docker", "ps", "--filter", "name=payu-loki"])
        assert result.returncode == 0, "Failed to check Loki status"
        assert "payu-loki" in result.stdout, "Loki container not running"

    def test_grafana_container_running(self, compose_up):
        """Test that Grafana container is running"""
        result = self.run_command(["docker", "ps", "--filter", "name=payu-grafana"])
        assert result.returncode == 0, "Failed to check Grafana status"
        assert "payu-grafana" in result.stdout, "Grafana container not running"

    def test_alertmanager_container_running(self, compose_up):
        """Test that Alertmanager container is running"""
        result = self.run_command(["docker", "ps", "--filter", "name=payu-alertmanager"])
        assert result.returncode == 0, "Failed to check Alertmanager status"
        assert "payu-alertmanager" in result.stdout, "Alertmanager container not running"

    def test_promtail_container_running(self, compose_up):
        """Test that Promtail container is running"""
        result = self.run_command(["docker", "ps", "--filter", "name=payu-promtail"])
        assert result.returncode == 0, "Failed to check Promtail status"
        assert "payu-promtail" in result.stdout, "Promtail container not running"

    def test_prometheus_accessible(self, compose_up):
        """Test that Prometheus is accessible via HTTP"""
        max_retries = 30
        for i in range(max_retries):
            try:
                response = requests.get("http://localhost:9090/-/healthy", timeout=5)
                if response.status_code == 200:
                    break
            except requests.exceptions.RequestException:
                pass
            time.sleep(2)
        else:
            pytest.fail("Prometheus did not become accessible")

    def test_prometheus_targets_configured(self, compose_up):
        """Test that Prometheus targets are configured"""
        response = requests.get("http://localhost:9090/api/v1/targets", timeout=10)
        assert response.status_code == 200

        data = response.json()
        assert data["status"] == "success", "Failed to get Prometheus targets"
        assert len(data["data"]["activeTargets"]) > 0, "No targets configured in Prometheus"

    def test_prometheus_scraping_services(self, compose_up):
        """Test that Prometheus is scraping service metrics"""
        max_retries = 30
        for i in range(max_retries):
            try:
                response = requests.get("http://localhost:9090/api/v1/query", params={
                    "query": "up"
                }, timeout=10)
                if response.status_code == 200:
                    data = response.json()
                    if data["status"] == "success" and len(data["data"]["result"]) > 0:
                        break
            except requests.exceptions.RequestException:
                pass
            time.sleep(5)
        else:
            pytest.fail("Prometheus is not scraping any services")

    def test_loki_accessible(self, compose_up):
        """Test that Loki is accessible via HTTP"""
        max_retries = 30
        for i in range(max_retries):
            try:
                response = requests.get("http://localhost:3100/ready", timeout=5)
                if response.status_code == 200:
                    break
            except requests.exceptions.RequestException:
                pass
            time.sleep(2)
        else:
            pytest.fail("Loki did not become accessible")

    def test_loki_config_loaded(self, compose_up):
        """Test that Loki configuration is loaded"""
        response = requests.get("http://localhost:3100/config", timeout=10)
        assert response.status_code == 200
        data = response.json()
        assert "limits_config" in data, "Loki limits_config not found"
        assert "schema_config" in data, "Loki schema_config not found"

    def test_grafana_accessible(self, compose_up):
        """Test that Grafana is accessible via HTTP"""
        max_retries = 30
        for i in range(max_retries):
            try:
                response = requests.get("http://localhost:3000/api/health", timeout=5)
                if response.status_code == 200:
                    break
            except requests.exceptions.RequestException:
                pass
            time.sleep(2)
        else:
            pytest.fail("Grafana did not become accessible")

    def test_grafana_datasources_configured(self, compose_up):
        """Test that Grafana datasources are configured"""
        # Wait for Grafana to initialize
        time.sleep(30)

        # Login to Grafana
        session = requests.Session()
        login_data = {
            "user": "admin",
            "password": "admin"
        }
        response = session.post("http://localhost:3000/api/login", json=login_data)
        assert response.status_code == 200, "Failed to login to Grafana"

        response = session.get("http://localhost:3000/api/datasources", timeout=10)
        assert response.status_code == 200

        datasources = response.json()
        datasource_names = [ds["name"] for ds in datasources]

        assert "Prometheus" in datasource_names, "Prometheus datasource not configured"
        assert "Loki" in datasource_names, "Loki datasource not configured"

    def test_grafana_dashboards_installed(self, compose_up):
        """Test that Grafana dashboards are installed"""
        # Wait for Grafana to initialize
        time.sleep(30)

        session = requests.Session()
        login_data = {
            "user": "admin",
            "password": "admin"
        }
        session.post("http://localhost:3000/api/login", json=login_data)

        response = session.get("http://localhost:3000/api/search", params={
            "query": "PayU"
        }, timeout=10)
        assert response.status_code == 200

        dashboards = response.json()
        assert len(dashboards) > 0, "No PayU dashboards found"

        dashboard_titles = [db["title"] for db in dashboards]
        assert "PayU Service Health Dashboard" in dashboard_titles, "Service Health Dashboard not found"
        assert "PayU Transaction Dashboard" in dashboard_titles, "Transaction Dashboard not found"
        assert "PayU Infrastructure Dashboard" in dashboard_titles, "Infrastructure Dashboard not found"

    def test_alertmanager_accessible(self, compose_up):
        """Test that Alertmanager is accessible via HTTP"""
        max_retries = 30
        for i in range(max_retries):
            try:
                response = requests.get("http://localhost:9093/-/healthy", timeout=5)
                if response.status_code == 200:
                    break
            except requests.exceptions.RequestException:
                pass
            time.sleep(2)
        else:
            pytest.fail("Alertmanager did not become accessible")

    def test_alertmanager_config_loaded(self, compose_up):
        """Test that Alertmanager configuration is loaded"""
        response = requests.get("http://localhost:9093/api/v1/status/config", timeout=10)
        assert response.status_code == 200

        data = response.json()
        assert data["status"] == "success", "Failed to get Alertmanager config"
        assert "route" in data["data"], "Alertmanager route not configured"
        assert "receivers" in data["data"], "Alertmanager receivers not configured"

    def test_prometheus_alert_rules_loaded(self, compose_up):
        """Test that Prometheus alert rules are loaded"""
        response = requests.get("http://localhost:9090/api/v1/rules", timeout=10)
        assert response.status_code == 200

        data = response.json()
        assert data["status"] == "success", "Failed to get Prometheus rules"

        rules = data["data"]["groups"]
        assert len(rules) > 0, "No alert rules configured"

        group_names = [group["name"] for group in rules]
        assert "payu_service_health" in group_names, "Service health alert rules not found"
        assert "payu_performance" in group_names, "Performance alert rules not found"
        assert "payu_transactions" in group_names, "Transaction alert rules not found"

    def test_prometheus_volumes_mounted(self, compose_up):
        """Test that Prometheus volumes are mounted"""
        result = self.run_command([
            "docker", "inspect", "payu-prometheus", "--format", "{{.Mounts}}"
        ])
        assert result.returncode == 0
        assert "prometheus_data" in result.stdout, "Prometheus data volume not mounted"

    def test_loki_volumes_mounted(self, compose_up):
        """Test that Loki volumes are mounted"""
        result = self.run_command([
            "docker", "inspect", "payu-loki", "--format", "{{.Mounts}}"
        ])
        assert result.returncode == 0
        assert "loki_data" in result.stdout, "Loki data volume not mounted"

    def test_grafana_volumes_mounted(self, compose_up):
        """Test that Grafana volumes are mounted"""
        result = self.run_command([
            "docker", "inspect", "payu-grafana", "--format", "{{.Mounts}}"
        ])
        assert result.returncode == 0
        assert "grafana_data" in result.stdout, "Grafana data volume not mounted"

    def test_monitoring_service_dependencies(self, compose_up):
        """Test that monitoring services have proper dependencies"""
        # Grafana should depend on Prometheus and Loki
        result = self.run_command([
            "docker", "inspect", "payu-grafana", "--format", "{{json .HostConfig.Dependencies}}"
        ])
        assert result.returncode == 0
        deps = json.loads(result.stdout)
        assert len(deps) > 0, "Grafana has no dependencies"

    def test_prometheus_retention_configured(self, compose_up):
        """Test that Prometheus retention is configured"""
        result = self.run_command([
            "docker", "exec", "payu-prometheus",
            "cat", "/etc/prometheus/prometheus.yml"
        ])
        assert result.returncode == 0
        assert "retention.time" in result.stdout, "Prometheus retention not configured"

    def test_loki_retention_configured(self, compose_up):
        """Test that Loki retention is configured"""
        result = self.run_command([
            "docker", "exec", "payu-loki",
            "cat", "/etc/loki/local-config.yaml"
        ])
        assert result.returncode == 0
        assert "retention_enabled: true" in result.stdout, "Loki retention not enabled"

    def test_monitoring_configuration_files_exist(self):
        """Test that monitoring configuration files exist"""
        import os

        config_files = [
            "infrastructure/docker/loki-config.yml",
            "infrastructure/docker/promtail-config.yml",
            "infrastructure/docker/prometheus-alerts.yml",
            "infrastructure/docker/alertmanager-config.yml",
            "infrastructure/docker/grafana/provisioning/datasources/datasources.yml",
            "infrastructure/docker/grafana/provisioning/dashboards/dashboards.yml"
        ]

        for config_file in config_files:
            assert os.path.exists(config_file), f"Configuration file {config_file} does not exist"

    def test_grafana_dashboard_files_exist(self):
        """Test that Grafana dashboard files exist"""
        import os

        dashboard_files = [
            "infrastructure/docker/grafana/dashboards/service-health.json",
            "infrastructure/docker/grafana/dashboards/transactions.json",
            "infrastructure/docker/grafana/dashboards/infrastructure.json"
        ]

        for dashboard_file in dashboard_files:
            assert os.path.exists(dashboard_file), f"Dashboard file {dashboard_file} does not exist"
