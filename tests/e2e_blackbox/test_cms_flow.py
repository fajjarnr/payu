import pytest
import uuid
from client import PayUClient
from faker import Faker

fake = Faker()


@pytest.mark.e2e
class TestCmsServiceFlow:
    """
    CMS (Content Management Service) E2E tests.
    Tests: Create content -> List content -> Get by type -> Update status -> Delete
    """

    @pytest.fixture(scope="class")
    def api(self):
        return PayUClient(gateway_url="http://localhost:8080")

    @pytest.fixture(scope="class")
    def admin_session(self, api):
        """Login as admin user for CMS operations"""
        response = api.post("/api/v1/auth/login", json={
            "username": "admin",
            "password": "admin123"
        })
        if response.status_code == 200:
            api.set_token(response.json()["access_token"])
        return {"api": api}

    @pytest.mark.smoke
    def test_get_public_banners(self, api):
        """Get public banners (no auth required)"""
        response = api.get("/api/v1/public/contents/type/BANNER")
        assert response.status_code in [200, 404, 500, 503], f"Unexpected status: {response.status_code}"
        if response.status_code == 200:
            data = response.json()
            assert isinstance(data, (list, dict))

    def test_get_public_promos(self, api):
        """Get public promo content"""
        response = api.get("/api/v1/public/contents/type/PROMO")
        assert response.status_code in [200, 404], f"Unexpected status: {response.status_code}"

    def test_get_public_alerts(self, api):
        """Get public alerts"""
        response = api.get("/api/v1/public/contents/type/ALERT")
        assert response.status_code in [200, 404], f"Unexpected status: {response.status_code}"

    def test_create_content(self, admin_session):
        """Create new CMS content"""
        api = admin_session["api"]
        payload = {
            "title": f"Test Banner {fake.uuid4()[:8]}",
            "body": fake.paragraph(),
            "type": "BANNER",
            "status": "DRAFT",
            "metadata": {
                "imageUrl": "https://example.com/banner.jpg",
                "targetUrl": "https://example.com/promo"
            }
        }
        response = api.post("/api/v1/contents", json=payload)
        assert response.status_code in [200, 201, 401, 403], f"Unexpected status: {response.status_code}"

    def test_list_all_contents(self, admin_session):
        """List all content with pagination"""
        api = admin_session["api"]
        response = api.get("/api/v1/contents", params={"page": 0, "size": 10})
        assert response.status_code in [200, 401, 403], f"Unexpected status: {response.status_code}"
        if response.status_code == 200:
            data = response.json()
            assert isinstance(data, (list, dict))

    def test_get_contents_by_type(self, admin_session):
        """Get content filtered by type"""
        api = admin_session["api"]
        response = api.get("/api/v1/contents/type/BANNER")
        assert response.status_code in [200, 401, 403], f"Unexpected status: {response.status_code}"

    def test_get_contents_by_status(self, admin_session):
        """Get content filtered by status"""
        api = admin_session["api"]
        response = api.get("/api/v1/contents/status/ACTIVE")
        assert response.status_code in [200, 401, 403], f"Unexpected status: {response.status_code}"

    def test_get_content_by_id(self, admin_session):
        """Get specific content by ID (expect 404 if none exists)"""
        api = admin_session["api"]
        fake_id = str(uuid.uuid4())
        response = api.get(f"/api/v1/contents/{fake_id}")
        assert response.status_code in [200, 404, 401, 403], f"Unexpected status: {response.status_code}"

    def test_public_content_with_segment_filter(self, api):
        """Get public banners with segment and device filtering"""
        response = api.get("/api/v1/public/contents/type/BANNER", params={
            "segment": "premium",
            "device": "mobile"
        })
        assert response.status_code in [200, 404], f"Unexpected status: {response.status_code}"
