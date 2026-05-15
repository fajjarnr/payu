package id.payu.backoffice.application.service;

import id.payu.backoffice.domain.CustomerCase;
import id.payu.backoffice.dto.CustomerCaseRequest;
import id.payu.backoffice.dto.CustomerCaseUpdateRequest;
import id.payu.backoffice.adapter.persistence.repository.CustomerCaseRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerCaseService {

    private final CustomerCaseRepository repository;

    @Transactional
    @CircuitBreaker(name = "backofficeService", fallbackMethod = "createFallback")
    @Retry(name = "backofficeService")
    public CustomerCase create(CustomerCaseRequest request) {
        log.info("Creating customer case for user: {}, type: {}", request.userId(), request.caseType());

        CustomerCase customerCase = CustomerCase.builder()
                .userId(request.userId())
                .accountNumber(request.accountNumber())
                .caseType(request.caseType())
                .priority(request.priority() != null ? request.priority() : CustomerCase.Priority.MEDIUM)
                .subject(request.subject())
                .description(request.description())
                .notes(request.notes())
                .status(CustomerCase.CaseStatus.OPEN)
                .caseNumber("CASE-" + System.currentTimeMillis())
                .build();

        CustomerCase saved = repository.save(customerCase);
        log.info("Customer case created: id={}, caseNumber={}", saved.getId(), saved.getCaseNumber());
        return saved;
    }

    public Optional<CustomerCase> getById(UUID id) {
        return repository.findById(id);
    }

    public Optional<CustomerCase> getByCaseNumber(String caseNumber) {
        // Repository doesn't have searching by case number yet, could add it or use example matcher
        // For simplicity returning empty or using manually added repo method
         return repository.findAll().stream().filter(cc -> cc.getCaseNumber().equals(caseNumber)).findFirst();
    }

    public List<CustomerCase> getByUserId(String userId) {
        return repository.findByUserId(userId);
    }

    public List<CustomerCase> listByStatus(CustomerCase.CaseStatus status, int page, int size) {
        // BUG-BE-043: Use DB-level pagination instead of ignoring page/size
        return repository.findByStatus(status, PageRequest.of(page, size)).getContent();
    }

    public List<CustomerCase> listByPriority(CustomerCase.Priority priority, int page, int size) {
        // Fallback or add to repo
        return repository.findAll().stream().filter(cc -> cc.getPriority() == priority).toList();
    }

    public List<CustomerCase> listAll(int page, int size) {
        return repository.findAll(PageRequest.of(page, size)).getContent();
    }

    @Transactional
    public CustomerCase assign(UUID id, String assignedTo) {
        log.info("Assigning customer case: id={}, to={}", id, assignedTo);

        CustomerCase customerCase = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer case not found: " + id));

        customerCase.setAssignedTo(assignedTo);
        if (customerCase.getStatus() == CustomerCase.CaseStatus.OPEN) {
            customerCase.setStatus(CustomerCase.CaseStatus.IN_PROGRESS);
        }

        return repository.save(customerCase);
    }

    @Transactional
    public CustomerCase update(UUID id, CustomerCaseUpdateRequest request, String updatedBy) {
        log.info("Updating customer case: id={}, status={}", id, request.status());

        CustomerCase customerCase = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer case not found: " + id));

        customerCase.setStatus(request.status());
        customerCase.setNotes(request.notes());

        if (request.status() == CustomerCase.CaseStatus.RESOLVED || 
            request.status() == CustomerCase.CaseStatus.CLOSED) {
            customerCase.setResolvedBy(updatedBy);
            customerCase.setResolvedAt(LocalDateTime.now());
        }

        CustomerCase saved = repository.save(customerCase);
        log.info("Customer case updated: id={}, newStatus={}", saved.getId(), saved.getStatus());
        return saved;
    }

    @Transactional
    public void delete(UUID id) {
        log.info("Deleting customer case: id={}", id);
        repository.deleteById(id);
    }

    // ─── Fallback methods ──────────────────────────────────────────────────────

    private CustomerCase createFallback(CustomerCaseRequest request, Throwable ex) {
        log.error("Circuit breaker triggered for CustomerCaseService.create [userId={}]: {}",
                request.userId(), ex.getMessage());
        throw new IllegalStateException("Backoffice service temporarily unavailable. Please retry later.", ex);
    }
}
