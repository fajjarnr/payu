package id.payu.backoffice.domain.port.in;

import id.payu.backoffice.adapter.persistence.entity.CustomerCaseEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import id.payu.backoffice.domain.CaseType;
import id.payu.backoffice.domain.CustomerCaseStatus;
import id.payu.backoffice.domain.Priority;

/**
 * Inbound port for Customer Case use cases.
 */
public interface CustomerCaseUseCase {

    CustomerCaseEntity create(String userId, String accountNumber, CaseType caseType,
                        Priority priority, String subject, String description, String notes);

    Optional<CustomerCaseEntity> getById(UUID id);

    Optional<CustomerCaseEntity> getByCaseNumber(String caseNumber);

    List<CustomerCaseEntity> getByUserId(String userId);

    List<CustomerCaseEntity> listByStatus(CustomerCaseStatus status, int page, int size);

    List<CustomerCaseEntity> listByPriority(Priority priority, int page, int size);

    List<CustomerCaseEntity> listAll(int page, int size);

    CustomerCaseEntity assign(UUID id, String assignedTo);

    CustomerCaseEntity update(UUID id, CustomerCaseStatus status, String notes, String updatedBy);

    void delete(UUID id);
}
