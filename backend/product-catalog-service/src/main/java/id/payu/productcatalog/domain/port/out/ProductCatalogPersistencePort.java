package id.payu.productcatalog.domain.port.out;

import id.payu.productcatalog.domain.model.ProductDefinition;
import id.payu.productcatalog.domain.model.ProductType;

import java.util.List;
import java.util.Optional;

/**
 * Output port for ProductCatalog persistence operations.
 */
public interface ProductCatalogPersistencePort {
    ProductDefinition save(ProductDefinition product);
    Optional<ProductDefinition> findByCode(String productCode);
    List<ProductDefinition> findAllActive();
    List<ProductDefinition> findAll();
    List<ProductDefinition> findByType(ProductType productType);
    boolean existsByCode(String productCode);
}
