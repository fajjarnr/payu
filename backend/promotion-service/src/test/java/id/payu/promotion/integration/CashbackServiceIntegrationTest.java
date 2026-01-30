package id.payu.promotion.integration;

import id.payu.promotion.domain.Cashback;
import id.payu.promotion.dto.CashbackSummaryResponse;
import id.payu.promotion.dto.CreateCashbackRequest;
import id.payu.promotion.service.CashbackService;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service-level integration tests for Cashback operations.
 * Tests the complete data flow from service to database.
 *
 * NOTE: These tests require Docker to be running for PostgreSQL Testcontainers.
 * To run these tests: mvn test -Dtest=CashbackServiceIntegrationTest -Ddocker.enabled=true
 * To skip these tests: mvn test (they will be skipped by default)
 */
@QuarkusTest
@EnabledIfSystemProperty(named = "docker.enabled", matches = "true", disabledReason = "Docker not available")
@QuarkusTestResource(value = id.payu.promotion.test.resource.PostgresTestResource.class)
class CashbackServiceIntegrationTest {

    @Inject
    CashbackService cashbackService;

    @BeforeEach
    void setup() {
        // Clean up database before each test
        Cashback.deleteAll();
    }

    // ==================== CREATE CASHBACK TESTS ====================

    @Test
    void testCreateCashback_WithDiningCategory_ShouldApply3Percent() {
        CreateCashbackRequest request = new CreateCashbackRequest(
            "acc-cashback-001",
            "txn-cashback-001",
            new BigDecimal("100000"),
            "MERCHANT-DINING",
            "DINING",
            "CASHBACK-DINING-001"
        );

        Cashback cashback = cashbackService.createCashback(request);

        Assertions.assertNotNull(cashback.id);
        Assertions.assertEquals("acc-cashback-001", cashback.accountId);
        Assertions.assertEquals("txn-cashback-001", cashback.transactionId);
        Assertions.assertEquals(new BigDecimal("100000"), cashback.transactionAmount);
        Assertions.assertEquals(new BigDecimal("3000"), cashback.cashbackAmount); // 3% of 100000
        Assertions.assertEquals(new BigDecimal("3.0000"), cashback.percentage);
        Assertions.assertEquals("MERCHANT-DINING", cashback.merchantCode);
        Assertions.assertEquals("DINING", cashback.categoryCode);
        Assertions.assertEquals("CASHBACK-DINING-001", cashback.cashbackCode);
        Assertions.assertEquals(Cashback.Status.CREDITED, cashback.status);
        Assertions.assertNotNull(cashback.creditedAt);
        Assertions.assertNotNull(cashback.createdAt);

        // Verify persistence by fetching from database
        Optional<Cashback> fetched = cashbackService.getCashback(cashback.id);
        Assertions.assertTrue(fetched.isPresent());
        Assertions.assertEquals("acc-cashback-001", fetched.get().accountId);
    }

    @Test
    void testCreateCashback_WithGroceryCategory_ShouldApply2Percent() {
        CreateCashbackRequest request = new CreateCashbackRequest(
            "acc-cashback-002",
            "txn-cashback-002",
            new BigDecimal("50000"),
            null,
            "GROCERY",
            null
        );

        Cashback cashback = cashbackService.createCashback(request);

        Assertions.assertEquals(new BigDecimal("1000"), cashback.cashbackAmount); // 2% of 50000
        Assertions.assertEquals(new BigDecimal("2.0000"), cashback.percentage);
        Assertions.assertEquals("GROCERY", cashback.categoryCode);
    }

    @Test
    void testCreateCashback_WithShoppingCategory_ShouldApply1Point5Percent() {
        CreateCashbackRequest request = new CreateCashbackRequest(
            "acc-cashback-003",
            "txn-cashback-003",
            new BigDecimal("200000"),
            null,
            "SHOPPING",
            null
        );

        Cashback cashback = cashbackService.createCashback(request);

        Assertions.assertEquals(new BigDecimal("3000"), cashback.cashbackAmount); // 1.5% of 200000
        Assertions.assertEquals(new BigDecimal("1.5000"), cashback.percentage);
    }

