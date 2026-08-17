package id.payu.promotion.application.service;

import id.payu.promotion.domain.PromotionRewardType;
import id.payu.promotion.domain.PromotionStatus;
import id.payu.promotion.domain.PromotionType;
import id.payu.promotion.domain.model.Promotion;
import id.payu.promotion.domain.model.Reward;
import id.payu.promotion.domain.port.out.PromotionEventPublisher;
import id.payu.promotion.domain.port.out.PromotionPersistencePort;
import id.payu.promotion.domain.port.out.RewardPersistencePort;
import id.payu.promotion.interfaces.dto.ClaimPromotionRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PROMO-003 (CB-032): claimPromotion must be idempotent per transactionId.
 * PROMO-004 (CB-033): PERCENTAGE rewards must be scale 4 (ADR-0022).
 */
class PromotionServiceClaimDedupAndScaleTest {

    private final PromotionPersistencePort promotionRepository = mock(PromotionPersistencePort.class);
    private final RewardPersistencePort rewardRepository = mock(RewardPersistencePort.class);
    private final PromotionEventPublisher eventPublisher = mock(PromotionEventPublisher.class);
    private final PromotionService service =
            new PromotionService(promotionRepository, rewardRepository, eventPublisher, "payu.promotion.promotion-event.v1");

    private Promotion activePromotion(PromotionRewardType rewardType, BigDecimal rewardValue) {
        Promotion p = new Promotion();
        p.setId(UUID.randomUUID());
        p.setCode("PROMO-10");
        p.setStatus(PromotionStatus.ACTIVE);
        p.setStartDate(LocalDateTime.now().minusDays(1));
        p.setEndDate(LocalDateTime.now().plusDays(1));
        p.setRewardType(rewardType);
        p.setRewardValue(rewardValue);
        p.setPromotionType(PromotionType.CASHBACK);
        return p;
    }

    @Test
    void percentageRewardIsScaledToFourDecimals() {
        Promotion promotion = activePromotion(PromotionRewardType.PERCENTAGE, new BigDecimal("10.0000"));
        when(promotionRepository.findByCode("PROMO-10")).thenReturn(Optional.of(promotion));
        when(promotionRepository.incrementRedemptionIfAvailable(promotion.getId())).thenReturn(Optional.of(promotion));
        when(rewardRepository.findByTransactionId("TXN-1")).thenReturn(Optional.empty());
        when(rewardRepository.save(any(Reward.class))).thenAnswer(inv -> {
            Reward r = inv.getArgument(0);
            return new Reward(UUID.randomUUID(), r.accountId(), r.transactionId(), r.promotionCode(), r.type(),
                    r.amount(), r.pointsEarned(), r.transactionAmount(), r.merchantCode(), r.categoryCode(),
                    r.status(), r.expiryDate(), r.createdAt(), r.version());
        });

        Reward reward = service.claimPromotion("PROMO-10",
                new ClaimPromotionRequest("ACC-1", "TXN-1", new BigDecimal("100.0000"), null, null));

        assertEquals(0, new BigDecimal("10.0000").compareTo(reward.amount()));
        assertEquals(4, reward.amount().scale());
    }

    @Test
    void replayingSameTransactionIdIsRejectedAndRewardSavedOnce() {
        Promotion promotion = activePromotion(PromotionRewardType.FIXED_AMOUNT, new BigDecimal("5000.0000"));
        when(promotionRepository.findByCode("PROMO-10")).thenReturn(Optional.of(promotion));
        when(promotionRepository.incrementRedemptionIfAvailable(promotion.getId())).thenReturn(Optional.of(promotion));
        Reward existing = new Reward(UUID.randomUUID(), "ACC-1", "TXN-1", "PROMO-10", null,
                new BigDecimal("5000.0000"), null, new BigDecimal("100.0000"), null, null, null, null, null, null);
        when(rewardRepository.findByTransactionId("TXN-1"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existing));
        when(rewardRepository.save(any(Reward.class))).thenAnswer(inv -> {
            Reward r = inv.getArgument(0);
            return new Reward(UUID.randomUUID(), r.accountId(), r.transactionId(), r.promotionCode(), r.type(),
                    r.amount(), r.pointsEarned(), r.transactionAmount(), r.merchantCode(), r.categoryCode(),
                    r.status(), r.expiryDate(), r.createdAt(), r.version());
        });

        ClaimPromotionRequest request =
                new ClaimPromotionRequest("ACC-1", "TXN-1", new BigDecimal("100.0000"), null, null);
        service.claimPromotion("PROMO-10", request);
        org.mockito.Mockito.clearInvocations(promotionRepository, rewardRepository);

        assertThrows(IllegalArgumentException.class, () -> service.claimPromotion("PROMO-10", request));
        verify(rewardRepository, never()).save(any(Reward.class));
        verify(promotionRepository, never()).incrementRedemptionIfAvailable(any());
    }
}
