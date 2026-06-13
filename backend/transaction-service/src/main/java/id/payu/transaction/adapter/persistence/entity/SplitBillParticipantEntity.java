package id.payu.transaction.adapter.persistence.entity;

import id.payu.transaction.domain.model.*;

import id.payu.security.multitenancy.TenantAware;
import id.payu.security.multitenancy.TenantEntityListener;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "split_bill_participants")
@TenantAware
@EntityListeners(TenantEntityListener.class)
public class SplitBillParticipantEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "split_bill_id")
    private UUID splitBillId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "account_number", nullable = false)
    private String accountNumber;

    @Column(name = "account_name", nullable = false)
    private String accountName;

    @Column(name = "amount_owed", nullable = false)
    private BigDecimal amountOwed;

    @Column(name = "amount_paid", nullable = false)
    private BigDecimal amountPaid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParticipantStatus status;

    @Column(name = "settled_at")
    private Instant settledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    // BUG-TXN-SPLITBILL-001: @Version on child for consistent isNew() check
    // (see SplitBillEntity for full rationale).
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    // Default constructor
    public SplitBillParticipantEntity() {
    }

    // All-args constructor
    public SplitBillParticipantEntity(UUID id, UUID splitBillId, UUID accountId, String accountNumber,
                                String accountName, BigDecimal amountOwed, BigDecimal amountPaid,
                                ParticipantStatus status, Instant settledAt, Instant createdAt, Instant updatedAt, Long version) {
        this.id = id;
        this.splitBillId = splitBillId;
        this.accountId = accountId;
        this.accountNumber = accountNumber;
        this.accountName = accountName;
        this.amountOwed = amountOwed;
        this.amountPaid = amountPaid;
        this.status = status;
        this.settledAt = settledAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    // Builder
    public static SplitBillParticipantBuilder builder() {
        return new SplitBillParticipantBuilder();
    }

    public static class SplitBillParticipantBuilder {
        private UUID id;
        private UUID splitBillId;
        private UUID accountId;
        private String accountNumber;
        private String accountName;
        private BigDecimal amountOwed;
        private BigDecimal amountPaid;
        private ParticipantStatus status;
        private Instant settledAt;
        private Instant createdAt;
        private Instant updatedAt;
        private Long version;

        public SplitBillParticipantBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public SplitBillParticipantBuilder splitBillId(UUID splitBillId) {
            this.splitBillId = splitBillId;
            return this;
        }

        public SplitBillParticipantBuilder accountId(UUID accountId) {
            this.accountId = accountId;
            return this;
        }

        public SplitBillParticipantBuilder accountNumber(String accountNumber) {
            this.accountNumber = accountNumber;
            return this;
        }

        public SplitBillParticipantBuilder accountName(String accountName) {
            this.accountName = accountName;
            return this;
        }

        public SplitBillParticipantBuilder amountOwed(BigDecimal amountOwed) {
            this.amountOwed = amountOwed;
            return this;
        }

        public SplitBillParticipantBuilder amountPaid(BigDecimal amountPaid) {
            this.amountPaid = amountPaid;
            return this;
        }

        public SplitBillParticipantBuilder status(ParticipantStatus status) {
            this.status = status;
            return this;
        }

        public SplitBillParticipantBuilder settledAt(Instant settledAt) {
            this.settledAt = settledAt;
            return this;
        }

        public SplitBillParticipantBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public SplitBillParticipantBuilder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public SplitBillParticipantEntity build() {
            return new SplitBillParticipantEntity(id, splitBillId, accountId, accountNumber, accountName,
                    amountOwed, amountPaid, status, settledAt, createdAt, updatedAt, version);
        }
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getSplitBillId() {
        return splitBillId;
    }

    public void setSplitBillId(UUID splitBillId) {
        this.splitBillId = splitBillId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public BigDecimal getAmountOwed() {
        return amountOwed;
    }

    public void setAmountOwed(BigDecimal amountOwed) {
        this.amountOwed = amountOwed;
    }

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(BigDecimal amountPaid) {
        this.amountPaid = amountPaid;
    }

    public ParticipantStatus getStatus() {
        return status;
    }

    public void setStatus(ParticipantStatus status) {
        this.status = status;
    }

    public Instant getSettledAt() {
        return settledAt;
    }

    public void setSettledAt(Instant settledAt) {
        this.settledAt = settledAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    // Business methods
    public BigDecimal getRemainingAmount() {
        return amountOwed.subtract(amountPaid);
    }

    public boolean isFullyPaid() {
        return amountPaid.compareTo(amountOwed) >= 0;
    }

    public boolean canMakePayment() {
        return status == ParticipantStatus.ACCEPTED || status == ParticipantStatus.PARTIALLY_PAID;
    }
}
