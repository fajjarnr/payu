package id.payu.promotion.integration;

import id.payu.promotion.domain.LoyaltyPoints;
import id.payu.promotion.dto.CreateLoyaltyPointsRequest;
import id.payu.promotion.dto.LoyaltyBalanceResponse;
import id.payu.promotion.dto.RedeemLoyaltyPointsRequest;
import id.payu.promotion.service.LoyaltyPointsService;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service-level integration tests for Loyalty Points operations.
 * Tests the complete data flow from service to database.
 *
 * NOTE: These tests require Docker to be running for PostgreSQL Testcontainers.
 * To run these tests: mvn test -Dtest=LoyaltyPointsServiceIntegrationTest -Ddocker.enabled=true
 * To skip these tests: mvn test (they will be skipped by default)
 */
@QuarkusTest
@EnabledIfSystemProperty(named = "docker.enabled", matches = "true", disabledReason = "Docker not available")
@QuarkusTestResource(value = id.payu.promotion.test.resource.PostgresTestResource.class)
class LoyaltyPointsServiceIntegrationTest {

    @Inject
    LoyaltyPointsService loyaltyPointsService;

    @BeforeEach
    void setup() {
        // Clean up database before each test
        LoyaltyPoints.deleteAll();
    }

    // ==================== ADD POINTS TESTS ====================

    @Test
    void testAddPoints_WithValidData_ShouldPersistToDatabase() {
        CreateLoyaltyPointsRequest request = new CreateLoyaltyPointsRequest(
            "acc-points-001",
            "txn-earn-001",
            LoyaltyPoints.TransactionType.EARNED,
            100,
            LocalDateTime.now().plusMonths(6)
        );

        LoyaltyPoints points = loyaltyPointsService.addPoints(request);

        Assertions.assertNotNull(points.id);
        Assertions.assertEquals("acc-points-001", points.accountId);
        Assertions.assertEquals("txn-earn-001", points.transactionId);
        Assertions.assertEquals(LoyaltyPoints.TransactionType.EARNED, points.transactionType);
        Assertions.assertEquals(100, points.points);
        Assertions.assertEquals(100, points.balanceAfter);
        Assertions.assertNotNull(points.expiryDate);
        Assertions.assertNotNull(points.createdAt);

        // Verify persistence by fetching from database
        Optional<LoyaltyPoints> fetched = loyaltyPointsService.getLoyaltyPoints(points.id);
        Assertions.assertTrue(fetched.isPresent());
        Assertions.assertEquals("acc-points-001", fetched.get().accountId);
    }

    @Test
    void testAddPoints_WithInitialBalance_ShouldStartFromZero() {
        CreateLoyaltyPointsRequest request = new CreateLoyaltyPointsRequest(
            "acc-new-user",
            "txn-first-earn",
            LoyaltyPoints.TransactionType.EARNED,
            50,
            LocalDateTime.now().plusMonths(12)
        );

        LoyaltyPoints points = loyaltyPointsService.addPoints(request);

        // New user should have balance of 50 (0 + 50)
        Assertions.assertEquals(50, points.balanceAfter);
    }

    @Test
    void testAddPoints_MultipleTransactions_ShouldAccumulateBalance() {
        String accountId = "acc-accumulation";

        // First transaction
        loyaltyPointsService.addPoints(new CreateLoyaltyPointsRequest(
            accountId,
            "txn-001",
            LoyaltyPoints.TransactionType.EARNED,
            100,
            LocalDateTime.now().plusMonths(6)
        ));

        // Second transaction
        loyaltyPointsService.addPoints(new CreateLoyaltyPointsRequest(
            accountId,
            "txn-002",
            LoyaltyPoints.TransactionType.EARNED,
            50,
            LocalDateTime.now().plusMonths(6)
        ));

        // Third transaction
        LoyaltyPoints third = loyaltyPointsService.addPoints(new CreateLoyaltyPointsRequest(
            accountId,
            "txn-003",
            LoyaltyPoints.TransactionType.EARNED,
            75,
            LocalDateTime.now().plusMonths(6)
        ));

        // Balance should be 100 + 50 + 75 = 225
        Assertions.assertEquals(225, third.balanceAfter);

        // Verify balance
        LoyaltyBalanceResponse balance = loyaltyPointsService.getBalance(accountId);
        Assertions.assertEquals(225, balance.currentBalance());
    }

