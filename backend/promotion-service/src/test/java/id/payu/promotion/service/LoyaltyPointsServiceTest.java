package id.payu.promotion.service;

import id.payu.promotion.domain.LoyaltyPoints;
import id.payu.promotion.dto.CreateLoyaltyPointsRequest;
import id.payu.promotion.dto.RedeemLoyaltyPointsRequest;
import id.payu.promotion.test.resource.PostgresTestResource;
import io.quarkus.test.InjectMock;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@QuarkusTestResource(value = PostgresTestResource.class)
class LoyaltyPointsServiceTest {

    @Inject
    LoyaltyPointsService loyaltyPointsService;

    @Inject
    EntityManager entityManager;

    @InjectMock
    @SuppressWarnings("unused")
    Emitter<Map<String, Object>> promotionEvents;

    private static final String TEST_ACCOUNT_ID = "acc-123";
    private static final String TEST_TRANSACTION_ID = "txn-456";

    @BeforeEach
    void setUp() {
        LoyaltyPoints.deleteAll();
    }

    @Test
    @TestTransaction
    void testAddPoints_Success() {
        CreateLoyaltyPointsRequest request = new CreateLoyaltyPointsRequest(
            TEST_ACCOUNT_ID,
            TEST_TRANSACTION_ID,
            LoyaltyPoints.TransactionType.EARNED,
            100,
            LocalDateTime.now().plusMonths(6)
        );

        LoyaltyPoints result = loyaltyPointsService.addPoints(request);

        assertNotNull(result.id);
        assertEquals(TEST_ACCOUNT_ID, result.accountId);
        assertEquals(TEST_TRANSACTION_ID, result.transactionId);
        assertEquals(LoyaltyPoints.TransactionType.EARNED, result.transactionType);
        assertEquals(100, result.points);
        assertEquals(100, result.balanceAfter);
        assertNotNull(result.createdAt);
    }

    @Test
    @TestTransaction
    void testAddPoints_MultipleTransactions_BalanceIncrements() {
        CreateLoyaltyPointsRequest request1 = new CreateLoyaltyPointsRequest(
            TEST_ACCOUNT_ID,
            "txn-1",
            LoyaltyPoints.TransactionType.EARNED,
            100,
            LocalDateTime.now().plusMonths(6)
        );

        CreateLoyaltyPointsRequest request2 = new CreateLoyaltyPointsRequest(
            TEST_ACCOUNT_ID,
            "txn-2",
            LoyaltyPoints.TransactionType.EARNED,
            50,
            LocalDateTime.now().plusMonths(6)
        );

        LoyaltyPoints result1 = loyaltyPointsService.addPoints(request1);
        assertEquals(100, result1.balanceAfter);

        LoyaltyPoints result2 = loyaltyPointsService.addPoints(request2);
        assertEquals(150, result2.balanceAfter);
    }

    @Test
    @TestTransaction
    void testRedeemPoints_Success() {
        CreateLoyaltyPointsRequest earnRequest = new CreateLoyaltyPointsRequest(
            TEST_ACCOUNT_ID,
            TEST_TRANSACTION_ID,
            LoyaltyPoints.TransactionType.EARNED,
            100,
            LocalDateTime.now().plusMonths(6)
        );

        loyaltyPointsService.addPoints(earnRequest);

        RedeemLoyaltyPointsRequest redeemRequest = new RedeemLoyaltyPointsRequest(
            TEST_ACCOUNT_ID,
            50,
            "redeem-txn-789"
        );

        LoyaltyPoints result = loyaltyPointsService.redeemPoints(redeemRequest);

        assertNotNull(result.id);
        assertEquals(TEST_ACCOUNT_ID, result.accountId);
        assertEquals(LoyaltyPoints.TransactionType.REDEEMED, result.transactionType);
        assertEquals(-50, result.points);
        assertEquals(50, result.balanceAfter);
        assertNotNull(result.redeemedAt);
    }

