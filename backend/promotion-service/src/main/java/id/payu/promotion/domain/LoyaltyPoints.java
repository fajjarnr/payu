package id.payu.promotion.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "loyalty_points", indexes = {
    @Index(name = "idx_loyalty_account", columnList = "accountId"),
    @Index(name = "idx_loyalty_expiry", columnList = "expiryDate")
})
public class LoyaltyPoints extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(name = "account_id", nullable = false)
    public String accountId;

    @Column(name = "transaction_id")
    public String transactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    public TransactionType transactionType;

    @Column(nullable = false)
    public Integer points;

    @Column(name = "balance_after", nullable = false)
    public Integer balanceAfter;

    @Column(name = "expiry_date")
    public LocalDateTime expiryDate;

    @Column(name = "redeemed_at")
    public LocalDateTime redeemedAt;

    @Column(name = "created_at", updatable = false)
    public LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum TransactionType {
        EARNED,
        REDEEMED,
        EXPIRED,
        ADJUSTED,
        REFERRAL_BONUS
    }
}