    @Test
    void testAddPoints_WithReferralBonus_ShouldTrackCorrectly() {
        CreateLoyaltyPointsRequest request = new CreateLoyaltyPointsRequest(
            "acc-referral-001",
            "txn-referral-bonus",
            LoyaltyPoints.TransactionType.REFERRAL_BONUS,
            500,
            LocalDateTime.now().plusMonths(12)
        );

        LoyaltyPoints points = loyaltyPointsService.addPoints(request);

        Assertions.assertEquals(LoyaltyPoints.TransactionType.REFERRAL_BONUS, points.transactionType);
        Assertions.assertEquals(500, points.points);
        Assertions.assertEquals(500, points.balanceAfter);

        // Verify in balance
        LoyaltyBalanceResponse balance = loyaltyPointsService.getBalance("acc-referral-001");
        Assertions.assertEquals(500, balance.currentBalance());
    }

    @Test
    void testAddPoints_WithAdjustedType_ShouldSupportAdjustments() {
        String accountId = "acc-adjust";

        // Initial earn
        loyaltyPointsService.addPoints(new CreateLoyaltyPointsRequest(
            accountId,
            "txn-initial",
            LoyaltyPoints.TransactionType.EARNED,
            200,
            LocalDateTime.now().plusMonths(6)
        ));

        // Adjustment (manual correction)
        LoyaltyPoints adjustment = loyaltyPointsService.addPoints(new CreateLoyaltyPointsRequest(
            accountId,
            "txn-adjustment",
            LoyaltyPoints.TransactionType.ADJUSTED,
            -50, // Deduct 50 points as adjustment
            null
        ));

        Assertions.assertEquals(LoyaltyPoints.TransactionType.ADJUSTED, adjustment.transactionType);
        Assertions.assertEquals(-50, adjustment.points);
        Assertions.assertEquals(150, adjustment.balanceAfter);
    }

    // ==================== REDEEM POINTS TESTS ====================

    @Test
    void testRedeemPoints_WithSufficientBalance_ShouldDeductSuccessfully() {
        String accountId = "acc-redeem-001";

        // First, earn some points
        loyaltyPointsService.addPoints(new CreateLoyaltyPointsRequest(
            accountId,
            "txn-earn",
            LoyaltyPoints.TransactionType.EARNED,
            500,
            LocalDateTime.now().plusMonths(6)
        ));

        // Redeem some points
        RedeemLoyaltyPointsRequest redeemRequest = new RedeemLoyaltyPointsRequest(
            accountId,
            100,
            "txn-redeem-001"
        );

        LoyaltyPoints redemption = loyaltyPointsService.redeemPoints(redeemRequest);

        Assertions.assertNotNull(redemption.id);
        Assertions.assertEquals(accountId, redemption.accountId);
        Assertions.assertEquals("txn-redeem-001", redemption.transactionId);
        Assertions.assertEquals(LoyaltyPoints.TransactionType.REDEEMED, redemption.transactionType);
        Assertions.assertEquals(-100, redemption.points);
        Assertions.assertEquals(400, redemption.balanceAfter); // 500 - 100 = 400
        Assertions.assertNotNull(redemption.redeemedAt);

        // Verify persistence
        Optional<LoyaltyPoints> fetched = loyaltyPointsService.getLoyaltyPoints(redemption.id);
        Assertions.assertTrue(fetched.isPresent());
        Assertions.assertEquals(-100, fetched.get().points);
    }

