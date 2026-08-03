package id.payu.dispute.application.service;

import id.payu.dispute.domain.model.Dispute;
import id.payu.dispute.domain.model.DisputeResolutionType;
import id.payu.dispute.domain.model.DisputeStatus;
import id.payu.dispute.domain.port.in.DisputeUseCase;
import id.payu.dispute.domain.port.in.RefundUseCase;
import id.payu.dispute.domain.port.out.DisputePersistencePort;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Application service for managing disputes.
 *
 * <p>This service orchestrates dispute operations and implements the DisputeUseCase port.
 * It handles the business logic for creating and managing disputes.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DisputeService implements DisputeUseCase {

    private final DisputePersistencePort disputePersistencePort;
    private final RefundUseCase refundUseCase;

    @Override
    @CircuitBreaker(name = "disputeService", fallbackMethod = "openDisputeFallback")
    @Retry(name = "disputeService")
    public Dispute openDispute(UUID transactionId, UUID customerId, UUID merchantId,
                               BigDecimal disputedAmount, String currency, String reason) {
        log.info("Opening dispute for transaction: {} by customer: {}", transactionId, customerId);

        Dispute dispute = Dispute.create(transactionId, customerId, merchantId, disputedAmount, currency, reason);
        Dispute saved = disputePersistencePort.save(dispute);
        log.info("Opened dispute with ID: {} for transaction: {}", saved.getId(), transactionId);
        return saved;
    }

    @Override
    public Dispute startInvestigation(UUID disputeId, String investigationId) {
        log.info("Starting investigation for dispute: {} with ID: {}", disputeId, investigationId);

        Dispute dispute = disputePersistencePort.findById(disputeId)
                .orElseThrow(() -> new IllegalArgumentException("Dispute not found: " + disputeId));

        dispute.startInvestigation(investigationId);
        Dispute saved = disputePersistencePort.save(dispute);
        log.info("Started investigation for dispute: {}", disputeId);
        return saved;
    }

    @Override
    public Dispute resolveDispute(UUID disputeId, DisputeResolutionType resolutionType, String resolution) {
        log.info("Resolving dispute: {} with type: {}", disputeId, resolutionType);

        Dispute dispute = disputePersistencePort.findById(disputeId)
                .orElseThrow(() -> new IllegalArgumentException("Dispute not found: " + disputeId));

        dispute.resolve(resolutionType, resolution);
        Dispute saved = disputePersistencePort.save(dispute);
        log.info("Resolved dispute: {} with type: {}", disputeId, resolutionType);

        String refundReason = "Dispute " + disputeId + ": " + resolution;
        if (resolutionType == DisputeResolutionType.REFUND_CUSTOMER) {
            refundUseCase.createFullRefund(dispute.getTransactionId(), refundReason);
        } else if (resolutionType == DisputeResolutionType.PARTIAL_REFUND) {
            refundUseCase.createPartialRefund(
                    dispute.getTransactionId(), dispute.getDisputedAmount(), dispute.getCurrency(), refundReason);
        }

        return saved;
    }

    @Override
    public Dispute rejectDispute(UUID disputeId, String rejectionReason) {
        log.info("Rejecting dispute: {} with reason: {}", disputeId, rejectionReason);

        Dispute dispute = disputePersistencePort.findById(disputeId)
                .orElseThrow(() -> new IllegalArgumentException("Dispute not found: " + disputeId));

        dispute.reject(rejectionReason);
        Dispute saved = disputePersistencePort.save(dispute);
        log.info("Rejected dispute: {}", disputeId);
        return saved;
    }

    @Override
    public Dispute escalateDispute(UUID disputeId, String escalationReason) {
        log.info("Escalating dispute: {} with reason: {}", disputeId, escalationReason);

        Dispute dispute = disputePersistencePort.findById(disputeId)
                .orElseThrow(() -> new IllegalArgumentException("Dispute not found: " + disputeId));

        dispute.escalate(escalationReason);
        Dispute saved = disputePersistencePort.save(dispute);
        log.info("Escalated dispute: {}", disputeId);
        return saved;
    }

    @Override
    public Dispute addEvidence(UUID disputeId, String fileName, String fileUrl, String uploadedBy) {
        log.info("Adding evidence to dispute: {} - file: {}", disputeId, fileName);

        Dispute dispute = disputePersistencePort.findById(disputeId)
                .orElseThrow(() -> new IllegalArgumentException("Dispute not found: " + disputeId));

        dispute.addEvidence(fileName, fileUrl, uploadedBy);
        Dispute saved = disputePersistencePort.save(dispute);
        log.info("Added evidence to dispute: {}", disputeId);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Dispute> getDispute(UUID disputeId) {
        log.debug("Getting dispute: {}", disputeId);
        return disputePersistencePort.findById(disputeId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Dispute> getDisputeForCustomer(UUID disputeId, UUID customerId) {
        log.debug("Getting dispute {} for customer {}", disputeId, customerId);
        return disputePersistencePort.findByIdAndCustomerId(disputeId, customerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Dispute> getDisputesByTransaction(UUID transactionId) {
        log.debug("Getting disputes for transaction: {}", transactionId);
        return disputePersistencePort.findByTransactionId(transactionId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Dispute> getDisputesByTransactionForCustomer(UUID transactionId, UUID customerId) {
        log.debug("Getting disputes for transaction {} and customer {}", transactionId, customerId);
        return disputePersistencePort.findByTransactionIdAndCustomerId(transactionId, customerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Dispute> getDisputesByCustomer(UUID customerId) {
        log.debug("Getting disputes for customer: {}", customerId);
        return disputePersistencePort.findByCustomerId(customerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Dispute> getDisputesByMerchant(UUID merchantId) {
        log.debug("Getting disputes for merchant: {}", merchantId);
        return disputePersistencePort.findByMerchantId(merchantId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Dispute> getDisputesByStatus(String status) {
        log.debug("Getting disputes by status: {}", status);
        try {
            DisputeStatus disputeStatus = DisputeStatus.valueOf(status.toUpperCase());
            return disputePersistencePort.findByStatus(disputeStatus);
        } catch (IllegalArgumentException e) {
            log.error("Invalid dispute status: {}", status);
            throw new IllegalArgumentException("Invalid dispute status: " + status);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Dispute> getAllDisputes() {
        log.debug("Getting all disputes");
        return disputePersistencePort.findAll();
    }

    // ─── Fallback methods ──────────────────────────────────────────────────────

    private Dispute openDisputeFallback(UUID transactionId, UUID customerId, UUID merchantId,
                                        BigDecimal disputedAmount, String currency, String reason,
                                        Throwable ex) {
        if (ex instanceof DataIntegrityViolationException
                || ex instanceof IllegalArgumentException
                || ex instanceof ConstraintViolationException
                || ex instanceof HttpMessageNotReadableException
                || ex instanceof AccessDeniedException) {
            throw (RuntimeException) ex;
        }
        log.error("Circuit breaker triggered for openDispute [transactionId={}]: {}",
                transactionId, ex.getMessage());
        throw new IllegalStateException("Dispute service temporarily unavailable. Please retry later.", ex);
    }
}