    @Test
    void testCreateCashback_WithDefaultCategory_ShouldApply1Percent() {
        CreateCashbackRequest request = new CreateCashbackRequest(
            "acc-cashback-004",
            "txn-cashback-004",
            new BigDecimal("75000"),
            "MERCHANT-OTHER",
            "OTHER",
            null
        );

        Cashback cashback = cashbackService.createCashback(request);

        Assertions.assertEquals(new BigDecimal("750"), cashback.cashbackAmount); // 1% of 75000
        Assertions.assertEquals(new BigDecimal("1.0000"), cashback.percentage);
    }

    @Test
    void testCreateCashback_WithNullCategory_ShouldApplyDefaultRate() {
        CreateCashbackRequest request = new CreateCashbackRequest(
            "acc-cashback-005",
            "txn-cashback-005",
            new BigDecimal("100000"),
            null,
            null,
            null
        );

        Cashback cashback = cashbackService.createCashback(request);

        Assertions.assertEquals(new BigDecimal("1000"), cashback.cashbackAmount); // 1% default
    }

    @Test
    void testCreateCashback_WithZeroTransactionAmount_ShouldReturnZeroCashback() {
        CreateCashbackRequest request = new CreateCashbackRequest(
            "acc-cashback-006",
            "txn-cashback-006",
            BigDecimal.ZERO,
            null,
            null,
            null
        );

        Cashback cashback = cashbackService.createCashback(request);

        Assertions.assertEquals(BigDecimal.ZERO, cashback.cashbackAmount);
        Assertions.assertEquals(BigDecimal.ZERO, cashback.percentage);
    }

    @Test
    void testCreateCashback_CalculationPrecision_ShouldRoundCorrectly() {
        // Test odd amount that requires precise rounding
        CreateCashbackRequest request = new CreateCashbackRequest(
            "acc-cashback-007",
            "txn-cashback-007",
            new BigDecimal("99999"),
            null,
            "DINING",
            null
        );

        Cashback cashback = cashbackService.createCashback(request);

        // 3% of 99999 = 2999.97
        Assertions.assertEquals(new BigDecimal("2999.97"), cashback.cashbackAmount);
    }

    // ==================== GET CASHBACK TESTS ====================

    @Test
    void testGetCashback_WithValidId_ShouldReturnCashback() {
        CreateCashbackRequest request = new CreateCashbackRequest(
            "acc-cashback-008",
            "txn-cashback-008",
            new BigDecimal("100000"),
            null,
            "DINING",
            null
        );

        Cashback created = cashbackService.createCashback(request);

        Optional<Cashback> fetched = cashbackService.getCashback(created.id);

        Assertions.assertTrue(fetched.isPresent());
        Assertions.assertEquals("acc-cashback-008", fetched.get().accountId);
        Assertions.assertEquals("txn-cashback-008", fetched.get().transactionId);
    }

    @Test
    void testGetCashback_WithInvalidId_ShouldReturnEmpty() {
        Optional<Cashback> fetched = cashbackService.getCashback(UUID.randomUUID());

        Assertions.assertTrue(fetched.isEmpty());
    }

    @Test
    void testGetCashbacksByAccount_WithMultipleTransactions_ShouldReturnAll() {
        String accountId = "acc-cashback-multi";

        // Create multiple cashbacks for the same account
        cashbackService.createCashback(new CreateCashbackRequest(
            accountId, "txn-001", new BigDecimal("100000"), null, "DINING", null
        ));
        cashbackService.createCashback(new CreateCashbackRequest(
            accountId, "txn-002", new BigDecimal("50000"), null, "GROCERY", null
        ));
        cashbackService.createCashback(new CreateCashbackRequest(
            accountId, "txn-003", new BigDecimal("200000"), null, "SHOPPING", null
        ));

        List<Cashback> cashbacks = cashbackService.getCashbacksByAccount(accountId);

        Assertions.assertEquals(3, cashbacks.size());
        Assertions.assertTrue(cashbacks.stream().allMatch(c -> c.accountId.equals(accountId)));
    }

    @Test
    void testGetCashbacksByAccount_WithNoTransactions_ShouldReturnEmpty() {
        List<Cashback> cashbacks = cashbackService.getCashbacksByAccount("acc-no-transactions");

        Assertions.assertTrue(cashbacks.isEmpty());
    }

