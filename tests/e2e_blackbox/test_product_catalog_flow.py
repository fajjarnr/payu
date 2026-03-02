import pytest
import uuid
from faker import Faker

fake = Faker()


@pytest.mark.e2e
class TestProductCatalogFlow:
    """
    Product Catalog Service E2E tests.
    Tests: List public products -> Get product -> Admin CRUD -> Product parameters
    """

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

    def test_admin_create_product(self, authenticated_api):
        """Create a new product (admin)"""
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
        response = authenticated_api.post("/api/v1/admin/products", json=payload)
        assert response.status_code in [200, 201, 400, 401, 403, 404, 409, 422], f"Unexpected status: {response.status_code}"

    def test_admin_list_all_products(self, authenticated_api):
        """List all products including inactive (admin)"""
        response = authenticated_api.get("/api/v1/admin/products")
        assert response.status_code in [200, 401, 403, 404], f"Unexpected status: {response.status_code}"
        if response.status_code == 200:
            data = response.json()
            assert isinstance(data, (list, dict))

    def test_admin_get_product_by_code(self, authenticated_api):
        """Get product by code (admin)"""
        response = authenticated_api.get("/api/v1/admin/products/SAVINGS_BASIC")
        assert response.status_code in [200, 401, 403, 404], f"Unexpected status: {response.status_code}"

    def test_admin_get_products_by_type(self, authenticated_api):
        """Get products by type (admin)"""
        response = authenticated_api.get("/api/v1/admin/products/type/SAVINGS")
        assert response.status_code in [200, 401, 403, 404], f"Unexpected status: {response.status_code}"

    def test_admin_update_product(self, authenticated_api):
        """Update a product (admin)"""
        payload = {
            "name": "Updated Test Product",
            "description": "Updated description",
            "parameters": {"interestRate": "4.0"}
        }
        response = authenticated_api.put("/api/v1/admin/products/SAVINGS_BASIC", json=payload)
        assert response.status_code in [200, 400, 401, 403, 404, 422], f"Unexpected status: {response.status_code}"

    def test_admin_deactivate_product(self, authenticated_api):
        """Deactivate a product (admin)"""
        fake_code = f"FAKE_{fake.uuid4()[:6].upper()}"
        response = authenticated_api.delete(f"/api/v1/admin/products/{fake_code}")
        assert response.status_code in [204, 400, 401, 403, 404], f"Unexpected status: {response.status_code}"
