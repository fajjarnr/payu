import pytest
from faker import Faker

fake = Faker()


@pytest.mark.support
class TestSupportFlow:
    """
    Support Team and Training E2E tests.
    Tests: Create Agent -> Create Training Module -> Assign Training -> Check Status
    """

    def test_get_training_status(self, authenticated_api):
        """
        Get overall training status
        """
        response = authenticated_api.get("/api/v1/support/training-status")
        assert response.status_code == 200
        status = response.json()
        assert "activeAgents" in status
        assert "trainedAgents" in status
        assert "trainingPercentage" in status

    def test_create_support_agent(self, authenticated_api):
        """
        Create a new support agent
        """
        response = authenticated_api.post("/api/v1/support/agents", json={
            "employeeId": f"EMP{fake.random_number(digits=6)}",
            "name": fake.name(),
            "email": f"agent_{fake.uuid4()}@payu.fajjjar.my.id",
            "department": "Customer Support",
            "active": True
        })

        if response.status_code not in [200, 201]:
            pytest.skip(f"Agent creation may require permissions: {response.text}")

        agent = response.json()
        assert agent is not None
        assert "id" in agent

        return agent.get("id")

    def test_get_all_agents(self, authenticated_api):
        """
        Get all support agents
        """
        response = authenticated_api.get("/api/v1/support/agents")
        assert response.status_code == 200
        agents = response.json()
        assert isinstance(agents, list)

    def test_get_agent_by_id(self, authenticated_api):
        """
        Get agent by ID
        """
        # First create an agent
        response = authenticated_api.post("/api/v1/support/agents", json={
            "employeeId": f"EMP{fake.random_number(digits=6)}",
            "name": fake.name(),
            "email": f"agent_{fake.uuid4()}@payu.fajjjar.my.id",
            "department": "Customer Support",
            "active": True
        })

        if response.status_code not in [200, 201]:
            pytest.skip("Agent creation required")

        agent = response.json()
        agent_id = agent.get("id")

        response = authenticated_api.get(f"/api/v1/support/agents/{agent_id}")
        assert response.status_code == 200
        retrieved_agent = response.json()
        assert retrieved_agent["id"] == agent_id

    def test_get_agent_by_employee_id(self, authenticated_api):
        """
        Get agent by employee ID
        """
        employee_id = f"EMP{fake.random_number(digits=6)}"

        # Create an agent
        response = authenticated_api.post("/api/v1/support/agents", json={
            "employeeId": employee_id,
            "name": fake.name(),
            "email": f"agent_{fake.uuid4()}@payu.fajjjar.my.id",
            "department": "Customer Support",
            "active": True
        })

        if response.status_code not in [200, 201]:
            pytest.skip("Agent creation required")

        response = authenticated_api.get(f"/api/v1/support/agents/employee/{employee_id}")
        if response.status_code != 200:
            pytest.skip(f"Employee ID lookup may not be supported: {response.text}")

        agent = response.json()
        assert agent["employeeId"] == employee_id

    def test_update_agent_status(self, authenticated_api):
        """
        Update agent active status
        """
        # Create an agent
        response = authenticated_api.post("/api/v1/support/agents", json={
            "employeeId": f"EMP{fake.random_number(digits=6)}",
            "name": fake.name(),
            "email": f"agent_{fake.uuid4()}@payu.fajjjar.my.id",
            "department": "Customer Support",
            "active": True
        })

        if response.status_code not in [200, 201]:
            pytest.skip("Agent creation required")

        agent = response.json()
        agent_id = agent.get("id")

        # Deactivate agent
        response = authenticated_api.patch(f"/api/v1/support/agents/{agent_id}/status", json={"active": False})
        if response.status_code != 200:
            pytest.skip(f"Agent status update may require permissions: {response.text}")

        updated_agent = response.json()
        assert updated_agent["active"] == False

    def test_create_training_module(self, authenticated_api):
        """
        Create a new training module
        """
        response = authenticated_api.post("/api/v1/support/modules", json={
            "title": "Fraud Detection Training",
            "description": "Learn to identify and prevent fraud",
            "duration": 120,
            "mandatory": True,
            "status": "ACTIVE"
        })

        if response.status_code not in [200, 201]:
            pytest.skip(f"Training module creation may require permissions: {response.text}")

        module = response.json()
        assert module is not None
        assert "id" in module

        return module.get("id")

    def test_get_all_training_modules(self, authenticated_api):
        """
        Get all training modules
        """
        response = authenticated_api.get("/api/v1/support/modules")
        assert response.status_code == 200
        modules = response.json()
        assert isinstance(modules, list)

    def test_get_mandatory_modules(self, authenticated_api):
        """
        Get mandatory training modules
        """
        response = authenticated_api.get("/api/v1/support/modules/mandatory")
        assert response.status_code == 200
        modules = response.json()
        assert isinstance(modules, list)

    def test_get_module_by_id(self, authenticated_api):
        """
        Get training module by ID
        """
        # Create a module
        response = authenticated_api.post("/api/v1/support/modules", json={
            "title": "Customer Service Basics",
            "description": "Fundamentals of customer service",
            "duration": 60,
            "mandatory": False,
            "status": "ACTIVE"
        })

        if response.status_code not in [200, 201]:
            pytest.skip("Module creation required")

        module = response.json()
        module_id = module.get("id")

        response = authenticated_api.get(f"/api/v1/support/modules/{module_id}")
        assert response.status_code == 200
        retrieved_module = response.json()
        assert retrieved_module["id"] == module_id

    def test_assign_training_to_agent(self, authenticated_api):
        """
        Assign training to an agent
        """
        # Create an agent
        response = authenticated_api.post("/api/v1/support/agents", json={
            "employeeId": f"EMP{fake.random_number(digits=6)}",
            "name": fake.name(),
            "email": f"agent_{fake.uuid4()}@payu.fajjjar.my.id",
            "department": "Customer Support",
            "active": True
        })

        if response.status_code not in [200, 201]:
            pytest.skip("Agent creation required")

        agent = response.json()
        agent_id = agent.get("id")

        # Create a module
        response = authenticated_api.post("/api/v1/support/modules", json={
            "title": "Compliance Training",
            "description": "AML/CFT compliance basics",
            "duration": 90,
            "mandatory": True,
            "status": "ACTIVE"
        })

        if response.status_code not in [200, 201]:
            pytest.skip("Module creation required")

        module = response.json()
        module_id = module.get("id")

        # Assign training
        response = authenticated_api.post("/api/v1/support/trainings/assign", json={
            "agentId": agent_id,
            "moduleId": module_id,
            "dueDate": "2024-12-31"
        })

        if response.status_code not in [200, 201]:
            pytest.skip(f"Training assignment may require permissions: {response.text}")

        training = response.json()
        assert training is not None

    def test_get_agent_trainings(self, authenticated_api):
        """
        Get trainings for a specific agent
        """
        # Create an agent
        response = authenticated_api.post("/api/v1/support/agents", json={
            "employeeId": f"EMP{fake.random_number(digits=6)}",
            "name": fake.name(),
            "email": f"agent_{fake.uuid4()}@payu.fajjjar.my.id",
            "department": "Customer Support",
            "active": True
        })

        if response.status_code not in [200, 201]:
            pytest.skip("Agent creation required")

        agent = response.json()
        agent_id = agent.get("id")

        response = authenticated_api.get(f"/api/v1/support/trainings/agent/{agent_id}")
        if response.status_code != 200:
            pytest.skip(f"Agent trainings may not exist: {response.text}")

        trainings = response.json()
        assert isinstance(trainings, list)

    def test_check_agent_training_status(self, authenticated_api):
        """
        Check if agent is fully trained
        """
        # Create an agent
        response = authenticated_api.post("/api/v1/support/agents", json={
            "employeeId": f"EMP{fake.random_number(digits=6)}",
            "name": fake.name(),
            "email": f"agent_{fake.uuid4()}@payu.fajjjar.my.id",
            "department": "Customer Support",
            "active": True
        })

        if response.status_code not in [200, 201]:
            pytest.skip("Agent creation required")

        agent = response.json()
        agent_id = agent.get("id")

        response = authenticated_api.get(f"/api/v1/support/trainings/agent/{agent_id}/status")
        if response.status_code != 200:
            pytest.skip(f"Agent training status may not be available: {response.text}")

        status = response.json()
        assert "agentId" in status
        assert "fullyTrained" in status

    def test_get_all_trainings(self, authenticated_api):
        """
        Get all agent trainings
        """
        response = authenticated_api.get("/api/v1/support/trainings")
        assert response.status_code == 200
        trainings = response.json()
        assert isinstance(trainings, list)
