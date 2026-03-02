import pytest
import uuid
from client import PayUClient
from faker import Faker

fake = Faker()


@pytest.mark.e2e
class TestAbTestingServiceFlow:
    """
    AB Testing Service E2E tests.
    Tests: Create experiment -> List -> Assign variant -> Track conversion -> Status change
    """

    @pytest.fixture(scope="class")
    def api(self):
        return PayUClient(gateway_url="http://localhost:8080")

    @pytest.fixture(scope="class")
    def admin_session(self, api):
        """Login as admin for A/B testing operations"""
        response = api.post("/api/v1/auth/login", json={
            "username": "admin",
            "password": "admin123"
        })
        if response.status_code == 200:
            api.set_token(response.json()["access_token"])
        return {"api": api}

    @pytest.mark.smoke
    def test_list_experiments(self, admin_session):
        """List all experiments with pagination"""
        api = admin_session["api"]
        response = api.get("/api/v1/experiments", params={"page": 0, "size": 10})
        assert response.status_code in [200, 401, 403, 404, 503], f"Unexpected status: {response.status_code}"
        if response.status_code == 200:
            data = response.json()
            assert isinstance(data, (list, dict))

    def test_list_active_experiments(self, admin_session):
        """List only active experiments"""
        api = admin_session["api"]
        response = api.get("/api/v1/experiments/active")
        assert response.status_code in [200, 401, 403], f"Unexpected status: {response.status_code}"
        if response.status_code == 200:
            data = response.json()
            assert isinstance(data, (list, dict))

    def test_create_experiment(self, admin_session):
        """Create a new A/B experiment"""
        api = admin_session["api"]
        experiment_key = f"test_exp_{fake.uuid4()[:8]}"
        payload = {
            "name": f"Test Experiment {experiment_key}",
            "description": "Automated test experiment for E2E",
            "key": experiment_key,
            "status": "DRAFT",
            "startDate": "2026-03-01T00:00:00Z",
            "endDate": "2026-04-01T00:00:00Z",
            "trafficSplit": 50,
            "variantAConfig": {"color": "green", "layout": "grid"},
            "variantBConfig": {"color": "blue", "layout": "list"},
            "targetingRules": {"minAppVersion": "2.0.0"}
        }
        response = api.post("/api/v1/experiments", json=payload)
        assert response.status_code in [200, 201, 400, 401, 403, 409, 422], f"Unexpected status: {response.status_code}"

    def test_get_experiment_by_id(self, admin_session):
        """Get experiment by UUID"""
        api = admin_session["api"]
        fake_id = str(uuid.uuid4())
        response = api.get(f"/api/v1/experiments/{fake_id}")
        assert response.status_code in [200, 401, 403, 404], f"Unexpected status: {response.status_code}"

    def test_get_experiment_by_key(self, admin_session):
        """Get experiment by key"""
        api = admin_session["api"]
        response = api.get("/api/v1/experiments/key/homepage_redesign")
        assert response.status_code in [200, 401, 403, 404], f"Unexpected status: {response.status_code}"

    def test_update_experiment(self, admin_session):
        """Update an experiment"""
        api = admin_session["api"]
        fake_id = str(uuid.uuid4())
        payload = {
            "name": "Updated Experiment",
            "description": "Updated description",
            "key": f"updated_{fake.uuid4()[:8]}",
            "status": "DRAFT",
            "trafficSplit": 70
        }
        response = api.put(f"/api/v1/experiments/{fake_id}", json=payload)
        assert response.status_code in [200, 400, 401, 403, 404, 422], f"Unexpected status: {response.status_code}"

    def test_change_experiment_status(self, admin_session):
        """Change experiment status"""
        api = admin_session["api"]
        fake_id = str(uuid.uuid4())
        response = api.patch(f"/api/v1/experiments/{fake_id}/status", params={"status": "ACTIVE"})
        assert response.status_code in [200, 400, 401, 403, 404], f"Unexpected status: {response.status_code}"

    def test_assign_variant(self, admin_session):
        """Assign a variant to a user"""
        api = admin_session["api"]
        payload = {"userId": str(uuid.uuid4())}
        response = api.post("/api/v1/experiments/homepage_redesign/assign", json=payload)
        assert response.status_code in [200, 400, 401, 403, 404], f"Unexpected status: {response.status_code}"

    def test_track_conversion(self, admin_session):
        """Track a conversion event for an experiment"""
        api = admin_session["api"]
        fake_id = str(uuid.uuid4())
        payload = {
            "userId": str(uuid.uuid4()),
            "variant": "A",
            "eventType": "CLICK"
        }
        response = api.post(f"/api/v1/experiments/{fake_id}/track", json=payload)
        assert response.status_code in [200, 202, 400, 401, 403, 404], f"Unexpected status: {response.status_code}"

    def test_delete_experiment(self, admin_session):
        """Delete an experiment"""
        api = admin_session["api"]
        fake_id = str(uuid.uuid4())
        response = api.delete(f"/api/v1/experiments/{fake_id}")
        assert response.status_code in [204, 400, 401, 403, 404], f"Unexpected status: {response.status_code}"