    // ==================== CASHBACK SUMMARY TESTS ====================

    @Test
    void testGetCashbackSummary_WithMultipleTransactions_ShouldCalculateCorrectly() {
        String accountId = "acc-cashback-summary";

        // Create cashbacks with different amounts
        cashbackService.createCashback(new CreateCashbackRequest(
            accountId, "txn-s001", new BigDecimal("100000"), null, "DINING", null
        )); // 3000
        cashbackService.createCashback(new CreateCashbackRequest(
            accountId, "txn-s002", new BigDecimal("50000"), null, "GROCERY", null
        )); // 1000
        cashbackService.createCashback(new CreateCashbackRequest(
            accountId, "txn-s003", new BigDecimal("200000"), null, "SHOPPING", null
        )); // 3000

        CashbackSummaryResponse summary = cashbackService.getCashbackSummary(accountId);

        Assertions.assertEquals(new BigDecimal("7000"), summary.totalCashback());
        Assertions.assertEquals(new BigDecimal("7000"), summary.creditedCashback());
        Assertions.assertEquals(BigDecimal.ZERO, summary.pendingCashback());
        Assertions.assertEquals(3, summary.transactionCount());
    }

    @Test
    void testGetCashbackSummary_WithNoTransactions_ShouldReturnZeros() {
        String accountId = "acc-no-summary";

        CashbackSummaryResponse summary = cashbackService.getCashbackSummary(accountId);

        Assertions.assertEquals(BigDecimal.ZERO, summary.totalCashback());
        Assertions.assertEquals(BigDecimal.ZERO, summary.creditedCashback());
        Assertions.assertEquals(BigDecimal.ZERO, summary.pendingCashback());
        Assertions.assertEquals(0, summary.transactionCount());
    }

    @Test
    void testGetCashbackSummary_WithDifferentAccounts_ShouldBeIndependent() {
        // Create cashbacks for account 1
        cashbackService.createCashback(new CreateCashbackRequest(
            "acc-summary-1", "txn-001", new BigDecimal("100000"), null, "DINING", null
        ));

        // Create cashbacks for account 2
        cashbackService.createCashback(new CreateCashbackRequest(
            "acc-summary-2", "txn-002", new BigDecimal("200000"), null, "GROCERY", null
        ));

        CashbackSummaryResponse summary1 = cashbackService.getCashbackSummary("acc-summary-1");
        CashbackSummaryResponse summary2 = cashbackService.getCashbackSummary("acc-summary-2");

        Assertions.assertEquals(new BigDecimal("3000"), summary1.totalCashback());
        Assertions.assertEquals(new BigDecimal("4000"), summary2.totalCashback());
    }

    // ==================== CASHBACK BY CATEGORY TESTS ====================

    @Test
    void testCashbackByCategory_AllCategories_ShouldApplyCorrectRates() {
        // Test all supported categories
        Cashback dining = cashbackService.createCashback(new CreateCashbackRequest(
            "acc-cat-1", "txn-dining", new BigDecimal("100000"), null, "DINING", null
        ));
        Assertions.assertEquals(new BigDecimal("3000"), dining.cashbackAmount);

        Cashback grocery = cashbackService.createCashback(new CreateCashbackRequest(
            "acc-cat-2", "txn-grocery", new BigDecimal("100000"), null, "GROCERY", null
        ));
        Assertions.assertEquals(new BigDecimal("2000"), grocery.cashbackAmount);

        Cashback shopping = cashbackService.createCashback(new CreateCashbackRequest(
            "acc-cat-3", "txn-shopping", new BigDecimal("100000"), null, "SHOPPING", null
        ));
        Assertions.assertEquals(new BigDecimal("1500"), shopping.cashbackAmount);

        Cashback other = cashbackService.createCashback(new CreateCashbackRequest(
            "acc-cat-4", "txn-other", new BigDecimal("100000"), null, "OTHER", null
        ));
        Assertions.assertEquals(new BigDecimal("1000"), other.cashbackAmount);
    }

