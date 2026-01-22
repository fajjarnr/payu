package id.payu.lending.adapter.persistence;

import id.payu.lending.domain.model.RepaymentSchedule;
import id.payu.lending.domain.port.out.RepaymentSchedulePersistencePort;
import id.payu.lending.entity.RepaymentScheduleEntity;
import id.payu.lending.repository.RepaymentScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RepaymentSchedulePersistenceAdapter implements RepaymentSchedulePersistencePort {

    private final RepaymentScheduleRepository repository;

    @Override
    public RepaymentSchedule save(RepaymentSchedule repaymentSchedule) {
        RepaymentScheduleEntity entity = toEntity(repaymentSchedule);
        RepaymentScheduleEntity savedEntity = repository.save(entity);
        return toDomain(savedEntity);
    }

    @Override
    public Optional<RepaymentSchedule> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public List<RepaymentSchedule> findByLoanId(UUID loanId) {
        return repository.findByLoanId(loanId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteByLoanId(UUID loanId) {
        List<RepaymentScheduleEntity> entities = repository.findByLoanId(loanId);
        repository.deleteAll(entities);
    }

    private RepaymentScheduleEntity toEntity(RepaymentSchedule domain) {
        return RepaymentScheduleEntity.builder()
                .id(domain.getId())
                .loanId(domain.getLoanId())
                .installmentNumber(domain.getInstallmentNumber())
                .installmentAmount(domain.getInstallmentAmount())
                .principalAmount(domain.getPrincipalAmount())
                .interestAmount(domain.getInterestAmount())
                .outstandingPrincipal(domain.getOutstandingPrincipal())
                .dueDate(domain.getDueDate())
                .status(domain.getStatus())
                .paidDate(domain.getPaidDate())
                .paidAmount(domain.getPaidAmount())
                .createdAt(domain.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private RepaymentSchedule toDomain(RepaymentScheduleEntity entity) {
        RepaymentSchedule domain = new RepaymentSchedule();
        domain.setId(entity.getId());
        domain.setLoanId(entity.getLoanId());
        domain.setInstallmentNumber(entity.getInstallmentNumber());
        domain.setInstallmentAmount(entity.getInstallmentAmount());
        domain.setPrincipalAmount(entity.getPrincipalAmount());
        domain.setInterestAmount(entity.getInterestAmount());
        domain.setOutstandingPrincipal(entity.getOutstandingPrincipal());
        domain.setDueDate(entity.getDueDate());
        domain.setStatus(entity.getStatus());
        domain.setPaidDate(entity.getPaidDate());
        domain.setPaidAmount(entity.getPaidAmount());
        domain.setCreatedAt(entity.getCreatedAt());
        domain.setUpdatedAt(entity.getUpdatedAt());
        return domain;
    }
}
