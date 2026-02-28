package id.payu.productcatalog.domain.model;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Domain model representing a product definition in the catalog.
 * This is the aggregate root for the product catalog domain.
 */
public class ProductDefinition {

    private String productCode;
    private ProductType productType;
    private String name;
    private String description;
    private boolean active;
    private Map<String, Object> parameters;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;

    public ProductDefinition() {
    }

    public ProductDefinition(String productCode, ProductType productType, String name, String description,
                             boolean active, Map<String, Object> parameters, LocalDateTime createdAt,
                             LocalDateTime updatedAt, Long version) {
        this.productCode = productCode;
        this.productType = productType;
        this.name = name;
        this.description = description;
        this.active = active;
        this.parameters = parameters;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    /**
     * Activate this product definition.
     */
    public void activate() {
        this.active = true;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Deactivate this product definition (soft delete).
     */
    public void deactivate() {
        this.active = false;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Update parameters for this product.
     */
    public void updateParameters(Map<String, Object> newParameters) {
        this.parameters = newParameters;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Get a parameter value by key.
     */
    @SuppressWarnings("unchecked")
    public <T> T getParameter(String key) {
        if (parameters == null) {
            return null;
        }
        return (T) parameters.get(key);
    }

    /**
     * Check if this product has a specific parameter.
     */
    public boolean hasParameter(String key) {
        return parameters != null && parameters.containsKey(key);
    }

    /**
     * Validate that required parameters are present.
     */
    public boolean hasRequiredParameters(String... requiredKeys) {
        if (parameters == null) {
            return false;
        }
        for (String key : requiredKeys) {
            if (!parameters.containsKey(key)) {
                return false;
            }
        }
        return true;
    }

    public static ProductDefinitionBuilder builder() {
        return new ProductDefinitionBuilder();
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public ProductType getProductType() {
        return productType;
    }

    public void setProductType(ProductType productType) {
        this.productType = productType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public static class ProductDefinitionBuilder {
        private String productCode;
        private ProductType productType;
        private String name;
        private String description;
        private boolean active;
        private Map<String, Object> parameters;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private Long version;

        ProductDefinitionBuilder() {
        }

        public ProductDefinitionBuilder productCode(String productCode) {
            this.productCode = productCode;
            return this;
        }

        public ProductDefinitionBuilder productType(ProductType productType) {
            this.productType = productType;
            return this;
        }

        public ProductDefinitionBuilder name(String name) {
            this.name = name;
            return this;
        }

        public ProductDefinitionBuilder description(String description) {
            this.description = description;
            return this;
        }

        public ProductDefinitionBuilder active(boolean active) {
            this.active = active;
            return this;
        }

        public ProductDefinitionBuilder parameters(Map<String, Object> parameters) {
            this.parameters = parameters;
            return this;
        }

        public ProductDefinitionBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public ProductDefinitionBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public ProductDefinitionBuilder version(Long version) {
            this.version = version;
            return this;
        }

        public ProductDefinition build() {
            return new ProductDefinition(productCode, productType, name, description, active,
                    parameters, createdAt, updatedAt, version);
        }
    }
}
