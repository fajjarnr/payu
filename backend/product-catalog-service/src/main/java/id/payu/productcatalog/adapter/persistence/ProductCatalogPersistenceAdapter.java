package id.payu.productcatalog.adapter.persistence;

import id.payu.productcatalog.adapter.persistence.entity.ProductDefinitionEntity;
import id.payu.productcatalog.adapter.persistence.repository.ProductDefinitionJpaRepository;
import id.payu.productcatalog.domain.model.ProductDefinition;
import id.payu.productcatalog.domain.model.ProductType;
import id.payu.productcatalog.domain.port.out.ProductCatalogPersistencePort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Persistence adapter implementing the output port.
 * Translates between domain model and JPA entities.
 */
@Component
public class ProductCatalogPersistenceAdapter implements ProductCatalogPersistencePort {

    private final ProductDefinitionJpaRepository jpaRepository;

    public ProductCatalogPersistenceAdapter(ProductDefinitionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public ProductDefinition save(ProductDefinition product) {
        ProductDefinitionEntity entity = toEntity(product);
        ProductDefinitionEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<ProductDefinition> findByCode(String productCode) {
        return jpaRepository.findById(productCode)
                .map(this::toDomain);
    }

    @Override
    public List<ProductDefinition> findAllActive() {
        return jpaRepository.findByActiveTrue().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductDefinition> findAll() {
        return jpaRepository.findAll().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductDefinition> findByType(ProductType productType) {
        return jpaRepository.findByProductTypeAndActiveTrue(productType).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByCode(String productCode) {
        return jpaRepository.existsByProductCode(productCode);
    }

    private ProductDefinitionEntity toEntity(ProductDefinition domain) {
        return ProductDefinitionEntity.builder()
                .productCode(domain.getProductCode())
                .productType(domain.getProductType())
                .name(domain.getName())
                .description(domain.getDescription())
                .active(domain.isActive())
                .parameters(domain.getParameters())
                .version(domain.getVersion())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    private ProductDefinition toDomain(ProductDefinitionEntity entity) {
        return ProductDefinition.builder()
                .productCode(entity.getProductCode())
                .productType(entity.getProductType())
                .name(entity.getName())
                .description(entity.getDescription())
                .active(entity.isActive())
                .parameters(entity.getParameters())
                .version(entity.getVersion())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
