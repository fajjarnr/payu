package id.payu.transaction.adapter.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * ARCH-TXN-001: append-only record of a VA payment. Inserted once per bank
 * callback; never updated or deleted (immutable ledger rule).
 */
@Entity
@Table(name = "va_payment_records")
public class VaPaymentRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "va_id", nullable = false)
    private UUID vaId;

    @Column(name = "va_number", nullable = false)
    private String vaNumber;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "payment_reference")
    private String paymentReference;

    @Column(name = "paid_at", nullable = false)
    private Instant paidAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected VaPaymentRecordEntity() {
    }

    public static VaPaymentRecordEntity of(UUID vaId, String vaNumber, BigDecimal amount,
                                           String paymentReference, Instant paidAt) {
        VaPaymentRecordEntity record = new VaPaymentRecordEntity();
        record.vaId = vaId;
        record.vaNumber = vaNumber;
        record.amount = amount;
        record.paymentReference = paymentReference;
        record.paidAt = paidAt;
        return record;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getVaId() {
        return vaId;
    }

    public String getVaNumber() {
        return vaNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
