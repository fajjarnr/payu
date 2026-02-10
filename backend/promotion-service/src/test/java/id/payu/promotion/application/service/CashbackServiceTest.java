package id.payu.promotion.application.service;

import id.payu.promotion.domain.Cashback;
import id.payu.promotion.dto.CreateCashbackRequest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.test.mock.mockito.MockBean;
import id.payu.promotion.adapter.persistence.repository.CashbackRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CashbackServiceTest {

    @Autowired
    CashbackService cashbackService;

    @Autowired
    EntityManager entityManager;

    @Autowired
    CashbackRepository cashbackRepository;

    @MockBean
    @SuppressWarnings("rawtypes")
    id.payu.promotion.application.service.EmitterPlaceholder promotionEvents;

    private static final String TEST_ACCOUNT_ID = "acc-123";
    private static final String TEST_TRANSACTION_ID = "txn-456";

    @BeforeEach
    void setUp() {
        cashbackRepository.deleteAll();
    }

    @Test
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

        assertNotNull(result.getId());
        assertEquals(TEST_ACCOUNT_ID, result.getAccountId());
        assertEquals(TEST_TRANSACTION_ID, result.getTransactionId());
        assertEquals(new BigDecimal("1000.00"), result.getTransactionAmount());
        assertEquals("MERCHANT001", result.getMerchantCode());
        assertEquals("GROCERY", result.getCategoryCode());
        assertEquals(Cashback.Status.CREDITED, result.getStatus());
        assertNotNull(result.getCreditedAt());
        assertNotNull(result.getCreatedAt());
    }

    @Test
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

        assertEquals(new BigDecimal("20.00"), result.getCashbackAmount());
        assertEquals(new BigDecimal("2.0000"), result.getPercentage());
    }

    @Test
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

        assertEquals(new BigDecimal("30.00"), result.getCashbackAmount());
        assertEquals(new BigDecimal("3.0000"), result.getPercentage());
    }

    @Test
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

        assertEquals(new BigDecimal("15.00"), result.getCashbackAmount());
        assertEquals(new BigDecimal("1.5000"), result.getPercentage());
    }

    @Test
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

        assertEquals(new BigDecimal("10.00"), result.getCashbackAmount());
        assertEquals(new BigDecimal("1.0000"), result.getPercentage());
    }

    @Test
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

        assertEquals(new BigDecimal("10.00"), result.getCashbackAmount());
        assertEquals(new BigDecimal("1.0000"), result.getPercentage());
    }

    @Test
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

        var result = cashbackService.getCashback(created.getId());

        assertTrue(result.isPresent());
        assertEquals(created.getId(), result.get().getId());
        assertEquals(TEST_ACCOUNT_ID, result.get().getAccountId());
        assertEquals(new BigDecimal("20.00"), result.get().getCashbackAmount());
    }

    @Test
    void testGetCashback_NotFound() {
        UUID nonExistentId = UUID.randomUUID();

        var result = cashbackService.getCashback(nonExistentId);

        assertFalse(result.isPresent());
    }

    @Test
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
        assertTrue(results.stream().allMatch(c -> TEST_ACCOUNT_ID.equals(c.getAccountId())));
    }

    @Test
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
    void testGetCashbackSummary_NoTransactions() {
        var summary = cashbackService.getCashbackSummary("non-existent-account");

        assertEquals(BigDecimal.ZERO, summary.totalCashback());
        assertEquals(BigDecimal.ZERO, summary.pendingCashback());
        assertEquals(BigDecimal.ZERO, summary.creditedCashback());
        assertEquals(0, summary.transactionCount());
    }

    @Test
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

        assertEquals(new BigDecimal("24.69"), result.getCashbackAmount());
        assertEquals(new BigDecimal("2.0000"), result.getPercentage());
    }

    @Test
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

        assertEquals("PROMO2024", result.getCashbackCode());
    }
}
