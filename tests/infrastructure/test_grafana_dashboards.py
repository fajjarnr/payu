"""
Standalone tests for Grafana dashboards - no docker-compose required
"""

import os
import json
import pytest


class TestGrafanaDashboards:
    """Test Grafana dashboard configurations"""

    DASHBOARD_DIR = "infrastructure/docker/grafana/dashboards"

    REQUIRED_DASHBOARDS = [
        "service-health.json",
        "transactions.json",
        "infrastructure.json",
        "core-banking-services.json",
        "supporting-services.json",
        "ml-analytics-services.json",
        "business-operations-services.json",
        "infrastructure-monitoring.json"
    ]

    def test_all_dashboard_files_exist(self):
        """Test that all required dashboard files exist"""
        for dashboard_file in self.REQUIRED_DASHBOARDS:
            dashboard_path = os.path.join(self.DASHBOARD_DIR, dashboard_file)
            assert os.path.exists(dashboard_path), f"Dashboard file {dashboard_path} does not exist"

    def test_dashboard_files_are_valid_json(self):
        """Test that all dashboard files are valid JSON"""
        for dashboard_file in self.REQUIRED_DASHBOARDS:
            dashboard_path = os.path.join(self.DASHBOARD_DIR, dashboard_file)
            with open(dashboard_path, 'r') as f:
                try:
                    dashboard = json.load(f)
                except json.JSONDecodeError as e:
                    pytest.fail(f"Dashboard file {dashboard_file} contains invalid JSON: {e}")

    def test_dashboard_structure_valid(self):
        """Test that dashboards have required structure"""
        for dashboard_file in self.REQUIRED_DASHBOARDS:
            dashboard_path = os.path.join(self.DASHBOARD_DIR, dashboard_file)
            with open(dashboard_path, 'r') as f:
                dashboard = json.load(f)

            assert "dashboard" in dashboard, f"Dashboard {dashboard_file} missing 'dashboard' key"
            assert "title" in dashboard["dashboard"], f"Dashboard {dashboard_file} missing 'title'"
            assert "panels" in dashboard["dashboard"], f"Dashboard {dashboard_file} missing 'panels'"
            assert isinstance(dashboard["dashboard"]["panels"], list), f"Dashboard {dashboard_file} panels must be a list"

    def test_dashboard_titles_are_unique(self):
        """Test that dashboard titles are unique"""
        titles = []
        for dashboard_file in self.REQUIRED_DASHBOARDS:
            dashboard_path = os.path.join(self.DASHBOARD_DIR, dashboard_file)
            with open(dashboard_path, 'r') as f:
                dashboard = json.load(f)
                title = dashboard["dashboard"]["title"]
                titles.append(title)

        assert len(titles) == len(set(titles)), "Dashboard titles are not unique"

    def test_dashboard_panels_have_targets(self):
        """Test that dashboard panels have Prometheus targets configured"""
        for dashboard_file in self.REQUIRED_DASHBOARDS:
            dashboard_path = os.path.join(self.DASHBOARD_DIR, dashboard_file)
            with open(dashboard_path, 'r') as f:
                dashboard = json.load(f)

            panels = dashboard["dashboard"]["panels"]
            assert len(panels) > 0, f"Dashboard {dashboard_file} has no panels"

            for panel in panels:
                if "targets" in panel:
                    assert len(panel["targets"]) > 0, f"Panel in {dashboard_file} has no targets"
                    for target in panel["targets"]:
                        assert "expr" in target, f"Target in {dashboard_file} missing 'expr' field"

    def test_core_banking_dashboard_targets(self):
        """Test that Core Banking dashboard has correct service targets"""
        dashboard_path = os.path.join(self.DASHBOARD_DIR, "core-banking-services.json")
        with open(dashboard_path, 'r') as f:
            dashboard = json.load(f)

        content = json.dumps(dashboard)
        expected_services = ["account-service", "auth-service", "transaction-service", "wallet-service"]
        for service in expected_services:
            assert service in content, f"Core Banking dashboard missing target for {service}"

    def test_supporting_services_dashboard_targets(self):
        """Test that Supporting Services dashboard has correct service targets"""
        dashboard_path = os.path.join(self.DASHBOARD_DIR, "supporting-services.json")
        with open(dashboard_path, 'r') as f:
            dashboard = json.load(f)

        content = json.dumps(dashboard)
        expected_services = ["billing-service", "notification-service", "gateway-service", "compliance-service"]
        for service in expected_services:
            assert service in content, f"Supporting Services dashboard missing target for {service}"

    def test_ml_analytics_dashboard_targets(self):
        """Test that ML & Analytics dashboard has correct service targets"""
        dashboard_path = os.path.join(self.DASHBOARD_DIR, "ml-analytics-services.json")
        with open(dashboard_path, 'r') as f:
            dashboard = json.load(f)

        content = json.dumps(dashboard)
        expected_services = ["kyc-service", "analytics-service"]
        for service in expected_services:
            assert service in content, f"ML & Analytics dashboard missing target for {service}"

    def test_business_operations_dashboard_targets(self):
        """Test that Business & Operations dashboard has correct service targets"""
        dashboard_path = os.path.join(self.DASHBOARD_DIR, "business-operations-services.json")
        with open(dashboard_path, 'r') as f:
            dashboard = json.load(f)

        content = json.dumps(dashboard)
        expected_services = [
            "investment-service",
            "lending-service",
            "backoffice-service",
            "partner-service",
            "promotion-service",
            "support-service"
        ]
        for service in expected_services:
            assert service in content, f"Business & Operations dashboard missing target for {service}"
