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
public class SubscriptionCharge {
    private UUID id;
    private UUID subscriptionId;
    private String accountId;
    private BigDecimal amount;
    private String currency;
    private ChargeStatus status;
    private int attemptNumber;
    private String failureReason;
    private String idempotencyKey;
    private LocalDateTime billingPeriodStart;
    private LocalDateTime billingPeriodEnd;
    private LocalDateTime createdAt;
    private LocalDateTime chargedAt;
    private String tenantId;
    private Long version;

    public void markSucceeded() {
        status = ChargeStatus.SUCCEEDED;
        chargedAt = LocalDateTime.now();
    }

    public void markFailed(String reason) {
        status = ChargeStatus.FAILED;
        failureReason = reason;
    }
}