    @Test
    void testRedeemPoints_WithInsufficientBalance_ShouldThrowException() {
        String accountId = "acc-insufficient";

        // Only earn 50 points
        loyaltyPointsService.addPoints(new CreateLoyaltyPointsRequest(
            accountId,
            "txn-small-earn",
            LoyaltyPoints.TransactionType.EARNED,
            50,
            LocalDateTime.now().plusMonths(6)
        ));

        // Try to redeem 100 points
        RedeemLoyaltyPointsRequest redeemRequest = new RedeemLoyaltyPointsRequest(
            accountId,
            100, // More than available
            "txn-failed-redeem"
        );

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            loyaltyPointsService.redeemPoints(redeemRequest);
        });
    }

    @Test
    void testRedeemPoints_WithExactBalance_ShouldReduceToZero() {
        String accountId = "acc-exact-redeem";

        // Earn exactly 200 points
        loyaltyPointsService.addPoints(new CreateLoyaltyPointsRequest(
            accountId,
            "txn-exact-earn",
            LoyaltyPoints.TransactionType.EARNED,
            200,
            LocalDateTime.now().plusMonths(6)
        ));

        // Redeem exactly 200 points
        RedeemLoyaltyPointsRequest redeemRequest = new RedeemLoyaltyPointsRequest(
            accountId,
            200,
            "txn-exact-redeem"
        );

        LoyaltyPoints redemption = loyaltyPointsService.redeemPoints(redeemRequest);

        Assertions.assertEquals(-200, redemption.points);
        Assertions.assertEquals(0, redemption.balanceAfter);

        // Verify balance
        LoyaltyBalanceResponse balance = loyaltyPointsService.getBalance(accountId);
        Assertions.assertEquals(0, balance.currentBalance());
    }

    @Test
    void testRedeemPoints_MultipleRedemptions_ShouldTrackCorrectly() {
        String accountId = "acc-multi-redeem";

        // Earn 1000 points
        loyaltyPointsService.addPoints(new CreateLoyaltyPointsRequest(
            accountId,
            "txn-big-earn",
            LoyaltyPoints.TransactionType.EARNED,
            1000,
            LocalDateTime.now().plusMonths(6)
        ));

        // First redemption
        loyaltyPointsService.redeemPoints(new RedeemLoyaltyPointsRequest(
            accountId, 200, "txn-redeem-1"
        ));

        // Second redemption
        loyaltyPointsService.redeemPoints(new RedeemLoyaltyPointsRequest(
            accountId, 150, "txn-redeem-2"
        ));

        // Third redemption
        LoyaltyPoints thirdRedemption = loyaltyPointsService.redeemPoints(new RedeemLoyaltyPointsRequest(
            accountId, 300, "txn-redeem-3"
        ));

        // Balance should be: 1000 - 200 - 150 - 300 = 350
        Assertions.assertEquals(350, thirdRedemption.balanceAfter);

        // Verify final balance
        LoyaltyBalanceResponse balance = loyaltyPointsService.getBalance(accountId);
        Assertions.assertEquals(350, balance.currentBalance());
        Assertions.assertEquals(3, balance.totalRedeemed());
    }

    // ==================== GET POINTS TESTS ====================

    @Test
    void testGetLoyaltyPoints_WithValidId_ShouldReturnPoints() {
        CreateLoyaltyPointsRequest request = new CreateLoyaltyPointsRequest(
            "acc-get-001",
            "txn-get-001",
            LoyaltyPoints.TransactionType.EARNED,
            100,
            LocalDateTime.now().plusMonths(6)
        );

        LoyaltyPoints created = loyaltyPointsService.addPoints(request);

        Optional<LoyaltyPoints> fetched = loyaltyPointsService.getLoyaltyPoints(created.id);

        Assertions.assertTrue(fetched.isPresent());
        Assertions.assertEquals("acc-get-001", fetched.get().accountId);
        Assertions.assertEquals("txn-get-001", fetched.get().transactionId);
    }

    @Test
    void testGetLoyaltyPoints_WithInvalidId_ShouldReturnEmpty() {
        Optional<LoyaltyPoints> fetched = loyaltyPointsService.getLoyaltyPoints(UUID.randomUUID());

        Assertions.assertTrue(fetched.isEmpty());
    }

    @Test
    void testGetLoyaltyPointsByAccount_ShouldReturnAllTransactions() {
        String accountId = "acc-history-001";

        // Create multiple transactions
        loyaltyPointsService.addPoints(new CreateLoyaltyPointsRequest(
            accountId, "txn-001", LoyaltyPoints.TransactionType.EARNED, 100,
            LocalDateTime.now().plusMonths(6)
        ));
        loyaltyPointsService.addPoints(new CreateLoyaltyPointsRequest(
            accountId, "txn-002", LoyaltyPoints.TransactionType.EARNED, 50,
            LocalDateTime.now().plusMonths(6)
        ));
        loyaltyPointsService.redeemPoints(new RedeemLoyaltyPointsRequest(
            accountId, 75, "txn-003"
        ));

        List<LoyaltyPoints> transactions = loyaltyPointsService.getLoyaltyPointsByAccount(accountId);

        Assertions.assertEquals(3, transactions.size());
        Assertions.assertTrue(transactions.stream().allMatch(t -> t.accountId.equals(accountId)));

        // Should be ordered by createdAt desc (most recent first)
        // The redemption should be last since it was added after the earnings
        Assertions.assertEquals(LoyaltyPoints.TransactionType.REDEEMED, transactions.get(0).transactionType);
    }

    @Test
    void testGetLoyaltyPointsByAccount_WithNoTransactions_ShouldReturnEmpty() {
        List<LoyaltyPoints> transactions = loyaltyPointsService.getLoyaltyPointsByAccount("acc-no-history");

        Assertions.assertTrue(transactions.isEmpty());
    }

    // ==================== BALANCE TESTS ====================

    @Test
    void testGetBalance_WithNewAccount_ShouldReturnZero() {
        LoyaltyBalanceResponse balance = loyaltyPointsService.getBalance("acc-new-balance");

        Assertions.assertEquals(0, balance.currentBalance());
        Assertions.assertEquals(0, balance.totalEarned());
        Assertions.assertEquals(0, balance.totalRedeemed());
        Assertions.assertEquals(0, balance.expiredPoints());
    }

    @Test
    void testGetBalance_WithEarningsOnly_ShouldCalculateCorrectly() {
        String accountId = "acc-earn-only";

        loyaltyPointsService.addPoints(new CreateLoyaltyPointsRequest(
            accountId, "txn-001", LoyaltyPoints.TransactionType.EARNED, 100,
            LocalDateTime.now().plusMonths(6)
        ));
        loyaltyPointsService.addPoints(new CreateLoyaltyPointsRequest(
            accountId, "txn-002", LoyaltyPoints.TransactionType.EARNED, 200,
            LocalDateTime.now().plusMonths(6)
        ));
        loyaltyPointsService.addPoints(new CreateLoyaltyPointsRequest(
            accountId, "txn-003", LoyaltyPoints.TransactionType.REFERRAL_BONUS, 50,
            LocalDateTime.now().plusMonths(6)
        ));

        LoyaltyBalanceResponse balance = loyaltyPointsService.getBalance(accountId);

        Assertions.assertEquals(350, balance.currentBalance());
        Assertions.assertEquals(3, balance.totalEarned()); // 2 EARNED + 1 REFERRAL_BONUS
        Assertions.assertEquals(0, balance.totalRedeemed());
    }

    @Test
    void testGetBalance_WithEarningsAndRedemptions_ShouldCalculateCorrectly() {
        String accountId = "acc-mixed";

        // Earn 500
        loyaltyPointsService.addPoints(new CreateLoyaltyPointsRequest(
            accountId, "txn-earn-1", LoyaltyPoints.TransactionType.EARNED, 300,
            LocalDateTime.now().plusMonths(6)
        ));
        loyaltyPointsService.addPoints(new CreateLoyaltyPointsRequest(
            accountId, "txn-earn-2", LoyaltyPoints.TransactionType.EARNED, 200,
            LocalDateTime.now().plusMonths(6)
        ));

        // Redeem 150
        loyaltyPointsService.redeemPoints(new RedeemLoyaltyPointsRequest(
            accountId, 150, "txn-redeem-1"
        ));

        LoyaltyBalanceResponse balance = loyaltyPointsService.getBalance(accountId);

        Assertions.assertEquals(350, balance.currentBalance()); // 500 - 150
        Assertions.assertEquals(2, balance.totalEarned());
        Assertions.assertEquals(1, balance.totalRedeemed());
    }

    @Test
    void testGetBalance_MultipleAccounts_ShouldBeIndependent() {
        // Account 1: earns 200, redeems 50
        loyaltyPointsService.addPoints(new CreateLoyaltyPointsRequest(
            "acc-balance-1", "txn-1", LoyaltyPoints.TransactionType.EARNED, 200,
            LocalDateTime.now().plusMonths(6)
        ));
        loyaltyPointsService.redeemPoints(new RedeemLoyaltyPointsRequest(
            "acc-balance-1", 50, "txn-r1"
        ));

        // Account 2: earns 500, redeems 200
        loyaltyPointsService.addPoints(new CreateLoyaltyPointsRequest(
            "acc-balance-2", "txn-2", LoyaltyPoints.TransactionType.EARNED, 500,
            LocalDateTime.now().plusMonths(6)
        ));
        loyaltyPointsService.redeemPoints(new RedeemLoyaltyPointsRequest(
            "acc-balance-2", 200, "txn-r2"
        ));

        LoyaltyBalanceResponse balance1 = loyaltyPointsService.getBalance("acc-balance-1");
        LoyaltyBalanceResponse balance2 = loyaltyPointsService.getBalance("acc-balance-2");

        Assertions.assertEquals(150, balance1.currentBalance());
        Assertions.assertEquals(300, balance2.currentBalance());
    }

    @Test
    void testGetBalance_WithExpiredPoints_ShouldTrackExpiredCount() {
        String accountId = "acc-expired";

        // Simulate expired points
        loyaltyPointsService.addPoints(new CreateLoyaltyPointsRequest(
            accountId, "txn-earned", LoyaltyPoints.TransactionType.EARNED, 100,
            LocalDateTime.now().plusMonths(6)
        ));

        // Simulate expiration (manual entry)
        LoyaltyPoints expired = loyaltyPointsService.addPoints(new CreateLoyaltyPointsRequest(
            accountId, "txn-expired", LoyaltyPoints.TransactionType.EXPIRED, -100,
            null
        ));

        LoyaltyBalanceResponse balance = loyaltyPointsService.getBalance(accountId);

        // Current balance: 100 (earned) - 100 (expired) = 0
        Assertions.assertEquals(0, balance.currentBalance());
        Assertions.assertTrue(balance.expiredPoints() >= 1);
    }

    // ==================== EXPIRY DATE TESTS ====================

    @Test
    void testAddPoints_WithExpiryDate_ShouldStoreCorrectly() {
        LocalDateTime expiryDate = LocalDateTime.now().plusMonths(12);

        CreateLoyaltyPointsRequest request = new CreateLoyaltyPointsRequest(
            "acc-expiry-001",
            "txn-with-expiry",
            LoyaltyPoints.TransactionType.EARNED,
            100,
            expiryDate
        );

        LoyaltyPoints points = loyaltyPointsService.addPoints(request);

        Assertions.assertEquals(expiryDate.withNano(0), points.expiryDate.withNano(0));

        // Verify persistence
        Optional<LoyaltyPoints> fetched = loyaltyPointsService.getLoyaltyPoints(points.id);
        Assertions.assertTrue(fetched.isPresent());
        Assertions.assertNotNull(fetched.get().expiryDate);
    }

    @Test
    void testAddPoints_WithNullExpiryDate_ShouldAllowNull() {
        CreateLoyaltyPointsRequest request = new CreateLoyaltyPointsRequest(
            "acc-no-expiry",
            "txn-no-expiry",
            LoyaltyPoints.TransactionType.ADJUSTED,
            100,
            null // No expiry date
        );

        LoyaltyPoints points = loyaltyPointsService.addPoints(request);

        Assertions.assertNull(points.expiryDate);
    }

    // ==================== HIGH VOLUME TESTS ====================

    @Test
    void testHighVolumePointsAccumulation() {
        String accountId = "acc-high-volume";

        // Add points in small increments
        for (int i = 0; i < 100; i++) {
            loyaltyPointsService.addPoints(new CreateLoyaltyPointsRequest(
                accountId,
                "txn-" + i,
                LoyaltyPoints.TransactionType.EARNED,
                10, // 10 points each
                LocalDateTime.now().plusMonths(6)
            ));
        }

        LoyaltyBalanceResponse balance = loyaltyPointsService.getBalance(accountId);

        // 100 transactions x 10 points = 1000 points
        Assertions.assertEquals(1000, balance.currentBalance());
        Assertions.assertEquals(100, balance.totalEarned());
    }

    @Test
    void testLargePointsValue_ShouldHandleCorrectly() {
        CreateLoyaltyPointsRequest request = new CreateLoyaltyPointsRequest(
            "acc-large-points",
            "txn-large",
            LoyaltyPoints.TransactionType.EARNED,
            999999, // Nearly 1 million points
            LocalDateTime.now().plusMonths(12)
        );

        LoyaltyPoints points = loyaltyPointsService.addPoints(request);

        Assertions.assertEquals(999999, points.points);
        Assertions.assertEquals(999999, points.balanceAfter);

        LoyaltyBalanceResponse balance = loyaltyPointsService.getBalance("acc-large-points");
        Assertions.assertEquals(999999, balance.currentBalance());
    }
}
