package id.payu.lending.entity;

import id.payu.security.multitenancy.TenantAware;
import id.payu.security.multitenancy.TenantEntityListener;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import id.payu.lending.domain.model.LoanStatus;
import id.payu.lending.domain.model.LoanType;

@Entity
@Table(name = "loans")
@TenantAware
@EntityListeners(TenantEntityListener.class)
public class LoanEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "external_id", unique = true, nullable = false)
    private String externalId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private id.payu.lending.domain.model.LoanType type;

    @Column(name = "principal_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal principalAmount; // AUDIT-042

    @Column(name = "interest_rate", precision = 5, scale = 4)
    private BigDecimal interestRate;

    @Column(name = "tenure_months")
    private Integer tenureMonths;

    @Column(name = "monthly_installment", precision = 19, scale = 4)
    private BigDecimal monthlyInstallment; // AUDIT-042

    @Column(name = "outstanding_balance", precision = 19, scale = 4)
    private BigDecimal outstandingBalance; // AUDIT-042

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private id.payu.lending.domain.model.LoanStatus status;

    @Column(name = "purpose")
    private String purpose;

    @Column(name = "disbursement_date")
    private LocalDate disbursementDate;

    @Column(name = "maturity_date")
    private LocalDate maturityDate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public LoanEntity() {}

    public LoanEntity(UUID id, String externalId, UUID userId, id.payu.lending.domain.model.LoanType type, 
                      BigDecimal principalAmount, BigDecimal interestRate, Integer tenureMonths, 
                      BigDecimal monthlyInstallment, BigDecimal outstandingBalance, 
                      id.payu.lending.domain.model.LoanStatus status, String purpose, 
                      LocalDate disbursementDate, LocalDate maturityDate, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.externalId = externalId;
        this.userId = userId;
        this.type = type;
        this.principalAmount = principalAmount;
        this.interestRate = interestRate;
        this.tenureMonths = tenureMonths;
        this.monthlyInstallment = monthlyInstallment;
        this.outstandingBalance = outstandingBalance;
        this.status = status;
        this.purpose = purpose;
        this.disbursementDate = disbursementDate;
        this.maturityDate = maturityDate;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public id.payu.lending.domain.model.LoanType getType() { return type; }
    public void setType(id.payu.lending.domain.model.LoanType type) { this.type = type; }
    public BigDecimal getPrincipalAmount() { return principalAmount; }
    public void setPrincipalAmount(BigDecimal principalAmount) { this.principalAmount = principalAmount; }
    public BigDecimal getInterestRate() { return interestRate; }
    public void setInterestRate(BigDecimal interestRate) { this.interestRate = interestRate; }
    public Integer getTenureMonths() { return tenureMonths; }
    public void setTenureMonths(Integer tenureMonths) { this.tenureMonths = tenureMonths; }
    public BigDecimal getMonthlyInstallment() { return monthlyInstallment; }
    public void setMonthlyInstallment(BigDecimal monthlyInstallment) { this.monthlyInstallment = monthlyInstallment; }
    public BigDecimal getOutstandingBalance() { return outstandingBalance; }
    public void setOutstandingBalance(BigDecimal outstandingBalance) { this.outstandingBalance = outstandingBalance; }
    public id.payu.lending.domain.model.LoanStatus getStatus() { return status; }
    public void setStatus(id.payu.lending.domain.model.LoanStatus status) { this.status = status; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public LocalDate getDisbursementDate() { return disbursementDate; }
    public void setDisbursementDate(LocalDate disbursementDate) { this.disbursementDate = disbursementDate; }
    public LocalDate getMaturityDate() { return maturityDate; }
    public void setMaturityDate(LocalDate maturityDate) { this.maturityDate = maturityDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;
    @Version
    private Long version;


    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UUID id;
        private String externalId;
        private UUID userId;
        private id.payu.lending.domain.model.LoanType type;
        private BigDecimal principalAmount;
        private BigDecimal interestRate;
        private Integer tenureMonths;
        private BigDecimal monthlyInstallment;
        private BigDecimal outstandingBalance;
        private id.payu.lending.domain.model.LoanStatus status;
        private String purpose;
        private LocalDate disbursementDate;
        private LocalDate maturityDate;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder externalId(String externalId) { this.externalId = externalId; return this; }
        public Builder userId(UUID userId) { this.userId = userId; return this; }
        public Builder type(id.payu.lending.domain.model.LoanType type) { this.type = type; return this; }
        public Builder principalAmount(BigDecimal principalAmount) { this.principalAmount = principalAmount; return this; }
        public Builder interestRate(BigDecimal interestRate) { this.interestRate = interestRate; return this; }
        public Builder tenureMonths(Integer tenureMonths) { this.tenureMonths = tenureMonths; return this; }
        public Builder monthlyInstallment(BigDecimal monthlyInstallment) { this.monthlyInstallment = monthlyInstallment; return this; }
        public Builder outstandingBalance(BigDecimal outstandingBalance) { this.outstandingBalance = outstandingBalance; return this; }
        public Builder status(id.payu.lending.domain.model.LoanStatus status) { this.status = status; return this; }
        public Builder purpose(String purpose) { this.purpose = purpose; return this; }
        public Builder disbursementDate(LocalDate disbursementDate) { this.disbursementDate = disbursementDate; return this; }
        public Builder maturityDate(LocalDate maturityDate) { this.maturityDate = maturityDate; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public LoanEntity build() {
            return new LoanEntity(id, externalId, userId, type, principalAmount, interestRate, tenureMonths, 
                                 monthlyInstallment, outstandingBalance, status, purpose, disbursementDate, 
                                 maturityDate, createdAt, updatedAt);
        }
    }
}
