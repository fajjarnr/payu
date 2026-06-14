package id.payu.promotion.application.service;

import id.payu.promotion.adapter.persistence.entity.LoyaltyPointsEntity;
import id.payu.promotion.domain.TransactionType;
import id.payu.promotion.dto.CreateLoyaltyPointsRequest;
import id.payu.promotion.dto.RedeemLoyaltyPointsRequest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.test.mock.mockito.MockBean;
import id.payu.promotion.adapter.persistence.repository.LoyaltyPointsRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LoyaltyPointsServiceTest {

    @Autowired
    LoyaltyPointsService loyaltyPointsService;

    @Autowired
    EntityManager entityManager;

    @Autowired
    LoyaltyPointsRepository loyaltyPointsRepository;

    @MockBean
    @SuppressWarnings("rawtypes")
    id.payu.promotion.application.service.EmitterPlaceholder promotionEvents;

    private static final String TEST_ACCOUNT_ID = "acc-123";
    private static final String TEST_TRANSACTION_ID = "txn-456";

    @BeforeEach
    void setUp() {
        loyaltyPointsRepository.deleteAll();
    }

    @Test
    void testAddPoints_Success() {
        CreateLoyaltyPointsRequest request = new CreateLoyaltyPointsRequest(
            TEST_ACCOUNT_ID,
            TEST_TRANSACTION_ID,
            TransactionType.EARNED,
            100,
            LocalDateTime.now().plusMonths(6)
        );

        LoyaltyPointsEntity result = loyaltyPointsService.addPoints(request);

        assertNotNull(result.getId());
        assertEquals(TEST_ACCOUNT_ID, result.getAccountId());
        assertEquals(TEST_TRANSACTION_ID, result.getTransactionId());
        assertEquals(TransactionType.EARNED, result.getTransactionType());
        assertEquals(100, result.getPoints());
        assertEquals(100, result.getBalanceAfter());
        assertNotNull(result.getCreatedAt());
    }

    @Test
    void testAddPoints_MultipleTransactions_BalanceIncrements() {
        CreateLoyaltyPointsRequest request1 = new CreateLoyaltyPointsRequest(
            TEST_ACCOUNT_ID,
            "txn-1",
            TransactionType.EARNED,
            100,
            LocalDateTime.now().plusMonths(6)
        );

        CreateLoyaltyPointsRequest request2 = new CreateLoyaltyPointsRequest(
            TEST_ACCOUNT_ID,
            "txn-2",
            TransactionType.EARNED,
            50,
            LocalDateTime.now().plusMonths(6)
        );

        LoyaltyPointsEntity result1 = loyaltyPointsService.addPoints(request1);
        assertEquals(100, result1.getBalanceAfter());

        LoyaltyPointsEntity result2 = loyaltyPointsService.addPoints(request2);
        assertEquals(150, result2.getBalanceAfter());
    }

    @Test
    void testRedeemPoints_Success() {
        CreateLoyaltyPointsRequest earnRequest = new CreateLoyaltyPointsRequest(
            TEST_ACCOUNT_ID,
            TEST_TRANSACTION_ID,
            TransactionType.EARNED,
            100,
            LocalDateTime.now().plusMonths(6)
        );

        loyaltyPointsService.addPoints(earnRequest);

        RedeemLoyaltyPointsRequest redeemRequest = new RedeemLoyaltyPointsRequest(
            TEST_ACCOUNT_ID,
            50,
            "redeem-txn-789"
        );

        LoyaltyPointsEntity result = loyaltyPointsService.redeemPoints(redeemRequest);

        assertNotNull(result.getId());
        assertEquals(TEST_ACCOUNT_ID, result.getAccountId());
        assertEquals(TransactionType.REDEEMED, result.getTransactionType());
        assertEquals(-50, result.getPoints());
        assertEquals(50, result.getBalanceAfter());
        assertNotNull(result.getRedeemedAt());
    }

    @Test
    void testRedeemPoints_InsufficientBalance_ThrowsException() {
        CreateLoyaltyPointsRequest earnRequest = new CreateLoyaltyPointsRequest(
            TEST_ACCOUNT_ID,
            TEST_TRANSACTION_ID,
            TransactionType.EARNED,
            30,
            LocalDateTime.now().plusMonths(6)
        );

        loyaltyPointsService.addPoints(earnRequest);

        RedeemLoyaltyPointsRequest redeemRequest = new RedeemLoyaltyPointsRequest(
            TEST_ACCOUNT_ID,
            50,
            "redeem-txn-789"
        );

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> loyaltyPointsService.redeemPoints(redeemRequest)
        );

        assertEquals("Insufficient loyalty points balance", exception.getMessage());
    }

    @Test
    void testGetLoyaltyPoints_Success() {
        CreateLoyaltyPointsRequest request = new CreateLoyaltyPointsRequest(
            TEST_ACCOUNT_ID,
            TEST_TRANSACTION_ID,
            TransactionType.EARNED,
            100,
            LocalDateTime.now().plusMonths(6)
        );

        LoyaltyPointsEntity created = loyaltyPointsService.addPoints(request);

        var result = loyaltyPointsService.getLoyaltyPoints(created.getId());

        assertTrue(result.isPresent());
        assertEquals(created.getId(), result.get().getId());
        assertEquals(TEST_ACCOUNT_ID, result.get().getAccountId());
    }

    @Test
    void testGetLoyaltyPoints_NotFound() {
        UUID nonExistentId = UUID.randomUUID();

        var result = loyaltyPointsService.getLoyaltyPoints(nonExistentId);

        assertFalse(result.isPresent());
    }

    @Test
    void testGetLoyaltyPointsByAccount() {
        CreateLoyaltyPointsRequest request1 = new CreateLoyaltyPointsRequest(
            TEST_ACCOUNT_ID,
            "txn-1",
            TransactionType.EARNED,
            100,
            LocalDateTime.now().plusMonths(6)
        );

        CreateLoyaltyPointsRequest request2 = new CreateLoyaltyPointsRequest(
            TEST_ACCOUNT_ID,
            "txn-2",
            TransactionType.EARNED,
            50,
            LocalDateTime.now().plusMonths(6)
        );

        loyaltyPointsService.addPoints(request1);
        loyaltyPointsService.addPoints(request2);

        var results = loyaltyPointsService.getLoyaltyPointsByAccount(TEST_ACCOUNT_ID);

        assertEquals(2, results.size());
        assertEquals(TEST_ACCOUNT_ID, results.get(0).getAccountId());
        assertEquals(TEST_ACCOUNT_ID, results.get(1).getAccountId());
    }

    @Test
    void testGetBalance() {
        CreateLoyaltyPointsRequest earnRequest = new CreateLoyaltyPointsRequest(
            TEST_ACCOUNT_ID,
            "earn-txn-1",
            TransactionType.EARNED,
            100,
            LocalDateTime.now().plusMonths(6)
        );

        CreateLoyaltyPointsRequest earnRequest2 = new CreateLoyaltyPointsRequest(
            TEST_ACCOUNT_ID,
            "earn-txn-2",
            TransactionType.EARNED,
            50,
            LocalDateTime.now().plusMonths(6)
        );

        loyaltyPointsService.addPoints(earnRequest);
        loyaltyPointsService.addPoints(earnRequest2);

        var balance = loyaltyPointsService.getBalance(TEST_ACCOUNT_ID);

        assertEquals(150, balance.currentBalance());
        assertEquals(150, balance.totalEarned());
        assertEquals(0, balance.totalRedeemed());
        assertEquals(0, balance.expiredPoints());
    }

    @Test
    void testGetBalance_WithRedeemedPoints() {
        CreateLoyaltyPointsRequest earnRequest = new CreateLoyaltyPointsRequest(
            TEST_ACCOUNT_ID,
            "earn-txn-1",
            TransactionType.EARNED,
            100,
            LocalDateTime.now().plusMonths(6)
        );

        loyaltyPointsService.addPoints(earnRequest);

        RedeemLoyaltyPointsRequest redeemRequest = new RedeemLoyaltyPointsRequest(
            TEST_ACCOUNT_ID,
            30,
            "redeem-txn-1"
        );

        loyaltyPointsService.redeemPoints(redeemRequest);

        var balance = loyaltyPointsService.getBalance(TEST_ACCOUNT_ID);

        assertEquals(70, balance.currentBalance());
        assertEquals(100, balance.totalEarned());
        assertEquals(30, balance.totalRedeemed());
        assertEquals(0, balance.expiredPoints());
    }

    @Test
    void testCalculateCurrentBalance() {
        CreateLoyaltyPointsRequest request = new CreateLoyaltyPointsRequest(
            TEST_ACCOUNT_ID,
            TEST_TRANSACTION_ID,
            TransactionType.EARNED,
            100,
            LocalDateTime.now().plusMonths(6)
        );

        loyaltyPointsService.addPoints(request);

        Integer balance = loyaltyPointsService.calculateCurrentBalance(TEST_ACCOUNT_ID);

        assertEquals(100, balance);
    }

    @Test
    void testCalculateCurrentBalance_NoTransactions_ReturnsZero() {
        Integer balance = loyaltyPointsService.calculateCurrentBalance("non-existent-account");

        assertEquals(0, balance);
    }
}
