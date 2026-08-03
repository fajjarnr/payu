package id.payu.promotion.adapter.persistence.entity;

import id.payu.promotion.domain.model.CashbackStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cashback_records", indexes = {
        @Index(name = "idx_cashback_records_transaction", columnList = "transaction_id"),
        @Index(name = "idx_cashback_records_account", columnList = "account_id"),
        @Index(name = "idx_cashback_records_status", columnList = "status")
})
public class CashbackRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "transaction_id", nullable = false, length = 255)
    private String transactionId;

    @Column(name = "account_id", nullable = false, length = 255)
    private String accountId;

    @Column(name = "rule_id", nullable = false, length = 50)
    private String ruleId;

    @Column(name = "cashback_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal cashbackAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CashbackStatus status;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    @Column(name = "wallet_reference_id", length = 255)
    private String walletReferenceId;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    public String getRuleId() { return ruleId; }
    public void setRuleId(String ruleId) { this.ruleId = ruleId; }
    public BigDecimal getCashbackAmount() { return cashbackAmount; }
    public void setCashbackAmount(BigDecimal cashbackAmount) { this.cashbackAmount = cashbackAmount; }
    public CashbackStatus getStatus() { return status; }
    public void setStatus(CashbackStatus status) { this.status = status; }
    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }
    public String getWalletReferenceId() { return walletReferenceId; }
    public void setWalletReferenceId(String walletReferenceId) { this.walletReferenceId = walletReferenceId; }
}
