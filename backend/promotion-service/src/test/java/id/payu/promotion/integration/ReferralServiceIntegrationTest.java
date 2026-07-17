package id.payu.promotion.integration;

import id.payu.promotion.adapter.persistence.entity.LoyaltyPointsEntity;
import id.payu.promotion.domain.model.Referral;
import id.payu.promotion.adapter.persistence.entity.RewardEntity;
import id.payu.promotion.domain.ReferralRewardType;
import id.payu.promotion.domain.ReferralStatus;
import id.payu.promotion.domain.RewardType;
import id.payu.promotion.domain.TransactionType;
import id.payu.promotion.dto.CompleteReferralRequest;
import id.payu.promotion.dto.CreateReferralRequest;
import id.payu.promotion.dto.ReferralSummaryResponse;
import id.payu.promotion.application.service.ReferralService;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import id.payu.promotion.adapter.persistence.repository.*;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service-level integration tests for Referral operations.
 * Tests the complete data flow from service to database.
 *
 * NOTE: These tests require Docker to be running for PostgreSQL Testcontainers.
 * To run these tests: mvn test -Dtest=ReferralServiceIntegrationTest -Ddocker.enabled=true
 * To skip these tests: mvn test (they will be skipped by default)
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReferralServiceIntegrationTest {

    @Autowired
    ReferralService referralService;

    @Autowired
    ReferralRepository referralRepository;

    @Autowired
    RewardRepository rewardRepository;

    @Autowired
    LoyaltyPointsRepository loyaltyPointsRepository;

    @BeforeEach
    void setup() {
        // Clean up database before each test
        loyaltyPointsRepository.deleteAll();
        rewardRepository.deleteAll();
        referralRepository.deleteAll();
    }

    // ==================== CREATE REFERRAL TESTS ====================

    @Test
    void testCreateReferral_WithCashbackReward_ShouldGenerateUniqueCode() {
        CreateReferralRequest request = new CreateReferralRequest(
            "acc-referrer-001",
            new BigDecimal("50000"),
            new BigDecimal("25000"),
            ReferralRewardType.CASHBACK,
            LocalDateTime.now().plusMonths(3)
        );

        Referral referral = referralService.createReferral(request);

        Assertions.assertNotNull(referral.getId());
        Assertions.assertEquals("acc-referrer-001", referral.getReferrerAccountId());
        Assertions.assertNotNull(referral.getReferralCode());
        Assertions.assertEquals(8, referral.getReferralCode().length()); // 8-character code
        Assertions.assertEquals(new BigDecimal("50000.0000"), referral.getReferrerReward());
        Assertions.assertEquals(new BigDecimal("25000.0000"), referral.getRefereeReward());
        Assertions.assertEquals(ReferralRewardType.CASHBACK, referral.getRewardType());
        Assertions.assertEquals(ReferralStatus.PENDING, referral.getStatus());
        Assertions.assertNull(referral.getRefereeAccountId());
        Assertions.assertNull(referral.getCompletedAt());
        Assertions.assertNotNull(referral.getCreatedAt());

        // Verify persistence by fetching from database
        Optional<Referral> fetched = referralService.getReferral(referral.getId());
        Assertions.assertTrue(fetched.isPresent());
        Assertions.assertEquals("acc-referrer-001", fetched.get().getReferrerAccountId());
    }

    @Test
    void testCreateReferral_WithPointsReward_ShouldStoreCorrectly() {
        CreateReferralRequest request = new CreateReferralRequest(
            "acc-referrer-002",
            new BigDecimal("100"), // 100 points
            new BigDecimal("50"),  // 50 points
            ReferralRewardType.POINTS,
            LocalDateTime.now().plusMonths(6)
        );

        Referral referral = referralService.createReferral(request);

        Assertions.assertEquals(ReferralRewardType.POINTS, referral.getRewardType());
        Assertions.assertEquals(new BigDecimal("100.0000"), referral.getReferrerReward());
        Assertions.assertEquals(new BigDecimal("50.0000"), referral.getRefereeReward());
    }

    @Test
    void testCreateReferral_WithNullExpiryDate_ShouldAllowNull() {
        CreateReferralRequest request = new CreateReferralRequest(
            "acc-referrer-003",
            new BigDecimal("10000"),
            new BigDecimal("5000"),
            ReferralRewardType.CASHBACK,
            null // No expiry date
        );

        Referral referral = referralService.createReferral(request);

        Assertions.assertNull(referral.getExpiryDate());
    }

    @Test
    void testCreateReferral_MultipleReferralsForSameReferrer_ShouldGenerateDifferentCodes() {
        String referrerId = "acc-multi-referral";

        Referral referral1 = referralService.createReferral(new CreateReferralRequest(
            referrerId,
            new BigDecimal("10000"),
            new BigDecimal("5000"),
            ReferralRewardType.CASHBACK,
            LocalDateTime.now().plusMonths(3)
        ));

        Referral referral2 = referralService.createReferral(new CreateReferralRequest(
            referrerId,
            new BigDecimal("10000"),
            new BigDecimal("5000"),
            ReferralRewardType.CASHBACK,
            LocalDateTime.now().plusMonths(3)
        ));

        // Codes should be different
        Assertions.assertNotEquals(referral1.getReferralCode(), referral2.getReferralCode());

        // Both should belong to same referrer
        Assertions.assertEquals(referrerId, referral1.getReferrerAccountId());
        Assertions.assertEquals(referrerId, referral2.getReferrerAccountId());
    }

    // ==================== COMPLETE REFERRAL TESTS ====================

    @Test
    void testCompleteReferral_WithCashbackReward_ShouldGrantRewards() {
        // Create referral
        CreateReferralRequest createRequest = new CreateReferralRequest(
            "acc-cashback-referrer",
            new BigDecimal("50000"),
            new BigDecimal("25000"),
            ReferralRewardType.CASHBACK,
            LocalDateTime.now().plusMonths(3)
        );

        Referral referral = referralService.createReferral(createRequest);

        // Complete referral
        CompleteReferralRequest completeRequest = new CompleteReferralRequest(
            referral.getReferralCode(),
            "acc-cashback-referee"
        );

        Referral completed = referralService.completeReferral(completeRequest);

        Assertions.assertEquals(ReferralStatus.COMPLETED, completed.getStatus());
        Assertions.assertEquals("acc-cashback-referee", completed.getRefereeAccountId());
        Assertions.assertNotNull(completed.getCompletedAt());

        // Verify referrer reward
        List<RewardEntity> referrerRewards = rewardRepository.findByAccountId("acc-cashback-referrer");
        Assertions.assertFalse(referrerRewards.isEmpty());
        Assertions.assertTrue(referrerRewards.stream()
            .anyMatch(r -> r.getType() == RewardType.REFERRAL_BONUS
                && r.getAmount().compareTo(new BigDecimal("50000")) == 0));

        // Verify referee reward
        List<RewardEntity> refereeRewards = rewardRepository.findByAccountId("acc-cashback-referee");
        Assertions.assertFalse(refereeRewards.isEmpty());
        Assertions.assertTrue(refereeRewards.stream()
            .anyMatch(r -> r.getType() == RewardType.REFERRAL_BONUS
                && r.getAmount().compareTo(new BigDecimal("25000")) == 0));
    }

    @Test
    void testCompleteReferral_WithPointsReward_ShouldGrantPoints() {
        // Create referral with points reward
        CreateReferralRequest createRequest = new CreateReferralRequest(
            "acc-points-referrer",
            new BigDecimal("100"),
            new BigDecimal("50"),
            ReferralRewardType.POINTS,
            LocalDateTime.now().plusMonths(6)
        );

        Referral referral = referralService.createReferral(createRequest);

        // Complete referral
        CompleteReferralRequest completeRequest = new CompleteReferralRequest(
            referral.getReferralCode(),
            "acc-points-referee"
        );

        Referral completed = referralService.completeReferral(completeRequest);

        Assertions.assertEquals(ReferralStatus.COMPLETED, completed.getStatus());

        // Verify referrer points
        List<LoyaltyPointsEntity> referrerPoints = loyaltyPointsRepository.findByAccountId("acc-points-referrer");
        Assertions.assertFalse(referrerPoints.isEmpty());
        Assertions.assertTrue(referrerPoints.stream()
            .anyMatch(p -> p.getTransactionType() == TransactionType.REFERRAL_BONUS
                && p.getPoints() == 100));

        // Verify referee points
        List<LoyaltyPointsEntity> refereePoints = loyaltyPointsRepository.findByAccountId("acc-points-referee");
        Assertions.assertFalse(refereePoints.isEmpty());
        Assertions.assertTrue(refereePoints.stream()
            .anyMatch(p -> p.getTransactionType() == TransactionType.REFERRAL_BONUS
                && p.getPoints() == 50));
    }

    @Test
    void testCompleteReferral_WithInvalidCode_ShouldThrowException() {
        CompleteReferralRequest request = new CompleteReferralRequest(
            "INVALID-CODE",
            "acc-referee-001"
        );

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            referralService.completeReferral(request);
        });
    }

    @Test
    void testCompleteReferral_AlreadyCompleted_ShouldThrowException() {
        // Create and complete referral
        CreateReferralRequest createRequest = new CreateReferralRequest(
            "acc-already-completed",
            new BigDecimal("10000"),
            new BigDecimal("5000"),
            ReferralRewardType.CASHBACK,
            LocalDateTime.now().plusMonths(3)
        );

        Referral referral = referralService.createReferral(createRequest);

        CompleteReferralRequest completeRequest = new CompleteReferralRequest(
            referral.getReferralCode(),
            "acc-referee-first"
        );

        referralService.completeReferral(completeRequest);

        // Try to complete again
        CompleteReferralRequest secondAttempt = new CompleteReferralRequest(
            referral.getReferralCode(),
            "acc-referee-second"
        );

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            referralService.completeReferral(secondAttempt);
        });
    }

    @Test
    void testCompleteReferral_WithExpiredCode_ShouldThrowException() {
        // Create referral with past expiry date
        CreateReferralRequest createRequest = new CreateReferralRequest(
            "acc-expired-referrer",
            new BigDecimal("10000"),
            new BigDecimal("5000"),
            ReferralRewardType.CASHBACK,
            LocalDateTime.now().minusDays(1) // Expired yesterday
        );

        Referral referral = referralService.createReferral(createRequest);

        CompleteReferralRequest completeRequest = new CompleteReferralRequest(
            referral.getReferralCode(),
            "acc-referee-expired"
        );

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            referralService.completeReferral(completeRequest);
        });

        // Verify status was updated to EXPIRED
        Optional<Referral> fetched = referralService.getReferral(referral.getId());
        Assertions.assertTrue(fetched.isPresent());
        Assertions.assertEquals(ReferralStatus.EXPIRED, fetched.get().getStatus());
    }

    // ==================== GET REFERRAL TESTS ====================

    @Test
    void testGetReferral_WithValidId_ShouldReturnReferral() {
        CreateReferralRequest request = new CreateReferralRequest(
            "acc-get-referral",
            new BigDecimal("10000"),
            new BigDecimal("5000"),
            ReferralRewardType.CASHBACK,
            LocalDateTime.now().plusMonths(3)
        );

        Referral created = referralService.createReferral(request);

        Optional<Referral> fetched = referralService.getReferral(created.getId());

        Assertions.assertTrue(fetched.isPresent());
        Assertions.assertEquals("acc-get-referral", fetched.get().getReferrerAccountId());
    }

    @Test
    void testGetReferral_WithInvalidId_ShouldReturnEmpty() {
        Optional<Referral> fetched = referralService.getReferral(UUID.randomUUID());

        Assertions.assertTrue(fetched.isEmpty());
    }

    @Test
    void testGetReferralByCode_WithValidCode_ShouldReturnReferral() {
        CreateReferralRequest request = new CreateReferralRequest(
            "acc-by-code",
            new BigDecimal("10000"),
            new BigDecimal("5000"),
            ReferralRewardType.CASHBACK,
            LocalDateTime.now().plusMonths(3)
        );

        Referral created = referralService.createReferral(request);

        Optional<Referral> fetched = referralService.getReferralByCode(created.getReferralCode());

        Assertions.assertTrue(fetched.isPresent());
        Assertions.assertEquals(created.getReferralCode(), fetched.get().getReferralCode());
    }

    @Test
    void testGetReferralByCode_WithInvalidCode_ShouldReturnEmpty() {
        Optional<Referral> fetched = referralService.getReferralByCode("INVALID-CODE");

        Assertions.assertTrue(fetched.isEmpty());
    }

    // ==================== GET REFERRALS BY REFERRER TESTS ====================

    @Test
    void testGetReferralsByReferrer_ShouldReturnAllReferrals() {
        String referrerId = "acc-multi-referrals";

        // Create multiple referrals
        referralService.createReferral(new CreateReferralRequest(
            referrerId, new BigDecimal("10000"), new BigDecimal("5000"),
            ReferralRewardType.CASHBACK, LocalDateTime.now().plusMonths(3)
        ));
        referralService.createReferral(new CreateReferralRequest(
            referrerId, new BigDecimal("10000"), new BigDecimal("5000"),
            ReferralRewardType.CASHBACK, LocalDateTime.now().plusMonths(3)
        ));
        referralService.createReferral(new CreateReferralRequest(
            referrerId, new BigDecimal("10000"), new BigDecimal("5000"),
            ReferralRewardType.CASHBACK, LocalDateTime.now().plusMonths(3)
        ));

        List<Referral> referrals = referralService.getReferralsByReferrer(referrerId);

        Assertions.assertEquals(3, referrals.size());
        Assertions.assertTrue(referrals.stream().allMatch(r -> r.getReferrerAccountId().equals(referrerId)));
    }

    @Test
    void testGetReferralsByReferrer_WithNoReferrals_ShouldReturnEmpty() {
        List<Referral> referrals = referralService.getReferralsByReferrer("acc-no-referrals");

        Assertions.assertTrue(referrals.isEmpty());
    }

    @Test
    void testGetReferralsByReferrer_WithMixedStatuses_ShouldReturnAll() {
        String referrerId = "acc-mixed-status";

        // Create first referral and complete it
        Referral referral1 = referralService.createReferral(new CreateReferralRequest(
            referrerId, new BigDecimal("10000"), new BigDecimal("5000"),
            ReferralRewardType.CASHBACK, LocalDateTime.now().plusMonths(3)
        ));
        referralService.completeReferral(new CompleteReferralRequest(
            referral1.getReferralCode(), "acc-referee-1"
        ));

        // Create second referral (still pending)
        referralService.createReferral(new CreateReferralRequest(
            referrerId, new BigDecimal("10000"), new BigDecimal("5000"),
            ReferralRewardType.CASHBACK, LocalDateTime.now().plusMonths(3)
        ));

        List<Referral> referrals = referralService.getReferralsByReferrer(referrerId);

        Assertions.assertEquals(2, referrals.size());
        Assertions.assertTrue(referrals.stream().anyMatch(r -> r.getStatus() == ReferralStatus.COMPLETED));
        Assertions.assertTrue(referrals.stream().anyMatch(r -> r.getStatus() == ReferralStatus.PENDING));
    }

    // ==================== REFERRAL SUMMARY TESTS ====================

    @Test
    void testGetReferralSummary_WithMultipleReferrals_ShouldCalculateCorrectly() {
        String referrerId = "acc-summary-test";

        // Create 3 referrals
        Referral referral1 = referralService.createReferral(new CreateReferralRequest(
            referrerId, new BigDecimal("10000"), new BigDecimal("5000"),
            ReferralRewardType.CASHBACK, LocalDateTime.now().plusMonths(3)
        ));

        // Complete first referral
        referralService.completeReferral(new CompleteReferralRequest(
            referral1.getReferralCode(), "acc-referee-1"
        ));

        // Create 2 more pending referrals
        referralService.createReferral(new CreateReferralRequest(
            referrerId, new BigDecimal("10000"), new BigDecimal("5000"),
            ReferralRewardType.CASHBACK, LocalDateTime.now().plusMonths(3)
        ));
        referralService.createReferral(new CreateReferralRequest(
            referrerId, new BigDecimal("10000"), new BigDecimal("5000"),
            ReferralRewardType.CASHBACK, LocalDateTime.now().plusMonths(3)
        ));

        ReferralSummaryResponse summary = referralService.getReferralSummary(referrerId);

        Assertions.assertNotNull(summary.referralCode());
        Assertions.assertEquals(3, summary.totalReferrals());
        Assertions.assertEquals(1, summary.completedReferrals());
        Assertions.assertEquals(2, summary.pendingReferrals());
    }

    @Test
    void testGetReferralSummary_WithNoReferrals_ShouldReturnZeros() {
        ReferralSummaryResponse summary = referralService.getReferralSummary("acc-no-summary");

        Assertions.assertNull(summary.referralCode());
        Assertions.assertEquals(0, summary.totalReferrals());
        Assertions.assertEquals(0, summary.completedReferrals());
        Assertions.assertEquals(0, summary.pendingReferrals());
    }

    @Test
    void testGetReferralSummary_ShouldReturnLatestReferralCode() {
        String referrerId = "acc-latest-code";

        Referral referral1 = referralService.createReferral(new CreateReferralRequest(
            referrerId, new BigDecimal("10000"), new BigDecimal("5000"),
            ReferralRewardType.CASHBACK, LocalDateTime.now().plusMonths(3)
        ));

        ReferralSummaryResponse summary1 = referralService.getReferralSummary(referrerId);
        Assertions.assertEquals(referral1.getReferralCode(), summary1.referralCode());

        // Create another referral
        Referral referral2 = referralService.createReferral(new CreateReferralRequest(
            referrerId, new BigDecimal("10000"), new BigDecimal("5000"),
            ReferralRewardType.CASHBACK, LocalDateTime.now().plusMonths(3)
        ));

        ReferralSummaryResponse summary2 = referralService.getReferralSummary(referrerId);
        Assertions.assertEquals(referral2.getReferralCode(), summary2.referralCode());
    }

    // ==================== REFERRAL CODE GENERATION TESTS ====================

    @Test
    void testReferralCodeGeneration_ShouldCreateUniqueCodes() {
        // Create 100 referrals and verify all codes are unique
        List<String> codes = new java.util.ArrayList<>();

        for (int i = 0; i < 100; i++) {
            Referral referral = referralService.createReferral(new CreateReferralRequest(
                "acc-unique-" + i,
                new BigDecimal("10000"),
                new BigDecimal("5000"),
                ReferralRewardType.CASHBACK,
                LocalDateTime.now().plusMonths(3)
            ));
            codes.add(referral.getReferralCode());
        }

        // Check uniqueness
        long uniqueCount = codes.stream().distinct().count();
        Assertions.assertEquals(100, uniqueCount, "All referral codes should be unique");
    }

    @Test
    void testReferralCodeFormat_ShouldBe8Characters() {
        Referral referral = referralService.createReferral(new CreateReferralRequest(
            "acc-code-format",
            new BigDecimal("10000"),
            new BigDecimal("5000"),
            ReferralRewardType.CASHBACK,
            LocalDateTime.now().plusMonths(3)
        ));

        Assertions.assertEquals(8, referral.getReferralCode().length());
        Assertions.assertTrue(referral.getReferralCode().matches("[A-Z0-9]+"));
    }

    // ==================== MULTI-ACCOUNT REFERRAL SCENARIOS ====================

    @Test
    void testMultipleReferrers_IndependentReferralPrograms() {
        // Create referrals for different referrers
        Referral referral1 = referralService.createReferral(new CreateReferralRequest(
            "acc-referrer-A",
            new BigDecimal("50000"),
            new BigDecimal("25000"),
            ReferralRewardType.CASHBACK,
            LocalDateTime.now().plusMonths(3)
        ));

        Referral referral2 = referralService.createReferral(new CreateReferralRequest(
            "acc-referrer-B",
            new BigDecimal("30000"),
            new BigDecimal("15000"),
            ReferralRewardType.CASHBACK,
            LocalDateTime.now().plusMonths(3)
        ));

        // Complete both
        referralService.completeReferral(new CompleteReferralRequest(
            referral1.getReferralCode(), "acc-referee-A"
        ));

        referralService.completeReferral(new CompleteReferralRequest(
            referral2.getReferralCode(), "acc-referee-B"
        ));

        // Verify referrer A got 50000
        List<RewardEntity> rewardsA = rewardRepository.findByAccountId("acc-referrer-A");
        Assertions.assertTrue(rewardsA.stream()
            .anyMatch(r -> r.getAmount().compareTo(new BigDecimal("50000")) == 0));

        // Verify referrer B got 30000
        List<RewardEntity> rewardsB = rewardRepository.findByAccountId("acc-referrer-B");
        Assertions.assertTrue(rewardsB.stream()
            .anyMatch(r -> r.getAmount().compareTo(new BigDecimal("30000")) == 0));
    }
}
