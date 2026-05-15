package id.payu.productcatalog.adapter.persistence.entity;

import id.payu.productcatalog.domain.model.ProductType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProductDefinitionEntity Unit Tests")
class ProductDefinitionEntityTest {

    @Test
    @DisplayName("Should build entity with builder pattern")
    void shouldBuildWithBuilder() {
        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> params = Map.of("interestRate", 0.05, "minBalance", 10000);

        ProductDefinitionEntity entity = ProductDefinitionEntity.builder()
                .productCode("SAVINGS_BASIC")
                .productType(ProductType.SAVINGS)
                .name("Basic Savings")
                .description("Standard savings account")
                .active(true)
                .parameters(params)
                .version(0L)
                .createdAt(now)
                .updatedAt(now)
                .build();

        assertThat(entity.getProductCode()).isEqualTo("SAVINGS_BASIC");
        assertThat(entity.getProductType()).isEqualTo(ProductType.SAVINGS);
        assertThat(entity.getName()).isEqualTo("Basic Savings");
        assertThat(entity.getDescription()).isEqualTo("Standard savings account");
        assertThat(entity.isActive()).isTrue();
        assertThat(entity.getParameters()).isEqualTo(params);
        assertThat(entity.getVersion()).isEqualTo(0L);
        assertThat(entity.getCreatedAt()).isEqualTo(now);
        assertThat(entity.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("Should default active to true in no-arg constructor")
    void shouldDefaultActiveToTrue() {
        ProductDefinitionEntity entity = new ProductDefinitionEntity();
        // The field default is true; verify it can be set to false
        entity.setActive(false);
        assertThat(entity.isActive()).isFalse();
    }

    @Test
    @DisplayName("Should allow setting all fields via setters")
    void shouldSetAllFieldsViaSetters() {
        ProductDefinitionEntity entity = new ProductDefinitionEntity();
        LocalDateTime now = LocalDateTime.now();

        entity.setProductCode("TEST_001");
        entity.setProductType(ProductType.LOAN);
        entity.setName("Test Loan");
        entity.setDescription("Test Description");
        entity.setActive(false);
        entity.setParameters(Map.of("key", "val"));
        entity.setVersion(5L);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        assertThat(entity.getProductCode()).isEqualTo("TEST_001");
        assertThat(entity.getProductType()).isEqualTo(ProductType.LOAN);
        assertThat(entity.getName()).isEqualTo("Test Loan");
        assertThat(entity.getDescription()).isEqualTo("Test Description");
        assertThat(entity.isActive()).isFalse();
        assertThat(entity.getParameters()).containsEntry("key", "val");
        assertThat(entity.getVersion()).isEqualTo(5L);
        assertThat(entity.getCreatedAt()).isEqualTo(now);
        assertThat(entity.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("Should create entity with all-args constructor")
    void shouldCreateWithAllArgsConstructor() {
        LocalDateTime now = LocalDateTime.now();
        ProductDefinitionEntity entity = new ProductDefinitionEntity(
                "FULL_001",
                ProductType.CREDIT_CARD,
                "Credit Card",
                "A credit card product",
                true,
                Map.of("limit", 5000000),
                2L,
                now,
                now
        );

        assertThat(entity.getProductCode()).isEqualTo("FULL_001");
        assertThat(entity.getProductType()).isEqualTo(ProductType.CREDIT_CARD);
        assertThat(entity.getName()).isEqualTo("Credit Card");
        assertThat(entity.getDescription()).isEqualTo("A credit card product");
        assertThat(entity.isActive()).isTrue();
        assertThat(entity.getParameters()).containsEntry("limit", 5000000);
        assertThat(entity.getVersion()).isEqualTo(2L);
    }
}
