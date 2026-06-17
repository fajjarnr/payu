package id.payu.backoffice.application.service;

import id.payu.backoffice.adapter.persistence.entity.CustomerCaseEntity;
import id.payu.backoffice.dto.CustomerCaseRequest;
import id.payu.backoffice.dto.CustomerCaseUpdateRequest;
import id.payu.backoffice.adapter.persistence.repository.CustomerCaseRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.ConstraintViolationException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import id.payu.backoffice.domain.CustomerCaseStatus;
import id.payu.backoffice.domain.Priority;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerCaseService {

    private final CustomerCaseRepository repository;

    @Transactional
    @CircuitBreaker(name = "backofficeService", fallbackMethod = "createFallback")
    @Retry(name = "backofficeService")
    public CustomerCaseEntity create(CustomerCaseRequest request) {
        log.info("Creating customer case for user: {}, type: {}", request.userId(), request.caseType());

        CustomerCaseEntity customerCase = CustomerCaseEntity.builder()
                .userId(request.userId())
                .accountNumber(request.accountNumber())
                .caseType(request.caseType())
                .priority(request.priority() != null ? request.priority() : Priority.MEDIUM)
                .subject(request.subject())
                .description(request.description())
                .notes(request.notes())
                .status(CustomerCaseStatus.OPEN)
                .caseNumber("CASE-" + System.currentTimeMillis())
                .build();

        CustomerCaseEntity saved = repository.save(customerCase);
        log.info("Customer case created: id={}, caseNumber={}", saved.getId(), saved.getCaseNumber());
        return saved;
    }

    public Optional<CustomerCaseEntity> getById(UUID id) {
        return repository.findById(id);
    }

    public Optional<CustomerCaseEntity> getByCaseNumber(String caseNumber) {
        // Repository doesn't have searching by case number yet, could add it or use example matcher
        // For simplicity returning empty or using manually added repo method
         return repository.findAll().stream().filter(cc -> cc.getCaseNumber().equals(caseNumber)).findFirst();
    }

    public List<CustomerCaseEntity> getByUserId(String userId) {
        return repository.findByUserId(userId);
    }

    public List<CustomerCaseEntity> listByStatus(CustomerCaseStatus status, int page, int size) {
        // BUG-BE-043: Use DB-level pagination instead of ignoring page/size
        return repository.findByStatus(status, PageRequest.of(page, size)).getContent();
    }

    public List<CustomerCaseEntity> listByPriority(Priority priority, int page, int size) {
        // Fallback or add to repo
        return repository.findAll().stream().filter(cc -> cc.getPriority() == priority).toList();
    }

    public List<CustomerCaseEntity> listAll(int page, int size) {
        return repository.findAll(PageRequest.of(page, size)).getContent();
    }

    @Transactional
    public CustomerCaseEntity assign(UUID id, String assignedTo) {
        log.info("Assigning customer case: id={}, to={}", id, assignedTo);

        CustomerCaseEntity customerCase = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer case not found: " + id));

        customerCase.setAssignedTo(assignedTo);
        if (customerCase.getStatus() == CustomerCaseStatus.OPEN) {
            customerCase.setStatus(CustomerCaseStatus.IN_PROGRESS);
        }

        return repository.save(customerCase);
    }

    @Transactional
    public CustomerCaseEntity update(UUID id, CustomerCaseUpdateRequest request, String updatedBy) {
        log.info("Updating customer case: id={}, status={}", id, request.status());

        CustomerCaseEntity customerCase = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer case not found: " + id));

        customerCase.setStatus(request.status());
        customerCase.setNotes(request.notes());

        if (request.status() == CustomerCaseStatus.RESOLVED || 
            request.status() == CustomerCaseStatus.CLOSED) {
            customerCase.setResolvedBy(updatedBy);
            customerCase.setResolvedAt(LocalDateTime.now());
        }

        CustomerCaseEntity saved = repository.save(customerCase);
        log.info("Customer case updated: id={}, newStatus={}", saved.getId(), saved.getStatus());
        return saved;
    }

    @Transactional
    public void delete(UUID id) {
        log.info("Deleting customer case: id={}", id);
        repository.deleteById(id);
    }

    // ─── Fallback methods ──────────────────────────────────────────────────────

    private CustomerCaseEntity createFallback(CustomerCaseRequest request, Throwable ex) {
        if (ex instanceof DataIntegrityViolationException
                || ex instanceof IllegalArgumentException
                || ex instanceof ConstraintViolationException
                || ex instanceof HttpMessageNotReadableException
                || ex instanceof AccessDeniedException) {
            throw (RuntimeException) ex;
        }
        log.error("Circuit breaker triggered for CustomerCaseService.create [userId={}]: {}",
                request.userId(), ex.getMessage());
        throw new IllegalStateException("Backoffice service temporarily unavailable. Please retry later.", ex);
    }
}
