package id.payu.promotion.service;

import id.payu.promotion.domain.Referral;
import id.payu.promotion.domain.Reward;
import id.payu.promotion.dto.CompleteReferralRequest;
import id.payu.promotion.dto.CreateReferralRequest;
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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@QuarkusTestResource(value = PostgresTestResource.class)
class ReferralServiceTest {

    @Inject
    ReferralService referralService;

    @Inject
    EntityManager entityManager;

    @InjectMock
    @SuppressWarnings("unused")
    Emitter<Map<String, Object>> promotionEvents;

    private static final String REFERRER_ACCOUNT_ID = "acc-referrer";
    private static final String REFEREE_ACCOUNT_ID = "acc-referee";

    @BeforeEach
    void setUp() {
        Referral.deleteAll();
        Reward.deleteAll();
    }

    @Test
    @TestTransaction
    void testCreateReferral_Success() {
        CreateReferralRequest request = new CreateReferralRequest(
            REFERRER_ACCOUNT_ID,
            new BigDecimal("50.00"),
            new BigDecimal("25.00"),
            Referral.RewardType.CASHBACK,
            LocalDateTime.now().plusMonths(3)
        );

        Referral result = referralService.createReferral(request);

        assertNotNull(result.id);
        assertEquals(REFERRER_ACCOUNT_ID, result.referrerAccountId);
        assertNotNull(result.referralCode);
        assertEquals(new BigDecimal("50.00"), result.referrerReward);
        assertEquals(new BigDecimal("25.00"), result.refereeReward);
        assertEquals(Referral.RewardType.CASHBACK, result.rewardType);
        assertEquals(Referral.Status.PENDING, result.status);
        assertNotNull(result.createdAt);
        assertEquals(8, result.referralCode.length());
    }

    @Test
    @TestTransaction
    void testCreateReferral_WithPointsRewardType() {
        CreateReferralRequest request = new CreateReferralRequest(
            REFERRER_ACCOUNT_ID,
            new BigDecimal("1000.00"),
            new BigDecimal("500.00"),
            Referral.RewardType.POINTS,
            LocalDateTime.now().plusMonths(3)
        );

        Referral result = referralService.createReferral(request);

        assertEquals(Referral.RewardType.POINTS, result.rewardType);
        assertNotNull(result.referralCode);
    }

    @Test
    @TestTransaction
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
            created.referralCode,
            REFEREE_ACCOUNT_ID
        );

        Referral result = referralService.completeReferral(completeRequest);

        assertEquals(created.id, result.id);
        assertEquals(REFEREE_ACCOUNT_ID, result.refereeAccountId);
        assertEquals(Referral.Status.COMPLETED, result.status);
        assertNotNull(result.completedAt);
    }

    @Test
    @TestTransaction
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
            created.referralCode,
            REFEREE_ACCOUNT_ID
        );

        Referral result = referralService.completeReferral(completeRequest);

        assertEquals(Referral.Status.COMPLETED, result.status);

        var referrerRewards = Reward.list("accountId = ?1", REFERRER_ACCOUNT_ID);
        var refereeRewards = Reward.list("accountId = ?1", REFEREE_ACCOUNT_ID);

        assertTrue(referrerRewards.size() > 0 || refereeRewards.size() > 0);
    }

    @Test
    @TestTransaction
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
    @TestTransaction
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
            created.referralCode,
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
    @TestTransaction
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
            created.referralCode,
            REFEREE_ACCOUNT_ID
        );

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> referralService.completeReferral(completeRequest)
        );

        assertEquals("Referral code has expired", exception.getMessage());

        var expiredReferral = referralService.getReferral(created.id);
        assertTrue(expiredReferral.isPresent());
        assertEquals(Referral.Status.EXPIRED, expiredReferral.get().status);
    }

    @Test
    @TestTransaction
    void testGetReferral_Success() {
        CreateReferralRequest request = new CreateReferralRequest(
            REFERRER_ACCOUNT_ID,
            new BigDecimal("50.00"),
            new BigDecimal("25.00"),
            Referral.RewardType.CASHBACK,
            LocalDateTime.now().plusMonths(3)
        );

        Referral created = referralService.createReferral(request);

        var result = referralService.getReferral(created.id);

        assertTrue(result.isPresent());
        assertEquals(created.id, result.get().id);
        assertEquals(REFERRER_ACCOUNT_ID, result.get().referrerAccountId);
    }

    @Test
    void testGetReferral_NotFound() {
        UUID nonExistentId = UUID.randomUUID();

        var result = referralService.getReferral(nonExistentId);

        assertFalse(result.isPresent());
    }

    @Test
    @TestTransaction
    void testGetReferralByCode_Success() {
        CreateReferralRequest request = new CreateReferralRequest(
            REFERRER_ACCOUNT_ID,
            new BigDecimal("50.00"),
            new BigDecimal("25.00"),
            Referral.RewardType.CASHBACK,
            LocalDateTime.now().plusMonths(3)
        );

        Referral created = referralService.createReferral(request);

        var result = referralService.getReferralByCode(created.referralCode);

        assertTrue(result.isPresent());
        assertEquals(created.id, result.get().id);
        assertEquals(created.referralCode, result.get().referralCode);
    }

    @Test
    void testGetReferralByCode_NotFound() {
        var result = referralService.getReferralByCode("NONEXISTENT");

        assertFalse(result.isPresent());
    }

    @Test
    @TestTransaction
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
        assertTrue(results.stream().allMatch(r -> REFERRER_ACCOUNT_ID.equals(r.referrerAccountId)));
    }

    @Test
    @TestTransaction
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
            created.referralCode,
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
    @TestTransaction
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

        assertNotEquals(result1.referralCode, result2.referralCode);
        assertEquals(8, result1.referralCode.length());
        assertEquals(8, result2.referralCode.length());
    }
}
