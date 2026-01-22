package id.payu.lending.adapter.persistence;

import id.payu.lending.domain.model.Loan;
import id.payu.lending.domain.port.out.LoanPersistencePort;
import id.payu.lending.entity.LoanEntity;
import id.payu.lending.repository.LoanRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@org.springframework.stereotype.Component
@RequiredArgsConstructor
public class LoanPersistenceAdapter implements LoanPersistencePort {

    private final LoanRepository loanRepository;

    @Override
    public Loan save(Loan loan) {
        LoanEntity entity = toEntity(loan);
        LoanEntity saved = loanRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Loan> findById(UUID id) {
        return loanRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Loan> findByExternalId(String externalId) {
        return loanRepository.findByExternalId(externalId).map(this::toDomain);
    }

    @Override
    public List<Loan> findByUserId(UUID userId) {
        return loanRepository.findByUserId(userId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void delete(Loan loan) {
        loanRepository.deleteById(loan.getId());
    }

    private Loan toDomain(LoanEntity entity) {
        Loan loan = new Loan();
        loan.setId(entity.getId());
        loan.setExternalId(entity.getExternalId());
        loan.setUserId(entity.getUserId());
        loan.setType(entity.getType());
        loan.setPrincipalAmount(entity.getPrincipalAmount());
        loan.setInterestRate(entity.getInterestRate());
        loan.setTenureMonths(entity.getTenureMonths());
        loan.setMonthlyInstallment(entity.getMonthlyInstallment());
        loan.setOutstandingBalance(entity.getOutstandingBalance());
        loan.setStatus(entity.getStatus());
        loan.setPurpose(entity.getPurpose());
        loan.setDisbursementDate(entity.getDisbursementDate());
        loan.setMaturityDate(entity.getMaturityDate());
        loan.setCreatedAt(entity.getCreatedAt());
        loan.setUpdatedAt(entity.getUpdatedAt());
        return loan;
    }

    private LoanEntity toEntity(Loan loan) {
        LoanEntity entity = new LoanEntity();
        entity.setId(loan.getId());
        entity.setExternalId(loan.getExternalId());
        entity.setUserId(loan.getUserId());
        entity.setType(loan.getType());
        entity.setPrincipalAmount(loan.getPrincipalAmount());
        entity.setInterestRate(loan.getInterestRate());
        entity.setTenureMonths(loan.getTenureMonths());
        entity.setMonthlyInstallment(loan.getMonthlyInstallment());
        entity.setOutstandingBalance(loan.getOutstandingBalance());
        entity.setStatus(loan.getStatus());
        entity.setPurpose(loan.getPurpose());
        entity.setDisbursementDate(loan.getDisbursementDate());
        entity.setMaturityDate(loan.getMaturityDate());
        entity.setCreatedAt(loan.getCreatedAt());
        entity.setUpdatedAt(loan.getUpdatedAt());
        return entity;
    }
}
