package id.payu.promotion.application.service;

import id.payu.promotion.domain.Referral;
import id.payu.promotion.domain.Reward;
import id.payu.promotion.dto.CompleteReferralRequest;
import id.payu.promotion.dto.CreateReferralRequest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.test.mock.mockito.MockBean;
import id.payu.promotion.adapter.persistence.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReferralServiceTest {

    @Autowired
    ReferralService referralService;

    @Autowired
    EntityManager entityManager;

    @Autowired
    ReferralRepository referralRepository;

    @Autowired
    RewardRepository rewardRepository;

    @Autowired
    LoyaltyPointsRepository loyaltyPointsRepository;

    @MockBean
    @SuppressWarnings("rawtypes")
    id.payu.promotion.application.service.EmitterPlaceholder promotionEvents;

    private static final String REFERRER_ACCOUNT_ID = "acc-referrer";
    private static final String REFEREE_ACCOUNT_ID = "acc-referee";

    @BeforeEach
    void setUp() {
        referralRepository.deleteAll();
        rewardRepository.deleteAll();
        loyaltyPointsRepository.deleteAll();
    }

    @Test
    void testCreateReferral_Success() {
        CreateReferralRequest request = new CreateReferralRequest(
            REFERRER_ACCOUNT_ID,
            new BigDecimal("50.00"),
            new BigDecimal("25.00"),
            Referral.RewardType.CASHBACK,
            LocalDateTime.now().plusMonths(3)
        );

        Referral result = referralService.createReferral(request);

        assertNotNull(result.getId());
        assertEquals(REFERRER_ACCOUNT_ID, result.getReferrerAccountId());
        assertNotNull(result.getReferralCode());
        assertEquals(new BigDecimal("50.00"), result.getReferrerReward());
        assertEquals(new BigDecimal("25.00"), result.getRefereeReward());
        assertEquals(Referral.RewardType.CASHBACK, result.getRewardType());
        assertEquals(Referral.Status.PENDING, result.getStatus());
        assertNotNull(result.getCreatedAt());
        assertEquals(8, result.getReferralCode().length());
    }

    @Test
    void testCreateReferral_WithPointsRewardType() {
        CreateReferralRequest request = new CreateReferralRequest(
            REFERRER_ACCOUNT_ID,
            new BigDecimal("1000.00"),
            new BigDecimal("500.00"),
            Referral.RewardType.POINTS,
            LocalDateTime.now().plusMonths(3)
        );

        Referral result = referralService.createReferral(request);

        assertEquals(Referral.RewardType.POINTS, result.getRewardType());
        assertNotNull(result.getReferralCode());
    }

    @Test
    void testCompleteReferral_Success() {
        CreateReferralRequest createRequest = new CreateReferralRequest(
            REFERRER_ACCOUNT_ID,
            new BigDecimal("50.00"),
            new BigDecimal("25.00"),
            Referral.RewardType.CASHBACK,
            LocalDateTime.now().plusMonths(3)
        );

        Referral created = referralService.createReferral(createRequest);

        CompleteReferralRequest completeRequest = new CompleteReferralRequest(
            created.getReferralCode(),
            REFEREE_ACCOUNT_ID
        );

        Referral result = referralService.completeReferral(completeRequest);

        assertEquals(created.getId(), result.getId());
        assertEquals(REFEREE_ACCOUNT_ID, result.getRefereeAccountId());
        assertEquals(Referral.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getCompletedAt());
    }

    @Test
    void testCompleteReferral_WithPointsReward_GrantsPoints() {
        CreateReferralRequest createRequest = new CreateReferralRequest(
            REFERRER_ACCOUNT_ID,
            new BigDecimal("1000.00"),
            new BigDecimal("500.00"),
            Referral.RewardType.POINTS,
            LocalDateTime.now().plusMonths(3)
        );

        Referral created = referralService.createReferral(createRequest);

        CompleteReferralRequest completeRequest = new CompleteReferralRequest(
            created.getReferralCode(),
            REFEREE_ACCOUNT_ID
        );

        Referral result = referralService.completeReferral(completeRequest);

        assertEquals(Referral.Status.COMPLETED, result.getStatus());

        var referrerPoints = loyaltyPointsRepository.findByAccountIdOrderByCreatedAtDesc(REFERRER_ACCOUNT_ID);
        var refereePoints = loyaltyPointsRepository.findByAccountIdOrderByCreatedAtDesc(REFEREE_ACCOUNT_ID);

        assertTrue(referrerPoints.size() > 0 && refereePoints.size() > 0,
                "Both referrer and referee must receive loyalty points on referral completion");
    }

    @Test
    void testCompleteReferral_InvalidCode_ThrowsException() {
        CompleteReferralRequest request = new CompleteReferralRequest(
            "INVALID_CODE",
            REFEREE_ACCOUNT_ID
        );

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> referralService.completeReferral(request)
        );

        assertEquals("Invalid referral code", exception.getMessage());
    }

    @Test
    void testCompleteReferral_AlreadyCompleted_ThrowsException() {
        CreateReferralRequest createRequest = new CreateReferralRequest(
            REFERRER_ACCOUNT_ID,
            new BigDecimal("50.00"),
            new BigDecimal("25.00"),
            Referral.RewardType.CASHBACK,
            LocalDateTime.now().plusMonths(3)
        );

        Referral created = referralService.createReferral(createRequest);

        CompleteReferralRequest completeRequest = new CompleteReferralRequest(
            created.getReferralCode(),
            REFEREE_ACCOUNT_ID
        );

        referralService.completeReferral(completeRequest);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> referralService.completeReferral(completeRequest)
        );

        assertEquals("Referral already completed or expired", exception.getMessage());
    }

    @Test
    void testCompleteReferral_ExpiredCode_ThrowsException() {
        CreateReferralRequest createRequest = new CreateReferralRequest(
            REFERRER_ACCOUNT_ID,
            new BigDecimal("50.00"),
            new BigDecimal("25.00"),
            Referral.RewardType.CASHBACK,
            LocalDateTime.now().minusDays(1)
        );

        Referral created = referralService.createReferral(createRequest);

        CompleteReferralRequest completeRequest = new CompleteReferralRequest(
            created.getReferralCode(),
            REFEREE_ACCOUNT_ID
        );

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> referralService.completeReferral(completeRequest)
        );

        assertEquals("Referral code has expired", exception.getMessage());

        var expiredReferral = referralService.getReferral(created.getId());
        assertTrue(expiredReferral.isPresent());
        assertEquals(Referral.Status.EXPIRED, expiredReferral.get().getStatus());
    }

    @Test
    void testGetReferral_Success() {
        CreateReferralRequest request = new CreateReferralRequest(
            REFERRER_ACCOUNT_ID,
            new BigDecimal("50.00"),
            new BigDecimal("25.00"),
            Referral.RewardType.CASHBACK,
            LocalDateTime.now().plusMonths(3)
        );

        Referral created = referralService.createReferral(request);

        var result = referralService.getReferral(created.getId());

        assertTrue(result.isPresent());
        assertEquals(created.getId(), result.get().getId());
        assertEquals(REFERRER_ACCOUNT_ID, result.get().getReferrerAccountId());
    }

    @Test
    void testGetReferral_NotFound() {
        UUID nonExistentId = UUID.randomUUID();

        var result = referralService.getReferral(nonExistentId);

        assertFalse(result.isPresent());
    }

    @Test
    void testGetReferralByCode_Success() {
        CreateReferralRequest request = new CreateReferralRequest(
            REFERRER_ACCOUNT_ID,
            new BigDecimal("50.00"),
            new BigDecimal("25.00"),
            Referral.RewardType.CASHBACK,
            LocalDateTime.now().plusMonths(3)
        );

        Referral created = referralService.createReferral(request);

        var result = referralService.getReferralByCode(created.getReferralCode());

        assertTrue(result.isPresent());
        assertEquals(created.getId(), result.get().getId());
        assertEquals(created.getReferralCode(), result.get().getReferralCode());
    }

    @Test
    void testGetReferralByCode_NotFound() {
        var result = referralService.getReferralByCode("NONEXISTENT");

        assertFalse(result.isPresent());
    }

    @Test
    void testGetReferralsByReferrer() {
        CreateReferralRequest request1 = new CreateReferralRequest(
            REFERRER_ACCOUNT_ID,
            new BigDecimal("50.00"),
            new BigDecimal("25.00"),
            Referral.RewardType.CASHBACK,
            LocalDateTime.now().plusMonths(3)
        );

        CreateReferralRequest request2 = new CreateReferralRequest(
            REFERRER_ACCOUNT_ID,
            new BigDecimal("50.00"),
            new BigDecimal("25.00"),
            Referral.RewardType.CASHBACK,
            LocalDateTime.now().plusMonths(3)
        );

        referralService.createReferral(request1);
        referralService.createReferral(request2);

        List<Referral> results = referralService.getReferralsByReferrer(REFERRER_ACCOUNT_ID);

        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(r -> REFERRER_ACCOUNT_ID.equals(r.getReferrerAccountId())));
    }

    @Test
    void testGetReferralSummary() {
        CreateReferralRequest request1 = new CreateReferralRequest(
            REFERRER_ACCOUNT_ID,
            new BigDecimal("50.00"),
            new BigDecimal("25.00"),
            Referral.RewardType.CASHBACK,
            LocalDateTime.now().plusMonths(3)
        );

        CreateReferralRequest request2 = new CreateReferralRequest(
            REFERRER_ACCOUNT_ID,
            new BigDecimal("50.00"),
            new BigDecimal("25.00"),
            Referral.RewardType.CASHBACK,
            LocalDateTime.now().plusMonths(3)
        );

        Referral created = referralService.createReferral(request1);
        referralService.createReferral(request2);

        CompleteReferralRequest completeRequest = new CompleteReferralRequest(
            created.getReferralCode(),
            REFEREE_ACCOUNT_ID
        );

        referralService.completeReferral(completeRequest);

        var summary = referralService.getReferralSummary(REFERRER_ACCOUNT_ID);

        assertNotNull(summary.referralCode());
        assertEquals(2, summary.totalReferrals());
        assertEquals(1, summary.completedReferrals());
        assertEquals(1, summary.pendingReferrals());
    }

    @Test
    void testGenerateReferralCode_Uniqueness() {
        CreateReferralRequest request1 = new CreateReferralRequest(
            REFERRER_ACCOUNT_ID,
            new BigDecimal("50.00"),
            new BigDecimal("25.00"),
            Referral.RewardType.CASHBACK,
            LocalDateTime.now().plusMonths(3)
        );

        CreateReferralRequest request2 = new CreateReferralRequest(
            REFERRER_ACCOUNT_ID,
            new BigDecimal("50.00"),
            new BigDecimal("25.00"),
            Referral.RewardType.CASHBACK,
            LocalDateTime.now().plusMonths(3)
        );

        Referral result1 = referralService.createReferral(request1);
        Referral result2 = referralService.createReferral(request2);

        assertNotEquals(result1.getReferralCode(), result2.getReferralCode());
        assertEquals(8, result1.getReferralCode().length());
        assertEquals(8, result2.getReferralCode().length());
    }
}
