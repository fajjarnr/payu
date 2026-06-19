package id.payu.lending.entity;

import id.payu.security.multitenancy.TenantAware;
import id.payu.security.multitenancy.TenantEntityListener;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "repayment_schedules")
@TenantAware
@EntityListeners(TenantEntityListener.class)
public class RepaymentScheduleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "loan_id", nullable = false)
    private UUID loanId;

    @Column(name = "installment_number", nullable = false)
    private Integer installmentNumber;

    @Column(name = "installment_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal installmentAmount;

    @Column(name = "principal_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal principalAmount;

    @Column(name = "interest_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal interestAmount;

    @Column(name = "outstanding_principal", nullable = false, precision = 19, scale = 2)
    private BigDecimal outstandingPrincipal;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private id.payu.lending.domain.model.RepaymentStatus status;

    @Column(name = "paid_date")
    private LocalDate paidDate;

    @Column(name = "paid_amount", precision = 19, scale = 2)
    private BigDecimal paidAmount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public RepaymentScheduleEntity() {}

    public RepaymentScheduleEntity(UUID id, UUID loanId, Integer installmentNumber, BigDecimal installmentAmount,
                                  BigDecimal principalAmount, BigDecimal interestAmount, BigDecimal outstandingPrincipal,
                                  LocalDate dueDate, id.payu.lending.domain.model.RepaymentStatus status,
                                  LocalDate paidDate, BigDecimal paidAmount, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.loanId = loanId;
        this.installmentNumber = installmentNumber;
        this.installmentAmount = installmentAmount;
        this.principalAmount = principalAmount;
        this.interestAmount = interestAmount;
        this.outstandingPrincipal = outstandingPrincipal;
        this.dueDate = dueDate;
        this.status = status;
        this.paidDate = paidDate;
        this.paidAmount = paidAmount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getLoanId() { return loanId; }
    public void setLoanId(UUID loanId) { this.loanId = loanId; }
    public Integer getInstallmentNumber() { return installmentNumber; }
    public void setInstallmentNumber(Integer installmentNumber) { this.installmentNumber = installmentNumber; }
    public BigDecimal getInstallmentAmount() { return installmentAmount; }
    public void setInstallmentAmount(BigDecimal installmentAmount) { this.installmentAmount = installmentAmount; }
    public BigDecimal getPrincipalAmount() { return principalAmount; }
    public void setPrincipalAmount(BigDecimal principalAmount) { this.principalAmount = principalAmount; }
    public BigDecimal getInterestAmount() { return interestAmount; }
    public void setInterestAmount(BigDecimal interestAmount) { this.interestAmount = interestAmount; }
    public BigDecimal getOutstandingPrincipal() { return outstandingPrincipal; }
    public void setOutstandingPrincipal(BigDecimal outstandingPrincipal) { this.outstandingPrincipal = outstandingPrincipal; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public id.payu.lending.domain.model.RepaymentStatus getStatus() { return status; }
    public void setStatus(id.payu.lending.domain.model.RepaymentStatus status) { this.status = status; }
    public LocalDate getPaidDate() { return paidDate; }
    public void setPaidDate(LocalDate paidDate) { this.paidDate = paidDate; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }
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
        private UUID loanId;
        private Integer installmentNumber;
        private BigDecimal installmentAmount;
        private BigDecimal principalAmount;
        private BigDecimal interestAmount;
        private BigDecimal outstandingPrincipal;
        private LocalDate dueDate;
        private id.payu.lending.domain.model.RepaymentStatus status;
        private LocalDate paidDate;
        private BigDecimal paidAmount;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder loanId(UUID loanId) { this.loanId = loanId; return this; }
        public Builder installmentNumber(Integer installmentNumber) { this.installmentNumber = installmentNumber; return this; }
        public Builder installmentAmount(BigDecimal installmentAmount) { this.installmentAmount = installmentAmount; return this; }
        public Builder principalAmount(BigDecimal principalAmount) { this.principalAmount = principalAmount; return this; }
        public Builder interestAmount(BigDecimal interestAmount) { this.interestAmount = interestAmount; return this; }
        public Builder outstandingPrincipal(BigDecimal outstandingPrincipal) { this.outstandingPrincipal = outstandingPrincipal; return this; }
        public Builder dueDate(LocalDate dueDate) { this.dueDate = dueDate; return this; }
        public Builder status(id.payu.lending.domain.model.RepaymentStatus status) { this.status = status; return this; }
        public Builder paidDate(LocalDate paidDate) { this.paidDate = paidDate; return this; }
        public Builder paidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public RepaymentScheduleEntity build() {
            return new RepaymentScheduleEntity(id, loanId, installmentNumber, installmentAmount, principalAmount,
                    interestAmount, outstandingPrincipal, dueDate, status, paidDate, paidAmount, createdAt, updatedAt);
        }
    }
}
