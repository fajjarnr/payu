package id.payu.wallet.adapter.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ledger_entries", indexes = {
    @Index(name = "idx_ledger_account_id", columnList = "account_id"),
    @Index(name = "idx_ledger_transaction_id", columnList = "transaction_id"),
    @Index(name = "idx_ledger_created_at", columnList = "account_id, created_at")
})
@NamedEntityGraph
public class LedgerEntryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Column(name = "account_id", nullable = false, updatable = false)
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 10)
    private String entryType;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "balance_after", nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceAfter;

    @Column(name = "reference_type", nullable = true, length = 50)
    private String referenceType;

    @Column(name = "reference_id", nullable = true, length = 100)
    private String referenceId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (referenceId == null && referenceType == null) {
            referenceId = "INTERNAL";
            referenceType = "RESERVATION";
        }
    }
}
