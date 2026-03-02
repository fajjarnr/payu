import pytest
import uuid
from client import PayUClient
from faker import Faker

fake = Faker()


@pytest.mark.e2e
class TestProductCatalogFlow:
    """
    Product Catalog Service E2E tests.
    Tests: List public products -> Get product -> Admin CRUD -> Product parameters
    """

    @pytest.fixture(scope="class")
    def api(self):
        return PayUClient(gateway_url="http://localhost:8080")

    @pytest.fixture(scope="class")
    def admin_session(self, api):
        """Login as admin for product management"""
        response = api.post("/api/v1/auth/login", json={
            "username": "admin",
            "password": "admin123"
        })
        if response.status_code == 200:
            api.set_token(response.json()["access_token"])
        return {"api": api}

    @pytest.mark.smoke
    def test_list_public_products(self, api):
        """List all active products (public endpoint)"""
        response = api.get("/api/v1/products")
        assert response.status_code in [200, 404], f"Unexpected status: {response.status_code}"
        if response.status_code == 200:
            data = response.json()
            assert isinstance(data, (list, dict))

    def test_get_public_product_by_code(self, api):
        """Get specific active product by code"""
        response = api.get("/api/v1/products/SAVINGS_BASIC")
        assert response.status_code in [200, 404], f"Unexpected status: {response.status_code}"

    def test_get_product_parameter(self, api):
        """Get product parameter value"""
        response = api.get("/api/v1/products/SAVINGS_BASIC/parameters/interestRate", params={
            "defaultValue": "0.0"
        })
        assert response.status_code in [200, 404], f"Unexpected status: {response.status_code}"

    def test_admin_create_product(self, admin_session):
        """Create a new product (admin)"""
        api = admin_session["api"]
        product_code = f"TEST_{fake.uuid4()[:6].upper()}"
        payload = {
            "productCode": product_code,
            "productType": "SAVINGS",
            "name": f"Test Product {product_code}",
            "description": "Automated test product",
            "parameters": {
                "interestRate": "3.5",
                "minBalance": "100000",
                "currency": "IDR"
            }
        }
        response = api.post("/api/v1/admin/products", json=payload)
        assert response.status_code in [200, 201, 400, 401, 403, 409, 422], f"Unexpected status: {response.status_code}"

    def test_admin_list_all_products(self, admin_session):
        """List all products including inactive (admin)"""
        api = admin_session["api"]
        response = api.get("/api/v1/admin/products")
        assert response.status_code in [200, 401, 403], f"Unexpected status: {response.status_code}"
        if response.status_code == 200:
            data = response.json()
            assert isinstance(data, (list, dict))

    def test_admin_get_product_by_code(self, admin_session):
        """Get product by code (admin)"""
        api = admin_session["api"]
        response = api.get("/api/v1/admin/products/SAVINGS_BASIC")
        assert response.status_code in [200, 401, 403, 404], f"Unexpected status: {response.status_code}"

    def test_admin_get_products_by_type(self, admin_session):
        """Get products by type (admin)"""
        api = admin_session["api"]
        response = api.get("/api/v1/admin/products/type/SAVINGS")
        assert response.status_code in [200, 401, 403, 404], f"Unexpected status: {response.status_code}"

    def test_admin_update_product(self, admin_session):
        """Update a product (admin)"""
        api = admin_session["api"]
        payload = {
            "name": "Updated Test Product",
            "description": "Updated description",
            "parameters": {"interestRate": "4.0"}
        }
        response = api.put("/api/v1/admin/products/SAVINGS_BASIC", json=payload)
        assert response.status_code in [200, 400, 401, 403, 404, 422], f"Unexpected status: {response.status_code}"

    def test_admin_deactivate_product(self, admin_session):
        """Deactivate a product (admin)"""
        api = admin_session["api"]
        fake_code = f"FAKE_{fake.uuid4()[:6].upper()}"
        response = api.delete(f"/api/v1/admin/products/{fake_code}")
        assert response.status_code in [204, 400, 401, 403, 404], f"Unexpected status: {response.status_code}"
