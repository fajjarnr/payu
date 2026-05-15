package id.payu.promotion.domain;

import id.payu.security.annotation.Sensitive;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cashbacks", indexes = {
    @Index(name = "idx_cashback_account", columnList = "accountId"),
    @Index(name = "idx_cashback_transaction", columnList = "transactionId"),
    @Index(name = "idx_cashback_status", columnList = "status"),
    @Index(name = "idx_cashback_date", columnList = "createdAt DESC")
})
@EntityListeners(AuditingEntityListener.class)
public class Cashback implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Sensitive(value = Sensitive.SensitivityLevel.HIGH)
    @Column(name = "account_id", nullable = false)
    private String accountId;

    @Column(name = "transaction_id", nullable = false)
    private String transactionId;

    @Column(name = "cashback_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal cashbackAmount;

    @Column(name = "transaction_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal transactionAmount;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal percentage;

    @Column(name = "merchant_code")
    private String merchantCode;

    @Column(name = "category_code")
    private String categoryCode;

    @Column(name = "cashback_code")
    private String cashbackCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status;

    @Column(name = "credited_at")
    private LocalDateTime creditedAt;

    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (percentage == null) {
            percentage = BigDecimal.ZERO;
        }
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public BigDecimal getCashbackAmount() { return cashbackAmount; }
    public void setCashbackAmount(BigDecimal cashbackAmount) { this.cashbackAmount = cashbackAmount; }

    public BigDecimal getTransactionAmount() { return transactionAmount; }
    public void setTransactionAmount(BigDecimal transactionAmount) { this.transactionAmount = transactionAmount; }

    public BigDecimal getPercentage() { return percentage; }
    public void setPercentage(BigDecimal percentage) { this.percentage = percentage; }

    public String getMerchantCode() { return merchantCode; }
    public void setMerchantCode(String merchantCode) { this.merchantCode = merchantCode; }

    public String getCategoryCode() { return categoryCode; }
    public void setCategoryCode(String categoryCode) { this.categoryCode = categoryCode; }

    public String getCashbackCode() { return cashbackCode; }
    public void setCashbackCode(String cashbackCode) { this.cashbackCode = cashbackCode; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public LocalDateTime getCreditedAt() { return creditedAt; }
    public void setCreditedAt(LocalDateTime creditedAt) { this.creditedAt = creditedAt; }

    public LocalDateTime getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public enum Status {
        PENDING,
        CREDITED,
        EXPIRED,
        VOIDED
    }
}
