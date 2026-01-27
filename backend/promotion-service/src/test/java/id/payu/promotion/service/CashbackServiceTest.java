package id.payu.promotion.service;

import id.payu.promotion.domain.Cashback;
import id.payu.promotion.dto.CreateCashbackRequest;
import id.payu.promotion.test.resource.PostgresTestResource;
import io.quarkus.test.InjectMock;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@Disabled("Service tests require PostgreSQL Testcontainers - disabled when Docker not available")
@QuarkusTestResource(value = PostgresTestResource.class)
class CashbackServiceTest {

    @Inject
    CashbackService cashbackService;

    @Inject
    EntityManager entityManager;

    @InjectMock
    @SuppressWarnings("unused")
    Emitter<Map<String, Object>> promotionEvents;

    private static final String TEST_ACCOUNT_ID = "acc-123";
    private static final String TEST_TRANSACTION_ID = "txn-456";

    @BeforeEach
    void setUp() {
        Cashback.deleteAll();
    }

    @Test
    @TestTransaction
    void testCreateCashback_Success() {
        CreateCashbackRequest request = new CreateCashbackRequest(
            TEST_ACCOUNT_ID,
            TEST_TRANSACTION_ID,
            new BigDecimal("1000.00"),
            "MERCHANT001",
            "GROCERY",
            "CASHBACK10"
        );

        Cashback result = cashbackService.createCashback(request);

        assertNotNull(result.id);
        assertEquals(TEST_ACCOUNT_ID, result.accountId);
        assertEquals(TEST_TRANSACTION_ID, result.transactionId);
        assertEquals(new BigDecimal("1000.00"), result.transactionAmount);
        assertEquals("MERCHANT001", result.merchantCode);
        assertEquals("GROCERY", result.categoryCode);
        assertEquals(Cashback.Status.CREDITED, result.status);
        assertNotNull(result.creditedAt);
        assertNotNull(result.createdAt);
    }

    @Test
    @TestTransaction
    void testCreateCashback_GroceryCategory_Returns2Percent() {
        CreateCashbackRequest request = new CreateCashbackRequest(
            TEST_ACCOUNT_ID,
            TEST_TRANSACTION_ID,
            new BigDecimal("1000.00"),
            "MERCHANT001",
            "GROCERY",
            null
        );

        Cashback result = cashbackService.createCashback(request);

        assertEquals(new BigDecimal("20.00"), result.cashbackAmount);
        assertEquals(new BigDecimal("2.0000"), result.percentage);
    }

    @Test
    @TestTransaction
    void testCreateCashback_DiningCategory_Returns3Percent() {
        CreateCashbackRequest request = new CreateCashbackRequest(
            TEST_ACCOUNT_ID,
            TEST_TRANSACTION_ID,
            new BigDecimal("1000.00"),
            "MERCHANT001",
            "DINING",
            null
        );

        Cashback result = cashbackService.createCashback(request);

        assertEquals(new BigDecimal("30.00"), result.cashbackAmount);
        assertEquals(new BigDecimal("3.0000"), result.percentage);
    }

    @Test
    @TestTransaction
    void testCreateCashback_ShoppingCategory_Returns1Point5Percent() {
        CreateCashbackRequest request = new CreateCashbackRequest(
            TEST_ACCOUNT_ID,
            TEST_TRANSACTION_ID,
            new BigDecimal("1000.00"),
            "MERCHANT001",
            "SHOPPING",
            null
        );

        Cashback result = cashbackService.createCashback(request);

        assertEquals(new BigDecimal("15.00"), result.cashbackAmount);
        assertEquals(new BigDecimal("1.5000"), result.percentage);
    }

    @Test
    @TestTransaction
    void testCreateCashback_DefaultCategory_Returns1Percent() {
        CreateCashbackRequest request = new CreateCashbackRequest(
            TEST_ACCOUNT_ID,
            TEST_TRANSACTION_ID,
            new BigDecimal("1000.00"),
            "MERCHANT001",
            "OTHER",
            null
        );

        Cashback result = cashbackService.createCashback(request);

        assertEquals(new BigDecimal("10.00"), result.cashbackAmount);
        assertEquals(new BigDecimal("1.0000"), result.percentage);
    }

