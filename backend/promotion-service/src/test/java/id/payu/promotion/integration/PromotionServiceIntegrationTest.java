package id.payu.promotion.integration;

import id.payu.promotion.domain.Promotion;
import id.payu.promotion.domain.Reward;
import id.payu.promotion.dto.*;
import id.payu.promotion.application.service.PromotionService;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import id.payu.promotion.adapter.persistence.repository.PromotionRepository;
import id.payu.promotion.adapter.persistence.repository.RewardRepository;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Service-level integration tests for Promotion CRUD operations.
 * Tests the complete data flow from service to database.
 *
 * NOTE: These tests require Docker to be running for PostgreSQL Testcontainers.
 * To run these tests: mvn test -Dtest=PromotionServiceIntegrationTest -Ddocker.enabled=true
 * To skip these tests: mvn test (they will be skipped by default)
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PromotionServiceIntegrationTest {

    @Autowired
    PromotionService promotionService;

    @Autowired
    PromotionRepository promotionRepository;

    @Autowired
    RewardRepository rewardRepository;

    @BeforeEach
    void setup() {
        // Clean up database before each test
        rewardRepository.deleteAll();
        promotionRepository.deleteAll();
    }

    // ==================== CREATE PROMOTION TESTS ====================

    @Test
    void testCreatePromotion_WithValidData_ShouldPersistToDatabase() {
        LocalDateTime now = LocalDateTime.now();
        CreatePromotionRequest request = new CreatePromotionRequest(
            "TEST-CASHBACK-001",
            "Test Cashback Promotion",
            "10% cashback on all transactions",
            Promotion.PromotionType.CASHBACK,
            Promotion.RewardType.PERCENTAGE,
            new BigDecimal("10"),
            1000,
            new BigDecimal("50000"),
            now,
            now.plusMonths(1)
        );

        Promotion promotion = promotionService.createPromotion(request);

        Assertions.assertNotNull(promotion.getId());
        Assertions.assertEquals("TEST-CASHBACK-001", promotion.code);
        Assertions.assertEquals("Test Cashback Promotion", promotion.name);
        Assertions.assertEquals(Promotion.PromotionType.CASHBACK, promotion.promotionType);
        Assertions.assertEquals(Promotion.RewardType.PERCENTAGE, promotion.rewardType);
        Assertions.assertEquals(new BigDecimal("10"), promotion.rewardValue);
        Assertions.assertEquals(1000, promotion.maxRedemptions);
        Assertions.assertEquals(new BigDecimal("50000"), promotion.minTransactionAmount);
        Assertions.assertEquals(Promotion.Status.DRAFT, promotion.status);
        Assertions.assertEquals(0, promotion.getRedemptionCount());
        Assertions.assertNotNull(promotion.getCreatedAt());
        Assertions.assertNotNull(promotion.getUpdatedAt());

        // Verify persistence by fetching from database
        Optional<Promotion> fetched = promotionService.getPromotion(promotion.getId());
        Assertions.assertTrue(fetched.isPresent());
        Assertions.assertEquals("TEST-CASHBACK-001", fetched.get().getCode());
    }

    @Test
    void testCreatePromotion_WithEndDateBeforeStartDate_ShouldThrowException() {
        LocalDateTime now = LocalDateTime.now();
        CreatePromotionRequest request = new CreatePromotionRequest(
            "TEST-INVALID-001",
            "Invalid Promotion",
            "End date before start date",
            Promotion.PromotionType.DISCOUNT,
            Promotion.RewardType.FIXED_AMOUNT,
            new BigDecimal("5000"),
            100,
            new BigDecimal("10000"),
            now.plusDays(1),
            now.minusDays(1) // End date before start date
        );

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            promotionService.createPromotion(request);
        });
    }

    @Test
    void testCreatePromotion_WithNullDates_ShouldThrowException() {
        CreatePromotionRequest request = new CreatePromotionRequest(
            "TEST-NODATE-001",
            "No Date Promotion",
            "Missing dates",
            Promotion.PromotionType.DISCOUNT,
            Promotion.RewardType.FIXED_AMOUNT,
            new BigDecimal("5000"),
            100,
            new BigDecimal("10000"),
            null,
            null
        );

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            promotionService.createPromotion(request);
        });
    }

    // ==================== UPDATE PROMOTION TESTS ====================

    @Test
    void testUpdatePromotion_WithValidData_ShouldUpdateDatabase() {
        // First create a promotion
        LocalDateTime now = LocalDateTime.now();
        CreatePromotionRequest createRequest = new CreatePromotionRequest(
            "TEST-UPDATE-001",
            "Original Name",
            "Original description",
            Promotion.PromotionType.DISCOUNT,
            Promotion.RewardType.PERCENTAGE,
            new BigDecimal("15"),
            500,
            new BigDecimal("30000"),
            now,
            now.plusMonths(1)
        );

        Promotion created = promotionService.createPromotion(createRequest);

        // Update the promotion
        UpdatePromotionRequest updateRequest = new UpdatePromotionRequest(
            "Updated Name",
            "Updated description",
            Promotion.Status.ACTIVE,
            null,
            null
        );

        Promotion updated = promotionService.updatePromotion(created.getId(), updateRequest);

        Assertions.assertEquals("Updated Name", updated.getName());
        Assertions.assertEquals("Updated description", updated.getDescription());
        Assertions.assertEquals(Promotion.Status.ACTIVE, updated.getStatus());

        // Verify persistence
        Optional<Promotion> fetched = promotionService.getPromotion(created.getId());
        Assertions.assertTrue(fetched.isPresent());
        Assertions.assertEquals("Updated Name", fetched.get().getName());
        Assertions.assertEquals(Promotion.Status.ACTIVE, fetched.get().getStatus());
    }

    @Test
    void testUpdatePromotion_WithNonExistentId_ShouldThrowException() {
        UpdatePromotionRequest updateRequest = new UpdatePromotionRequest(
            "New Name",
            "New description",
            Promotion.Status.ACTIVE,
            null,
            null
        );

        UUID nonExistentId = UUID.randomUUID();

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            promotionService.updatePromotion(nonExistentId, updateRequest);
        });
    }

    @Test
    void testUpdatePromotion_WithInvalidEndDate_ShouldThrowException() {
        LocalDateTime now = LocalDateTime.now();
        CreatePromotionRequest createRequest = new CreatePromotionRequest(
            "TEST-UPDATE-DATE-001",
            "Date Test",
            "Test date validation",
            Promotion.PromotionType.DISCOUNT,
            Promotion.RewardType.PERCENTAGE,
            new BigDecimal("10"),
            100,
            new BigDecimal("10000"),
            now,
            now.plusMonths(3)
        );

        Promotion created = promotionService.createPromotion(createRequest);

        // Try to update with invalid end date
        UpdatePromotionRequest updateRequest = new UpdatePromotionRequest(
            null,
            null,
            null,
            now.minusDays(5), // End date before start date
            now.plusMonths(1)
        );

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            promotionService.updatePromotion(created.getId(), updateRequest);
        });
    }

    // ==================== ACTIVATE PROMOTION TESTS ====================

    @Test
    void testActivatePromotion_WithinValidityPeriod_ShouldActivate() {
        LocalDateTime now = LocalDateTime.now();
        CreatePromotionRequest request = new CreatePromotionRequest(
            "TEST-ACTIVATE-001",
            "Test Activation",
            "Test promotion activation",
            Promotion.PromotionType.CASHBACK,
            Promotion.RewardType.FIXED_AMOUNT,
            new BigDecimal("2000"),
            1000,
            new BigDecimal("25000"),
            now.minusHours(1), // Started 1 hour ago
            now.plusMonths(1)
        );

        Promotion created = promotionService.createPromotion(request);
        Assertions.assertEquals(Promotion.Status.DRAFT, created.getStatus());

        Promotion activated = promotionService.activatePromotion(created.getId());

        Assertions.assertEquals(Promotion.Status.ACTIVE, activated.getStatus());

        // Verify persistence
        Optional<Promotion> fetched = promotionService.getPromotion(created.getId());
        Assertions.assertTrue(fetched.isPresent());
        Assertions.assertEquals(Promotion.Status.ACTIVE, fetched.get().getStatus());
    }

    @Test
    void testActivatePromotion_OutsideValidityPeriod_ShouldThrowException() {
        LocalDateTime now = LocalDateTime.now();
        CreatePromotionRequest request = new CreatePromotionRequest(
            "TEST-ACTIVATE-INVALID-001",
            "Future Promotion",
            "Promotion that starts in the future",
            Promotion.PromotionType.DISCOUNT,
            Promotion.RewardType.PERCENTAGE,
            new BigDecimal("20"),
            500,
            new BigDecimal("10000"),
            now.plusDays(7), // Starts in 7 days
            now.plusDays(30)
        );

        Promotion created = promotionService.createPromotion(request);

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            promotionService.activatePromotion(created.getId());
        });
    }

    @Test
    void testActivatePromotion_WithNonExistentId_ShouldThrowException() {
        UUID nonExistentId = UUID.randomUUID();

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            promotionService.activatePromotion(nonExistentId);
        });
    }

    // ==================== CLAIM PROMOTION TESTS ====================

    @Test
    void testClaimPromotion_WithPercentageReward_ShouldCalculateCorrectly() {
        LocalDateTime now = LocalDateTime.now();
        CreatePromotionRequest request = new CreatePromotionRequest(
            "TEST-CLAIM-PCT-001",
            "Percentage Test",
            "10% discount",
            Promotion.PromotionType.DISCOUNT,
            Promotion.RewardType.PERCENTAGE,
            new BigDecimal("10"),
            1000,
            new BigDecimal("10000"),
            now.minusHours(1),
            now.plusMonths(1)
        );

        Promotion promotion = promotionService.createPromotion(request);
        promotionService.activatePromotion(promotion.getId());

        ClaimPromotionRequest claimRequest = new ClaimPromotionRequest(
            "acc-test-claim-1",
            "txn-claim-001",
            new BigDecimal("50000"),
            "MERCHANT-001",
            "SHOPPING"
        );

        Reward reward = promotionService.claimPromotion("TEST-CLAIM-PCT-001", claimRequest);

        Assertions.assertNotNull(reward.getId());
        Assertions.assertEquals("acc-test-claim-1", reward.getAccountId());
        Assertions.assertEquals("txn-claim-001", reward.getTransactionId());
        Assertions.assertEquals("TEST-CLAIM-PCT-001", reward.getPromotionCode());
        Assertions.assertEquals(Reward.RewardType.PROMOTION_REWARD, reward.getType());
        Assertions.assertEquals(new BigDecimal("5000"), reward.getAmount()); // 10% of 50000
        Assertions.assertEquals(new BigDecimal("50000"), reward.getTransactionAmount());
        Assertions.assertEquals(Reward.Status.AWARDED, reward.getStatus());

        // Verify redemption count incremented
        Optional<Promotion> updatedPromo = promotionService.getPromotion(promotion.getId());
        Assertions.assertTrue(updatedPromo.isPresent());
        Assertions.assertEquals(1, updatedPromo.get().getRedemptionCount());
    }

    @Test
    void testClaimPromotion_WithFixedAmountReward_ShouldAwardFixedAmount() {
        LocalDateTime now = LocalDateTime.now();
        CreatePromotionRequest request = new CreatePromotionRequest(
            "TEST-CLAIM-FIX-001",
            "Fixed Amount Test",
            "Fixed 5000 reward",
            Promotion.PromotionType.CASHBACK,
            Promotion.RewardType.FIXED_AMOUNT,
            new BigDecimal("5000"),
            1000,
            new BigDecimal("10000"),
            now.minusHours(1),
            now.plusMonths(1)
        );

        Promotion promotion = promotionService.createPromotion(request);
        promotionService.activatePromotion(promotion.getId());

        ClaimPromotionRequest claimRequest = new ClaimPromotionRequest(
            "acc-test-claim-2",
            "txn-claim-002",
            new BigDecimal("100000"), // Large transaction
            "MERCHANT-002",
            "DINING"
        );

        Reward reward = promotionService.claimPromotion("TEST-CLAIM-FIX-001", claimRequest);

        // Fixed amount should be awarded regardless of transaction amount
        Assertions.assertEquals(new BigDecimal("5000"), reward.getAmount());
    }

    @Test
    void testClaimPromotion_WithPointsReward_ShouldAwardPoints() {
        LocalDateTime now = LocalDateTime.now();
        CreatePromotionRequest request = new CreatePromotionRequest(
            "TEST-CLAIM-PTS-001",
            "Points Test",
            "Award 100 points",
            Promotion.PromotionType.REWARD_POINTS,
            Promotion.RewardType.POINTS,
            new BigDecimal("100"),
            1000,
            new BigDecimal("10000"),
            now.minusHours(1),
            now.plusMonths(1)
        );

        Promotion promotion = promotionService.createPromotion(request);
        promotionService.activatePromotion(promotion.getId());

        ClaimPromotionRequest claimRequest = new ClaimPromotionRequest(
            "acc-test-claim-3",
            "txn-claim-003",
            new BigDecimal("50000"),
            null,
            null
        );

        Reward reward = promotionService.claimPromotion("TEST-CLAIM-PTS-001", claimRequest);

        Assertions.assertEquals(new BigDecimal("100"), reward.getAmount());
        Assertions.assertEquals(100, reward.getPointsEarned());
    }

    @Test
    void testClaimPromotion_WithInactiveStatus_ShouldThrowException() {
        LocalDateTime now = LocalDateTime.now();
        CreatePromotionRequest request = new CreatePromotionRequest(
            "TEST-CLAIM-INACTIVE-001",
            "Inactive Test",
            "Draft promotion",
            Promotion.PromotionType.DISCOUNT,
            Promotion.RewardType.PERCENTAGE,
            new BigDecimal("10"),
            1000,
            new BigDecimal("10000"),
            now,
            now.plusMonths(1)
        );

        promotionService.createPromotion(request);

        ClaimPromotionRequest claimRequest = new ClaimPromotionRequest(
            "acc-test-claim-4",
            "txn-claim-004",
            new BigDecimal("50000"),
            null,
            null
        );

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            promotionService.claimPromotion("TEST-CLAIM-INACTIVE-001", claimRequest);
        });
    }

    @Test
    void testClaimPromotion_WithExpiredPromotion_ShouldThrowException() {
        LocalDateTime now = LocalDateTime.now();
        CreatePromotionRequest request = new CreatePromotionRequest(
            "TEST-CLAIM-EXP-001",
            "Expired Test",
            "Expired promotion",
            Promotion.PromotionType.DISCOUNT,
            Promotion.RewardType.PERCENTAGE,
            new BigDecimal("10"),
            1000,
            new BigDecimal("10000"),
            now.minusDays(10),
            now.minusDays(1) // Expired yesterday
        );

        Promotion promotion = promotionService.createPromotion(request);
        promotion.setStatus(Promotion.Status.ACTIVE);
        promotionRepository.save(promotion);

        ClaimPromotionRequest claimRequest = new ClaimPromotionRequest(
            "acc-test-claim-5",
            "txn-claim-005",
            new BigDecimal("50000"),
            null,
            null
        );

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            promotionService.claimPromotion("TEST-CLAIM-EXP-001", claimRequest);
        });
    }

    @Test
    void testClaimPromotion_WithMaxRedemptionsReached_ShouldThrowException() {
        LocalDateTime now = LocalDateTime.now();
        CreatePromotionRequest request = new CreatePromotionRequest(
            "TEST-CLAIM-MAX-001",
            "Max Redemptions Test",
            "Limited redemptions",
            Promotion.PromotionType.CASHBACK,
            Promotion.RewardType.FIXED_AMOUNT,
            new BigDecimal("1000"),
            2, // Only 2 redemptions allowed
            new BigDecimal("10000"),
            now.minusHours(1),
            now.plusMonths(1)
        );

        Promotion promotion = promotionService.createPromotion(request);
        promotionService.activatePromotion(promotion.getId());

        // First claim - should succeed
        ClaimPromotionRequest claim1 = new ClaimPromotionRequest(
            "acc-test-claim-6a",
            "txn-claim-006a",
            new BigDecimal("50000"),
            null,
            null
        );
        promotionService.claimPromotion("TEST-CLAIM-MAX-001", claim1);

        // Second claim - should succeed
        ClaimPromotionRequest claim2 = new ClaimPromotionRequest(
            "acc-test-claim-6b",
            "txn-claim-006b",
            new BigDecimal("50000"),
            null,
            null
        );
        promotionService.claimPromotion("TEST-CLAIM-MAX-001", claim2);

        // Third claim - should fail
        ClaimPromotionRequest claim3 = new ClaimPromotionRequest(
            "acc-test-claim-6c",
            "txn-claim-006c",
            new BigDecimal("50000"),
            null,
            null
        );

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            promotionService.claimPromotion("TEST-CLAIM-MAX-001", claim3);
        });
    }

    @Test
    void testClaimPromotion_WithInsufficientTransactionAmount_ShouldThrowException() {
        LocalDateTime now = LocalDateTime.now();
        CreatePromotionRequest request = new CreatePromotionRequest(
            "TEST-CLAIM-MIN-001",
            "Min Amount Test",
            "Minimum transaction required",
            Promotion.PromotionType.DISCOUNT,
            Promotion.RewardType.PERCENTAGE,
            new BigDecimal("10"),
            1000,
            new BigDecimal("100000"), // Minimum 100,000
            now.minusHours(1),
            now.plusMonths(1)
        );

        Promotion promotion = promotionService.createPromotion(request);
        promotionService.activatePromotion(promotion.getId());

        ClaimPromotionRequest claimRequest = new ClaimPromotionRequest(
            "acc-test-claim-7",
            "txn-claim-007",
            new BigDecimal("50000"), // Below minimum
            null,
            null
        );

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            promotionService.claimPromotion("TEST-CLAIM-MIN-001", claimRequest);
        });
    }

    @Test
    void testClaimPromotion_WithInvalidCode_ShouldThrowException() {
        ClaimPromotionRequest claimRequest = new ClaimPromotionRequest(
            "acc-test-claim-8",
            "txn-claim-008",
            new BigDecimal("50000"),
            null,
            null
        );

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            promotionService.claimPromotion("INVALID-CODE", claimRequest);
        });
    }

    // ==================== GET PROMOTION TESTS ====================

    @Test
    void testGetPromotion_WithValidId_ShouldReturnPromotion() {
        LocalDateTime now = LocalDateTime.now();
        CreatePromotionRequest request = new CreatePromotionRequest(
            "TEST-GET-001",
            "Get Test",
            "Test get by ID",
            Promotion.PromotionType.DISCOUNT,
            Promotion.RewardType.PERCENTAGE,
            new BigDecimal("10"),
            1000,
            new BigDecimal("10000"),
            now,
            now.plusMonths(1)
        );

        Promotion created = promotionService.createPromotion(request);

        Optional<Promotion> fetched = promotionService.getPromotion(created.getId());

        Assertions.assertTrue(fetched.isPresent());
        Assertions.assertEquals("TEST-GET-001", fetched.get().getCode());
    }

    @Test
    void testGetPromotion_WithInvalidId_ShouldReturnEmpty() {
        Optional<Promotion> fetched = promotionService.getPromotion(UUID.randomUUID());

        Assertions.assertTrue(fetched.isEmpty());
    }

    @Test
    void testGetPromotionByCode_WithValidCode_ShouldReturnPromotion() {
        LocalDateTime now = LocalDateTime.now();
        CreatePromotionRequest request = new CreatePromotionRequest(
            "TEST-GET-CODE-001",
            "Get By Code Test",
            "Test get by code",
            Promotion.PromotionType.DISCOUNT,
            Promotion.RewardType.PERCENTAGE,
            new BigDecimal("10"),
            1000,
            new BigDecimal("10000"),
            now,
            now.plusMonths(1)
        );

        promotionService.createPromotion(request);

        Optional<Promotion> fetched = promotionService.getPromotionByCode("TEST-GET-CODE-001");

        Assertions.assertTrue(fetched.isPresent());
        Assertions.assertEquals("TEST-GET-CODE-001", fetched.get().getCode());
    }

    @Test
    void testGetPromotionByCode_WithInvalidCode_ShouldReturnEmpty() {
        Optional<Promotion> fetched = promotionService.getPromotionByCode("INVALID-CODE");

        Assertions.assertTrue(fetched.isEmpty());
    }

    // ==================== CAMPAIGN MANAGEMENT TESTS ====================

    @Test
    void testCampaignLifecycle_FromDraftToExpired() {
        LocalDateTime now = LocalDateTime.now();

        // Create campaign
        CreatePromotionRequest request = new CreatePromotionRequest(
            "TEST-CAMPAIGN-001",
            "Test Campaign",
            "Full campaign lifecycle",
            Promotion.PromotionType.CASHBACK,
            Promotion.RewardType.FIXED_AMOUNT,
            new BigDecimal("1000"),
            100,
            new BigDecimal("10000"),
            now.minusHours(1),
            now.plusDays(7)
        );

        Promotion campaign = promotionService.createPromotion(request);
        Assertions.assertEquals(Promotion.Status.DRAFT, campaign.getStatus());

        // Activate
        campaign = promotionService.activatePromotion(campaign.getId());
        Assertions.assertEquals(Promotion.Status.ACTIVE, campaign.getStatus());

        // Simulate claims
        for (int i = 0; i < 3; i++) {
            ClaimPromotionRequest claimRequest = new ClaimPromotionRequest(
                "acc-campaign-" + i,
                "txn-campaign-" + i,
                new BigDecimal("50000"),
                null,
                null
            );
            promotionService.claimPromotion("TEST-CAMPAIGN-001", claimRequest);
        }

        // Verify claims
        Optional<Promotion> finalCampaign = promotionService.getPromotion(campaign.getId());
        Assertions.assertTrue(finalCampaign.isPresent());
        Assertions.assertEquals(3, finalCampaign.get().getRedemptionCount());
    }

    @Test
    void testMultiplePromotions_IndependentRedemptionCounts() {
        LocalDateTime now = LocalDateTime.now();

        // Create first promotion
        CreatePromotionRequest request1 = new CreatePromotionRequest(
            "TEST-MULTI-001",
            "First Promotion",
            "First test",
            Promotion.PromotionType.DISCOUNT,
            Promotion.RewardType.PERCENTAGE,
            new BigDecimal("10"),
            100,
            new BigDecimal("10000"),
            now.minusHours(1),
            now.plusMonths(1)
        );

        // Create second promotion
        CreatePromotionRequest request2 = new CreatePromotionRequest(
            "TEST-MULTI-002",
            "Second Promotion",
            "Second test",
            Promotion.PromotionType.CASHBACK,
            Promotion.RewardType.FIXED_AMOUNT,
            new BigDecimal("2000"),
            100,
            new BigDecimal("10000"),
            now.minusHours(1),
            now.plusMonths(1)
        );

        Promotion promo1 = promotionService.createPromotion(request1);
        Promotion promo2 = promotionService.createPromotion(request2);

        promotionService.activatePromotion(promo1.getId());
        promotionService.activatePromotion(promo2.getId());

        // Claim first promotion twice
        ClaimPromotionRequest claim1 = new ClaimPromotionRequest(
            "acc-multi-1",
            "txn-multi-1",
            new BigDecimal("50000"),
            null,
            null
        );
        promotionService.claimPromotion("TEST-MULTI-001", claim1);

        ClaimPromotionRequest claim2 = new ClaimPromotionRequest(
            "acc-multi-2",
            "txn-multi-2",
            new BigDecimal("50000"),
            null,
            null
        );
        promotionService.claimPromotion("TEST-MULTI-001", claim2);

        // Claim second promotion once
        ClaimPromotionRequest claim3 = new ClaimPromotionRequest(
            "acc-multi-3",
            "txn-multi-3",
            new BigDecimal("50000"),
            null,
            null
        );
        promotionService.claimPromotion("TEST-MULTI-002", claim3);

        // Verify independent counts
        Optional<Promotion> fetched1 = promotionService.getPromotion(promo1.getId());
        Optional<Promotion> fetched2 = promotionService.getPromotion(promo2.getId());

        Assertions.assertTrue(fetched1.isPresent());
        Assertions.assertTrue(fetched2.isPresent());
        Assertions.assertEquals(2, fetched1.get().getRedemptionCount());
        Assertions.assertEquals(1, fetched2.get().getRedemptionCount());
    }
}
