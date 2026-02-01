package id.payu.lending.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "paylater_accounts")
public class PayLaterEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "external_id", unique = true)
    private String externalId;

    @Column(name = "user_id", unique = true, nullable = false)
    private UUID userId;

    @Column(name = "credit_limit", nullable = false, precision = 19, scale = 2)
    private BigDecimal creditLimit;

    @Column(name = "used_credit", nullable = false, precision = 19, scale = 2)
    private BigDecimal usedCredit;

    @Column(name = "available_credit", nullable = false, precision = 19, scale = 2)
    private BigDecimal availableCredit;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private id.payu.lending.domain.model.PayLater.PayLaterStatus status;

    @Column(name = "billing_cycle_day")
    private Integer billingCycleDay;

    @Column(name = "interest_rate", precision = 5, scale = 4)
    private BigDecimal interestRate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public PayLaterEntity() {}

    public PayLaterEntity(UUID id, String externalId, UUID userId, BigDecimal creditLimit, BigDecimal usedCredit, 
                          BigDecimal availableCredit, id.payu.lending.domain.model.PayLater.PayLaterStatus status, 
                          Integer billingCycleDay, BigDecimal interestRate, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.externalId = externalId;
        this.userId = userId;
        this.creditLimit = creditLimit;
        this.usedCredit = usedCredit;
        this.availableCredit = availableCredit;
        this.status = status;
        this.billingCycleDay = billingCycleDay;
        this.interestRate = interestRate;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public BigDecimal getCreditLimit() { return creditLimit; }
    public void setCreditLimit(BigDecimal creditLimit) { this.creditLimit = creditLimit; }
    public BigDecimal getUsedCredit() { return usedCredit; }
    public void setUsedCredit(BigDecimal usedCredit) { this.usedCredit = usedCredit; }
    public BigDecimal getAvailableCredit() { return availableCredit; }
    public void setAvailableCredit(BigDecimal availableCredit) { this.availableCredit = availableCredit; }
    public id.payu.lending.domain.model.PayLater.PayLaterStatus getStatus() { return status; }
    public void setStatus(id.payu.lending.domain.model.PayLater.PayLaterStatus status) { this.status = status; }
    public Integer getBillingCycleDay() { return billingCycleDay; }
    public void setBillingCycleDay(Integer billingCycleDay) { this.billingCycleDay = billingCycleDay; }
    public BigDecimal getInterestRate() { return interestRate; }
    public void setInterestRate(BigDecimal interestRate) { this.interestRate = interestRate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UUID id;
        private String externalId;
        private UUID userId;
        private BigDecimal creditLimit;
        private BigDecimal usedCredit;
        private BigDecimal availableCredit;
        private id.payu.lending.domain.model.PayLater.PayLaterStatus status;
        private Integer billingCycleDay;
        private BigDecimal interestRate;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder externalId(String externalId) { this.externalId = externalId; return this; }
        public Builder userId(UUID userId) { this.userId = userId; return this; }
        public Builder creditLimit(BigDecimal creditLimit) { this.creditLimit = creditLimit; return this; }
        public Builder usedCredit(BigDecimal usedCredit) { this.usedCredit = usedCredit; return this; }
        public Builder availableCredit(BigDecimal availableCredit) { this.availableCredit = availableCredit; return this; }
        public Builder status(id.payu.lending.domain.model.PayLater.PayLaterStatus status) { this.status = status; return this; }
        public Builder billingCycleDay(Integer billingCycleDay) { this.billingCycleDay = billingCycleDay; return this; }
        public Builder interestRate(BigDecimal interestRate) { this.interestRate = interestRate; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public PayLaterEntity build() {
            return new PayLaterEntity(id, externalId, userId, creditLimit, usedCredit, availableCredit, status, billingCycleDay, interestRate, createdAt, updatedAt);
        }
    }
}
