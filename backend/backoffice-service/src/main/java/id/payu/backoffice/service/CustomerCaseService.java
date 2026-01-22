package id.payu.backoffice.service;

import id.payu.backoffice.domain.CustomerCase;
import id.payu.backoffice.dto.CustomerCaseRequest;
import id.payu.backoffice.dto.CustomerCaseUpdateRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class CustomerCaseService {

    private static final Logger LOG = Logger.getLogger(CustomerCaseService.class);

    @Transactional
    public CustomerCase create(CustomerCaseRequest request) {
        LOG.infof("Creating customer case for user: %s, type: %s", request.userId(), request.caseType());

        CustomerCase customerCase = new CustomerCase();
        customerCase.userId = request.userId();
        customerCase.accountNumber = request.accountNumber();
        customerCase.caseType = request.caseType();
        customerCase.priority = request.priority() != null ? request.priority() : CustomerCase.Priority.MEDIUM;
        customerCase.subject = request.subject();
        customerCase.description = request.description();
        customerCase.notes = request.notes();
        customerCase.status = CustomerCase.CaseStatus.OPEN;
        customerCase.caseNumber = "CASE-" + System.currentTimeMillis();

        customerCase.persist();
        LOG.infof("Customer case created: id=%s, caseNumber=%s", customerCase.id, customerCase.caseNumber);
        return customerCase;
    }

    public Optional<CustomerCase> getById(UUID id) {
        return CustomerCase.findByIdOptional(id);
    }

    public Optional<CustomerCase> getByCaseNumber(String caseNumber) {
        return CustomerCase.<CustomerCase>find("caseNumber = ?1", caseNumber).firstResultOptional();
    }

    public List<CustomerCase> getByUserId(String userId) {
        return CustomerCase.<CustomerCase>find("userId = ?1 ORDER BY createdAt DESC", userId).list();
    }

    public List<CustomerCase> listByStatus(CustomerCase.CaseStatus status, int page, int size) {
        return CustomerCase.<CustomerCase>find("status = ?1 ORDER BY createdAt DESC", status)
                .page(page, size)
                .list();
    }

    public List<CustomerCase> listByPriority(CustomerCase.Priority priority, int page, int size) {
        return CustomerCase.<CustomerCase>find("priority = ?1 ORDER BY createdAt DESC", priority)
                .page(page, size)
                .list();
    }

    public List<CustomerCase> listAll(int page, int size) {
        return CustomerCase.<CustomerCase>findAll()
                .page(page, size)
                .list();
    }

    @Transactional
    public CustomerCase assign(UUID id, String assignedTo) {
        LOG.infof("Assigning customer case: id=%s, to=%s", id, assignedTo);

        CustomerCase customerCase = CustomerCase.<CustomerCase>findByIdOptional(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer case not found: " + id));

        customerCase.assignedTo = assignedTo;
        if (customerCase.status == CustomerCase.CaseStatus.OPEN) {
            customerCase.status = CustomerCase.CaseStatus.IN_PROGRESS;
        }

        customerCase.persist();
        return customerCase;
    }

    @Transactional
    public CustomerCase update(UUID id, CustomerCaseUpdateRequest request, String updatedBy) {
        LOG.infof("Updating customer case: id=%s, status=%s", id, request.status());

        CustomerCase customerCase = CustomerCase.<CustomerCase>findByIdOptional(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer case not found: " + id));

        customerCase.status = request.status();
        customerCase.notes = request.notes();

        if (request.status() == CustomerCase.CaseStatus.RESOLVED || 
            request.status() == CustomerCase.CaseStatus.CLOSED) {
            customerCase.resolvedBy = updatedBy;
            customerCase.resolvedAt = LocalDateTime.now();
        }

        customerCase.persist();
        LOG.infof("Customer case updated: id=%s, newStatus=%s", customerCase.id, customerCase.status);
        return customerCase;
    }

    @Transactional
    public void delete(UUID id) {
        LOG.infof("Deleting customer case: id=%s", id);
        CustomerCase.deleteById(id);
    }
}
