package id.payu.lending.repository;

import id.payu.lending.domain.model.Loan;
import id.payu.lending.domain.model.Loan.LoanStatus;
import id.payu.lending.entity.LoanEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LoanRepository extends JpaRepository<LoanEntity, UUID> {
    Optional<LoanEntity> findByExternalId(String externalId);
    List<LoanEntity> findByUserId(UUID userId);
    List<LoanEntity> findByUserIdAndStatus(UUID userId, LoanStatus status);
}
