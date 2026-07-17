package id.payu.promotion.integration;

import id.payu.promotion.domain.model.LoyaltyPoints;
import id.payu.promotion.domain.TransactionType;
import id.payu.promotion.dto.CreateLoyaltyPointsRequest;
import id.payu.promotion.dto.LoyaltyBalanceResponse;
import id.payu.promotion.dto.RedeemLoyaltyPointsRequest;
import id.payu.promotion.application.service.LoyaltyPointsService;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import id.payu.promotion.adapter.persistence.repository.LoyaltyPointsRepository;
import org.junit.jupiter.api.*;

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
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LoyaltyPointsServiceIntegrationTest {

    @Autowired
    LoyaltyPointsService loyaltyPointsService;

    @Autowired
    LoyaltyPointsRepository loyaltyPointsRepository;

    @BeforeEach
    void setup() {
        // Clean up database before each test
        loyaltyPointsRepository.deleteAll();
    }

    // ==================== ADD POINTS TESTS ====================

    @Test
    void testAddPoints_WithValidData_ShouldPersistToDatabase() {
        CreateLoyaltyPointsRequest request = new CreateLoyaltyPointsRequest(
            "acc-points-001",
            "txn-earn-001",
            TransactionType.EARNED,
            100,
            LocalDateTime.now().plusMonths(6)
        );

        LoyaltyPoints points = loyaltyPointsService.addPoints(request);

        Assertions.assertNotNull(points.getId());
        Assertions.assertEquals("acc-points-001", points.getAccountId());
        Assertions.assertEquals("txn-earn-001", points.getTransactionId());
        Assertions.assertEquals(TransactionType.EARNED, points.getTransactionType());
        Assertions.assertEquals(100, points.getPoints());
        Assertions.assertEquals(100, points.getBalanceAfter());
        Assertions.assertNotNull(points.getExpiryDate());
        Assertions.assertNotNull(points.getCreatedAt());

        // Verify persistence by fetching from database
        Optional<LoyaltyPoints> fetched = loyaltyPointsService.getLoyaltyPoints(points.getId());
        Assertions.assertTrue(fetched.isPresent());
        Assertions.assertEquals("acc-points-001", fetched.get().getAccountId());
    }

    @Test
    void testAddPoints_WithInitialBalance_ShouldStartFromZero() {
        CreateLoyaltyPointsRequest request = new CreateLoyaltyPointsRequest(
            "acc-new-user",
            "txn-first-earn",
            TransactionType.EARNED,
            50,
            LocalDateTime.now().plusMonths(12)
        );

        LoyaltyPoints points = loyaltyPointsService.addPoints(request);

        // New user should have balance of 50 (0 + 50)
        Assertions.assertEquals(50, points.getBalanceAfter());
    }

    @Test
    void testAddPoints_MultipleTransactions_ShouldAccumulateBalance() {
        String accountId = "acc-accumulation";

        // First transaction
        loyaltyPointsService.addPoints(new CreateLoyaltyPointsRequest(
            accountId,
            "txn-001",
            TransactionType.EARNED,
            100,
            LocalDateTime.now().plusMonths(6)
        ));

        // Second transaction
        loyaltyPointsService.addPoints(new CreateLoyaltyPointsRequest(
            accountId,
            "txn-002",
            TransactionType.EARNED,
            50,
            LocalDateTime.now().plusMonths(6)
        ));

        // Third transaction
        LoyaltyPoints third = loyaltyPointsService.addPoints(new CreateLoyaltyPointsRequest(
            accountId,
            "txn-003",
            TransactionType.EARNED,
            75,
            LocalDateTime.now().plusMonths(6)
        ));

        // Balance should be 100 + 50 + 75 = 225
        Assertions.assertEquals(225, third.getBalanceAfter());

        // Verify balance
        LoyaltyBalanceResponse balance = loyaltyPointsService.getBalance(accountId);
        Assertions.assertEquals(225, balance.currentBalance());
    }

    @Test
    void testAddPoints_WithReferralBonus_ShouldTrackCorrectly() {
        CreateLoyaltyPointsRequest request = new CreateLoyaltyPointsRequest(
            "acc-referral-001",
            "txn-referral-bonus",
            TransactionType.REFERRAL_BONUS,
            500,
            LocalDateTime.now().plusMonths(12)
        );

        LoyaltyPoints points = loyaltyPointsService.addPoints(request);

        Assertions.assertEquals(TransactionType.REFERRAL_BONUS, points.getTransactionType());
        Assertions.assertEquals(500, points.getPoints());
        Assertions.assertEquals(500, points.getBalanceAfter());

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
            TransactionType.EARNED,
            200,
            LocalDateTime.now().plusMonths(6)
        ));

        // Adjustment (manual correction)
        LoyaltyPoints adjustment = loyaltyPointsService.addPoints(new CreateLoyaltyPointsRequest(
            accountId,
            "txn-adjustment",
            TransactionType.ADJUSTED,
            -50, // Deduct 50 points as adjustment
            null
        ));

        Assertions.assertEquals(TransactionType.ADJUSTED, adjustment.getTransactionType());
        Assertions.assertEquals(-50, adjustment.getPoints());
        Assertions.assertEquals(150, adjustment.getBalanceAfter());
    }

    // ==================== REDEEM POINTS TESTS ====================

    @Test
    void testRedeemPoints_WithSufficientBalance_ShouldDeductSuccessfully() {
        String accountId = "acc-redeem-001";

        // First, earn some points
        loyaltyPointsService.addPoints(new CreateLoyaltyPointsRequest(
            accountId,
            "txn-earn",
            TransactionType.EARNED,
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

        Assertions.assertNotNull(redemption.getId());
        Assertions.assertEquals(accountId, redemption.getAccountId());
        Assertions.assertEquals("txn-redeem-001", redemption.getTransactionId());
        Assertions.assertEquals(TransactionType.REDEEMED, redemption.getTransactionType());
        Assertions.assertEquals(-100, redemption.getPoints());
        Assertions.assertEquals(400, redemption.getBalanceAfter()); // 500 - 100 = 400
        Assertions.assertNotNull(redemption.getRedeemedAt());

        // Verify persistence
        Optional<LoyaltyPoints> fetched = loyaltyPointsService.getLoyaltyPoints(redemption.getId());
        Assertions.assertTrue(fetched.isPresent());
        Assertions.assertEquals(-100, fetched.get().getPoints());
    }

    @Test
    void testRedeemPoints_WithInsufficientBalance_ShouldThrowException() {
        String accountId = "acc-insufficient";

        // Only earn 50 points
        loyaltyPointsService.addPoints(new CreateLoyaltyPointsRequest(
            accountId,
            "txn-small-earn",
            TransactionType.EARNED,
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
            TransactionType.EARNED,
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

        Assertions.assertEquals(-200, redemption.getPoints());
        Assertions.assertEquals(0, redemption.getBalanceAfter());

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
            TransactionType.EARNED,
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
        Assertions.assertEquals(350, thirdRedemption.getBalanceAfter());

        // Verify final balance
        LoyaltyBalanceResponse balance = loyaltyPointsService.getBalance(accountId);
        Assertions.assertEquals(350, balance.currentBalance());
        Assertions.assertEquals(650, balance.totalRedeemed()); // sum of redeemed points (200+150+300)
    }

    // ==================== GET POINTS TESTS ====================

    @Test
    void testGetLoyaltyPoints_WithValidId_ShouldReturnPoints() {
        CreateLoyaltyPointsRequest request = new CreateLoyaltyPointsRequest(
            "acc-get-001",
            "txn-get-001",
            TransactionType.EARNED,
            100,
            LocalDateTime.now().plusMonths(6)
        );

        LoyaltyPoints created = loyaltyPointsService.addPoints(request);

        Optional<LoyaltyPoints> fetched = loyaltyPointsService.getLoyaltyPoints(created.getId());

        Assertions.assertTrue(fetched.isPresent());
        Assertions.assertEquals("acc-get-001", fetched.get().getAccountId());
        Assertions.assertEquals("txn-get-001", fetched.get().getTransactionId());
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
            accountId, "txn-001", TransactionType.EARNED, 100,
            LocalDateTime.now().plusMonths(6)
        ));
        loyaltyPointsService.addPoints(new CreateLoyaltyPointsRequest(
            accountId, "txn-002", TransactionType.EARNED, 50,
            LocalDateTime.now().plusMonths(6)
        ));
        loyaltyPointsService.redeemPoints(new RedeemLoyaltyPointsRequest(
            accountId, 75, "txn-003"
        ));

        List<LoyaltyPoints> transactions = loyaltyPointsService.getLoyaltyPointsByAccount(accountId);

        Assertions.assertEquals(3, transactions.size());
        Assertions.assertTrue(transactions.stream().allMatch(t -> t.getAccountId().equals(accountId)));

        // Should be ordered by createdAt desc (most recent first)
        // The redemption should be last since it was added after the earnings
        Assertions.assertEquals(TransactionType.REDEEMED, transactions.get(0).getTransactionType());
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
            accountId, "txn-001", TransactionType.EARNED, 100,
            LocalDateTime.now().plusMonths(6)
        ));
        loyaltyPointsService.addPoints(new CreateLoyaltyPointsRequest(
            accountId, "txn-002", TransactionType.EARNED, 200,
            LocalDateTime.now().plusMonths(6)
        ));
        loyaltyPointsService.addPoints(new CreateLoyaltyPointsRequest(
            accountId, "txn-003", TransactionType.REFERRAL_BONUS, 50,
            LocalDateTime.now().plusMonths(6)
        ));

        LoyaltyBalanceResponse balance = loyaltyPointsService.getBalance(accountId);

        Assertions.assertEquals(350, balance.currentBalance());
        Assertions.assertEquals(300, balance.totalEarned()); // only EARNED points (100+200)
        Assertions.assertEquals(0, balance.totalRedeemed());
    }

    @Test
    void testGetBalance_WithEarningsAndRedemptions_ShouldCalculateCorrectly() {
        String accountId = "acc-mixed";

        // Earn 500
        loyaltyPointsService.addPoints(new CreateLoyaltyPointsRequest(
            accountId, "txn-earn-1", TransactionType.EARNED, 300,
            LocalDateTime.now().plusMonths(6)
        ));
        loyaltyPointsService.addPoints(new CreateLoyaltyPointsRequest(
            accountId, "txn-earn-2", TransactionType.EARNED, 200,
            LocalDateTime.now().plusMonths(6)
        ));

        // Redeem 150
        loyaltyPointsService.redeemPoints(new RedeemLoyaltyPointsRequest(
            accountId, 150, "txn-redeem-1"
        ));

        LoyaltyBalanceResponse balance = loyaltyPointsService.getBalance(accountId);

        Assertions.assertEquals(350, balance.currentBalance()); // 500 - 150
        Assertions.assertEquals(500, balance.totalEarned()); // sum of earned points (300+200)
        Assertions.assertEquals(150, balance.totalRedeemed()); // sum of redeemed points
    }

    @Test
    void testGetBalance_MultipleAccounts_ShouldBeIndependent() {
        // Account 1: earns 200, redeems 50
        loyaltyPointsService.addPoints(new CreateLoyaltyPointsRequest(
            "acc-balance-1", "txn-1", TransactionType.EARNED, 200,
            LocalDateTime.now().plusMonths(6)
        ));
        loyaltyPointsService.redeemPoints(new RedeemLoyaltyPointsRequest(
            "acc-balance-1", 50, "txn-r1"
        ));

        // Account 2: earns 500, redeems 200
        loyaltyPointsService.addPoints(new CreateLoyaltyPointsRequest(
            "acc-balance-2", "txn-2", TransactionType.EARNED, 500,
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
            accountId, "txn-earned", TransactionType.EARNED, 100,
            LocalDateTime.now().plusMonths(6)
        ));

        // Simulate expiration (manual entry)
        LoyaltyPoints expired = loyaltyPointsService.addPoints(new CreateLoyaltyPointsRequest(
            accountId, "txn-expired", TransactionType.EXPIRED, -100,
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
            TransactionType.EARNED,
            100,
            expiryDate
        );

        LoyaltyPoints points = loyaltyPointsService.addPoints(request);

        Assertions.assertEquals(expiryDate.withNano(0), points.getExpiryDate().withNano(0));

        // Verify persistence
        Optional<LoyaltyPoints> fetched = loyaltyPointsService.getLoyaltyPoints(points.getId());
        Assertions.assertTrue(fetched.isPresent());
        Assertions.assertNotNull(fetched.get().getExpiryDate());
    }

    @Test
    void testAddPoints_WithNullExpiryDate_ShouldAllowNull() {
        CreateLoyaltyPointsRequest request = new CreateLoyaltyPointsRequest(
            "acc-no-expiry",
            "txn-no-expiry",
            TransactionType.ADJUSTED,
            100,
            null // No expiry date
        );

        LoyaltyPoints points = loyaltyPointsService.addPoints(request);

        Assertions.assertNull(points.getExpiryDate());
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
                TransactionType.EARNED,
                10, // 10 points each
                LocalDateTime.now().plusMonths(6)
            ));
        }

        LoyaltyBalanceResponse balance = loyaltyPointsService.getBalance(accountId);

        // 100 transactions x 10 points = 1000 points
        Assertions.assertEquals(1000, balance.currentBalance());
        Assertions.assertEquals(1000, balance.totalEarned()); // sum of points, not count
    }

    @Test
    void testLargePointsValue_ShouldHandleCorrectly() {
        CreateLoyaltyPointsRequest request = new CreateLoyaltyPointsRequest(
            "acc-large-points",
            "txn-large",
            TransactionType.EARNED,
            999999, // Nearly 1 million points
            LocalDateTime.now().plusMonths(12)
        );

        LoyaltyPoints points = loyaltyPointsService.addPoints(request);

        Assertions.assertEquals(999999, points.getPoints());
        Assertions.assertEquals(999999, points.getBalanceAfter());

        LoyaltyBalanceResponse balance = loyaltyPointsService.getBalance("acc-large-points");
        Assertions.assertEquals(999999, balance.currentBalance());
    }
}
