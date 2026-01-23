package id.payu.promotion.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "xp_transactions", indexes = {
    @Index(name = "idx_xp_transaction_account", columnList = "accountId")
})
public class XpTransaction extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(name = "account_id", nullable = false)
    public String accountId;

    @Column(name = "transaction_id")
    public String transactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 50)
    public SourceType sourceType;

    @Column(name = "xp_earned", nullable = false)
    public Integer xpEarned;

    @Column(name = "xp_after", nullable = false)
    public Integer xpAfter;

    @Column(name = "created_at", updatable = false)
    public LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum SourceType {
        TRANSACTION,
        BADGE,
        CHECKIN,
        REFERRAL,
        ADJUSTMENT
    }
}
