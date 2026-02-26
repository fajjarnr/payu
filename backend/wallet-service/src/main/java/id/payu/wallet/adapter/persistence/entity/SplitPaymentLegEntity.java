package id.payu.wallet.adapter.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "split_payment_legs", indexes = {
    @Index(name = "idx_split_legs_execution", columnList = "execution_id")
})
public class SplitPaymentLegEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "execution_id", nullable = false)
    private SplitPaymentExecutionEntity execution;

    @Column(name = "recipient_account_id", nullable = false, length = 128)
    private String recipientAccountId;

    @Column(name = "recipient_label", length = 64)
    private String recipientLabel;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private LegStatus status;

    @Column(name = "journal_entry_id")
    private UUID journalEntryId;

    @Column(name = "settled_at")
    private LocalDateTime settledAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum LegStatus {
        PENDING, CREDITED, FAILED, REVERSED
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public SplitPaymentExecutionEntity getExecution() { return execution; }
    public void setExecution(SplitPaymentExecutionEntity execution) { this.execution = execution; }
    public String getRecipientAccountId() { return recipientAccountId; }
    public void setRecipientAccountId(String recipientAccountId) { this.recipientAccountId = recipientAccountId; }
    public String getRecipientLabel() { return recipientLabel; }
    public void setRecipientLabel(String recipientLabel) { this.recipientLabel = recipientLabel; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LegStatus getStatus() { return status; }
    public void setStatus(LegStatus status) { this.status = status; }
    public UUID getJournalEntryId() { return journalEntryId; }
    public void setJournalEntryId(UUID journalEntryId) { this.journalEntryId = journalEntryId; }
    public LocalDateTime getSettledAt() { return settledAt; }
    public void setSettledAt(LocalDateTime settledAt) { this.settledAt = settledAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
