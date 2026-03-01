package id.payu.promotion.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Value object representing a cashback notification.
 */
public class CashbackNotification {

    private String accountId;
    private String transactionId;
    private BigDecimal amount;
    private String message;
    private Instant timestamp;

    public CashbackNotification() {
        this.timestamp = Instant.now();
    }

    public CashbackNotification(String accountId, String transactionId, BigDecimal amount, String message) {
        this();
        this.accountId = accountId;
        this.transactionId = transactionId;
        this.amount = amount;
        this.message = message;
    }

    // Getters and setters
    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