    @Test
    @TestTransaction
    void testCreateCashback_NoCategory_Returns1Percent() {
        CreateCashbackRequest request = new CreateCashbackRequest(
            TEST_ACCOUNT_ID,
            TEST_TRANSACTION_ID,
            new BigDecimal("1000.00"),
            "MERCHANT001",
            null,
            null
        );

        Cashback result = cashbackService.createCashback(request);

        assertEquals(new BigDecimal("10.00"), result.cashbackAmount);
        assertEquals(new BigDecimal("1.0000"), result.percentage);
    }

    @Test
    @TestTransaction
    void testGetCashback_Success() {
        CreateCashbackRequest request = new CreateCashbackRequest(
            TEST_ACCOUNT_ID,
            TEST_TRANSACTION_ID,
            new BigDecimal("1000.00"),
            "MERCHANT001",
            "GROCERY",
            null
        );

        Cashback created = cashbackService.createCashback(request);

        var result = cashbackService.getCashback(created.id);

        assertTrue(result.isPresent());
        assertEquals(created.id, result.get().id);
        assertEquals(TEST_ACCOUNT_ID, result.get().accountId);
        assertEquals(new BigDecimal("20.00"), result.get().cashbackAmount);
    }

    @Test
    void testGetCashback_NotFound() {
        UUID nonExistentId = UUID.randomUUID();

        var result = cashbackService.getCashback(nonExistentId);

        assertFalse(result.isPresent());
    }

    @Test
    @TestTransaction
    void testGetCashbacksByAccount() {
        CreateCashbackRequest request1 = new CreateCashbackRequest(
            TEST_ACCOUNT_ID,
            "txn-1",
            new BigDecimal("1000.00"),
            "MERCHANT001",
            "GROCERY",
            null
        );

        CreateCashbackRequest request2 = new CreateCashbackRequest(
            TEST_ACCOUNT_ID,
            "txn-2",
            new BigDecimal("500.00"),
            "MERCHANT002",
            "DINING",
            null
        );

        cashbackService.createCashback(request1);
        cashbackService.createCashback(request2);

        var results = cashbackService.getCashbacksByAccount(TEST_ACCOUNT_ID);

        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(c -> TEST_ACCOUNT_ID.equals(c.accountId)));
    }

    @Test
    @TestTransaction
    void testGetCashbackSummary() {
        CreateCashbackRequest request1 = new CreateCashbackRequest(
            TEST_ACCOUNT_ID,
            "txn-1",
            new BigDecimal("1000.00"),
            "MERCHANT001",
            "GROCERY",
            null
        );

        CreateCashbackRequest request2 = new CreateCashbackRequest(
            TEST_ACCOUNT_ID,
            "txn-2",
            new BigDecimal("500.00"),
            "MERCHANT002",
            "DINING",
            null
        );

        cashbackService.createCashback(request1);
        cashbackService.createCashback(request2);

        var summary = cashbackService.getCashbackSummary(TEST_ACCOUNT_ID);

        assertEquals(new BigDecimal("35.00"), summary.totalCashback());
        assertEquals(BigDecimal.ZERO, summary.pendingCashback());
        assertEquals(new BigDecimal("35.00"), summary.creditedCashback());
        assertEquals(2, summary.transactionCount());
    }

    @Test
    @TestTransaction
    void testGetCashbackSummary_NoTransactions() {
        var summary = cashbackService.getCashbackSummary("non-existent-account");

        assertEquals(BigDecimal.ZERO, summary.totalCashback());
        assertEquals(BigDecimal.ZERO, summary.pendingCashback());
        assertEquals(BigDecimal.ZERO, summary.creditedCashback());
        assertEquals(0, summary.transactionCount());
    }

    @Test
    @TestTransaction
    void testCreateCashback_DecimalPrecision() {
        CreateCashbackRequest request = new CreateCashbackRequest(
            TEST_ACCOUNT_ID,
            TEST_TRANSACTION_ID,
            new BigDecimal("1234.56"),
            "MERCHANT001",
            "GROCERY",
            null
        );

        Cashback result = cashbackService.createCashback(request);

        assertEquals(new BigDecimal("24.69"), result.cashbackAmount);
        assertEquals(new BigDecimal("2.0000"), result.percentage);
    }

    @Test
    @TestTransaction
    void testCreateCashback_WithCustomCashbackCode() {
        CreateCashbackRequest request = new CreateCashbackRequest(
            TEST_ACCOUNT_ID,
            TEST_TRANSACTION_ID,
            new BigDecimal("1000.00"),
            "MERCHANT001",
            "GROCERY",
            "PROMO2024"
        );

        Cashback result = cashbackService.createCashback(request);

        assertEquals("PROMO2024", result.cashbackCode);
    }
}
