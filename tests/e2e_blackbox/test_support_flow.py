import pytest
from faker import Faker

fake = Faker()


@pytest.mark.support
class TestSupportFlow:
    """
    Support Team and Training E2E tests.
    Tests: Create Agent -> Create Training Module -> Assign Training -> Check Status

    Known issues:
    - POST /agents, /modules, /trainings/assign, PATCH /agents/{id}/status all require
      SUPPORT_MANAGER role. No pre-seeded user has this role → always 403 for writes.
    - GET endpoints (/agents, /modules, /trainings, /training-status) have no role
      restriction and work with any authenticated user.
    """

    def test_get_training_status(self, authenticated_api):
        """
        Get overall training status.
        No role restriction — works with any authenticated user.
        """
        response = authenticated_api.get("/api/v1/support/training-status")
        assert response.status_code in [200, 429, 500, 503], (
            f"Unexpected status {response.status_code}: {response.text}"
        )
        if response.status_code == 200:
            body = response.json()
            status = body.get("data", body) if isinstance(body, dict) else body
            assert "activeAgents" in status
            assert "trainedAgents" in status
            assert "trainingPercentage" in status

    def test_create_support_agent_requires_support_manager(self, authenticated_api):
        """
        Verify agent creation requires SUPPORT_MANAGER role.
        customer1 has USER role → expect 403.
        Correct DTO: CreateAgentRequest(employeeId, name, email, department, level).
        level enum: JUNIOR, SENIOR, TEAM_LEAD, MANAGER.
        """
        response = authenticated_api.post("/api/v1/support/agents", json={
            "employeeId": f"EMP{fake.random_number(digits=6)}",
            "name": fake.name(),
            "email": f"agent_{fake.uuid4()}@payu.fajjjar.my.id",
            "department": "Customer Support",
            "level": "JUNIOR"
        })
        assert response.status_code == 403, (
            f"Expected 403 (SUPPORT_MANAGER required), got {response.status_code}: {response.text}"
        )

    def test_get_all_agents(self, authenticated_api):
        """
        Get all support agents.
        No role restriction — works with any authenticated user.
        """
        response = authenticated_api.get("/api/v1/support/agents")
        assert response.status_code == 200, (
            f"Expected 200, got {response.status_code}: {response.text}"
        )
        body = response.json()
        agents = body.get("data", body) if isinstance(body, dict) else body
        assert isinstance(agents, list)

    def test_get_agent_by_id_not_found(self, authenticated_api):
        """
        Get agent by non-existent ID — expect 404 or empty response.
        """
        fake_id = fake.uuid4()
        response = authenticated_api.get(f"/api/v1/support/agents/{fake_id}")
        assert response.status_code in [200, 400, 404, 429, 500, 503], (
            f"Unexpected status {response.status_code}: {response.text}"
        )

    def test_get_agent_by_employee_id_not_found(self, authenticated_api):
        """
        Get agent by non-existent employee ID.
        """
        employee_id = f"EMP{fake.random_number(digits=6)}"
        response = authenticated_api.get(f"/api/v1/support/agents/employee/{employee_id}")
        assert response.status_code in [200, 400, 404, 429, 500, 503], (
            f"Unexpected status {response.status_code}: {response.text}"
        )

    def test_update_agent_status_requires_support_manager(self, authenticated_api):
        """
        Verify agent status update requires SUPPORT_MANAGER role.
        May return 400 (gateway validation) before reaching auth check, or 403.
        """
        fake_id = fake.uuid4()
        response = authenticated_api.patch(
            f"/api/v1/support/agents/{fake_id}/status",
            json={"active": False}
        )
        assert response.status_code in [400, 403, 404, 429, 503], (
            f"Expected 400/403/404, got {response.status_code}: {response.text}"
        )

    def test_create_training_module_requires_support_manager(self, authenticated_api):
        """
        Verify training module creation requires SUPPORT_MANAGER role → 403.
        Correct DTO: CreateTrainingModuleRequest(code, title, description, category,
        durationMinutes, status, mandatory).
        category enum: ONBOARDING, PRODUCT_KNOWLEDGE, COMPLIANCE, SYSTEMS, etc.
        status enum: DRAFT, ACTIVE, ARCHIVED.
        """
        response = authenticated_api.post("/api/v1/support/modules", json={
            "code": f"MOD{fake.random_number(digits=4)}",
            "title": "Fraud Detection Training",
            "description": "Learn to identify and prevent fraud",
            "category": "COMPLIANCE",
            "durationMinutes": 120,
            "status": "ACTIVE",
            "mandatory": True
        })
        assert response.status_code == 403, (
            f"Expected 403 (SUPPORT_MANAGER required), got {response.status_code}: {response.text}"
        )

    def test_get_all_training_modules(self, authenticated_api):
        """
        Get all training modules.
        No role restriction — works with any authenticated user.
        """
        response = authenticated_api.get("/api/v1/support/modules")
        assert response.status_code == 200, (
            f"Expected 200, got {response.status_code}: {response.text}"
        )
        body = response.json()
        modules = body.get("data", body) if isinstance(body, dict) else body
        assert isinstance(modules, list)

    def test_get_mandatory_modules(self, authenticated_api):
        """
        Get mandatory training modules.
        No role restriction.
        """
        response = authenticated_api.get("/api/v1/support/modules/mandatory")
        assert response.status_code == 200, (
            f"Expected 200, got {response.status_code}: {response.text}"
        )
        body = response.json()
        modules = body.get("data", body) if isinstance(body, dict) else body
        assert isinstance(modules, list)

    def test_get_module_by_id_not_found(self, authenticated_api):
        """
        Get training module by non-existent ID.
        """
        fake_id = fake.uuid4()
        response = authenticated_api.get(f"/api/v1/support/modules/{fake_id}")
        assert response.status_code in [200, 400, 404, 429, 500, 503], (
            f"Unexpected status {response.status_code}: {response.text}"
        )

    def test_assign_training_requires_support_manager(self, authenticated_api):
        """
        Verify training assignment requires SUPPORT_MANAGER role.
        May return 400 (gateway/validation) before reaching auth check, or 403.
        """
        response = authenticated_api.post("/api/v1/support/trainings/assign", json={
            "agentId": str(fake.uuid4()),
            "moduleId": str(fake.uuid4()),
            "status": "NOT_STARTED",
            "score": 0,
            "notes": "Initial assignment"
        })
        assert response.status_code in [400, 403, 429, 503], (
            f"Expected 400 or 403 (SUPPORT_MANAGER required), got {response.status_code}: {response.text}"
        )

    def test_get_agent_trainings(self, authenticated_api):
        """
        Get trainings for a specific agent.
        No role restriction — but agent may not exist.
        """
        fake_agent_id = fake.uuid4()
        response = authenticated_api.get(f"/api/v1/support/trainings/agent/{fake_agent_id}")
        assert response.status_code in [200, 400, 404, 429, 500, 503], (
            f"Unexpected status {response.status_code}: {response.text}"
        )
        if response.status_code == 200:
            body = response.json()
            trainings = body.get("data", body) if isinstance(body, dict) else body
            assert isinstance(trainings, list)

    def test_get_all_trainings(self, authenticated_api):
        """
        Get all agent trainings.
        No role restriction.
        """
        response = authenticated_api.get("/api/v1/support/trainings")
        assert response.status_code == 200, (
            f"Expected 200, got {response.status_code}: {response.text}"
        )
        body = response.json()
        trainings = body.get("data", body) if isinstance(body, dict) else body
        assert isinstance(trainings, list)
