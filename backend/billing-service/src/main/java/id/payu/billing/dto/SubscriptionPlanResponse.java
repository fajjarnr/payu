package id.payu.billing.dto;

import id.payu.billing.adapter.persistence.entity.SubscriptionPlanEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record SubscriptionPlanResponse(
        UUID id,
        String partnerId,
        String planName,
        String description,
        String billingInterval,
        BigDecimal price,
        String currency,
        int trialDays,
        int gracePeriodDays,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static SubscriptionPlanResponse from(SubscriptionPlanEntity plan) {
        return new SubscriptionPlanResponse(
                plan.getId(),
                plan.getPartnerId(),
                plan.getPlanName(),
                plan.getDescription(),
                plan.getBillingInterval().name(),
                plan.getPrice(),
                plan.getCurrency(),
                plan.getTrialDays(),
                plan.getGracePeriodDays(),
                plan.isActive(),
                plan.getCreatedAt(),
                plan.getUpdatedAt()
        );
    }
}
