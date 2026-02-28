package id.payu.productcatalog.adapter.persistence.repository;

import id.payu.productcatalog.adapter.persistence.entity.ProductDefinitionEntity;
import id.payu.productcatalog.domain.model.ProductType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA Repository for ProductDefinitionEntity.
 */
@Repository
public interface ProductDefinitionJpaRepository extends JpaRepository<ProductDefinitionEntity, String> {

    /**
     * Find all active products.
     */
    List<ProductDefinitionEntity> findByActiveTrue();

    /**
     * Find products by type.
     */
    List<ProductDefinitionEntity> findByProductType(ProductType productType);

    /**
     * Find active products by type.
     */
    List<ProductDefinitionEntity> findByProductTypeAndActiveTrue(ProductType productType);

    /**
     * Check if product exists by code.
     */
    boolean existsByProductCode(String productCode);
}
