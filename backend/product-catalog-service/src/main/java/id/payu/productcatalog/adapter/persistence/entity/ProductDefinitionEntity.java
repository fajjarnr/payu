package id.payu.productcatalog.adapter.persistence.entity;

import id.payu.productcatalog.domain.model.ProductType;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * JPA Entity for ProductDefinition - Infrastructure layer.
 */
@Entity
@Table(name = "product_definitions", indexes = {
    @Index(name = "idx_product_type", columnList = "productType"),
    @Index(name = "idx_product_active", columnList = "active")
})
public class ProductDefinitionEntity {

    @Id
    @Column(name = "product_code", length = 50, nullable = false)
    private String productCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", length = 20, nullable = false)
    private ProductType productType;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Type(JsonType.class)
    @Column(name = "parameters", columnDefinition = "jsonb")
    private Map<String, Object> parameters;

    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public ProductDefinitionEntity() {
    }

    public ProductDefinitionEntity(String productCode, ProductType productType, String name, String description,
                                   boolean active, Map<String, Object> parameters, Long version,
                                   LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.productCode = productCode;
        this.productType = productType;
        this.name = name;
        this.description = description;
        this.active = active;
        this.parameters = parameters;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ProductDefinitionEntityBuilder builder() {
        return new ProductDefinitionEntityBuilder();
    }

    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }

    public ProductType getProductType() { return productType; }
    public void setProductType(ProductType productType) { this.productType = productType; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Map<String, Object> getParameters() { return parameters; }
    public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static class ProductDefinitionEntityBuilder {
        private String productCode;
        private ProductType productType;
        private String name;
        private String description;
        private boolean active;
        private Map<String, Object> parameters;
        private Long version;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        ProductDefinitionEntityBuilder() {}

        public ProductDefinitionEntityBuilder productCode(String productCode) {
            this.productCode = productCode;
            return this;
        }

        public ProductDefinitionEntityBuilder productType(ProductType productType) {
            this.productType = productType;
            return this;
        }

        public ProductDefinitionEntityBuilder name(String name) {
            this.name = name;
            return this;
        }

        public ProductDefinitionEntityBuilder description(String description) {
            this.description = description;
            return this;
        }

        public ProductDefinitionEntityBuilder active(boolean active) {
            this.active = active;
            return this;
        }

        public ProductDefinitionEntityBuilder parameters(Map<String, Object> parameters) {
            this.parameters = parameters;
            return this;
        }

        public ProductDefinitionEntityBuilder version(Long version) {
            this.version = version;
            return this;
        }

        public ProductDefinitionEntityBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public ProductDefinitionEntityBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public ProductDefinitionEntity build() {
            return new ProductDefinitionEntity(productCode, productType, name, description, active,
                    parameters, version, createdAt, updatedAt);
        }
    }
}
