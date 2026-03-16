package id.payu.billing.domain.model;

import id.payu.security.multitenancy.TenantAware;
import id.payu.security.multitenancy.TenantEntityListener;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Records each charge attempt for a subscription billing cycle.
 * Tracks success, failure, and retry history for dunning.
 */
@Entity
@Table(name = "subscription_charges", indexes = {
    @Index(name = "idx_charge_subscription", columnList = "subscription_id"),
    @Index(name = "idx_charge_status", columnList = "status"),
    @Index(name = "idx_charge_idempotency", columnList = "idempotency_key")
})
@TenantAware
@EntityListeners(TenantEntityListener.class)
public class SubscriptionCharge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "subscription_id", nullable = false)
    private UUID subscriptionId;

    @Column(name = "account_id", nullable = false, length = 128)
    private String accountId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ChargeStatus status;

    @Column(name = "attempt_number")
    private int attemptNumber;

    @Column(name = "failure_reason", length = 512)
    private String failureReason;

    @Column(name = "idempotency_key", length = 128, unique = true)
    private String idempotencyKey;

    @Column(name = "billing_period_start")
    private LocalDateTime billingPeriodStart;

    @Column(name = "billing_period_end")
    private LocalDateTime billingPeriodEnd;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "charged_at")
    private LocalDateTime chargedAt;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    public SubscriptionCharge() {
    }

    public enum ChargeStatus {
        PENDING,
        SUCCEEDED,
        FAILED,
        REFUNDED
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (currency == null) currency = "IDR";
    }

    public void markSucceeded() {
        this.status = ChargeStatus.SUCCEEDED;
        this.chargedAt = LocalDateTime.now();
    }

    public void markFailed(String reason) {
        this.status = ChargeStatus.FAILED;
        this.failureReason = reason;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getSubscriptionId() { return subscriptionId; }
    public void setSubscriptionId(UUID subscriptionId) { this.subscriptionId = subscriptionId; }
    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public ChargeStatus getStatus() { return status; }
    public void setStatus(ChargeStatus status) { this.status = status; }
    public int getAttemptNumber() { return attemptNumber; }
    public void setAttemptNumber(int attemptNumber) { this.attemptNumber = attemptNumber; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public LocalDateTime getBillingPeriodStart() { return billingPeriodStart; }
    public void setBillingPeriodStart(LocalDateTime billingPeriodStart) { this.billingPeriodStart = billingPeriodStart; }
    public LocalDateTime getBillingPeriodEnd() { return billingPeriodEnd; }
    public void setBillingPeriodEnd(LocalDateTime billingPeriodEnd) { this.billingPeriodEnd = billingPeriodEnd; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getChargedAt() { return chargedAt; }
    public void setChargedAt(LocalDateTime chargedAt) { this.chargedAt = chargedAt; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
}
