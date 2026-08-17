package id.payu.promotion.application.service;

import id.payu.promotion.domain.ReferralRewardType;
import id.payu.promotion.domain.ReferralStatus;
import id.payu.promotion.domain.model.Referral;
import id.payu.promotion.domain.port.out.DomainEventPublisher;
import id.payu.promotion.domain.port.out.ReferralRepositoryPort;
import id.payu.promotion.domain.port.out.ReferralRewardPort;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * REFERRAL-001 (CB-030): completeReferral must load the referral with a
 * pessimistic lock so two concurrent completions cannot both grant rewards.
 */
class ReferralServiceCompleteReferralLockTest {

    private final ReferralRepositoryPort referralRepository = mock(ReferralRepositoryPort.class);
    private final ReferralRewardPort referralRewardPort = mock(ReferralRewardPort.class);
    private final DomainEventPublisher outboxService = mock(DomainEventPublisher.class);
    private final ReferralService service =
            new ReferralService(referralRepository, referralRewardPort, outboxService, "payu.promotion.referral-event.v1");

    @Test
    void completeReferralLoadsThroughLockedQuery() {
        Referral referral = new Referral();
        referral.setId(UUID.randomUUID());
        referral.setReferrerAccountId("REFERRER-1");
        referral.setReferralCode("ABC12345");
        referral.setReferrerReward(new BigDecimal("25000.0000"));
        referral.setRefereeReward(new BigDecimal("10000.0000"));
        referral.setRewardType(ReferralRewardType.CASHBACK);
        referral.setStatus(ReferralStatus.PENDING);

        when(referralRepository.findByReferralCodeForUpdate("ABC12345")).thenReturn(Optional.of(referral));
        when(referralRepository.save(referral)).thenReturn(referral);

        Referral completed = service.completeReferral(
                new id.payu.promotion.interfaces.dto.CompleteReferralRequest("ABC12345", "REFEREE-1"));

        assertNotNull(completed.getCompletedAt());
        verify(referralRepository).findByReferralCodeForUpdate("ABC12345");
        verify(referralRepository, never()).findByReferralCode("ABC12345");
    }
}
