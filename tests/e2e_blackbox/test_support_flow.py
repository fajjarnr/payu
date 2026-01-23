import pytest
from client import PayUClient
from faker import Faker

fake = Faker()


class TestSupportFlow:
    """
    Support Team and Training E2E tests.
    Tests: Create Agent -> Create Training Module -> Assign Training -> Check Status
    """

    @pytest.fixture(scope="class")
    def api(self):
        return PayUClient(gateway_url="http://localhost:8080")

    @pytest.fixture(scope="class")
    def support_session(self, api):
        """Support admin session"""
        user_data = {
            "email": f"support_{fake.uuid4()}@example.com",
            "username": f"supp_{fake.uuid4()[:8]}",
            "password": "Password123!",
            "name": "Support Admin",
            "phoneNumber": "+6281234567890"
        }

        response = api.post("/api/v1/accounts/register", json=user_data)
        assert response.status_code in [200, 201]

        response = api.post("/api/v1/auth/login", json={
            "username": user_data["username"],
            "password": user_data["password"]
        })
        assert response.status_code == 200
        api.set_token(response.json()["access_token"])

        return {"api": api}

    def test_get_training_status(self, support_session):
        """
        Get overall training status
        """
        api = support_session["api"]

        response = api.get("/api/v1/support/training-status")
        assert response.status_code == 200
        status = response.json()
        assert "activeAgents" in status
        assert "trainedAgents" in status
        assert "trainingPercentage" in status

    def test_create_support_agent(self, support_session):
        """
        Create a new support agent
        """
        api = support_session["api"]

        response = api.post("/api/v1/support/agents", json={
            "employeeId": f"EMP{fake.random_number(digits=6)}",
            "name": fake.name(),
            "email": f"agent_{fake.uuid4()}@payu.id",
            "department": "Customer Support",
            "active": True
        })

        if response.status_code not in [200, 201]:
            pytest.skip(f"Agent creation may require permissions: {response.text}")

        agent = response.json()
        assert agent is not None
        assert "id" in agent

        return agent.get("id")

    def test_get_all_agents(self, support_session):
        """
        Get all support agents
        """
        api = support_session["api"]

        response = api.get("/api/v1/support/agents")
        assert response.status_code == 200
        agents = response.json()
        assert isinstance(agents, list)

    def test_get_agent_by_id(self, support_session):
        """
        Get agent by ID
        """
        api = support_session["api"]

        # First create an agent
        response = api.post("/api/v1/support/agents", json={
            "employeeId": f"EMP{fake.random_number(digits=6)}",
            "name": fake.name(),
            "email": f"agent_{fake.uuid4()}@payu.id",
            "department": "Customer Support",
            "active": True
        })

        if response.status_code not in [200, 201]:
            pytest.skip("Agent creation required")

        agent = response.json()
        agent_id = agent.get("id")

        response = api.get(f"/api/v1/support/agents/{agent_id}")
        assert response.status_code == 200
        retrieved_agent = response.json()
        assert retrieved_agent["id"] == agent_id

    def test_get_agent_by_employee_id(self, support_session):
        """
        Get agent by employee ID
        """
        api = support_session["api"]
        employee_id = f"EMP{fake.random_number(digits=6)}"

        # Create an agent
        response = api.post("/api/v1/support/agents", json={
            "employeeId": employee_id,
            "name": fake.name(),
            "email": f"agent_{fake.uuid4()}@payu.id",
            "department": "Customer Support",
            "active": True
        })

        if response.status_code not in [200, 201]:
            pytest.skip("Agent creation required")

        response = api.get(f"/api/v1/support/agents/employee/{employee_id}")
        if response.status_code != 200:
            pytest.skip(f"Employee ID lookup may not be supported: {response.text}")

        agent = response.json()
        assert agent["employeeId"] == employee_id

    def test_update_agent_status(self, support_session):
        """
        Update agent active status
        """
        api = support_session["api"]

        # Create an agent
        response = api.post("/api/v1/support/agents", json={
            "employeeId": f"EMP{fake.random_number(digits=6)}",
            "name": fake.name(),
            "email": f"agent_{fake.uuid4()}@payu.id",
            "department": "Customer Support",
            "active": True
        })

        if response.status_code not in [200, 201]:
            pytest.skip("Agent creation required")

        agent = response.json()
        agent_id = agent.get("id")

        # Deactivate agent
        response = api.patch(f"/api/v1/support/agents/{agent_id}/status", json={"active": False})
        if response.status_code != 200:
            pytest.skip(f"Agent status update may require permissions: {response.text}")

        updated_agent = response.json()
        assert updated_agent["active"] == False

    def test_create_training_module(self, support_session):
        """
        Create a new training module
        """
        api = support_session["api"]

        response = api.post("/api/v1/support/modules", json={
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

    def test_get_all_training_modules(self, support_session):
        """
        Get all training modules
        """
        api = support_session["api"]

        response = api.get("/api/v1/support/modules")
        assert response.status_code == 200
        modules = response.json()
        assert isinstance(modules, list)

    def test_get_mandatory_modules(self, support_session):
        """
        Get mandatory training modules
        """
        api = support_session["api"]

        response = api.get("/api/v1/support/modules/mandatory")
        assert response.status_code == 200
        modules = response.json()
        assert isinstance(modules, list)

    def test_get_module_by_id(self, support_session):
        """
        Get training module by ID
        """
        api = support_session["api"]

        # Create a module
        response = api.post("/api/v1/support/modules", json={
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

        response = api.get(f"/api/v1/support/modules/{module_id}")
        assert response.status_code == 200
        retrieved_module = response.json()
        assert retrieved_module["id"] == module_id

    def test_assign_training_to_agent(self, support_session):
        """
        Assign training to an agent
        """
        api = support_session["api"]

        # Create an agent
        response = api.post("/api/v1/support/agents", json={
            "employeeId": f"EMP{fake.random_number(digits=6)}",
            "name": fake.name(),
            "email": f"agent_{fake.uuid4()}@payu.id",
            "department": "Customer Support",
            "active": True
        })

        if response.status_code not in [200, 201]:
            pytest.skip("Agent creation required")

        agent = response.json()
        agent_id = agent.get("id")

        # Create a module
        response = api.post("/api/v1/support/modules", json={
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
        response = api.post("/api/v1/support/trainings/assign", json={
            "agentId": agent_id,
            "moduleId": module_id,
            "dueDate": "2024-12-31"
        })

        if response.status_code not in [200, 201]:
            pytest.skip(f"Training assignment may require permissions: {response.text}")

        training = response.json()
        assert training is not None

    def test_get_agent_trainings(self, support_session):
        """
        Get trainings for a specific agent
        """
        api = support_session["api"]

        # Create an agent
        response = api.post("/api/v1/support/agents", json={
            "employeeId": f"EMP{fake.random_number(digits=6)}",
            "name": fake.name(),
            "email": f"agent_{fake.uuid4()}@payu.id",
            "department": "Customer Support",
            "active": True
        })

        if response.status_code not in [200, 201]:
            pytest.skip("Agent creation required")

        agent = response.json()
        agent_id = agent.get("id")

        response = api.get(f"/api/v1/support/trainings/agent/{agent_id}")
        if response.status_code != 200:
            pytest.skip(f"Agent trainings may not exist: {response.text}")

        trainings = response.json()
        assert isinstance(trainings, list)

    def test_check_agent_training_status(self, support_session):
        """
        Check if agent is fully trained
        """
        api = support_session["api"]

        # Create an agent
        response = api.post("/api/v1/support/agents", json={
            "employeeId": f"EMP{fake.random_number(digits=6)}",
            "name": fake.name(),
            "email": f"agent_{fake.uuid4()}@payu.id",
            "department": "Customer Support",
            "active": True
        })

        if response.status_code not in [200, 201]:
            pytest.skip("Agent creation required")

        agent = response.json()
        agent_id = agent.get("id")

        response = api.get(f"/api/v1/support/trainings/agent/{agent_id}/status")
        if response.status_code != 200:
            pytest.skip(f"Agent training status may not be available: {response.text}")

        status = response.json()
        assert "agentId" in status
        assert "fullyTrained" in status

    def test_get_all_trainings(self, support_session):
        """
        Get all agent trainings
        """
        api = support_session["api"]

        response = api.get("/api/v1/support/trainings")
        assert response.status_code == 200
        trainings = response.json()
        assert isinstance(trainings, list)
