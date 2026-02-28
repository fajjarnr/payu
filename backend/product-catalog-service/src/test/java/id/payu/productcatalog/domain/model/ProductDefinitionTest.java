package id.payu.productcatalog.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProductDefinitionTest {

    @Test
    void shouldActivateProduct() {
        ProductDefinition product = createInactiveProduct();
        assertFalse(product.isActive());

        product.activate();

        assertTrue(product.isActive());
        assertNotNull(product.getUpdatedAt());
    }

    @Test
    void shouldDeactivateProduct() {
        ProductDefinition product = createActiveProduct();
        assertTrue(product.isActive());

        product.deactivate();

        assertFalse(product.isActive());
        assertNotNull(product.getUpdatedAt());
    }

    @Test
    void shouldUpdateParameters() {
        ProductDefinition product = createActiveProduct();
        Map<String, Object> newParams = new HashMap<>();
        newParams.put("newKey", "newValue");

        product.updateParameters(newParams);

        assertEquals(newParams, product.getParameters());
        assertNotNull(product.getUpdatedAt());
    }

    @Test
    void shouldGetParameterValue() {
        Map<String, Object> params = new HashMap<>();
        params.put("interestRate", 0.05);
        params.put("minAmount", 10000);

        ProductDefinition product = ProductDefinition.builder()
                .productCode("TEST_001")
                .productType(ProductType.SAVINGS)
                .name("Test Product")
                .parameters(params)
                .build();

        Double interestRate = product.getParameter("interestRate");
        Integer minAmount = product.getParameter("minAmount");

        assertEquals(0.05, interestRate);
        assertEquals(10000, minAmount);
    }

    @Test
    void shouldReturnNullForMissingParameter() {
        ProductDefinition product = createActiveProduct();

        Object value = product.getParameter("nonExistent");

        assertNull(value);
    }

    @Test
    void shouldCheckIfParameterExists() {
        Map<String, Object> params = new HashMap<>();
        params.put("existingKey", "value");

        ProductDefinition product = ProductDefinition.builder()
                .productCode("TEST_001")
                .productType(ProductType.SAVINGS)
                .name("Test Product")
                .parameters(params)
                .build();

        assertTrue(product.hasParameter("existingKey"));
        assertFalse(product.hasParameter("missingKey"));
    }

    @Test
    void shouldValidateRequiredParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put("key1", "value1");
        params.put("key2", "value2");

        ProductDefinition product = ProductDefinition.builder()
                .productCode("TEST_001")
                .productType(ProductType.SAVINGS)
                .name("Test Product")
                .parameters(params)
                .build();

        assertTrue(product.hasRequiredParameters("key1", "key2"));
        assertFalse(product.hasRequiredParameters("key1", "key3"));
    }

    @Test
    void shouldReturnFalseForRequiredParametersWhenParamsNull() {
        ProductDefinition product = ProductDefinition.builder()
                .productCode("TEST_001")
                .productType(ProductType.SAVINGS)
                .name("Test Product")
                .build();

        assertFalse(product.hasRequiredParameters("anyKey"));
    }

    @Test
    void shouldBuildProductUsingBuilder() {
        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> params = new HashMap<>();
        params.put("key", "value");

        ProductDefinition product = ProductDefinition.builder()
                .productCode("TEST_001")
                .productType(ProductType.LOAN)
                .name("Test Loan")
                .description("Test Description")
                .active(true)
                .parameters(params)
                .createdAt(now)
                .updatedAt(now)
                .version(1L)
                .build();

        assertEquals("TEST_001", product.getProductCode());
        assertEquals(ProductType.LOAN, product.getProductType());
        assertEquals("Test Loan", product.getName());
        assertEquals("Test Description", product.getDescription());
        assertTrue(product.isActive());
        assertEquals(params, product.getParameters());
        assertEquals(now, product.getCreatedAt());
        assertEquals(now, product.getUpdatedAt());
        assertEquals(1L, product.getVersion());
    }

    private ProductDefinition createActiveProduct() {
        return ProductDefinition.builder()
                .productCode("TEST_001")
                .productType(ProductType.SAVINGS)
                .name("Test Product")
                .active(true)
                .build();
    }

    private ProductDefinition createInactiveProduct() {
        return ProductDefinition.builder()
                .productCode("TEST_002")
                .productType(ProductType.LOAN)
                .name("Test Product")
                .active(false)
                .build();
    }
}
