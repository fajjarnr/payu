package id.payu.productcatalog.adapter.persistence;

import id.payu.productcatalog.adapter.persistence.entity.ProductDefinitionEntity;
import id.payu.productcatalog.adapter.persistence.repository.ProductDefinitionJpaRepository;
import id.payu.productcatalog.domain.model.ProductDefinition;
import id.payu.productcatalog.domain.model.ProductType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductCatalogPersistenceAdapter Unit Tests")
class ProductCatalogPersistenceAdapterTest {

    @Mock
    private ProductDefinitionJpaRepository jpaRepository;

    @InjectMocks
    private ProductCatalogPersistenceAdapter adapter;

    // ── Helpers ─────────────────────────────────────────────────────────────

    private ProductDefinitionEntity sampleEntity(String code) {
        return ProductDefinitionEntity.builder()
                .productCode(code)
                .productType(ProductType.SAVINGS)
                .name("Test " + code)
                .description("Desc " + code)
                .active(true)
                .parameters(Map.of("key", "value"))
                .version(0L)
                .createdAt(LocalDateTime.of(2026, 1, 1, 10, 0))
                .updatedAt(LocalDateTime.of(2026, 1, 2, 10, 0))
                .build();
    }

    private ProductDefinition sampleDomain(String code) {
        return ProductDefinition.builder()
                .productCode(code)
                .productType(ProductType.SAVINGS)
                .name("Test " + code)
                .description("Desc " + code)
                .active(true)
                .parameters(Map.of("key", "value"))
                .version(0L)
                .createdAt(LocalDateTime.of(2026, 1, 1, 10, 0))
                .updatedAt(LocalDateTime.of(2026, 1, 2, 10, 0))
                .build();
    }

    // ── Save ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("save()")
    class Save {

        @Test
        @DisplayName("Should persist domain entity and return domain object")
        void shouldSaveAndReturnDomain() {
            ProductDefinition domain = sampleDomain("SAVE_001");
            ProductDefinitionEntity entity = sampleEntity("SAVE_001");
            when(jpaRepository.save(any(ProductDefinitionEntity.class))).thenReturn(entity);

            ProductDefinition result = adapter.save(domain);

            assertThat(result.getProductCode()).isEqualTo("SAVE_001");
            assertThat(result.getProductType()).isEqualTo(ProductType.SAVINGS);
            assertThat(result.getName()).isEqualTo("Test SAVE_001");
            assertThat(result.isActive()).isTrue();
            verify(jpaRepository).save(any(ProductDefinitionEntity.class));
        }
    }

    // ── FindByCode ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findByCode()")
    class FindByCode {

        @Test
        @DisplayName("Should return domain object when entity found")
        void shouldReturnDomainWhenFound() {
            ProductDefinitionEntity entity = sampleEntity("FOUND_001");
            when(jpaRepository.findById("FOUND_001")).thenReturn(Optional.of(entity));

            Optional<ProductDefinition> result = adapter.findByCode("FOUND_001");

            assertThat(result).isPresent();
            assertThat(result.get().getProductCode()).isEqualTo("FOUND_001");
            assertThat(result.get().getProductType()).isEqualTo(ProductType.SAVINGS);
            assertThat(result.get().getName()).isEqualTo("Test FOUND_001");
        }

        @Test
        @DisplayName("Should return empty optional when entity not found")
        void shouldReturnEmptyWhenNotFound() {
            when(jpaRepository.findById("MISSING")).thenReturn(Optional.empty());

            Optional<ProductDefinition> result = adapter.findByCode("MISSING");

            assertThat(result).isEmpty();
        }
    }

    // ── FindAllActive ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("findAllActive()")
    class FindAllActive {

        @Test
        @DisplayName("Should return list of active domain objects")
        void shouldReturnActiveProducts() {
            List<ProductDefinitionEntity> entities = List.of(
                    sampleEntity("A1"),
                    sampleEntity("A2")
            );
            when(jpaRepository.findByActiveTrue()).thenReturn(entities);

            List<ProductDefinition> result = adapter.findAllActive();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getProductCode()).isEqualTo("A1");
            assertThat(result.get(1).getProductCode()).isEqualTo("A2");
        }

        @Test
        @DisplayName("Should return empty list when no active products")
        void shouldReturnEmptyWhenNoneActive() {
            when(jpaRepository.findByActiveTrue()).thenReturn(List.of());

            List<ProductDefinition> result = adapter.findAllActive();

            assertThat(result).isEmpty();
        }
    }

    // ── FindAll ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findAll()")
    class FindAll {

        @Test
        @DisplayName("Should return all domain objects")
        void shouldReturnAllProducts() {
            List<ProductDefinitionEntity> entities = List.of(
                    sampleEntity("P1"),
                    sampleEntity("P2"),
                    sampleEntity("P3")
            );
            when(jpaRepository.findAll()).thenReturn(entities);

            List<ProductDefinition> result = adapter.findAll();

            assertThat(result).hasSize(3);
        }
    }

    // ── FindByType ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findByType()")
    class FindByType {

        @Test
        @DisplayName("Should return active products of specified type")
        void shouldReturnProductsByType() {
            List<ProductDefinitionEntity> entities = List.of(
                    sampleEntity("L1"),
                    sampleEntity("L2")
            );
            when(jpaRepository.findByProductTypeAndActiveTrue(ProductType.SAVINGS)).thenReturn(entities);

            List<ProductDefinition> result = adapter.findByType(ProductType.SAVINGS);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getProductType()).isEqualTo(ProductType.SAVINGS);
        }
    }

    // ── ExistsByCode ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("existsByCode()")
    class ExistsByCode {

        @Test
        @DisplayName("Should return true when product code exists")
        void shouldReturnTrueWhenExists() {
            when(jpaRepository.existsByProductCode("EXISTS")).thenReturn(true);

            boolean result = adapter.existsByCode("EXISTS");

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when product code does not exist")
        void shouldReturnFalseWhenNotExists() {
            when(jpaRepository.existsByProductCode("MISSING")).thenReturn(false);

            boolean result = adapter.existsByCode("MISSING");

            assertThat(result).isFalse();
        }
    }
}
