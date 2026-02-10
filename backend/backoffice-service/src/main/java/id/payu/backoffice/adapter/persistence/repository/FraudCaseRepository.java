package id.payu.backoffice.adapter.persistence.repository;

import id.payu.backoffice.domain.FraudCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FraudCaseRepository extends JpaRepository<FraudCase, UUID> {
    List<FraudCase> findByUserId(String userId);
    List<FraudCase> findByStatus(FraudCase.CaseStatus status);

    // Search methods
    List<FraudCase> findByUserIdContainingIgnoreCase(String userId);
    List<FraudCase> findByAccountNumberContainingIgnoreCase(String accountNumber);
    List<FraudCase> findByFraudTypeContainingIgnoreCase(String fraudType);
}
