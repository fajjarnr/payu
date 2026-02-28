package id.payu.productcatalog.application.service;

/**
 * Exception thrown when a product is not found.
 */
public class ProductNotFoundException extends RuntimeException {

    private final String productCode;

    public ProductNotFoundException(String productCode) {
        super("Product not found: " + productCode);
        this.productCode = productCode;
    }

    public String getProductCode() {
        return productCode;
    }
}
