package id.payu.billing.interfaces.dto;

import id.payu.billing.domain.model.Subscription;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record SubscriptionResponse(
        UUID id,
        String accountId,
        UUID planId,
        String partnerId,
        String status,
        BigDecimal currentPrice,
        String currency,
        String externalReferenceId,
        LocalDateTime trialEndAt,
        LocalDateTime currentPeriodStart,
        LocalDateTime currentPeriodEnd,
        LocalDateTime nextBillingAt,
        int dunningAttempts,
        LocalDateTime lastChargeAt,
        LocalDateTime cancelledAt,
        String cancellationReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static SubscriptionResponse from(Subscription sub) {
        return new SubscriptionResponse(
                sub.getId(),
                sub.getAccountId(),
                sub.getPlanId(),
                sub.getPartnerId(),
                sub.getStatus().name(),
                sub.getCurrentPrice(),
                sub.getCurrency(),
                sub.getExternalReferenceId(),
                sub.getTrialEndAt(),
                sub.getCurrentPeriodStart(),
                sub.getCurrentPeriodEnd(),
                sub.getNextBillingAt(),
                sub.getDunningAttempts(),
                sub.getLastChargeAt(),
                sub.getCancelledAt(),
                sub.getCancellationReason(),
                sub.getCreatedAt(),
                sub.getUpdatedAt()
        );
    }
}
