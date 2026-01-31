package id.payu.backoffice.repository;

import id.payu.backoffice.domain.CustomerCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CustomerCaseRepository extends JpaRepository<CustomerCase, UUID> {
    List<CustomerCase> findByUserId(String userId);
    List<CustomerCase> findByStatus(CustomerCase.CaseStatus status);
    List<CustomerCase> findByAssignedTo(String assignedTo);

    // Search methods
    List<CustomerCase> findByUserIdContainingIgnoreCase(String userId);
    List<CustomerCase> findByAccountNumberContainingIgnoreCase(String accountNumber);
    List<CustomerCase> findByCaseNumberContainingIgnoreCase(String caseNumber);
    List<CustomerCase> findBySubjectContainingIgnoreCase(String subject);
}
