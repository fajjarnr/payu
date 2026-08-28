package id.payu.productcatalog.application.service;

import id.payu.cache.annotation.CacheInvalidate;
import id.payu.cache.annotation.CacheWithTTL;
import id.payu.productcatalog.domain.model.ProductDefinition;
import id.payu.productcatalog.domain.model.ProductType;
import id.payu.productcatalog.domain.port.in.ProductCatalogUseCase;
import id.payu.productcatalog.domain.port.out.ProductCatalogPersistencePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Application service for product catalog operations.
 * Implements the use cases defined in ProductCatalogUseCase.
 */
@Service
@Transactional
public class ProductCatalogService implements ProductCatalogUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProductCatalogService.class);

    private final ProductCatalogPersistencePort persistencePort;

    public ProductCatalogService(ProductCatalogPersistencePort persistencePort) {
        this.persistencePort = persistencePort;
    }

    @Override
    public ProductDefinition createProduct(ProductDefinition product) {
        log.info("Creating product: {}", product.getProductCode());

        if (persistencePort.existsByCode(product.getProductCode())) {
            throw new IllegalArgumentException(
                    "Product with code " + product.getProductCode() + " already exists");
        }

        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        product.setActive(true);

        ProductDefinition saved = persistencePort.save(product);
        log.info("Product created successfully: {}", saved.getProductCode());

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    // ponytail: cache disabled - LinkedHashMap cast bug (Jackson 3 + generic List) same as cms-service 1.8.12 TypedJsonRedisSerializer; re-enable with typed serializer if throughput matters
    public Optional<ProductDefinition> getProduct(String productCode) {
        log.debug("Getting product: {}", productCode);
        return persistencePort.findByCode(productCode);
    }

    @Override
    @Transactional(readOnly = true)
    // ponytail: cache disabled for getAllActiveProducts
    public List<ProductDefinition> getAllActiveProducts() {
        log.debug("Getting all active products");
        return persistencePort.findAllActive();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDefinition> getAllProducts() {
        log.debug("Getting all products");
        return persistencePort.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    // ponytail: cache disabled for getProductsByType
    public List<ProductDefinition> getProductsByType(ProductType productType) {
        log.debug("Getting products by type: {}", productType);
        return persistencePort.findByType(productType);
    }
    @Override
    public ProductDefinition updateProduct(String productCode, ProductDefinition product) {
        log.info("Updating product: {}", productCode);

        ProductDefinition existing = persistencePort.findByCode(productCode)
                .orElseThrow(() -> new ProductNotFoundException(productCode));

        existing.setName(product.getName());
        existing.setDescription(product.getDescription());
        existing.setProductType(product.getProductType());
        existing.setParameters(product.getParameters());
        existing.setUpdatedAt(LocalDateTime.now());

        ProductDefinition saved = persistencePort.save(existing);
        log.info("Product updated successfully: {}", saved.getProductCode());

        return saved;
    }

    @Override
    public void deactivateProduct(String productCode) {
        log.info("Deactivating product: {}", productCode);

        ProductDefinition product = persistencePort.findByCode(productCode)
                .orElseThrow(() -> new ProductNotFoundException(productCode));

        product.setActive(false);
        product.setUpdatedAt(LocalDateTime.now());
        persistencePort.save(product);

        log.info("Product deactivated: {}", productCode);
    }

    @Override
    public void activateProduct(String productCode) {
        log.info("Activating product: {}", productCode);

        ProductDefinition existing = persistencePort.findByCode(productCode)
                .orElseThrow(() -> new ProductNotFoundException(productCode));

        existing.activate();
        persistencePort.save(existing);

        log.info("Product activated successfully: {}", productCode);
    }

    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public <T> T getProductParameter(String productCode, String parameterKey, T defaultValue) {
        log.debug("Getting parameter {} for product {}", parameterKey, productCode);

        return persistencePort.findByCode(productCode)
                .map(product -> {
                    Object value = product.getParameter(parameterKey);
                    return value != null ? (T) value : defaultValue;
                })
                .orElse(defaultValue);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isProductActive(String productCode) {
        return persistencePort.findByCode(productCode)
                .map(ProductDefinition::isActive)
                .orElse(false);
    }
}