    @Test
    @TestTransaction
    void testRedeemPoints_InsufficientBalance_ThrowsException() {
        CreateLoyaltyPointsRequest earnRequest = new CreateLoyaltyPointsRequest(
            TEST_ACCOUNT_ID,
            TEST_TRANSACTION_ID,
            LoyaltyPoints.TransactionType.EARNED,
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
    @TestTransaction
    void testGetLoyaltyPoints_Success() {
        CreateLoyaltyPointsRequest request = new CreateLoyaltyPointsRequest(
            TEST_ACCOUNT_ID,
            TEST_TRANSACTION_ID,
            LoyaltyPoints.TransactionType.EARNED,
            100,
            LocalDateTime.now().plusMonths(6)
        );

        LoyaltyPoints created = loyaltyPointsService.addPoints(request);

        var result = loyaltyPointsService.getLoyaltyPoints(created.id);

        assertTrue(result.isPresent());
        assertEquals(created.id, result.get().id);
        assertEquals(TEST_ACCOUNT_ID, result.get().accountId);
    }

    @Test
    void testGetLoyaltyPoints_NotFound() {
        UUID nonExistentId = UUID.randomUUID();

        var result = loyaltyPointsService.getLoyaltyPoints(nonExistentId);

        assertFalse(result.isPresent());
    }

    @Test
    @TestTransaction
    void testGetLoyaltyPointsByAccount() {
        CreateLoyaltyPointsRequest request1 = new CreateLoyaltyPointsRequest(
            TEST_ACCOUNT_ID,
            "txn-1",
            LoyaltyPoints.TransactionType.EARNED,
            100,
            LocalDateTime.now().plusMonths(6)
        );

        CreateLoyaltyPointsRequest request2 = new CreateLoyaltyPointsRequest(
            TEST_ACCOUNT_ID,
            "txn-2",
            LoyaltyPoints.TransactionType.EARNED,
            50,
            LocalDateTime.now().plusMonths(6)
        );

        loyaltyPointsService.addPoints(request1);
        loyaltyPointsService.addPoints(request2);

        var results = loyaltyPointsService.getLoyaltyPointsByAccount(TEST_ACCOUNT_ID);

        assertEquals(2, results.size());
        assertEquals(TEST_ACCOUNT_ID, results.get(0).accountId);
        assertEquals(TEST_ACCOUNT_ID, results.get(1).accountId);
    }

    @Test
    @TestTransaction
    void testGetBalance() {
        CreateLoyaltyPointsRequest earnRequest = new CreateLoyaltyPointsRequest(
            TEST_ACCOUNT_ID,
            "earn-txn-1",
            LoyaltyPoints.TransactionType.EARNED,
            100,
            LocalDateTime.now().plusMonths(6)
        );

        CreateLoyaltyPointsRequest earnRequest2 = new CreateLoyaltyPointsRequest(
            TEST_ACCOUNT_ID,
            "earn-txn-2",
            LoyaltyPoints.TransactionType.EARNED,
            50,
            LocalDateTime.now().plusMonths(6)
        );

        loyaltyPointsService.addPoints(earnRequest);
        loyaltyPointsService.addPoints(earnRequest2);

        var balance = loyaltyPointsService.getBalance(TEST_ACCOUNT_ID);

        assertEquals(150, balance.currentBalance());
        assertEquals(2, balance.totalEarned());
        assertEquals(0, balance.totalRedeemed());
        assertEquals(0, balance.expiredPoints());
    }

    @Test
    @TestTransaction
    void testGetBalance_WithRedeemedPoints() {
        CreateLoyaltyPointsRequest earnRequest = new CreateLoyaltyPointsRequest(
            TEST_ACCOUNT_ID,
            "earn-txn-1",
            LoyaltyPoints.TransactionType.EARNED,
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
        assertEquals(1, balance.totalEarned());
        assertEquals(1, balance.totalRedeemed());
        assertEquals(0, balance.expiredPoints());
    }

    @Test
    @TestTransaction
    void testCalculateCurrentBalance() {
        CreateLoyaltyPointsRequest request = new CreateLoyaltyPointsRequest(
            TEST_ACCOUNT_ID,
            TEST_TRANSACTION_ID,
            LoyaltyPoints.TransactionType.EARNED,
            100,
            LocalDateTime.now().plusMonths(6)
        );

        loyaltyPointsService.addPoints(request);

        Integer balance = id.payu.promotion.service.LoyaltyPointsService.calculateCurrentBalance(TEST_ACCOUNT_ID);

        assertEquals(100, balance);
    }

    @Test
    void testCalculateCurrentBalance_NoTransactions_ReturnsZero() {
        Integer balance = id.payu.promotion.service.LoyaltyPointsService.calculateCurrentBalance("non-existent-account");

        assertEquals(0, balance);
    }
}
