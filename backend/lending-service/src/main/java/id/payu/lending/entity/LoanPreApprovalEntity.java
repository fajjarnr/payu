package id.payu.lending.entity;

import id.payu.lending.domain.model.Loan;
import id.payu.lending.domain.model.LoanPreApproval;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "loan_pre_approvals", indexes = {
    @Index(name = "idx_loan_pre_approval_user_id", columnList = "user_id"),
    @Index(name = "idx_loan_pre_approval_valid_until", columnList = "valid_until")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanPreApprovalEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "loan_type", nullable = false)
    private Loan.LoanType loanType;

    @Column(name = "requested_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal requestedAmount;

    @Column(name = "max_approved_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal maxApprovedAmount;

    @Column(name = "min_interest_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal minInterestRate;

    @Column(name = "max_tenure_months")
    private Integer maxTenureMonths;

    @Column(name = "estimated_monthly_payment", precision = 19, scale = 4)
    private BigDecimal estimatedMonthlyPayment;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private LoanPreApproval.PreApprovalStatus status;

    @Column(name = "credit_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal creditScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_category", nullable = false)
    private id.payu.lending.domain.model.CreditScore.RiskCategory riskCategory;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "valid_until", nullable = false)
    private LocalDate validUntil;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
