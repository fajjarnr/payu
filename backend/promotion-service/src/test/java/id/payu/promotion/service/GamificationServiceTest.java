package id.payu.promotion.service;

import id.payu.promotion.domain.*;
import id.payu.promotion.dto.*;
import id.payu.promotion.test.resource.PostgresTestResource;
import io.quarkus.test.InjectMock;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@QuarkusTest
@Disabled("Service tests require PostgreSQL Testcontainers - disabled when Docker not available")
@QuarkusTestResource(value = PostgresTestResource.class)
class GamificationServiceTest {

    @Inject
    GamificationService gamificationService;

    @InjectMock
    @SuppressWarnings("unused")
    LoyaltyPointsService loyaltyPointsService;

    private static final String TEST_ACCOUNT_ID = "acc-test-123";

    @BeforeEach
    @TestTransaction
    void setUp() {
        DailyCheckin.deleteAll();
        UserBadge.deleteAll();
        UserLevel.deleteAll();
        XpTransaction.deleteAll();
        Badge.deleteAll();
    }

    @Test
    @TestTransaction
    void testPerformDailyCheckin_Success() {
        DailyCheckinResponse response = gamificationService.performDailyCheckin(TEST_ACCOUNT_ID);

        assertNotNull(response.id());
        assertEquals(TEST_ACCOUNT_ID, response.accountId());
        assertEquals(LocalDate.now(), response.checkinDate());
        assertEquals(1, response.streakCount());
        assertTrue(response.pointsEarned() > 0);
    }

    @Test
    @TestTransaction
    void testPerformDailyCheckin_AlreadyCheckedIn_ThrowsException() {
        gamificationService.performDailyCheckin(TEST_ACCOUNT_ID);

        assertThrows(IllegalStateException.class,
            () -> gamificationService.performDailyCheckin(TEST_ACCOUNT_ID));
    }

    @Test
    @TestTransaction
    void testPerformDailyCheckin_WithStreak() {
        LocalDate yesterday = LocalDate.now().minusDays(1);

        DailyCheckin previousCheckin = new DailyCheckin();
        previousCheckin.accountId = TEST_ACCOUNT_ID;
        previousCheckin.checkinDate = yesterday;
        previousCheckin.streakCount = 3;
        previousCheckin.pointsEarned = 15;
        previousCheckin.persist();

        DailyCheckinResponse response = gamificationService.performDailyCheckin(TEST_ACCOUNT_ID);

        assertEquals(4, response.streakCount());
        assertTrue(response.pointsEarned() > 15);
    }

    @Test
    @TestTransaction
    void testGetTodayCheckin_CheckedIn() {
        gamificationService.performDailyCheckin(TEST_ACCOUNT_ID);

        DailyCheckinResponse response = gamificationService.getTodayCheckin(TEST_ACCOUNT_ID);

        assertNotNull(response);
        assertEquals(TEST_ACCOUNT_ID, response.accountId());
        assertEquals(LocalDate.now(), response.checkinDate());
    }

    @Test
    void testGetTodayCheckin_NotCheckedIn() {
        DailyCheckinResponse response = gamificationService.getTodayCheckin(TEST_ACCOUNT_ID);

        assertNull(response);
    }

    @Test
    @TestTransaction
    void testGetCurrentStreak_NoCheckins() {
        Integer streak = gamificationService.getCurrentStreak(TEST_ACCOUNT_ID);

        assertEquals(0, streak);
    }

    @Test
    @TestTransaction
    void testGetCurrentStreak_Today() {
        gamificationService.performDailyCheckin(TEST_ACCOUNT_ID);

        Integer streak = gamificationService.getCurrentStreak(TEST_ACCOUNT_ID);

        assertEquals(1, streak);
    }

    @Test
    @TestTransaction
    void testGetCurrentStreak_Broken() {
        LocalDate twoDaysAgo = LocalDate.now().minusDays(2);

        DailyCheckin oldCheckin = new DailyCheckin();
        oldCheckin.accountId = TEST_ACCOUNT_ID;
        oldCheckin.checkinDate = twoDaysAgo;
        oldCheckin.streakCount = 5;
        oldCheckin.pointsEarned = 40;
        oldCheckin.persist();

        Integer streak = gamificationService.getCurrentStreak(TEST_ACCOUNT_ID);

        assertEquals(0, streak);
    }

    @Test
    @TestTransaction
    void testGetTotalCheckins() {
        gamificationService.performDailyCheckin(TEST_ACCOUNT_ID);

        Long total = gamificationService.getTotalCheckins(TEST_ACCOUNT_ID);

        assertEquals(1L, total);
    }

    @Test
    @TestTransaction
    void testProcessTransaction_Success() {
        ProcessTransactionRequest request = new ProcessTransactionRequest(
            TEST_ACCOUNT_ID,
            "txn-001",
            BigDecimal.valueOf(2000000),
            "MERCHANT1",
            "FOOD"
        );

        GamificationEventResponse response = gamificationService.processTransaction(request);

        assertNotNull(response);
        assertTrue(response.xpEarned() > 0);

        UserLevelResponse level = gamificationService.getUserLevel(TEST_ACCOUNT_ID);
        assertNotNull(level);
        assertEquals(2, level.level());
    }

