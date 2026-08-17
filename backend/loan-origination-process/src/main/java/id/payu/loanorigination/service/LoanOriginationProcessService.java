package id.payu.loanorigination.service;

import id.payu.loanorigination.adapter.persistence.LoanOriginationProcessEntity;
import id.payu.loanorigination.adapter.persistence.LoanOriginationProcessRepository;
import id.payu.loanorigination.domain.LoanOriginationProcess;
import id.payu.loanorigination.domain.LoanOriginationRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class LoanOriginationProcessService {

    private static final BigDecimal MINIMUM_SCORE = new BigDecimal("600");

    private final CreditScoringService creditScoring;
    private final DisbursementService disbursement;
    private final LoanOriginationProcessRepository repository;

    public LoanOriginationProcessService(CreditScoringService creditScoring,
                                         DisbursementService disbursement,
                                         LoanOriginationProcessRepository repository) {
        this.creditScoring = creditScoring;
        this.disbursement = disbursement;
        this.repository = repository;
    }

    @Transactional
    public LoanOriginationProcess startProcess(LoanOriginationRequest request, String authenticatedUserId) {
        if (authenticatedUserId == null || authenticatedUserId.isBlank()) {
            throw new IllegalArgumentException("Authenticated user is required");
        }
        if (request.principalAmount() == null || request.principalAmount().signum() <= 0) {
            throw new IllegalArgumentException("Principal amount must be positive");
        }

        BigDecimal score = creditScoring.evaluate(request.principalAmount(), request.tenureMonths());
        var process = new LoanOriginationProcessEntity();
        process.setId(UUID.randomUUID());
        process.setUserId(authenticatedUserId);
        process.setPrincipalAmount(request.principalAmount());
        process.setTenureMonths(request.tenureMonths());
        process.setPurpose(request.purpose());
        process.setLoanType(request.loanType() == null ? "PERSONAL_LOAN" : request.loanType());
        process.setCreditScore(score);
        process.setApproved(score.compareTo(MINIMUM_SCORE) < 0 ? false : null);
        process.setStatus(score.compareTo(MINIMUM_SCORE) < 0 ? "REJECTED_LOW_SCORE" : "PENDING_APPROVAL");
        return toDomain(repository.save(process));
    }

    @Transactional(readOnly = true)
    public Optional<LoanOriginationProcess> getProcess(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Transactional(readOnly = true)
    public List<UUID> listProcessIds() {
        return repository.findAll().stream().map(LoanOriginationProcessEntity::getId).sorted().toList();
    }

    @Transactional
    public LoanOriginationProcess approve(UUID id, boolean approved, String comment, String approverId) {
        var process = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new java.util.NoSuchElementException("Loan process not found: " + id));

        if (!"PENDING_APPROVAL".equals(process.getStatus())) {
            if (Objects.equals(process.getApproved(), approved)) {
                return toDomain(process);
            }
            throw new IllegalStateException("Loan process is already completed");
        }

        process.setApproved(approved);
        process.setApprovedBy(approverId);
        process.setComment(comment);
        if (approved) {
            String reference = "LOAN-" + process.getId();
            disbursement.execute(
                    process.getUserId(),
                    process.getPrincipalAmount(),
                    process.getLoanType(),
                    process.getTenureMonths(),
                    reference);
            process.setDisbursementReference(reference);
            process.setStatus("APPROVED");
        } else {
            process.setStatus("REJECTED");
        }
        return toDomain(repository.save(process));
    }

    private LoanOriginationProcess toDomain(LoanOriginationProcessEntity entity) {
        if (entity == null) return null;
        return LoanOriginationProcess.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .principalAmount(entity.getPrincipalAmount())
                .tenureMonths(entity.getTenureMonths())
                .purpose(entity.getPurpose())
                .loanType(entity.getLoanType())
                .creditScore(entity.getCreditScore())
                .status(entity.getStatus())
                .approved(entity.getApproved())
                .comment(entity.getComment())
                .approvedBy(entity.getApprovedBy())
                .disbursementReference(entity.getDisbursementReference())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
