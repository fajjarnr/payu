package id.payu.promotion.application.service;

import id.payu.promotion.domain.TransactionType;
import id.payu.promotion.domain.model.LoyaltyPoints;
import id.payu.promotion.domain.port.out.DomainEventPublisher;
import id.payu.promotion.domain.port.out.LoyaltyPointsRepositoryPort;
import id.payu.promotion.dto.RedeemLoyaltyPointsRequest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PROMO-002 (CB-027): loyalty redeem must be deduplicated by
 * (accountId, transactionId) — a replayed redemption must not burn points twice.
 */
class LoyaltyPointsRedeemDedupTest {

    private final LoyaltyPointsRepositoryPort repository = mock(LoyaltyPointsRepositoryPort.class);
    private final DomainEventPublisher outboxService = mock(DomainEventPublisher.class);
    private final LoyaltyPointsService service =
            new LoyaltyPointsService(repository, outboxService, "payu.promotion.loyalty-event.v1");

    private LoyaltyPoints redeemedRecord(String accountId, String transactionId) {
        LoyaltyPoints p = new LoyaltyPoints();
        p.setId(UUID.randomUUID());
        p.setAccountId(accountId);
        p.setTransactionId(transactionId);
        p.setTransactionType(TransactionType.REDEEMED);
        p.setPoints(-10);
        p.setBalanceAfter(90);
        return p;
    }

    @Test
    void replayingSameRedeemTransactionIsRejected() {
        when(repository.calculateBalanceByAccountId("ACC-1")).thenReturn(100);
        when(repository.findByAccountIdAndTransactionIdAndTransactionType("ACC-1", "TXN-R", TransactionType.REDEEMED))
                .thenReturn(List.of())
                .thenReturn(List.of(redeemedRecord("ACC-1", "TXN-R")));
        when(repository.save(any(LoyaltyPoints.class))).thenAnswer(inv -> {
            LoyaltyPoints p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        RedeemLoyaltyPointsRequest request = new RedeemLoyaltyPointsRequest("ACC-1", 10, "TXN-R");
        service.redeemPoints(request);
        org.mockito.Mockito.clearInvocations(repository);

        assertThrows(IllegalArgumentException.class, () -> service.redeemPoints(request));
        verify(repository, never()).save(any(LoyaltyPoints.class));
        verify(repository, never()).lockAccount(any());
    }
}