    @Test
    void testCashbackWithCategoryCaseInsensitivity_ShouldApplyCorrectRate() {
        // Test lowercase category
        Cashback lower = cashbackService.createCashback(new CreateCashbackRequest(
            "acc-case-1", "txn-lower", new BigDecimal("100000"), null, "dining", null
        ));
        Assertions.assertEquals(new BigDecimal("3000"), lower.cashbackAmount);

        // Test mixed case category
        Cashback mixed = cashbackService.createCashback(new CreateCashbackRequest(
            "acc-case-2", "txn-mixed", new BigDecimal("100000"), null, "GrOcErY", null
        ));
        Assertions.assertEquals(new BigDecimal("2000"), mixed.cashbackAmount);
    }

    // ==================== MERCHANT-SPECIFIC CASHBACK TESTS ====================

    @Test
    void testCashbackWithMerchantCode_ShouldStoreMerchantInfo() {
        CreateCashbackRequest request = new CreateCashbackRequest(
            "acc-merchant-001",
            "txn-merchant-001",
            new BigDecimal("150000"),
            "MERCHANT-ABC-123",
            "DINING",
            "PROMO-MERCHANT"
        );

        Cashback cashback = cashbackService.createCashback(request);

        Assertions.assertEquals("MERCHANT-ABC-123", cashback.merchantCode);
        Assertions.assertEquals("PROMO-MERCHANT", cashback.cashbackCode);

        // Verify persistence
        Optional<Cashback> fetched = cashbackService.getCashback(cashback.id);
        Assertions.assertTrue(fetched.isPresent());
        Assertions.assertEquals("MERCHANT-ABC-123", fetched.get().merchantCode);
        Assertions.assertEquals("PROMO-MERCHANT", fetched.get().cashbackCode);
    }

    // ==================== HIGH VOLUME TRANSACTION TESTS ====================

    @Test
    void testCashback_WithLargeTransactionAmount_ShouldCalculateCorrectly() {
        CreateCashbackRequest request = new CreateCashbackRequest(
            "acc-high-001",
            "txn-high-001",
            new BigDecimal("999999999"), // Nearly 1 billion
            null,
            "DINING",
            null
        );

        Cashback cashback = cashbackService.createCashback(request);

        // 3% of 999,999,999 = 29,999,999.97
        Assertions.assertEquals(new BigDecimal("29999999.97"), cashback.cashbackAmount);
    }

    @Test
    void testCashback_WithSmallTransactionAmount_ShouldCalculateCorrectly() {
        CreateCashbackRequest request = new CreateCashbackRequest(
            "acc-small-001",
            "txn-small-001",
            new BigDecimal("1000"), // Small amount
            null,
            "GROCERY",
            null
        );

        Cashback cashback = cashbackService.createCashback(request);

        // 2% of 1,000 = 20
        Assertions.assertEquals(new BigDecimal("20"), cashback.cashbackAmount);
    }

    // ==================== MULTIPLE CASHBACK ACCUMULATION TESTS ====================

    @Test
    void testMultipleCashbacks_SummaryAccumulation() {
        String accountId = "acc-accumulation";

        // Create series of cashbacks
        for (int i = 1; i <= 10; i++) {
            cashbackService.createCashback(new CreateCashbackRequest(
                accountId,
                "txn-accum-" + i,
                new BigDecimal("100000"),
                null,
                "DINING",
                null
            ));
        }

        CashbackSummaryResponse summary = cashbackService.getCashbackSummary(accountId);

        // 10 transactions x 3000 each = 30000 total
        Assertions.assertEquals(new BigDecimal("30000"), summary.totalCashback());
        Assertions.assertEquals(10, summary.transactionCount());
    }

    @Test
    void testCashbackByAccount_MultipleTransactions_ShouldReturnAll() {
        String accountId = "acc-pagination";

        // Create 5 cashbacks
        for (int i = 1; i <= 5; i++) {
            cashbackService.createCashback(new CreateCashbackRequest(
                accountId,
                "txn-page-" + i,
                new BigDecimal("100000"),
                null,
                "DINING",
                null
            ));
        }

        // Verify all are returned
        List<Cashback> all = cashbackService.getCashbacksByAccount(accountId);
        Assertions.assertEquals(5, all.size());
    }
}
