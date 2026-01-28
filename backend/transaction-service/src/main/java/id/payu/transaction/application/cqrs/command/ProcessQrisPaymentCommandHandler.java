package id.payu.transaction.application.cqrs.command;

import id.payu.transaction.application.cqrs.CommandHandler;
import id.payu.transaction.domain.model.Transaction;
import id.payu.transaction.domain.port.out.QrisServicePort;
import id.payu.transaction.domain.port.out.TransactionEventPublisherPort;
import id.payu.transaction.domain.port.out.TransactionPersistencePort;
import id.payu.transaction.dto.QrisPaymentRequest;
import id.payu.transaction.dto.QrisPaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Handler for the ProcessQrisPaymentCommand.
 * Implements the write side of CQRS for QRIS payments.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProcessQrisPaymentCommandHandler implements CommandHandler<ProcessQrisPaymentCommand, Void> {

    private final TransactionPersistencePort transactionPersistencePort;
    private final QrisServicePort qrisServicePort;
    private final TransactionEventPublisherPort eventPublisherPort;

    @Override
    @Transactional
    public Void handle(ProcessQrisPaymentCommand command) {
        log.info("Handling ProcessQrisPaymentCommand for amount: {}", command.amount());

        String referenceNumber = generateReferenceNumber();

        Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID())
                .referenceNumber(referenceNumber)
                .amount(command.amount())
                .type(Transaction.TransactionType.QRIS_PAYMENT)
                .status(Transaction.TransactionStatus.PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        transaction = transactionPersistencePort.save(transaction);
        eventPublisherPort.publishTransactionInitiated(transaction);

        QrisPaymentRequest qrisRequest = QrisPaymentRequest.builder()
                .qrisCode(command.qrisCode())
                .amount(command.amount().getAmount())
                .currency(transaction.getAmount().getCurrency().getCurrencyCode())
                .merchantName("Merchant")
                .customerReference(referenceNumber)
                .build();

        QrisPaymentResponse qrisResponse = qrisServicePort.processPayment(qrisRequest);

        if ("SUCCESS".equals(qrisResponse.getStatus())) {
            transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
            transaction.setCompletedAt(Instant.now());
            eventPublisherPort.publishTransactionCompleted(transaction);
        } else {
            transaction.setStatus(Transaction.TransactionStatus.FAILED);
            transaction.setFailureReason(qrisResponse.getMessage());
            eventPublisherPort.publishTransactionFailed(transaction, qrisResponse.getMessage());
        }

        transactionPersistencePort.save(transaction);
        log.info("QRIS payment processed: {}, status: {}", transaction.getId(), transaction.getStatus());
        return null;
    }

    private String generateReferenceNumber() {
        return "QRI" + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }
}
