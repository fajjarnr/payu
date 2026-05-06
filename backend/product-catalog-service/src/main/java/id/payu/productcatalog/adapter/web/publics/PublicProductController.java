package id.payu.productcatalog.adapter.web.publics;

import id.payu.productcatalog.adapter.web.BaseController;
import id.payu.productcatalog.domain.model.ProductDefinition;
import id.payu.productcatalog.domain.port.in.ProductCatalogUseCase;
import id.payu.productcatalog.dto.ProductResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Public REST controller for product catalog queries.
 * Base path: /products
 */
@RestController
@RequestMapping("/products")
public class PublicProductController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(PublicProductController.class);

    private final ProductCatalogUseCase productCatalogUseCase;

    public PublicProductController(ProductCatalogUseCase productCatalogUseCase) {
        this.productCatalogUseCase = productCatalogUseCase;
    }

    /**
     * Get all active products.
     * BUG-BE-160 FIX: Explicitly map both "" and "/" to handle trailing slash
     * in Spring Boot 3.4 PathPatternParser, preventing 404 on /products/.
     */
    @GetMapping({"", "/"})
    public ResponseEntity<List<ProductResponse>> getActiveProducts() {
        log.debug("Listing active products");
        List<ProductDefinition> products = productCatalogUseCase.getAllActiveProducts();
        if (products == null) {
            return okList(List.of());
        }
        return okList(products.stream().map(this::toResponse).collect(Collectors.toList()));
    }

    /**
     * Get a specific active product by code.
     */
    @GetMapping("/{code}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable String code) {
        log.debug("Getting product: {}", code);
        return productCatalogUseCase.getProduct(code)
                .filter(ProductDefinition::isActive)
                .map(this::toResponse)
                .map(this::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get a specific product parameter value.
     */
    @GetMapping("/{code}/parameters/{key}")
    public ResponseEntity<Object> getProductParameter(
            @PathVariable String code,
            @PathVariable String key,
            @RequestParam(required = false) Object defaultValue) {
        log.debug("Getting parameter {} for product {}", key, code);

        if (!productCatalogUseCase.isProductActive(code)) {
            return ResponseEntity.notFound().build();
        }

        Object value = productCatalogUseCase.getProductParameter(code, key, defaultValue);
        return ok(value);
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
