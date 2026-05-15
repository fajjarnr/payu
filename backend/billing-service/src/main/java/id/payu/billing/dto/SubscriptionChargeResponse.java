package id.payu.billing.dto;

import id.payu.billing.adapter.persistence.entity.SubscriptionChargeEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record SubscriptionChargeResponse(
        UUID id,
        UUID subscriptionId,
        String accountId,
        BigDecimal amount,
        String currency,
        String status,
        int attemptNumber,
        String failureReason,
        String idempotencyKey,
        LocalDateTime billingPeriodStart,
        LocalDateTime billingPeriodEnd,
        LocalDateTime createdAt,
        LocalDateTime chargedAt
) {
    public static SubscriptionChargeResponse from(SubscriptionChargeEntity charge) {
        return new SubscriptionChargeResponse(
                charge.getId(),
                charge.getSubscriptionId(),
                charge.getAccountId(),
                charge.getAmount(),
                charge.getCurrency(),
                charge.getStatus().name(),
                charge.getAttemptNumber(),
                charge.getFailureReason(),
                charge.getIdempotencyKey(),
                charge.getBillingPeriodStart(),
                charge.getBillingPeriodEnd(),
                charge.getCreatedAt(),
                charge.getChargedAt()
        );
    }
}
