package id.payu.transaction.application.cqrs.command;

import id.payu.transaction.application.cqrs.CommandHandler;
import id.payu.transaction.application.service.AuthorizationService;
import id.payu.transaction.adapter.persistence.entity.TransactionEntity;
import id.payu.transaction.domain.port.out.QrisServicePort;
import id.payu.transaction.domain.port.out.TransactionEventPublisherPort;
import id.payu.transaction.domain.port.out.TransactionPersistencePort;
import id.payu.transaction.domain.port.out.WalletServicePort;
import id.payu.transaction.interfaces.dto.QrisPaymentRequest;
import id.payu.transaction.interfaces.dto.QrisPaymentResponse;
import id.payu.transaction.interfaces.dto.ReserveBalanceResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;
import id.payu.transaction.domain.model.TransactionStatus;
import id.payu.transaction.domain.model.TransactionType;

/**
 * Handler for the ProcessQrisPaymentCommand.
 * Implements the write side of CQRS for QRIS payments.
 *
 * <p>Flow: reserve wallet balance → call QRIS service → commit (success) or release (failure).</p>
 */
@Component
public class ProcessQrisPaymentCommandHandler implements CommandHandler<ProcessQrisPaymentCommand, Void> {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ProcessQrisPaymentCommandHandler.class);



    private final TransactionPersistencePort transactionPersistencePort;
    private final QrisServicePort qrisServicePort;
    private final WalletServicePort walletServicePort;
    private final TransactionEventPublisherPort eventPublisherPort;
    private final AuthorizationService authorizationService;

    public ProcessQrisPaymentCommandHandler(TransactionPersistencePort transactionPersistencePort,
                                            QrisServicePort qrisServicePort,
                                            WalletServicePort walletServicePort,
                                            TransactionEventPublisherPort eventPublisherPort,
                                            AuthorizationService authorizationService) {
        this.transactionPersistencePort = transactionPersistencePort;
        this.qrisServicePort = qrisServicePort;
        this.walletServicePort = walletServicePort;
        this.eventPublisherPort = eventPublisherPort;
        this.authorizationService = authorizationService;
    }

    @Override
    @Transactional
    public Void handle(ProcessQrisPaymentCommand command) {
        log.info("Handling ProcessQrisPaymentCommand for account: {}, amount: {}",
                command.accountId(), command.amount());

        // Verify the authenticated user owns the account being debited
        authorizationService.verifyAccountOwnership(command.accountId(), command.userId());

        // CB-017: DB fallback for idempotency — replay protection survives cache
        // expiry/eviction. The @Idempotent interceptor covers the fast path (cache
        // TTL 24h); this covers anything that slips past it. Check runs in the
        // same transaction that creates the row, so it cannot miss its own insert.
        if (command.idempotencyKey() != null
                && transactionPersistencePort.findByIdempotencyKey(command.idempotencyKey()).isPresent()) {
            log.warn("Duplicate QRIS payment rejected, idempotency key: {}", command.idempotencyKey());
            throw new id.payu.api.common.exception.BusinessException(
                    "QRIS_001", "Duplicate QRIS payment: Idempotency-Key already used");
        }

        String referenceNumber = generateReferenceNumber();

        TransactionEntity transaction = TransactionEntity.builder()
                .referenceNumber(referenceNumber)
                .senderAccountId(command.accountId())
                .amount(command.amount())
                .type(TransactionType.QRIS_PAYMENT)
                .status(TransactionStatus.PENDING)
                .idempotencyKey(command.idempotencyKey())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        transaction = transactionPersistencePort.save(transaction);
        eventPublisherPort.publishTransactionInitiated(transaction, command.userId());

        // BUG-BE-110 FIX: Reserve balance from wallet BEFORE calling QRIS service
        ReserveBalanceResponse balanceResponse = walletServicePort.reserveBalance(
                command.accountId(),
                transaction.getId().toString(),
                command.amount().getAmount()
        );
        String reservationId = balanceResponse.getReservationId();

        if (!balanceResponse.isSuccess()) {
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setFailureReason("Insufficient balance");
            transactionPersistencePort.save(transaction);
            eventPublisherPort.publishTransactionFailed(transaction, "Insufficient balance");
            throw new IllegalStateException("Insufficient balance for QRIS payment");
        }

        try {
            QrisPaymentRequest qrisRequest = QrisPaymentRequest.builder()
                    .qrisCode(command.qrisCode())
                    .amount(command.amount().getAmount())
                    .currency(transaction.getAmount().getCurrency().getCurrencyCode())
                    .merchantName("Merchant")
                    .customerReference(referenceNumber)
                    .build();

            QrisPaymentResponse qrisResponse = qrisServicePort.processPayment(qrisRequest);

            if ("SUCCESS".equals(qrisResponse.getStatus())) {
                // Commit the reserved balance — finalize the debit
                walletServicePort.commitBalance(
                        command.accountId(),
                        transaction.getId().toString(),
                        reservationId,
                        command.amount().getAmount()
                );
                transaction.setStatus(TransactionStatus.COMPLETED);
                transaction.setCompletedAt(Instant.now());
                eventPublisherPort.publishTransactionCompleted(transaction, command.userId());
            } else {
                // QRIS failed — release the reserved balance back to the wallet
                walletServicePort.releaseBalance(
                        command.accountId(),
                        transaction.getId().toString(),
                        reservationId,
                        command.amount().getAmount()
                );
                transaction.setStatus(TransactionStatus.FAILED);
                transaction.setFailureReason(qrisResponse.getMessage());
                eventPublisherPort.publishTransactionFailed(transaction, qrisResponse.getMessage());
            }
        } catch (Exception e) {
            // QRIS call threw an exception — release the reserved balance
            log.error("QRIS payment processing error, releasing reserved balance: {}", e.getMessage(), e);
            walletServicePort.releaseBalance(
                    command.accountId(),
                    transaction.getId().toString(),
                    reservationId,
                    command.amount().getAmount()
            );
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setFailureReason("QRIS processing error: " + e.getMessage());
            transactionPersistencePort.save(transaction);
            eventPublisherPort.publishTransactionFailed(transaction, e.getMessage());
            throw e;
        }

        transactionPersistencePort.save(transaction);
        log.info("QRIS payment processed: {}, status: {}", transaction.getId(), transaction.getStatus());
        return null;
    }

    private String generateReferenceNumber() {
        return "QRI-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
}
