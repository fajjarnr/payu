package id.payu.backoffice.application.service;

import id.payu.backoffice.dto.CustomerCaseRequest;
import id.payu.backoffice.dto.CustomerCaseUpdateRequest;
import id.payu.backoffice.domain.CustomerCase;
import id.payu.backoffice.domain.port.outbound.CustomerCaseRepositoryPort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.ConstraintViolationException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import id.payu.backoffice.domain.CustomerCaseStatus;
import id.payu.backoffice.domain.Priority;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerCaseService {

    private final CustomerCaseRepositoryPort repository;

    @Transactional
    @CircuitBreaker(name = "backofficeService", fallbackMethod = "createFallback")
    @Retry(name = "backofficeService")
    public CustomerCase create(CustomerCaseRequest request) {
        log.info("Creating customer case: type={}", request.caseType());
        CustomerCase saved = repository.save(CustomerCase.create(request.userId(), request.accountNumber(),
                request.caseType(), request.priority(), request.subject(), request.description(), request.notes()));
        log.info("Customer case created: id={}, caseNumber={}", saved.getId(), saved.getCaseNumber());
        return saved;
    }

    public Optional<CustomerCase> getById(UUID id) {
        return repository.findById(id);
    }

    public Optional<CustomerCase> getByCaseNumber(String caseNumber) {
        return repository.findByCaseNumber(caseNumber);
    }

    public List<CustomerCase> getByUserId(String userId) {
        return repository.findByUserId(userId);
    }

    public List<CustomerCase> listByStatus(CustomerCaseStatus status, int page, int size) {
        return repository.findByStatus(status, page, size);
    }

    public List<CustomerCase> listByPriority(Priority priority, int page, int size) {
        return repository.findByPriority(priority, page, size);
    }

    public List<CustomerCase> listAll(int page, int size) {
        return repository.findAll(page, size);
    }

    @Transactional
    public CustomerCase assign(UUID id, String assignedTo) {
        log.info("Assigning customer case: id={}", id);

        CustomerCase customerCase = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer case not found: " + id));

        customerCase.assignTo(assignedTo);

        return repository.save(customerCase);
    }

    @Transactional
    public CustomerCase update(UUID id, CustomerCaseUpdateRequest request, String updatedBy) {
        log.info("Updating customer case: id={}, status={}", id, request.status());

        CustomerCase customerCase = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer case not found: " + id));

        customerCase.update(request.status(), request.notes(), updatedBy);
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
        if (ex instanceof DataIntegrityViolationException
                || ex instanceof IllegalArgumentException
                || ex instanceof ConstraintViolationException
                || ex instanceof HttpMessageNotReadableException
                || ex instanceof AccessDeniedException) {
            throw (RuntimeException) ex;
        }
        log.error("Circuit breaker triggered for CustomerCaseService.create: {}", ex.getMessage());
        throw new IllegalStateException("Backoffice service temporarily unavailable. Please retry later.", ex);
    }
}
