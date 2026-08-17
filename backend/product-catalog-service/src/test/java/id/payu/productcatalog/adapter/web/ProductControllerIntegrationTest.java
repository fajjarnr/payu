package id.payu.productcatalog.adapter.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.productcatalog.domain.model.ProductType;
import id.payu.productcatalog.interfaces.dto.CreateProductRequest;
import id.payu.productcatalog.interfaces.dto.UpdateProductRequest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Product Catalog REST endpoints.
 * Uses Testcontainers for PostgreSQL database.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Tag("integration")
class ProductControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("product_catalog_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldGetAllActiveProducts() throws Exception {
        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].productCode").exists())
                .andExpect(jsonPath("$.data[0].active").value(true));
    }

    @Test
    void shouldGetProductByCode() throws Exception {
        // Assuming SAVINGS_BASIC is seeded
        mockMvc.perform(get("/products/SAVINGS_BASIC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.productCode").value("SAVINGS_BASIC"))
                .andExpect(jsonPath("$.data.productType").value("SAVINGS"))
                .andExpect(jsonPath("$.data.active").value(true));
    }

    @Test
    void shouldReturnNotFoundForUnknownProduct() throws Exception {
        mockMvc.perform(get("/products/UNKNOWN_PRODUCT"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldGetProductParameter() throws Exception {
        mockMvc.perform(get("/products/SAVINGS_BASIC/parameters/minimumBalance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void shouldGetProductsByType() throws Exception {
        mockMvc.perform(get("/products").param("type", "SAVINGS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].productType").value("SAVINGS"));
    }

    @Test
    void shouldCreateProductAsAdmin() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("testParam", "testValue");

        CreateProductRequest request = CreateProductRequest.builder()
                .productCode("TEST_PRODUCT_001")
                .productType(ProductType.LOAN)
                .name("Test Loan Product")
                .description("A test loan product")
                .parameters(params)
                .build();

        mockMvc.perform(post("/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.productCode").value("TEST_PRODUCT_001"))
                .andExpect(jsonPath("$.data.active").value(true));
    }

    @Test
    void shouldReturnConflictForDuplicateProduct() throws Exception {
        // First create
        CreateProductRequest request = CreateProductRequest.builder()
                .productCode("DUPLICATE_TEST")
                .productType(ProductType.SAVINGS)
                .name("Duplicate Test")
                .build();

        mockMvc.perform(post("/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Try to create again
        mockMvc.perform(post("/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldUpdateProductAsAdmin() throws Exception {
        // First create
        CreateProductRequest createRequest = CreateProductRequest.builder()
                .productCode("UPDATE_TEST")
                .productType(ProductType.LOAN)
                .name("Original Name")
                .build();

        mockMvc.perform(post("/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated());

        // Then update
        UpdateProductRequest updateRequest = UpdateProductRequest.builder()
                .name("Updated Name")
                .description("Updated Description")
                .build();

        mockMvc.perform(put("/admin/products/UPDATE_TEST")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Updated Name"))
                .andExpect(jsonPath("$.data.description").value("Updated Description"));
    }

    @Test
    void shouldDeactivateAndActivateProduct() throws Exception {
        // Create
        CreateProductRequest request = CreateProductRequest.builder()
                .productCode("TOGGLE_TEST")
                .productType(ProductType.SAVINGS)
                .name("Toggle Test")
                .build();

        mockMvc.perform(post("/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Deactivate
        mockMvc.perform(post("/admin/products/TOGGLE_TEST/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));

        // Verify not in public list
        mockMvc.perform(get("/products/TOGGLE_TEST"))
                .andExpect(status().isNotFound());

        // Activate
        mockMvc.perform(post("/admin/products/TOGGLE_TEST/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(true));

        // Verify back in public list
        mockMvc.perform(get("/products/TOGGLE_TEST"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturnBadRequestForInvalidProductType() throws Exception {
        // Use raw JSON to send an invalid product type string that can't deserialize to ProductType enum
        String invalidJson = "{\"productCode\":\"INVALID_TYPE\",\"productType\":\"INVALID\",\"name\":\"Invalid Type Test\"}";

        mockMvc.perform(post("/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestForMissingRequiredFields() throws Exception {
        CreateProductRequest request = CreateProductRequest.builder()
                .productCode("MISSING_FIELDS")
                // Missing productType and name — left null
                .build();

        mockMvc.perform(post("/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldSearchProducts() throws Exception {
        mockMvc.perform(get("/admin/products")
                        .param("search", "SAVINGS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].productCode").exists());
    }

    @Test
    void shouldPaginateResults() throws Exception {
        mockMvc.perform(get("/products")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.pagination").exists())
                .andExpect(jsonPath("$.pagination.page").value(0))
                .andExpect(jsonPath("$.pagination.size").value(5));
    }
}
