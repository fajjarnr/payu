package id.payu.promotion.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Domain entity representing a cashback record.
 */
public class CashbackRecord {

    private String id;
    private String transactionId;
    private String accountId;
    private String ruleId;
    private BigDecimal cashbackAmount;
    private CashbackStatus status;
    private Instant processedAt;
    private String walletReferenceId;

    public enum CashbackStatus {
        PENDING,
        CREDITED,
        FAILED
    }

    public CashbackRecord() {
        this.processedAt = Instant.now();
        this.status = CashbackStatus.PENDING;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public BigDecimal getCashbackAmount() {
        return cashbackAmount;
    }

    public void setCashbackAmount(BigDecimal cashbackAmount) {
        this.cashbackAmount = cashbackAmount;
    }

    public CashbackStatus getStatus() {
        return status;
    }

    public void setStatus(CashbackStatus status) {
        this.status = status;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }

    public String getWalletReferenceId() {
        return walletReferenceId;
    }

    public void setWalletReferenceId(String walletReferenceId) {
        this.walletReferenceId = walletReferenceId;
    }
}
