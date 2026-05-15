package id.payu.productcatalog.adapter.web.admin;

import id.payu.productcatalog.adapter.web.BaseController;
import id.payu.productcatalog.domain.model.ProductDefinition;
import id.payu.productcatalog.domain.model.ProductType;
import id.payu.productcatalog.domain.port.in.ProductCatalogUseCase;
import id.payu.productcatalog.dto.CreateProductRequest;
import id.payu.productcatalog.dto.ProductResponse;
import id.payu.productcatalog.dto.UpdateProductRequest;
import id.payu.security.annotation.Audited;
import id.payu.security.annotation.AuditOperation;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin REST controller for product catalog management.
 * Base path: /admin/products
 */
@RestController
@RequestMapping("/admin/products")
@PreAuthorize("hasRole('ADMIN')")
public class AdminProductController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(AdminProductController.class);

    private final ProductCatalogUseCase productCatalogUseCase;

    public AdminProductController(ProductCatalogUseCase productCatalogUseCase) {
        this.productCatalogUseCase = productCatalogUseCase;
    }

    /**
     * Create a new product definition.
     */
    @PostMapping
    @Audited(operation = AuditOperation.CREATE)
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
        log.info("Admin creating product: {}", request.getProductCode());

        ProductDefinition product = ProductDefinition.builder()
                .productCode(request.getProductCode())
                .productType(request.getProductType())
                .name(request.getName())
                .description(request.getDescription())
                .parameters(request.getParameters())
                .build();

        ProductDefinition created = productCatalogUseCase.createProduct(product);
        return created("/admin/products/" + created.getProductCode(), toResponse(created));
    }

    /**
     * Get all products (including inactive).
     */
    @GetMapping
    @Audited(operation = AuditOperation.READ)
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        log.debug("Admin listing all products");
        List<ProductDefinition> products = productCatalogUseCase.getAllProducts();
        return okList(products.stream().map(this::toResponse).collect(Collectors.toList()));
    }

    /**
     * Get a specific product by code.
     */
    @GetMapping("/{code}")
    @Audited(operation = AuditOperation.READ)
    public ResponseEntity<ProductResponse> getProduct(@PathVariable String code) {
        log.debug("Admin getting product: {}", code);
        return productCatalogUseCase.getProduct(code)
                .map(this::toResponse)
                .map(this::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update a product definition.
     */
    @PutMapping("/{code}")
    @Audited(operation = AuditOperation.UPDATE)
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable String code,
            @Valid @RequestBody UpdateProductRequest request) {
        log.info("Admin updating product: {}", code);

        ProductDefinition product = ProductDefinition.builder()
                .productType(request.getProductType())
                .name(request.getName())
                .description(request.getDescription())
                .parameters(request.getParameters())
                .build();

        ProductDefinition updated = productCatalogUseCase.updateProduct(code, product);
        return ok(toResponse(updated));
    }

    /**
     * Soft delete (deactivate) a product.
     */
    @DeleteMapping("/{code}")
    @Audited(operation = AuditOperation.UPDATE)
    public ResponseEntity<Void> deactivateProduct(@PathVariable String code) {
        log.info("Admin deactivating product: {}", code);
        productCatalogUseCase.deactivateProduct(code);
        return noContent();
    }

    /**
     * Activate a product.
     */
    @PostMapping("/{code}/activate")
    @Audited(operation = AuditOperation.UPDATE)
    public ResponseEntity<Void> activateProduct(@PathVariable String code) {
        log.info("Admin activating product: {}", code);
        productCatalogUseCase.activateProduct(code);
        return noContent();
    }

    /**
     * Get products by type.
     */
    @GetMapping("/type/{type}")
    @Audited(operation = AuditOperation.READ)
    public ResponseEntity<List<ProductResponse>> getProductsByType(@PathVariable ProductType type) {
        log.debug("Admin listing products by type: {}", type);
        List<ProductDefinition> products = productCatalogUseCase.getProductsByType(type);
        return okList(products.stream().map(this::toResponse).collect(Collectors.toList()));
    }

    private ProductResponse toResponse(ProductDefinition product) {
        ProductResponse response = new ProductResponse();
        response.setProductCode(product.getProductCode());
        response.setProductType(product.getProductType());
        response.setName(product.getName());
        response.setDescription(product.getDescription());
        response.setActive(product.isActive());
        response.setParameters(product.getParameters());
        response.setCreatedAt(product.getCreatedAt());
        response.setUpdatedAt(product.getUpdatedAt());
        return response;
    }
}
