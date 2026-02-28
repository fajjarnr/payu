package id.payu.productcatalog.application.service;

import id.payu.productcatalog.domain.model.ProductDefinition;
import id.payu.productcatalog.domain.model.ProductType;
import id.payu.productcatalog.domain.port.out.ProductCatalogPersistencePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductCatalogServiceTest {

    @Mock
    private ProductCatalogPersistencePort persistencePort;

    private ProductCatalogService service;

    @BeforeEach
    void setUp() {
        service = new ProductCatalogService(persistencePort);
    }

    @Test
    void shouldCreateProductSuccessfully() {
        ProductDefinition newProduct = ProductDefinition.builder()
                .productCode("SAVINGS_NEW")
                .productType(ProductType.SAVINGS)
                .name("New Savings")
                .build();

        when(persistencePort.existsByCode("SAVINGS_NEW")).thenReturn(false);
        when(persistencePort.save(any(ProductDefinition.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductDefinition created = service.createProduct(newProduct);

        assertNotNull(created.getCreatedAt());
        assertNotNull(created.getUpdatedAt());
        assertTrue(created.isActive());
        verify(persistencePort).save(any(ProductDefinition.class));
    }

    @Test
    void shouldThrowExceptionWhenCreatingDuplicateProduct() {
        ProductDefinition existingProduct = ProductDefinition.builder()
                .productCode("SAVINGS_EXISTING")
                .productType(ProductType.SAVINGS)
                .name("Existing Savings")
                .build();

        when(persistencePort.existsByCode("SAVINGS_EXISTING")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.createProduct(existingProduct)
        );

        assertTrue(exception.getMessage().contains("already exists"));
        verify(persistencePort, never()).save(any());
    }

    @Test
    void shouldGetProductByCode() {
        ProductDefinition product = createSampleProduct("LOAN_001", ProductType.LOAN);

        when(persistencePort.findByCode("LOAN_001")).thenReturn(Optional.of(product));

        Optional<ProductDefinition> result = service.getProduct("LOAN_001");

        assertTrue(result.isPresent());
        assertEquals("LOAN_001", result.get().getProductCode());
    }

    @Test
    void shouldReturnEmptyWhenProductNotFound() {
        when(persistencePort.findByCode("MISSING")).thenReturn(Optional.empty());

        Optional<ProductDefinition> result = service.getProduct("MISSING");

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldGetAllActiveProducts() {
        List<ProductDefinition> activeProducts = Arrays.asList(
                createSampleProduct("P1", ProductType.SAVINGS),
                createSampleProduct("P2", ProductType.LOAN)
        );

        when(persistencePort.findAllActive()).thenReturn(activeProducts);

        List<ProductDefinition> result = service.getAllActiveProducts();

        assertEquals(2, result.size());
    }

    @Test
    void shouldGetAllProducts() {
        List<ProductDefinition> allProducts = Arrays.asList(
                createSampleProduct("P1", ProductType.SAVINGS),
                createSampleProduct("P2", ProductType.LOAN),
                createInactiveProduct("P3")
        );

        when(persistencePort.findAll()).thenReturn(allProducts);

        List<ProductDefinition> result = service.getAllProducts();

        assertEquals(3, result.size());
    }

    @Test
    void shouldGetProductsByType() {
        List<ProductDefinition> savingsProducts = Arrays.asList(
                createSampleProduct("S1", ProductType.SAVINGS),
                createSampleProduct("S2", ProductType.SAVINGS)
        );

        when(persistencePort.findByType(ProductType.SAVINGS)).thenReturn(savingsProducts);

        List<ProductDefinition> result = service.getProductsByType(ProductType.SAVINGS);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(p -> p.getProductType() == ProductType.SAVINGS));
    }

    @Test
    void shouldUpdateProductSuccessfully() {
        ProductDefinition existing = createSampleProduct("UPDATE_001", ProductType.LOAN);
        ProductDefinition updateData = ProductDefinition.builder()
                .productType(ProductType.LOAN)
                .name("Updated Name")
                .description("Updated Description")
                .parameters(new HashMap<>())
                .build();

        when(persistencePort.findByCode("UPDATE_001")).thenReturn(Optional.of(existing));
        when(persistencePort.save(any(ProductDefinition.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductDefinition updated = service.updateProduct("UPDATE_001", updateData);

        assertEquals("Updated Name", updated.getName());
        assertEquals("Updated Description", updated.getDescription());
        assertNotNull(updated.getUpdatedAt());
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentProduct() {
        ProductDefinition updateData = ProductDefinition.builder()
                .productType(ProductType.LOAN)
                .name("Updated Name")
                .build();

        when(persistencePort.findByCode("MISSING")).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class,
                () -> service.updateProduct("MISSING", updateData));
    }

    @Test
    void shouldDeactivateProduct() {
        ProductDefinition existing = createSampleProduct("DEACT_001", ProductType.SAVINGS);

        when(persistencePort.findByCode("DEACT_001")).thenReturn(Optional.of(existing));
        when(persistencePort.save(any(ProductDefinition.class))).thenAnswer(inv -> inv.getArgument(0));

        service.deactivateProduct("DEACT_001");

        assertFalse(existing.isActive());
        assertNotNull(existing.getUpdatedAt());
    }

    @Test
    void shouldActivateProduct() {
        ProductDefinition existing = createInactiveProduct("ACT_001");

        when(persistencePort.findByCode("ACT_001")).thenReturn(Optional.of(existing));
        when(persistencePort.save(any(ProductDefinition.class))).thenAnswer(inv -> inv.getArgument(0));

        service.activateProduct("ACT_001");

        assertTrue(existing.isActive());
        assertNotNull(existing.getUpdatedAt());
    }

    @Test
    void shouldGetProductParameter() {
        Map<String, Object> params = new HashMap<>();
        params.put("interestRate", 0.05);

        ProductDefinition product = ProductDefinition.builder()
                .productCode("PARAM_001")
                .productType(ProductType.SAVINGS)
                .name("Test")
                .parameters(params)
                .build();

        when(persistencePort.findByCode("PARAM_001")).thenReturn(Optional.of(product));

        Double rate = service.getProductParameter("PARAM_001", "interestRate", 0.0);

        assertEquals(0.05, rate);
    }

    @Test
    void shouldReturnDefaultValueWhenParameterNotFound() {
        ProductDefinition product = ProductDefinition.builder()
                .productCode("PARAM_002")
                .productType(ProductType.SAVINGS)
                .name("Test")
                .parameters(new HashMap<>())
                .build();

        when(persistencePort.findByCode("PARAM_002")).thenReturn(Optional.of(product));

        Double rate = service.getProductParameter("PARAM_002", "missingRate", 0.10);

        assertEquals(0.10, rate);
    }

    @Test
    void shouldReturnDefaultValueWhenProductNotFound() {
        when(persistencePort.findByCode("MISSING")).thenReturn(Optional.empty());

        Double rate = service.getProductParameter("MISSING", "rate", 0.15);

        assertEquals(0.15, rate);
    }

    @Test
    void shouldCheckIfProductIsActive() {
        ProductDefinition activeProduct = createSampleProduct("ACTIVE_001", ProductType.SAVINGS);

        when(persistencePort.findByCode("ACTIVE_001")).thenReturn(Optional.of(activeProduct));
        when(persistencePort.findByCode("MISSING")).thenReturn(Optional.empty());

        assertTrue(service.isProductActive("ACTIVE_001"));
        assertFalse(service.isProductActive("MISSING"));
    }

    private ProductDefinition createSampleProduct(String code, ProductType type) {
        return ProductDefinition.builder()
                .productCode(code)
                .productType(type)
                .name("Test " + code)
                .active(true)
                .build();
    }

    private ProductDefinition createInactiveProduct(String code) {
        return ProductDefinition.builder()
                .productCode(code)
                .productType(ProductType.SAVINGS)
                .name("Inactive " + code)
                .active(false)
                .build();
    }
}