    @Test
    @TestTransaction
    void testProcessTransaction_Duplicate_IgnoresDuplicate() {
        ProcessTransactionRequest request = new ProcessTransactionRequest(
            TEST_ACCOUNT_ID,
            "txn-001",
            BigDecimal.valueOf(2000000),
            "MERCHANT1",
            "FOOD"
        );

        gamificationService.processTransaction(request);
        GamificationEventResponse response = gamificationService.processTransaction(request);

        assertEquals(0, response.xpEarned());
    }

    @Test
    @TestTransaction
    void testProcessTransaction_LevelUp() {
        ProcessTransactionRequest request1 = new ProcessTransactionRequest(
            TEST_ACCOUNT_ID,
            "txn-001",
            BigDecimal.valueOf(2000000),
            "MERCHANT1",
            "FOOD"
        );
        ProcessTransactionRequest request2 = new ProcessTransactionRequest(
            TEST_ACCOUNT_ID,
            "txn-002",
            BigDecimal.valueOf(5000000),
            "MERCHANT1",
            "FOOD"
        );

        gamificationService.processTransaction(request1);
        GamificationEventResponse response = gamificationService.processTransaction(request2);

        assertNotNull(response.levelUp());
        assertEquals(4, response.levelUp().level());
    }

    @Test
    @TestTransaction
    void testGetUserLevel_NotExists_ReturnsNull() {
        UserLevelResponse level = gamificationService.getUserLevel(TEST_ACCOUNT_ID);

        assertNull(level);
    }

    @Test
    @TestTransaction
    void testGetUserLevel_Exists() {
        ProcessTransactionRequest request = new ProcessTransactionRequest(
            TEST_ACCOUNT_ID,
            "txn-001",
            BigDecimal.valueOf(2000000),
            "MERCHANT1",
            "FOOD"
        );

        gamificationService.processTransaction(request);
        UserLevelResponse level = gamificationService.getUserLevel(TEST_ACCOUNT_ID);

        assertNotNull(level);
        assertEquals(TEST_ACCOUNT_ID, level.accountId());
        assertEquals(2, level.level());
        assertEquals("Pengunjung", level.levelName());
        assertTrue(level.xp() > 0);
    }

    @Test
    @TestTransaction
    void testGetUserBadges_NoneEarned() {
        List<EarnedBadgeResponse> badges = gamificationService.getUserBadges(TEST_ACCOUNT_ID);

        assertNotNull(badges);
        assertTrue(badges.isEmpty());
    }

    @Test
    @TestTransaction
    void testGetUserBadges_WithBadges() {
        Badge badge = new Badge();
        badge.name = "First Transaction";
        badge.description = "Complete your first transaction";
        badge.iconUrl = "https://example.com/badge1.png";
        badge.requirementType = Badge.RequirementType.TRANSACTION_COUNT;
        badge.requirementValue = BigDecimal.ONE;
        badge.pointsReward = 50;
        badge.category = "Transactions";
        badge.isActive = true;
        badge.persist();

        ProcessTransactionRequest request = new ProcessTransactionRequest(
            TEST_ACCOUNT_ID,
            "txn-001",
            BigDecimal.valueOf(2000000),
            "MERCHANT1",
            "FOOD"
        );

        gamificationService.processTransaction(request);
        List<EarnedBadgeResponse> badges = gamificationService.getUserBadges(TEST_ACCOUNT_ID);

        assertFalse(badges.isEmpty());
        assertEquals("First Transaction", badges.get(0).badgeName());
    }

    @Test
    @TestTransaction
    void testGetBadgeProgress_NoBadges() {
        List<BadgeProgressResponse> progress = gamificationService.getBadgeProgress(TEST_ACCOUNT_ID);

        assertNotNull(progress);
        assertTrue(progress.isEmpty());
    }

    @Test
    @TestTransaction
    void testGetBadgeProgress_WithBadges() {
        Badge badge = new Badge();
        badge.name = "Level 5 Master";
        badge.description = "Reach level 5";
        badge.iconUrl = "https://example.com/badge2.png";
        badge.requirementType = Badge.RequirementType.LEVEL_REACHED;
        badge.requirementValue = BigDecimal.valueOf(5);
        badge.pointsReward = 500;
        badge.category = "Levels";
        badge.isActive = true;
        badge.persist();

        List<BadgeProgressResponse> progress = gamificationService.getBadgeProgress(TEST_ACCOUNT_ID);

        assertNotNull(progress);
        assertEquals(1, progress.size());
        assertEquals("Level 5 Master", progress.get(0).name());
    }

    @Test
    @TestTransaction
    void testGetSummary() {
        gamificationService.performDailyCheckin(TEST_ACCOUNT_ID);
        ProcessTransactionRequest request = new ProcessTransactionRequest(
            TEST_ACCOUNT_ID,
            "txn-001",
            BigDecimal.valueOf(50000),
            "MERCHANT1",
            "FOOD"
        );
        gamificationService.processTransaction(request);

        GamificationSummaryResponse summary = gamificationService.getSummary(TEST_ACCOUNT_ID);

        assertNotNull(summary.level());
        assertNotNull(summary.lastCheckin());
        assertEquals(1, summary.currentStreak());
        assertEquals(1L, summary.totalCheckins());
    }

    @Test
    @TestTransaction
    void testCheckinAwardsLoyaltyPoints() {
        gamificationService.performDailyCheckin(TEST_ACCOUNT_ID);

        verify(loyaltyPointsService, atLeastOnce()).addPoints(any(CreateLoyaltyPointsRequest.class));
    }
}
