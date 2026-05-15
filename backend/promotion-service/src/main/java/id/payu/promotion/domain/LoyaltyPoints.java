package id.payu.promotion.domain;

import id.payu.security.annotation.Sensitive;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "loyalty_points", indexes = {
    @Index(name = "idx_loyalty_account", columnList = "accountId"),
    @Index(name = "idx_loyalty_expiry", columnList = "expiryDate")
})
@EntityListeners(AuditingEntityListener.class)
public class LoyaltyPoints {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Sensitive(value = Sensitive.SensitivityLevel.HIGH)
    @Column(name = "account_id", nullable = false)
    private String accountId;

    @Column(name = "transaction_id")
    private String transactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TransactionType transactionType;

    @Column(nullable = false)
    private Integer points;

    @Column(name = "balance_after", nullable = false)
    private Integer balanceAfter;

    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;

    @Column(name = "redeemed_at")
    private LocalDateTime redeemedAt;

    /**
     * BUG-BE-186: Optimistic locking version field for defense-in-depth.
     * Complements the existing pessimistic locking (pg_advisory_xact_lock) in LoyaltyPointsService.
     */
    @Version
    @Column(name = "version")
    private Long version;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (transactionType == null) {
            transactionType = TransactionType.EARNED;
        }
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public TransactionType getTransactionType() { return transactionType; }
    public void setTransactionType(TransactionType transactionType) { this.transactionType = transactionType; }

    public Integer getPoints() { return points; }
    public void setPoints(Integer points) { this.points = points; }

    public Integer getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(Integer balanceAfter) { this.balanceAfter = balanceAfter; }

    public LocalDateTime getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; }

    public LocalDateTime getRedeemedAt() { return redeemedAt; }
    public void setRedeemedAt(LocalDateTime redeemedAt) { this.redeemedAt = redeemedAt; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public enum TransactionType {
        EARNED,
        REDEEMED,
        EXPIRED,
        ADJUSTED,
        REFERRAL_BONUS
    }
}
