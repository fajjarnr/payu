package id.payu.promotion.integration;

import id.payu.promotion.domain.*;
import id.payu.promotion.dto.*;
import id.payu.promotion.application.service.GamificationService;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import id.payu.promotion.adapter.persistence.repository.*;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service-level integration tests for Gamification operations.
 * Tests the complete data flow from service to database.
 *
 * NOTE: These tests require Docker to be running for PostgreSQL Testcontainers.
 * To run these tests: mvn test -Dtest=GamificationServiceIntegrationTest -Ddocker.enabled=true
 * To skip these tests: mvn test (they will be skipped by default)
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class GamificationServiceIntegrationTest {

    @Autowired
    GamificationService gamificationService;

    @Autowired
    UserBadgeRepository userBadgeRepository;

    @Autowired
    BadgeRepository badgeRepository;

    @Autowired
    DailyCheckinRepository dailyCheckinRepository;

    @Autowired
    UserLevelRepository userLevelRepository;

    @Autowired
    XpTransactionRepository xpTransactionRepository;

    @Autowired
    LoyaltyPointsRepository loyaltyPointsRepository;

    @Autowired
    LevelRewardRepository levelRewardRepository;

    @BeforeEach
    void setup() {
        // Clean up database before each test
        userBadgeRepository.deleteAll();
        badgeRepository.deleteAll();
        dailyCheckinRepository.deleteAll();
        userLevelRepository.deleteAll();
        xpTransactionRepository.deleteAll();
        loyaltyPointsRepository.deleteAll();
    }

    // ==================== DAILY CHECKIN TESTS ====================

    @Test
    void testPerformDailyCheckin_FirstTime_ShouldCreateEntryWithPoints() {
        String accountId = "acc-checkin-001";

        DailyCheckinResponse response = gamificationService.performDailyCheckin(accountId);

        Assertions.assertNotNull(response.id());
        Assertions.assertEquals(accountId, response.accountId());
        Assertions.assertEquals(LocalDate.now(), response.checkinDate());
        Assertions.assertEquals(1, response.streakCount());
        Assertions.assertTrue(response.pointsEarned() > 0); // Should earn points
        Assertions.assertNotNull(response.createdAt());

        // Verify persistence
        DailyCheckin checkin = dailyCheckinRepository.findByAccountIdAndCheckinDate(accountId, LocalDate.now())
            .orElse(null);
        Assertions.assertNotNull(checkin);
        Assertions.assertEquals(1, checkin.getStreakCount());
    }

    @Test
    void testPerformDailyCheckin_ConsecutiveDays_ShouldIncrementStreak() {
        String accountId = "acc-checkin-streak";

        // First checkin (yesterday)
        LocalDate yesterday = LocalDate.now().minusDays(1);
        DailyCheckin yesterdayCheckin = new DailyCheckin();
        yesterdayCheckin.setAccountId(accountId);
        yesterdayCheckin.setCheckinDate(yesterday);
        yesterdayCheckin.setStreakCount(3);
        yesterdayCheckin.setPointsEarned(15);
        dailyCheckinRepository.save(yesterdayCheckin);

        // Checkin today
        DailyCheckinResponse response = gamificationService.performDailyCheckin(accountId);

        Assertions.assertEquals(4, response.streakCount()); // Streak should increment
        Assertions.assertTrue(response.pointsEarned() > 15); // More points for longer streak

        // Verify in database
        DailyCheckin todayCheckin = dailyCheckinRepository.findByAccountIdAndCheckinDate(accountId, LocalDate.now())
            .orElse(null);
        Assertions.assertNotNull(todayCheckin);
        Assertions.assertEquals(4, todayCheckin.getStreakCount());
    }

    @Test
    void testPerformDailyCheckin_BrokenStreak_ShouldResetToOne() {
        String accountId = "acc-checkin-reset";

        // Checkin 3 days ago
        LocalDate threeDaysAgo = LocalDate.now().minusDays(3);
        DailyCheckin oldCheckin = new DailyCheckin();
        oldCheckin.setAccountId(accountId);
        oldCheckin.setCheckinDate(threeDaysAgo);
        oldCheckin.setStreakCount(5);
        oldCheckin.setPointsEarned(25);
        dailyCheckinRepository.save(oldCheckin);

        // Checkin today (streak broken)
        DailyCheckinResponse response = gamificationService.performDailyCheckin(accountId);

        Assertions.assertEquals(1, response.streakCount()); // Streak reset to 1
    }

    @Test
    void testPerformDailyCheckin_AlreadyCheckedInToday_ShouldThrowException() {
        String accountId = "acc-checkin-duplicate";

        // First checkin
        gamificationService.performDailyCheckin(accountId);

        // Try to checkin again on the same day
        Assertions.assertThrows(IllegalStateException.class, () -> {
            gamificationService.performDailyCheckin(accountId);
        });
    }

    @Test
    void testGetTodayCheckin_WithCheckinToday_ShouldReturnCheckin() {
        String accountId = "acc-checkin-today";

        // Perform checkin
        DailyCheckinResponse performed = gamificationService.performDailyCheckin(accountId);

        // Get today's checkin
        DailyCheckinResponse retrieved = gamificationService.getTodayCheckin(accountId);

        Assertions.assertNotNull(retrieved);
        Assertions.assertEquals(performed.id(), retrieved.id());
        Assertions.assertEquals(accountId, retrieved.accountId());
        Assertions.assertEquals(LocalDate.now(), retrieved.checkinDate());
    }

    @Test
    void testGetTodayCheckin_WithNoCheckinToday_ShouldReturnNull() {
        String accountId = "acc-no-checkin";

        DailyCheckinResponse response = gamificationService.getTodayCheckin(accountId);

        Assertions.assertNull(response);
    }

    @Test
    void testGetCurrentStreak_WithActiveStreak_ShouldReturnCorrectCount() {
        String accountId = "acc-streak-active";

        // Create checkin from yesterday with streak of 5
        LocalDate yesterday = LocalDate.now().minusDays(1);
        DailyCheckin checkin = new DailyCheckin();
        checkin.setAccountId(accountId);
        checkin.setCheckinDate(yesterday);
        checkin.setStreakCount(5);
        checkin.setPointsEarned(25);
        dailyCheckinRepository.save(checkin);

        Integer streak = gamificationService.getCurrentStreak(accountId);

        Assertions.assertEquals(5, streak);
    }

    @Test
    void testGetCurrentStreak_WithBrokenStreak_ShouldReturnZero() {
        String accountId = "acc-streak-broken";

        // Create checkin from 3 days ago
        LocalDate threeDaysAgo = LocalDate.now().minusDays(3);
        DailyCheckin checkin = new DailyCheckin();
        checkin.setAccountId(accountId);
        checkin.setCheckinDate(threeDaysAgo);
        checkin.setStreakCount(5);
        checkin.setPointsEarned(25);
        dailyCheckinRepository.save(checkin);

        Integer streak = gamificationService.getCurrentStreak(accountId);

        Assertions.assertEquals(0, streak); // Streak broken
    }

    @Test
    void testGetCurrentStreak_WithNoCheckins_ShouldReturnZero() {
        Integer streak = gamificationService.getCurrentStreak("acc-no-streak");

        Assertions.assertEquals(0, streak);
    }

    @Test
    void testGetTotalCheckins_ShouldReturnCorrectCount() {
        String accountId = "acc-total-checkins";

        // Create multiple checkins
        for (int i = 0; i < 5; i++) {
            DailyCheckin checkin = new DailyCheckin();
            checkin.setAccountId(accountId);
            checkin.setCheckinDate(LocalDate.now().minusDays(i));
            checkin.setStreakCount(i + 1);
            checkin.setPointsEarned(5 * (i + 1));
            dailyCheckinRepository.save(checkin);
        }

        Long total = gamificationService.getTotalCheckins(accountId);

        Assertions.assertEquals(5, total);
    }

    // ==================== USER LEVEL TESTS ====================

    @Test
    void testProcessTransaction_FirstTransaction_ShouldCreateLevel1() {
        String accountId = "acc-level-new";
        ProcessTransactionRequest request = new ProcessTransactionRequest(
            accountId,
            "txn-level-001",
            new BigDecimal("50000"),
            null,
            null
        );

        GamificationEventResponse response = gamificationService.processTransaction(request);

        Assertions.assertNotNull(response);
        Assertions.assertTrue(response.xpEarned() > 0);

        UserLevel userLevel = userLevelRepository.findByAccountId(accountId).orElse(null);
        Assertions.assertNotNull(userLevel);
        Assertions.assertEquals(1, userLevel.getLevel());
        Assertions.assertTrue(userLevel.getXp() > 0);
        Assertions.assertEquals("Pemula", userLevel.getLevelName());
    }

    @Test
    void testProcessTransaction_EnoughXpForLevelUp_ShouldLevelUp() {
        String accountId = "acc-level-up";

        // Create initial level with XP close to level 2 threshold
        UserLevel userLevel = new UserLevel();
        userLevel.setAccountId(accountId);
        userLevel.setLevel(1);
        userLevel.setXp(50); // Need 100 for level 2
        userLevel.setLevelName("Pemula");
        userLevelRepository.save(userLevel);

        // Process large transaction to earn enough XP
        ProcessTransactionRequest request = new ProcessTransactionRequest(
            accountId,
            "txn-level-up",
            new BigDecimal("100000"), // Should earn ~10 XP
            null,
            null
        );

        GamificationEventResponse response = gamificationService.processTransaction(request);

        Assertions.assertNotNull(response.levelUp());
        Assertions.assertTrue(response.levelUp().level() >= 2);

        // Verify database
        UserLevel updated = userLevelRepository.findByAccountId(accountId).orElse(null);
        Assertions.assertTrue(updated.getLevel() >= 2);
    }

    @Test
    void testProcessTransaction_AlreadyProcessedTxn_ShouldSkip() {
        String accountId = "acc-duplicate-txn";

        // Create existing XP transaction
        XpTransaction existingTx = new XpTransaction();
        existingTx.setAccountId(accountId);
        existingTx.setTransactionId("txn-duplicate");
        existingTx.setSourceType(XpTransaction.SourceType.TRANSACTION);
        existingTx.setXpEarned(10);
        existingTx.setXpAfter(10);
        xpTransactionRepository.save(existingTx);

        // Try to process same transaction again
        ProcessTransactionRequest request = new ProcessTransactionRequest(
            accountId,
            "txn-duplicate", // Same transaction ID
            new BigDecimal("50000"),
            null,
            null
        );

        GamificationEventResponse response = gamificationService.processTransaction(request);

        Assertions.assertEquals(0, response.xpEarned());
        Assertions.assertNull(response.levelUp());
        Assertions.assertTrue(response.badgesEarned().isEmpty());
    }

    @Test
    void testGetUserLevel_WithExistingLevel_ShouldReturnLevel() {
        String accountId = "acc-get-level";

        UserLevel userLevel = new UserLevel();
        userLevel.setAccountId(accountId);
        userLevel.setLevel(3);
        userLevel.setXp(450);
        userLevel.setLevelName("Pengguna");
        userLevelRepository.save(userLevel);

        UserLevelResponse response = gamificationService.getUserLevel(accountId);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(accountId, response.accountId());
        Assertions.assertEquals(3, response.level());
        Assertions.assertEquals(450, response.xp());
        Assertions.assertEquals("Pengguna", response.levelName());
    }

    @Test
    void testGetUserLevel_WithNoLevel_ShouldReturnNull() {
        UserLevelResponse response = gamificationService.getUserLevel("acc-no-level");

        Assertions.assertNull(response);
    }

    // ==================== BADGE TESTS ====================

    @Test
    void testProcessTransaction_ShouldAwardEligibleBadges() {
        String accountId = "acc-badge-earn";

        // Create a badge for transaction count
        Badge badge = new Badge();
        badge.setName("First Transaction");
        badge.setDescription("Complete your first transaction");
        badge.setRequirementType(Badge.RequirementType.TRANSACTION_COUNT);
        badge.setRequirementValue(new BigDecimal("1"));
        badge.setPointsReward(50);
        badge.setCategory("MILESTONE");
        badge.setIsActive(true);
        badgeRepository.save(badge);

        // Process transaction
        ProcessTransactionRequest request = new ProcessTransactionRequest(
            accountId,
            "txn-badge-001",
            new BigDecimal("50000"),
            null,
            null
        );

        GamificationEventResponse response = gamificationService.processTransaction(request);

        Assertions.assertFalse(response.badgesEarned().isEmpty());
        Assertions.assertTrue(response.badgesEarned().stream()
            .anyMatch(b -> b.badgeName().equals("First Transaction")));
    }

    @Test
    void testGetUserBadges_WithEarnedBadges_ShouldReturnBadges() {
        String accountId = "acc-user-badges";

        // Create badge
        Badge badge = new Badge();
        badge.setName("Test Badge");
        badge.setDescription("Test description");
        badge.setRequirementType(Badge.RequirementType.LEVEL_REACHED);
        badge.setRequirementValue(new BigDecimal("1"));
        badge.setPointsReward(10);
        badge.setCategory("TEST");
        badge.setIsActive(true);
        badgeRepository.save(badge);

        // Award badge to user
        UserBadge userBadge = new UserBadge();
        userBadge.setAccountId(accountId);
        userBadge.setBadgeId(badge.getId());
        userBadgeRepository.save(userBadge);

        List<EarnedBadgeResponse> badges = gamificationService.getUserBadges(accountId);

        Assertions.assertFalse(badges.isEmpty());
        Assertions.assertTrue(badges.stream().anyMatch(b -> b.badgeName().equals("Test Badge")));
    }

    @Test
    void testGetUserBadges_WithNoBadges_ShouldReturnEmpty() {
        List<EarnedBadgeResponse> badges = gamificationService.getUserBadges("acc-no-badges");

        Assertions.assertTrue(badges.isEmpty());
    }

    @Test
    void testGetBadgeProgress_ShouldShowProgressForAllBadges() {
        String accountId = "acc-badge-progress";

        // Create badges
        Badge badge1 = new Badge();
        badge1.setName("Novice Shopper");
        badge1.setDescription("Complete 5 transactions");
        badge1.setRequirementType(Badge.RequirementType.TRANSACTION_COUNT);
        badge1.setRequirementValue(new BigDecimal("5"));
        badge1.setPointsReward(100);
        badge1.setCategory("SHOPPING");
        badge1.setIsActive(true);
        badgeRepository.save(badge1);

        Badge badge2 = new Badge();
        badge2.setName("Big Spender");
        badge2.setDescription("Spend 1,000,000 total");
        badge2.setRequirementType(Badge.RequirementType.TOTAL_AMOUNT);
        badge2.setRequirementValue(new BigDecimal("1000000"));
        badge2.setPointsReward(500);
        badge2.setCategory("SPENDING");
        badge2.setIsActive(true);
        badgeRepository.save(badge2);

        List<BadgeProgressResponse> progress = gamificationService.getBadgeProgress(accountId);

        Assertions.assertFalse(progress.isEmpty());
        Assertions.assertTrue(progress.stream().anyMatch(b -> b.name().equals("Novice Shopper")));
        Assertions.assertTrue(progress.stream().anyMatch(b -> b.name().equals("Big Spender")));
    }

    // ==================== GAMIFICATION SUMMARY TESTS ====================

    @Test
    void testGetSummary_ShouldReturnCompleteSummary() {
        String accountId = "acc-summary";

        // Create user level
        UserLevel userLevel = new UserLevel();
        userLevel.setAccountId(accountId);
        userLevel.setLevel(2);
        userLevel.setXp(150);
        userLevel.setLevelName("Pengunjung");
        userLevelRepository.save(userLevel);

        // Create checkin
        DailyCheckin checkin = new DailyCheckin();
        checkin.setAccountId(accountId);
        checkin.setCheckinDate(LocalDate.now());
        checkin.setStreakCount(3);
        checkin.setPointsEarned(15);
        dailyCheckinRepository.save(checkin);

        GamificationSummaryResponse summary = gamificationService.getSummary(accountId);

        Assertions.assertNotNull(summary);
        Assertions.assertNotNull(summary.level());
        Assertions.assertEquals(2, summary.level().level());
        Assertions.assertNotNull(summary.lastCheckin());
        Assertions.assertEquals(3, summary.currentStreak());
        Assertions.assertTrue(summary.totalCheckins() >= 1);
    }

    @Test
    void testGetSummary_WithNewUser_ShouldReturnMinimalSummary() {
        String accountId = "acc-new-user";

        GamificationSummaryResponse summary = gamificationService.getSummary(accountId);

        Assertions.assertNotNull(summary);
        Assertions.assertNull(summary.level());
        Assertions.assertNull(summary.lastCheckin());
        Assertions.assertEquals(0, summary.currentStreak());
        Assertions.assertEquals(0, summary.totalCheckins());
        Assertions.assertTrue(summary.badges().isEmpty());
    }

    // ==================== XP CALCULATION TESTS ====================

    @Test
    void testProcessTransaction_XpCalculationBasedOnAmount() {
        String accountId = "acc-xp-calc";

        // Transaction of 100,000 should give 10 XP (1 XP per 10,000)
        ProcessTransactionRequest request = new ProcessTransactionRequest(
            accountId,
            "txn-xp-001",
            new BigDecimal("100000"),
            null,
            null
        );

        GamificationEventResponse response = gamificationService.processTransaction(request);

        Assertions.assertEquals(10, response.xpEarned());
    }

    @Test
    void testProcessTransaction_MinimumXpPerTransaction() {
        String accountId = "acc-min-xp";

        // Small transaction should still give minimum 1 XP
        ProcessTransactionRequest request = new ProcessTransactionRequest(
            accountId,
            "txn-min-xp",
            new BigDecimal("1000"), // Small amount
            null,
            null
        );

        GamificationEventResponse response = gamificationService.processTransaction(request);

        Assertions.assertEquals(1, response.xpEarned()); // Minimum 1 XP
    }

    // ==================== LEVEL REWARD TESTS ====================

    @Test
    void testLevelUp_ShouldGrantLevelRewards() {
        String accountId = "acc-level-reward";

        // Create level reward
        LevelReward reward = new LevelReward();
        reward.setLevel(2);
        reward.setPointsReward(100);
        reward.setBonusDescription("Level 2 Bonus");
        levelRewardRepository.save(reward);

        // Create user at level 1
        UserLevel userLevel = new UserLevel();
        userLevel.setAccountId(accountId);
        userLevel.setLevel(1);
        userLevel.setXp(90);
        userLevel.setLevelName("Pemula");
        userLevelRepository.save(userLevel);

        // Process transaction to trigger level up
        ProcessTransactionRequest request = new ProcessTransactionRequest(
            accountId,
            "txn-level-reward",
            new BigDecimal("100000"), // 10 XP
            null,
            null
        );

        GamificationEventResponse response = gamificationService.processTransaction(request);

        Assertions.assertNotNull(response.levelUp());
        Assertions.assertEquals(2, response.levelUp().level());

        // Verify points were awarded
        List<LoyaltyPoints> points = loyaltyPointsRepository.findByAccountIdOrderByCreatedAtDesc(accountId);
        Assertions.assertTrue(points.stream().anyMatch(p ->
            p.getTransactionType() == LoyaltyPoints.TransactionType.EARNED && p.getPoints() >= 100));
    }

    // ==================== STREAK-BASED BADGE TESTS ====================

    @Test
    void testCheckin_ShouldAwardStreakBadges() {
        String accountId = "acc-streak-badge";

        // Create streak badge
        Badge badge = new Badge();
        badge.setName("3-Day Streak");
        badge.setDescription("Check in for 3 consecutive days");
        badge.setRequirementType(Badge.RequirementType.STREAK_DAYS);
        badge.setRequirementValue(new BigDecimal("3"));
        badge.setPointsReward(50);
        badge.setCategory("STREAK");
        badge.setIsActive(true);
        badgeRepository.save(badge);

        // Check in for 3 days
        LocalDate today = LocalDate.now();
        for (int i = 2; i >= 0; i--) {
            DailyCheckin checkin = new DailyCheckin();
            checkin.setAccountId(accountId);
            checkin.setCheckinDate(today.minusDays(i));
            checkin.setStreakCount(3 - i);
            checkin.setPointsEarned(5 * (3 - i));
            dailyCheckinRepository.save(checkin);
        }

        // 4th checkin should award badge
        gamificationService.performDailyCheckin(accountId);

        // Badge might be awarded via checkAndAwardBadges
        List<EarnedBadgeResponse> userBadges = gamificationService.getUserBadges(accountId);
        Assertions.assertTrue(userBadges.stream().anyMatch(b -> b.badgeName().equals("3-Day Streak")));
    }

    // ==================== LEVEL-BASED BADGE TESTS ====================

    @Test
    void testLevelUp_ShouldAwardLevelBadges() {
        String accountId = "acc-level-badge";

        // Create level badge
        Badge badge = new Badge();
        badge.setName("Level 3 Achiever");
        badge.setDescription("Reach level 3");
        badge.setRequirementType(Badge.RequirementType.LEVEL_REACHED);
        badge.setRequirementValue(new BigDecimal("3"));
        badge.setPointsReward(100);
        badge.setCategory("LEVEL");
        badge.setIsActive(true);
        badgeRepository.save(badge);

        // Create user at level 2 with high XP
        UserLevel userLevel = new UserLevel();
        userLevel.setAccountId(accountId);
        userLevel.setLevel(2);
        userLevel.setXp(250);
        userLevel.setLevelName("Pengunjung");
        userLevelRepository.save(userLevel);

        // Add more XP to reach level 3
        ProcessTransactionRequest request = new ProcessTransactionRequest(
            accountId,
            "txn-level-3",
            new BigDecimal("100000"), // 10 XP, should trigger level up to 3
            null,
            null
        );

        gamificationService.processTransaction(request);

        // Verify badge was awarded
        List<EarnedBadgeResponse> userBadges = gamificationService.getUserBadges(accountId);
        Assertions.assertTrue(userBadges.stream().anyMatch(b -> b.badgeName().equals("Level 3 Achiever")));
    }

    // ==================== MULTI-USER ISOLATION TESTS ====================

    @Test
    void testMultipleUsers_GamificationDataIsIndependent() {
        // User 1 checkin
        DailyCheckinResponse checkin1 = gamificationService.performDailyCheckin("acc-user-1");
        Assertions.assertEquals(1, checkin1.streakCount());

        // User 2 checkin (should not affect user 1)
        DailyCheckinResponse checkin2 = gamificationService.performDailyCheckin("acc-user-2");
        Assertions.assertEquals(1, checkin2.streakCount());

        // Verify independence
        DailyCheckinResponse retrieved1 = gamificationService.getTodayCheckin("acc-user-1");
        DailyCheckinResponse retrieved2 = gamificationService.getTodayCheckin("acc-user-2");

        Assertions.assertNotEquals(retrieved1.id(), retrieved2.id());
    }
}
