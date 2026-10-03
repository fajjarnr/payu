package id.payu.productcatalog.domain.port.in;

import id.payu.productcatalog.domain.model.ProductDefinition;
import id.payu.productcatalog.domain.model.ProductType;

import java.util.List;
import java.util.Optional;

/**
 * Input port defining product catalog use cases.
 * This is the primary interface for the application layer.
 */
public interface ProductCatalogUseCase {

    ProductDefinition createProduct(ProductDefinition product);

    Optional<ProductDefinition> getProduct(String productCode);

    List<ProductDefinition> getAllActiveProducts();

    List<ProductDefinition> getAllProducts();

    List<ProductDefinition> getProductsByType(ProductType productType);

    ProductDefinition updateProduct(String productCode, ProductDefinition product);

    void deactivateProduct(String productCode);

    void activateProduct(String productCode);

    <T> T getProductParameter(String productCode, String parameterKey, T defaultValue);

    boolean isProductActive(String productCode);
}
