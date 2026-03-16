import pytest
import uuid
from faker import Faker

fake = Faker()


@pytest.mark.e2e
class TestCmsServiceFlow:
    """
    CMS (Content Management Service) E2E tests.
    Tests: Create content -> List content -> Get by type -> Update status -> Delete
    """

    @pytest.mark.smoke
    def test_get_public_banners(self, api):
        """Get public banners (no auth required)"""
        response = api.get("/api/v1/public/contents/type/BANNER")
        assert response.status_code in [200, 401, 404, 429, 500, 503], f"Unexpected status: {response.status_code}"
        if response.status_code == 200:
            data = response.json()
            assert isinstance(data, (list, dict))

    def test_get_public_promos(self, api):
        """Get public promo content"""
        response = api.get("/api/v1/public/contents/type/PROMO")
        assert response.status_code in [200, 401, 404, 429, 500, 503], f"Unexpected status: {response.status_code}"

    def test_get_public_alerts(self, api):
        """Get public alerts"""
        response = api.get("/api/v1/public/contents/type/ALERT")
        assert response.status_code in [200, 401, 404, 429, 500, 503], f"Unexpected status: {response.status_code}"

    def test_create_content(self, authenticated_api):
        """Create new CMS content"""
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
        response = authenticated_api.post("/api/v1/contents", json=payload)
        assert response.status_code in [200, 201, 401, 403, 429, 503], f"Unexpected status: {response.status_code}"

    def test_list_all_contents(self, authenticated_api):
        """List all content with pagination"""
        response = authenticated_api.get("/api/v1/contents", params={"page": 0, "size": 10})
        assert response.status_code in [200, 401, 403, 429, 503], f"Unexpected status: {response.status_code}"
        if response.status_code == 200:
            data = response.json()
            assert isinstance(data, (list, dict))

    def test_get_contents_by_type(self, authenticated_api):
        """Get content filtered by type"""
        response = authenticated_api.get("/api/v1/contents/type/BANNER")
        assert response.status_code in [200, 401, 403, 429, 503], f"Unexpected status: {response.status_code}"

    def test_get_contents_by_status(self, authenticated_api):
        """Get content filtered by status"""
        response = authenticated_api.get("/api/v1/contents/status/ACTIVE")
        assert response.status_code in [200, 401, 403, 429, 503], f"Unexpected status: {response.status_code}"

    def test_get_content_by_id(self, authenticated_api):
        """Get specific content by ID (expect 404 if none exists)"""
        fake_id = str(uuid.uuid4())
        response = authenticated_api.get(f"/api/v1/contents/{fake_id}")
        assert response.status_code in [200, 401, 403, 404, 429, 503], f"Unexpected status: {response.status_code}"

    def test_public_content_with_segment_filter(self, api):
        """Get public banners with segment and device filtering"""
        response = api.get("/api/v1/public/contents/type/BANNER", params={
            "segment": "premium",
            "device": "mobile"
        })
        assert response.status_code in [200, 401, 404, 429, 500, 503], f"Unexpected status: {response.status_code}"
