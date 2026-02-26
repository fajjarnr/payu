package id.payu.transaction.application.cqrs.command;

import id.payu.transaction.application.cqrs.CommandHandler;
import id.payu.transaction.domain.model.Transaction;
import id.payu.transaction.domain.port.out.QrisServicePort;
import id.payu.transaction.domain.port.out.TransactionEventPublisherPort;
import id.payu.transaction.domain.port.out.TransactionPersistencePort;
import id.payu.transaction.domain.port.out.WalletServicePort;
import id.payu.transaction.dto.QrisPaymentRequest;
import id.payu.transaction.dto.QrisPaymentResponse;
import id.payu.transaction.dto.ReserveBalanceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Handler for the ProcessQrisPaymentCommand.
 * Implements the write side of CQRS for QRIS payments.
 *
 * <p>Flow: reserve wallet balance → call QRIS service → commit (success) or release (failure).</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProcessQrisPaymentCommandHandler implements CommandHandler<ProcessQrisPaymentCommand, Void> {

    private final TransactionPersistencePort transactionPersistencePort;
    private final QrisServicePort qrisServicePort;
    private final WalletServicePort walletServicePort;
    private final TransactionEventPublisherPort eventPublisherPort;

    @Override
    @Transactional
    public Void handle(ProcessQrisPaymentCommand command) {
        log.info("Handling ProcessQrisPaymentCommand for account: {}, amount: {}",
                command.accountId(), command.amount());

        String referenceNumber = generateReferenceNumber();

        Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID())
                .referenceNumber(referenceNumber)
                .senderAccountId(command.accountId())
                .amount(command.amount())
                .type(Transaction.TransactionType.QRIS_PAYMENT)
                .status(Transaction.TransactionStatus.PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        transaction = transactionPersistencePort.save(transaction);
        eventPublisherPort.publishTransactionInitiated(transaction);

        // BUG-BE-110 FIX: Reserve balance from wallet BEFORE calling QRIS service
        ReserveBalanceResponse balanceResponse = walletServicePort.reserveBalance(
                command.accountId(),
                transaction.getId().toString(),
                command.amount().getAmount()
        );
        String reservationId = balanceResponse.getReservationId();

        if (!balanceResponse.isSuccess()) {
            transaction.setStatus(Transaction.TransactionStatus.FAILED);
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
                transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
                transaction.setCompletedAt(Instant.now());
                eventPublisherPort.publishTransactionCompleted(transaction);
            } else {
                // QRIS failed — release the reserved balance back to the wallet
                walletServicePort.releaseBalance(
                        command.accountId(),
                        transaction.getId().toString(),
                        reservationId,
                        command.amount().getAmount()
                );
                transaction.setStatus(Transaction.TransactionStatus.FAILED);
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
            transaction.setStatus(Transaction.TransactionStatus.FAILED);
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
