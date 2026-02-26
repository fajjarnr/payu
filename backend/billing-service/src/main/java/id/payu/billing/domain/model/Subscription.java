package id.payu.billing.domain.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A user's active subscription to a plan.
 * <p>
 * Lifecycle:
 * <pre>
 *   TRIAL     → ACTIVE    (trial ended, first charge succeeds)
 *   ACTIVE    → ACTIVE    (recurring charge succeeds, next_billing_at advanced)
 *   ACTIVE    → PAST_DUE  (charge fails, enters dunning)
 *   PAST_DUE  → ACTIVE    (retry charge succeeds)
 *   PAST_DUE  → SUSPENDED (dunning exhausted — 3 retries failed)
 *   SUSPENDED → ACTIVE    (manual reactivation with payment)
 *   ANY       → CANCELLED (explicit cancellation)
 * </pre>
 */
@Entity
@Table(name = "subscriptions", indexes = {
    @Index(name = "idx_sub_account", columnList = "account_id"),
    @Index(name = "idx_sub_plan", columnList = "plan_id"),
    @Index(name = "idx_sub_status", columnList = "status"),
    @Index(name = "idx_sub_next_billing", columnList = "next_billing_at"),
    @Index(name = "idx_sub_partner", columnList = "partner_id")
})
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "account_id", nullable = false, length = 128)
    private String accountId;

    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @Column(name = "partner_id", nullable = false, length = 128)
    private String partnerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SubscriptionStatus status;

    @Column(name = "current_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal currentPrice;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "trial_end_at")
    private LocalDateTime trialEndAt;

    @Column(name = "current_period_start")
    private LocalDateTime currentPeriodStart;

    @Column(name = "current_period_end")
    private LocalDateTime currentPeriodEnd;

    @Column(name = "next_billing_at")
    private LocalDateTime nextBillingAt;

    @Column(name = "dunning_attempts")
    private int dunningAttempts;

    @Column(name = "last_charge_at")
    private LocalDateTime lastChargeAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancellation_reason", length = 512)
    private String cancellationReason;

    @Column(name = "external_reference_id", length = 128)
    private String externalReferenceId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Subscription() {
    }

    public enum SubscriptionStatus {
        TRIAL,
        ACTIVE,
        PAST_DUE,
        SUSPENDED,
        CANCELLED
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (currency == null) currency = "IDR";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // --- Domain Methods ---

    public void activate(LocalDateTime periodStart, LocalDateTime periodEnd, LocalDateTime nextBilling) {
        this.status = SubscriptionStatus.ACTIVE;
        this.currentPeriodStart = periodStart;
        this.currentPeriodEnd = periodEnd;
        this.nextBillingAt = nextBilling;
        this.dunningAttempts = 0;
        this.lastChargeAt = LocalDateTime.now();
    }

    public void markPastDue() {
        this.status = SubscriptionStatus.PAST_DUE;
        this.dunningAttempts++;
    }

    public void suspend() {
        this.status = SubscriptionStatus.SUSPENDED;
        this.nextBillingAt = null;
    }

    public void cancel(String reason) {
        this.status = SubscriptionStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.cancellationReason = reason;
        this.nextBillingAt = null;
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

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    public UUID getPlanId() { return planId; }
    public void setPlanId(UUID planId) { this.planId = planId; }
    public String getPartnerId() { return partnerId; }
    public void setPartnerId(String partnerId) { this.partnerId = partnerId; }
    public SubscriptionStatus getStatus() { return status; }
    public void setStatus(SubscriptionStatus status) { this.status = status; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public LocalDateTime getTrialEndAt() { return trialEndAt; }
    public void setTrialEndAt(LocalDateTime trialEndAt) { this.trialEndAt = trialEndAt; }
    public LocalDateTime getCurrentPeriodStart() { return currentPeriodStart; }
    public void setCurrentPeriodStart(LocalDateTime currentPeriodStart) { this.currentPeriodStart = currentPeriodStart; }
    public LocalDateTime getCurrentPeriodEnd() { return currentPeriodEnd; }
    public void setCurrentPeriodEnd(LocalDateTime currentPeriodEnd) { this.currentPeriodEnd = currentPeriodEnd; }
    public LocalDateTime getNextBillingAt() { return nextBillingAt; }
    public void setNextBillingAt(LocalDateTime nextBillingAt) { this.nextBillingAt = nextBillingAt; }
    public int getDunningAttempts() { return dunningAttempts; }
    public void setDunningAttempts(int dunningAttempts) { this.dunningAttempts = dunningAttempts; }
    public LocalDateTime getLastChargeAt() { return lastChargeAt; }
    public void setLastChargeAt(LocalDateTime lastChargeAt) { this.lastChargeAt = lastChargeAt; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }
    public String getExternalReferenceId() { return externalReferenceId; }
    public void setExternalReferenceId(String externalReferenceId) { this.externalReferenceId = externalReferenceId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
