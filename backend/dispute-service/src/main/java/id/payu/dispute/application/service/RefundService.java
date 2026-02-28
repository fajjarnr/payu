package id.payu.dispute.application.service;

import id.payu.dispute.domain.model.Refund;
import id.payu.dispute.domain.model.RefundStatus;
import id.payu.dispute.domain.port.in.RefundUseCase;
import id.payu.dispute.domain.port.out.RefundPersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Application service for managing refunds.
 *
 * <p>This service orchestrates refund operations and implements the RefundUseCase port.
 * It handles the business logic for creating and managing refunds.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RefundService implements RefundUseCase {

    private final RefundPersistencePort refundPersistencePort;

    @Override
    public Refund createFullRefund(UUID transactionId, String reason) {
        log.info("Creating full refund for transaction: {}", transactionId);
        // In a real implementation, we would fetch the transaction amount from transaction-service
        // For now, we use a placeholder that should be replaced with actual transaction lookup
        BigDecimal transactionAmount = fetchTransactionAmount(transactionId);
        String currency = "IDR"; // Should be fetched from transaction

        Refund refund = Refund.createFullRefund(transactionId, transactionAmount, currency, reason);
        Refund saved = refundPersistencePort.save(refund);
        log.info("Created refund with ID: {} for transaction: {}", saved.getId(), transactionId);
        return saved;
    }

    @Override
    public Refund createPartialRefund(UUID transactionId, BigDecimal amount, String currency, String reason) {
        log.info("Creating partial refund for transaction: {} with amount: {} {}", transactionId, amount, currency);

        Refund refund = Refund.createPartialRefund(transactionId, amount, currency, reason);
        Refund saved = refundPersistencePort.save(refund);
        log.info("Created partial refund with ID: {} for transaction: {}", saved.getId(), transactionId);
        return saved;
    }

    @Override
    public Refund processRefund(UUID refundId) {
        log.info("Processing refund: {}", refundId);

        Refund refund = refundPersistencePort.findById(refundId)
                .orElseThrow(() -> new IllegalArgumentException("Refund not found: " + refundId));

        refund.process();
        Refund saved = refundPersistencePort.save(refund);
        log.info("Processed refund: {}", refundId);
        return saved;
    }

    @Override
    public Refund completeRefund(UUID refundId) {
        log.info("Completing refund: {}", refundId);

        Refund refund = refundPersistencePort.findById(refundId)
                .orElseThrow(() -> new IllegalArgumentException("Refund not found: " + refundId));

        refund.complete();
        Refund saved = refundPersistencePort.save(refund);
        log.info("Completed refund: {}", refundId);
        return saved;
    }

    @Override
    public Refund failRefund(UUID refundId, String failureReason) {
        log.info("Failing refund: {} with reason: {}", refundId, failureReason);

        Refund refund = refundPersistencePort.findById(refundId)
                .orElseThrow(() -> new IllegalArgumentException("Refund not found: " + refundId));

        refund.fail(failureReason);
        Refund saved = refundPersistencePort.save(refund);
        log.info("Failed refund: {}", refundId);
        return saved;
    }

    @Override
    public Refund cancelRefund(UUID refundId, String cancellationReason) {
        log.info("Cancelling refund: {} with reason: {}", refundId, cancellationReason);

        Refund refund = refundPersistencePort.findById(refundId)
                .orElseThrow(() -> new IllegalArgumentException("Refund not found: " + refundId));

        refund.cancel(cancellationReason);
        Refund saved = refundPersistencePort.save(refund);
        log.info("Cancelled refund: {}", refundId);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Refund> getRefund(UUID refundId) {
        log.debug("Getting refund: {}", refundId);
        return refundPersistencePort.findById(refundId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Refund> getRefundsByTransaction(UUID transactionId) {
        log.debug("Getting refunds for transaction: {}", transactionId);
        return refundPersistencePort.findByTransactionId(transactionId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Refund> getRefundsByStatus(String status) {
        log.debug("Getting refunds by status: {}", status);
        try {
            RefundStatus refundStatus = RefundStatus.valueOf(status.toUpperCase());
            return refundPersistencePort.findByStatus(refundStatus);
        } catch (IllegalArgumentException e) {
            log.error("Invalid refund status: {}", status);
            throw new IllegalArgumentException("Invalid refund status: " + status);
        }
    }

    /**
     * Fetches the transaction amount from the transaction service.
     * This is a placeholder that should be replaced with actual integration.
     *
     * @param transactionId the transaction ID
     * @return the transaction amount
     */
    private BigDecimal fetchTransactionAmount(UUID transactionId) {
        // TODO: Integrate with transaction-service to fetch actual transaction amount
        // For now, return a placeholder
        log.warn("Using placeholder transaction amount. Integrate with transaction-service.");
        return new BigDecimal("0.00");
    }
}
