package id.payu.dispute.application.service;

import id.payu.dispute.domain.model.Refund;
import id.payu.dispute.domain.model.RefundStatus;
import id.payu.dispute.domain.model.TransactionDetails;
import id.payu.dispute.domain.port.in.RefundUseCase;
import id.payu.dispute.domain.port.out.RefundEventPublisherPort;
import id.payu.dispute.domain.port.out.RefundPersistencePort;
import id.payu.dispute.domain.port.out.TransactionLookupPort;
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
    private final RefundEventPublisherPort refundEventPublisherPort;
    private final TransactionLookupPort transactionLookupPort;

    @Override
    public Refund createFullRefund(UUID transactionId, String reason) {
        log.info("Creating full refund for transaction: {}", transactionId);
        TransactionDetails transaction = getTransactionDetails(transactionId);
        assertRefundable(transactionId, transaction.amount(), transaction.amount());

        Refund refund = Refund.createFullRefund(transactionId, transaction.amount(), transaction.currency(), reason);
        Refund saved = refundPersistencePort.save(refund);
        refundEventPublisherPort.publishRefundRequested(saved, transaction);
        log.info("Created refund with ID: {} for transaction: {}", saved.getId(), transactionId);
        return saved;
    }

    @Override
    public Refund createPartialRefund(UUID transactionId, BigDecimal amount, String currency, String reason) {
        log.info("Creating partial refund for transaction: {} with amount: {} {}", transactionId, amount, currency);

        TransactionDetails transaction = getTransactionDetails(transactionId);
        if (currency == null || !transaction.currency().equalsIgnoreCase(currency)) {
            throw new IllegalArgumentException("Refund currency must match transaction currency");
        }
        if (amount == null || amount.compareTo(transaction.amount()) > 0) {
            throw new IllegalArgumentException("Refund amount cannot exceed transaction amount");
        }
        assertRefundable(transactionId, amount, transaction.amount());

        Refund refund = Refund.createPartialRefund(transactionId, amount, transaction.currency(), reason);
        Refund saved = refundPersistencePort.save(refund);
        refundEventPublisherPort.publishRefundRequested(saved, transaction);
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

    private TransactionDetails getTransactionDetails(UUID transactionId) {
        return transactionLookupPort.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));
    }

    private void assertRefundable(UUID transactionId, BigDecimal requestedAmount, BigDecimal transactionAmount) {
        BigDecimal activeRefunds = refundPersistencePort.findByTransactionId(transactionId).stream()
                .filter(refund -> refund.getStatus() == RefundStatus.PENDING
                        || refund.getStatus() == RefundStatus.PROCESSING
                        || refund.getStatus() == RefundStatus.COMPLETED)
                .map(Refund::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (activeRefunds.add(requestedAmount).compareTo(transactionAmount) > 0) {
            throw new IllegalArgumentException("Refund amount exceeds the remaining refundable amount");
        }
    }
}
