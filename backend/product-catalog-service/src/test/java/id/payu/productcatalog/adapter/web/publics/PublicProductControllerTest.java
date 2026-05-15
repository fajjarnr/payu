package id.payu.productcatalog.adapter.web.publics;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.productcatalog.config.SecurityConfig;
import id.payu.productcatalog.domain.model.ProductDefinition;
import id.payu.productcatalog.domain.model.ProductType;
import id.payu.productcatalog.domain.port.in.ProductCatalogUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PublicProductController.class)
@Import(SecurityConfig.class)
@DisplayName("PublicProductController Unit Tests")
class PublicProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductCatalogUseCase productCatalogUseCase;

    // ── Helper ──────────────────────────────────────────────────────────────

    private ProductDefinition sampleProduct(String code, ProductType type, boolean active) {
        return ProductDefinition.builder()
                .productCode(code)
                .productType(type)
                .name("Test " + code)
                .description("Desc " + code)
                .active(active)
                .parameters(Map.of("key", "value"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ── Get Active Products ─────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /products")
    class GetActiveProducts {

        @Test
        @DisplayName("Should return active products")
        void shouldReturnActiveProducts() throws Exception {
            List<ProductDefinition> active = List.of(
                    sampleProduct("P1", ProductType.SAVINGS, true),
                    sampleProduct("P2", ProductType.LOAN, true)
            );
            when(productCatalogUseCase.getAllActiveProducts()).thenReturn(active);

            mockMvc.perform(get("/products"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].productCode").value("P1"))
                    .andExpect(jsonPath("$[0].active").value(true))
                    .andExpect(jsonPath("$[1].productCode").value("P2"));
        }

        @Test
        @DisplayName("Should return empty list when no active products")
        void shouldReturnEmptyListWhenNoneActive() throws Exception {
            when(productCatalogUseCase.getAllActiveProducts()).thenReturn(List.of());

            mockMvc.perform(get("/products"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("Should handle trailing slash on /products/")
        void shouldHandleTrailingSlash() throws Exception {
            when(productCatalogUseCase.getAllActiveProducts()).thenReturn(List.of());

            mockMvc.perform(get("/products/"))
                    .andExpect(status().isOk());
        }
    }

    // ── Get Product By Code ─────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /products/{code}")
    class GetProductByCode {

        @Test
        @DisplayName("Should return active product by code")
        void shouldReturnActiveProduct() throws Exception {
            ProductDefinition product = sampleProduct("SAVINGS_BASIC", ProductType.SAVINGS, true);
            when(productCatalogUseCase.getProduct("SAVINGS_BASIC")).thenReturn(Optional.of(product));

            mockMvc.perform(get("/products/SAVINGS_BASIC"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.productCode").value("SAVINGS_BASIC"))
                    .andExpect(jsonPath("$.productType").value("SAVINGS"))
                    .andExpect(jsonPath("$.active").value(true));
        }

        @Test
        @DisplayName("Should return 404 for inactive product")
        void shouldNotReturnInactiveProduct() throws Exception {
            ProductDefinition product = sampleProduct("INACTIVE_001", ProductType.SAVINGS, false);
            when(productCatalogUseCase.getProduct("INACTIVE_001")).thenReturn(Optional.of(product));

            mockMvc.perform(get("/products/INACTIVE_001"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 404 for non-existent product")
        void shouldReturn404ForUnknown() throws Exception {
            when(productCatalogUseCase.getProduct("UNKNOWN")).thenReturn(Optional.empty());

            mockMvc.perform(get("/products/UNKNOWN"))
                    .andExpect(status().isNotFound());
        }
    }

    // ── Get Product Parameter ───────────────────────────────────────────────

    @Nested
    @DisplayName("GET /products/{code}/parameters/{key}")
    class GetProductParameter {

        @Test
        @DisplayName("Should return parameter value for active product")
        void shouldReturnParameterValue() throws Exception {
            when(productCatalogUseCase.isProductActive("P1")).thenReturn(true);
            when(productCatalogUseCase.getProductParameter("P1", "rate", null)).thenReturn(0.05);

            mockMvc.perform(get("/products/P1/parameters/rate"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").value(0.05));
        }

        @Test
        @DisplayName("Should return default value when parameter not found")
        void shouldReturnDefaultValue() throws Exception {
            when(productCatalogUseCase.isProductActive("P1")).thenReturn(true);
            when(productCatalogUseCase.getProductParameter("P1", "missingKey", 0.0)).thenReturn(0.0);

            mockMvc.perform(get("/products/P1/parameters/missingKey")
                            .param("defaultValue", "0.0"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should return 404 when product is inactive")
        void shouldReturn404ForInactiveProduct() throws Exception {
            when(productCatalogUseCase.isProductActive("INACTIVE")).thenReturn(false);

            mockMvc.perform(get("/products/INACTIVE/parameters/rate"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 404 when product does not exist")
        void shouldReturn404ForUnknownProduct() throws Exception {
            when(productCatalogUseCase.isProductActive("UNKNOWN")).thenReturn(false);

            mockMvc.perform(get("/products/UNKNOWN/parameters/rate"))
                    .andExpect(status().isNotFound());
        }
    }
}
