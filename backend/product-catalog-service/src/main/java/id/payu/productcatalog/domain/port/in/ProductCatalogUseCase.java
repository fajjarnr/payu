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

    /**
     * Create a new product definition.
     *
     * @param product the product definition to create
     * @return the created product definition
     */
    ProductDefinition createProduct(ProductDefinition product);

    /**
     * Get a product definition by its code.
     *
     * @param productCode the unique product code
     * @return optional containing the product if found
     */
    Optional<ProductDefinition> getProduct(String productCode);

    /**
     * Get all active products.
     *
     * @return list of active product definitions
     */
    List<ProductDefinition> getAllActiveProducts();

    /**
     * Get all products (including inactive).
     *
     * @return list of all product definitions
     */
    List<ProductDefinition> getAllProducts();

    /**
     * Get products by type.
     *
     * @param productType the product type filter
     * @return list of products of the specified type
     */
    List<ProductDefinition> getProductsByType(ProductType productType);

    /**
     * Update an existing product definition.
     *
     * @param productCode the product code to update
     * @param product the updated product data
     * @return the updated product definition
     */
    ProductDefinition updateProduct(String productCode, ProductDefinition product);

    /**
     * Soft delete (deactivate) a product.
     *
     * @param productCode the product code to deactivate
     */
    void deactivateProduct(String productCode);

    /**
     * Activate a product.
     *
     * @param productCode the product code to activate
     */
    void activateProduct(String productCode);

    /**
     * Get a parameter value from a product.
     *
     * @param productCode the product code
     * @param parameterKey the parameter key
     * @param defaultValue the default value if parameter not found
     * @return the parameter value or default
     */
    <T> T getProductParameter(String productCode, String parameterKey, T defaultValue);

    /**
     * Check if a product exists and is active.
     *
     * @param productCode the product code
     * @return true if product exists and is active
     */
    boolean isProductActive(String productCode);
}
