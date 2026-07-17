package id.payu.billing.domain.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class Subscription {
    private UUID id;
    private String accountId;
    private UUID planId;
    private String partnerId;
    private SubscriptionStatus status;
    private BigDecimal currentPrice;
    private String currency;
    private LocalDateTime trialEndAt;
    private LocalDateTime currentPeriodStart;
    private LocalDateTime currentPeriodEnd;
    private LocalDateTime nextBillingAt;
    private int dunningAttempts;
    private LocalDateTime lastChargeAt;
    private LocalDateTime cancelledAt;
    private String cancellationReason;
    private String externalReferenceId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String tenantId;
    private Long version;

    public void activate(LocalDateTime periodStart, LocalDateTime periodEnd, LocalDateTime nextBilling) {
        status = SubscriptionStatus.ACTIVE;
        currentPeriodStart = periodStart;
        currentPeriodEnd = periodEnd;
        nextBillingAt = nextBilling;
        dunningAttempts = 0;
        lastChargeAt = LocalDateTime.now();
    }

    public void markPastDue() {
        status = SubscriptionStatus.PAST_DUE;
        dunningAttempts++;
    }

    public void suspend() {
        status = SubscriptionStatus.SUSPENDED;
        nextBillingAt = null;
    }

    public void cancel(String reason) {
        status = SubscriptionStatus.CANCELLED;
        cancelledAt = LocalDateTime.now();
        cancellationReason = reason;
        nextBillingAt = null;
    }

    public boolean isDunningExhausted() {
        return dunningAttempts >= 3;
    }

    public boolean isChargeable() {
        return status == SubscriptionStatus.ACTIVE || status == SubscriptionStatus.PAST_DUE;
    }

    public boolean isInTrial() {
        return status == SubscriptionStatus.TRIAL
                && trialEndAt != null
                && LocalDateTime.now().isBefore(trialEndAt);
    }

    public boolean isTrialExpired() {
        return status == SubscriptionStatus.TRIAL
                && trialEndAt != null
                && !LocalDateTime.now().isBefore(trialEndAt);
    }
}
