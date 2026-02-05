package id.payu.promotion.service;

import id.payu.promotion.domain.*;
import id.payu.promotion.dto.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.test.mock.mockito.MockBean;
import id.payu.promotion.repository.*;
import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class GamificationServiceTest {

    @Autowired
    GamificationService gamificationService;

    @MockBean
    LoyaltyPointsService loyaltyPointsService;

    @Autowired
    DailyCheckinRepository dailyCheckinRepository;

    @Autowired
    UserBadgeRepository userBadgeRepository;

    @Autowired
    UserLevelRepository userLevelRepository;

    @Autowired
    XpTransactionRepository xpTransactionRepository;

    @Autowired
    BadgeRepository badgeRepository;

    private static final String TEST_ACCOUNT_ID = "acc-test-123";

    @BeforeEach
    void setUp() {
        dailyCheckinRepository.deleteAll();
        userBadgeRepository.deleteAll();
        userLevelRepository.deleteAll();
        xpTransactionRepository.deleteAll();
        badgeRepository.deleteAll();
    }

    @Test
    void testPerformDailyCheckin_Success() {
        DailyCheckinResponse response = gamificationService.performDailyCheckin(TEST_ACCOUNT_ID);

        assertNotNull(response.id());
        assertEquals(TEST_ACCOUNT_ID, response.accountId());
        assertEquals(LocalDate.now(), response.checkinDate());
        assertEquals(1, response.streakCount());
        assertTrue(response.pointsEarned() > 0);
    }

    @Test
    void testPerformDailyCheckin_AlreadyCheckedIn_ThrowsException() {
        gamificationService.performDailyCheckin(TEST_ACCOUNT_ID);

        assertThrows(IllegalStateException.class,
            () -> gamificationService.performDailyCheckin(TEST_ACCOUNT_ID));
    }

    @Test
    void testPerformDailyCheckin_WithStreak() {
        LocalDate yesterday = LocalDate.now().minusDays(1);

        DailyCheckin previousCheckin = new DailyCheckin();
        previousCheckin.setAccountId(TEST_ACCOUNT_ID);
        previousCheckin.setCheckinDate(yesterday);
        previousCheckin.setStreakCount(3);
        previousCheckin.setPointsEarned(15);
        dailyCheckinRepository.save(previousCheckin);

        DailyCheckinResponse response = gamificationService.performDailyCheckin(TEST_ACCOUNT_ID);

        assertEquals(4, response.streakCount());
        assertTrue(response.pointsEarned() > 15);
    }

    @Test
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
    void testGetCurrentStreak_NoCheckins() {
        Integer streak = gamificationService.getCurrentStreak(TEST_ACCOUNT_ID);

        assertEquals(0, streak);
    }

    @Test
    void testGetCurrentStreak_Today() {
        gamificationService.performDailyCheckin(TEST_ACCOUNT_ID);

        Integer streak = gamificationService.getCurrentStreak(TEST_ACCOUNT_ID);

        assertEquals(1, streak);
    }

    @Test
    void testGetCurrentStreak_Broken() {
        LocalDate twoDaysAgo = LocalDate.now().minusDays(2);

        DailyCheckin oldCheckin = new DailyCheckin();
        oldCheckin.setAccountId(TEST_ACCOUNT_ID);
        oldCheckin.setCheckinDate(twoDaysAgo);
        oldCheckin.setStreakCount(5);
        oldCheckin.setPointsEarned(40);
        dailyCheckinRepository.save(oldCheckin);

        Integer streak = gamificationService.getCurrentStreak(TEST_ACCOUNT_ID);

        assertEquals(0, streak);
    }

    @Test
    void testGetTotalCheckins() {
        gamificationService.performDailyCheckin(TEST_ACCOUNT_ID);

        Long total = gamificationService.getTotalCheckins(TEST_ACCOUNT_ID);

        assertEquals(1L, total);
    }

    @Test
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
    void testGetUserLevel_NotExists_ReturnsNull() {
        UserLevelResponse level = gamificationService.getUserLevel(TEST_ACCOUNT_ID);

        assertNull(level);
    }

    @Test
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
    void testGetUserBadges_NoneEarned() {
        List<EarnedBadgeResponse> badges = gamificationService.getUserBadges(TEST_ACCOUNT_ID);

        assertNotNull(badges);
        assertTrue(badges.isEmpty());
    }

    @Test
    void testGetUserBadges_WithBadges() {
        id.payu.promotion.domain.Badge badge = new id.payu.promotion.domain.Badge();
        badge.setName("First Transaction");
        badge.setDescription("Complete your first transaction");
        badge.setIconUrl("https://example.com/badge1.png");
        badge.setRequirementType(id.payu.promotion.domain.Badge.RequirementType.TRANSACTION_COUNT);
        badge.setRequirementValue(BigDecimal.ONE);
        badge.setPointsReward(50);
        badge.setCategory("Transactions");
        badge.setIsActive(true);
        badgeRepository.save(badge);

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
    void testGetBadgeProgress_NoBadges() {
        List<BadgeProgressResponse> progress = gamificationService.getBadgeProgress(TEST_ACCOUNT_ID);

        assertNotNull(progress);
        assertTrue(progress.isEmpty());
    }

    @Test
    void testGetBadgeProgress_WithBadges() {
        id.payu.promotion.domain.Badge badge = new id.payu.promotion.domain.Badge();
        badge.setName("Level 5 Master");
        badge.setDescription("Reach level 5");
        badge.setIconUrl("https://example.com/badge2.png");
        badge.setRequirementType(id.payu.promotion.domain.Badge.RequirementType.LEVEL_REACHED);
        badge.setRequirementValue(BigDecimal.valueOf(5));
        badge.setPointsReward(500);
        badge.setCategory("Levels");
        badge.setIsActive(true);
        badgeRepository.save(badge);

        List<BadgeProgressResponse> progress = gamificationService.getBadgeProgress(TEST_ACCOUNT_ID);

        assertNotNull(progress);
        assertEquals(1, progress.size());
        assertEquals("Level 5 Master", progress.get(0).name());
    }

    @Test
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
    void testCheckinAwardsLoyaltyPoints() {
        gamificationService.performDailyCheckin(TEST_ACCOUNT_ID);

        verify(loyaltyPointsService, atLeastOnce()).addPoints(any(CreateLoyaltyPointsRequest.class));
    }
}
