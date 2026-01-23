package id.payu.lending.adapter.persistence;

import id.payu.lending.domain.model.LoanPreApproval;
import id.payu.lending.domain.port.out.LoanPreApprovalPersistencePort;
import id.payu.lending.entity.LoanPreApprovalEntity;
import id.payu.lending.repository.LoanPreApprovalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class LoanPreApprovalPersistenceAdapter implements LoanPreApprovalPersistencePort {

    private final LoanPreApprovalRepository loanPreApprovalRepository;

    @Override
    public LoanPreApproval save(LoanPreApproval preApproval) {
        log.info("Saving loan pre-approval for user: {}", preApproval.getUserId());

        LoanPreApprovalEntity entity = mapToEntity(preApproval);
        LoanPreApprovalEntity savedEntity = loanPreApprovalRepository.save(entity);

        return mapToDomain(savedEntity);
    }

    @Override
    public Optional<LoanPreApproval> findById(UUID id) {
        log.info("Finding loan pre-approval by ID: {}", id);
        return loanPreApprovalRepository.findById(id)
                .map(this::mapToDomain);
    }

    @Override
    public Optional<LoanPreApproval> findActiveByUserId(UUID userId) {
        log.info("Finding active loan pre-approval for user: {}", userId);
        return loanPreApprovalRepository.findActiveByUserId(userId, LocalDate.now())
                .map(this::mapToDomain);
    }

    @Override
    public void deleteById(UUID id) {
        log.info("Deleting loan pre-approval by ID: {}", id);
        loanPreApprovalRepository.deleteById(id);
    }

    private LoanPreApprovalEntity mapToEntity(LoanPreApproval preApproval) {
        return LoanPreApprovalEntity.builder()
                .id(preApproval.getId())
                .userId(preApproval.getUserId())
                .loanType(preApproval.getLoanType())
                .requestedAmount(preApproval.getRequestedAmount())
                .maxApprovedAmount(preApproval.getMaxApprovedAmount())
                .minInterestRate(preApproval.getMinInterestRate())
                .maxTenureMonths(preApproval.getMaxTenureMonths())
                .estimatedMonthlyPayment(preApproval.getEstimatedMonthlyPayment())
                .status(preApproval.getStatus())
                .creditScore(preApproval.getCreditScore())
                .riskCategory(preApproval.getRiskCategory())
                .reason(preApproval.getReason())
                .validUntil(preApproval.getValidUntil())
                .createdAt(preApproval.getCreatedAt())
                .updatedAt(preApproval.getUpdatedAt())
                .build();
    }

    private LoanPreApproval mapToDomain(LoanPreApprovalEntity entity) {
        LoanPreApproval preApproval = new LoanPreApproval();
        preApproval.setId(entity.getId());
        preApproval.setUserId(entity.getUserId());
        preApproval.setLoanType(entity.getLoanType());
        preApproval.setRequestedAmount(entity.getRequestedAmount());
        preApproval.setMaxApprovedAmount(entity.getMaxApprovedAmount());
        preApproval.setMinInterestRate(entity.getMinInterestRate());
        preApproval.setMaxTenureMonths(entity.getMaxTenureMonths());
        preApproval.setEstimatedMonthlyPayment(entity.getEstimatedMonthlyPayment());
        preApproval.setStatus(entity.getStatus());
        preApproval.setCreditScore(entity.getCreditScore());
        preApproval.setRiskCategory(entity.getRiskCategory());
        preApproval.setReason(entity.getReason());
        preApproval.setValidUntil(entity.getValidUntil());
        preApproval.setCreatedAt(entity.getCreatedAt());
        preApproval.setUpdatedAt(entity.getUpdatedAt());
        return preApproval;
    }
}
