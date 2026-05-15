package id.payu.backoffice.domain.port.in;

import id.payu.backoffice.domain.CustomerCase;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Inbound port for Customer Case use cases.
 */
public interface CustomerCaseUseCase {

    CustomerCase create(String userId, String accountNumber, CustomerCase.CaseType caseType,
                        CustomerCase.Priority priority, String subject, String description, String notes);

    Optional<CustomerCase> getById(UUID id);

    Optional<CustomerCase> getByCaseNumber(String caseNumber);

    List<CustomerCase> getByUserId(String userId);

    List<CustomerCase> listByStatus(CustomerCase.CaseStatus status, int page, int size);

    List<CustomerCase> listByPriority(CustomerCase.Priority priority, int page, int size);

    List<CustomerCase> listAll(int page, int size);

    CustomerCase assign(UUID id, String assignedTo);

    CustomerCase update(UUID id, CustomerCase.CaseStatus status, String notes, String updatedBy);

    void delete(UUID id);
}
