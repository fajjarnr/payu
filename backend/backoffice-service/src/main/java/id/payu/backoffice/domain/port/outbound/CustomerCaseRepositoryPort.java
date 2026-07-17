package id.payu.backoffice.domain.port.outbound;

import id.payu.backoffice.domain.CustomerCase;
import id.payu.backoffice.domain.CustomerCaseStatus;
import id.payu.backoffice.domain.Priority;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerCaseRepositoryPort {
    CustomerCase save(CustomerCase customerCase);
    Optional<CustomerCase> findById(UUID id);
    Optional<CustomerCase> findByCaseNumber(String caseNumber);
    List<CustomerCase> findByUserId(String userId);
    List<CustomerCase> findByStatus(CustomerCaseStatus status, int page, int size);
    List<CustomerCase> findByPriority(Priority priority, int page, int size);
    List<CustomerCase> findAll(int page, int size);
    List<CustomerCase> findByUserIdContainingIgnoreCase(String query);
    List<CustomerCase> findByAccountNumberContainingIgnoreCase(String query);
    List<CustomerCase> findByCaseNumberContainingIgnoreCase(String query);
    List<CustomerCase> findBySubjectContainingIgnoreCase(String query);
    void deleteById(UUID id);
}
