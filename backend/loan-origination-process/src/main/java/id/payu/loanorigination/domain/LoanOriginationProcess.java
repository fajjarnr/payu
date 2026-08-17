package id.payu.loanorigination.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain model representing a Loan Origination Process.
 */
public class LoanOriginationProcess {

    private UUID id;
    private String userId;
    private BigDecimal principalAmount;
    private Integer tenureMonths;
    private String purpose;
    private String loanType;
    private BigDecimal creditScore;
    private String status;
    private Boolean approved;
    private String comment;
    private String approvedBy;
    private String disbursementReference;
    private Instant createdAt;
    private Instant updatedAt;

    public LoanOriginationProcess() {
    }

    public LoanOriginationProcess(UUID id, String userId, BigDecimal principalAmount, Integer tenureMonths,
                                  String purpose, String loanType, BigDecimal creditScore, String status,
                                  Boolean approved, String comment, String approvedBy, String disbursementReference,
                                  Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.principalAmount = principalAmount;
        this.tenureMonths = tenureMonths;
        this.purpose = purpose;
        this.loanType = loanType;
        this.creditScore = creditScore;
        this.status = status;
        this.approved = approved;
        this.comment = comment;
        this.approvedBy = approvedBy;
        this.disbursementReference = disbursementReference;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public BigDecimal getPrincipalAmount() {
        return principalAmount;
    }

    public void setPrincipalAmount(BigDecimal principalAmount) {
        this.principalAmount = principalAmount;
    }

    public Integer getTenureMonths() {
        return tenureMonths;
    }

    public void setTenureMonths(Integer tenureMonths) {
        this.tenureMonths = tenureMonths;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getLoanType() {
        return loanType;
    }

    public void setLoanType(String loanType) {
        this.loanType = loanType;
    }

    public BigDecimal getCreditScore() {
        return creditScore;
    }

    public void setCreditScore(BigDecimal creditScore) {
        this.creditScore = creditScore;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getApproved() {
        return approved;
    }

    public void setApproved(Boolean approved) {
        this.approved = approved;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public String getDisbursementReference() {
        return disbursementReference;
    }

    public void setDisbursementReference(String disbursementReference) {
        this.disbursementReference = disbursementReference;
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

    public static class Builder {
        private UUID id;
        private String userId;
        private BigDecimal principalAmount;
        private Integer tenureMonths;
        private String purpose;
        private String loanType;
        private BigDecimal creditScore;
        private String status;
        private Boolean approved;
        private String comment;
        private String approvedBy;
        private String disbursementReference;
        private Instant createdAt;
        private Instant updatedAt;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder principalAmount(BigDecimal principalAmount) {
            this.principalAmount = principalAmount;
            return this;
        }

        public Builder tenureMonths(Integer tenureMonths) {
            this.tenureMonths = tenureMonths;
            return this;
        }

        public Builder purpose(String purpose) {
            this.purpose = purpose;
            return this;
        }

        public Builder loanType(String loanType) {
            this.loanType = loanType;
            return this;
        }

        public Builder creditScore(BigDecimal creditScore) {
            this.creditScore = creditScore;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder approved(Boolean approved) {
            this.approved = approved;
            return this;
        }

        public Builder comment(String comment) {
            this.comment = comment;
            return this;
        }

        public Builder approvedBy(String approvedBy) {
            this.approvedBy = approvedBy;
            return this;
        }

        public Builder disbursementReference(String disbursementReference) {
            this.disbursementReference = disbursementReference;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public LoanOriginationProcess build() {
            return new LoanOriginationProcess(id, userId, principalAmount, tenureMonths, purpose, loanType,
                    creditScore, status, approved, comment, approvedBy, disbursementReference, createdAt, updatedAt);
        }
    }
}
