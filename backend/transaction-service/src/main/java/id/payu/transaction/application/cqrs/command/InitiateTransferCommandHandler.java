package id.payu.transaction.application.cqrs.command;

import id.payu.transaction.application.cqrs.CommandHandler;
import id.payu.transaction.application.service.AuthorizationService;
import id.payu.transaction.domain.model.Transaction;
import id.payu.transaction.domain.port.out.*;
import id.payu.transaction.dto.BifastTransferRequest;
import id.payu.transaction.dto.InitiateTransferRequest;
import id.payu.transaction.dto.QrisPaymentRequest;
import id.payu.transaction.dto.QrisPaymentResponse;
import id.payu.transaction.dto.ReserveBalanceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Handler for the InitiateTransferCommand.
 * Implements the write side of CQRS for transfer initiation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InitiateTransferCommandHandler implements CommandHandler<InitiateTransferCommand, InitiateTransferCommandResult> {

    private final TransactionPersistencePort transactionPersistencePort;
    private final WalletServicePort walletServicePort;
    private final BifastServicePort bifastServicePort;
    private final SknServicePort sknServicePort;
    private final RgsServicePort rgsServicePort;
    private final TransactionEventPublisherPort eventPublisherPort;
    private final AuthorizationService authorizationService;

    @Override
    @Transactional
    public InitiateTransferCommandResult handle(InitiateTransferCommand command) {
        log.info("Handling InitiateTransferCommand for sender: {}", command.senderAccountId());

        // Verify the user owns the sender account
        authorizationService.verifySenderAccountOwnership(command.senderAccountId(), command.userId());

        // Check for idempotency
        if (command.idempotencyKey() != null) {
            InitiateTransferCommandResult existingResult = findByIdempotencyKey(command.idempotencyKey());
            if (existingResult != null) {
                log.info("Returning existing transaction for idempotency key: {}", command.idempotencyKey());
                return existingResult;
            }
        }

        // Create and persist the transaction
        Transaction transaction = createTransaction(command);
        transaction = transactionPersistencePort.save(transaction);
        eventPublisherPort.publishTransactionInitiated(transaction);

        // Reserve balance from wallet
        ReserveBalanceResponse balanceResponse = walletServicePort.reserveBalance(
                command.senderAccountId(),
                transaction.getId().toString(),
                command.amount().getAmount()
        );

        if (!balanceResponse.isSuccess()) {
            transaction.setStatus(Transaction.TransactionStatus.FAILED);
            transaction.setFailureReason("Insufficient balance");
            transactionPersistencePort.save(transaction);
            eventPublisherPort.publishTransactionFailed(transaction, "Insufficient balance");
            throw new IllegalStateException("Insufficient balance");
        }

        transaction.setStatus(Transaction.TransactionStatus.VALIDATING);
        transactionPersistencePort.save(transaction);

        // Process based on transfer type
        if (command.type() == InitiateTransferRequest.TransactionType.BIFAST_TRANSFER) {
            processBiFastTransfer(transaction, command);
        }

        log.info("Transfer initiated successfully: {}", transaction.getId());
        return buildResult(transaction);
    }

    private Transaction createTransaction(InitiateTransferCommand command) {
        String referenceNumber = generateReferenceNumber();

        return Transaction.builder()
                .id(UUID.randomUUID())
                .referenceNumber(referenceNumber)
                .senderAccountId(command.senderAccountId())
                .amount(command.amount())
                .description(command.description())
                .type(Transaction.TransactionType.valueOf(command.type().name()))
                .status(Transaction.TransactionStatus.PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .idempotencyKey(command.idempotencyKey())
                .build();
    }

    private InitiateTransferCommandResult findByIdempotencyKey(String idempotencyKey) {
        return transactionPersistencePort.findByIdempotencyKey(idempotencyKey)
                .map(this::buildResult)
                .orElse(null);
    }

    private InitiateTransferCommandResult buildResult(Transaction transaction) {
        return new InitiateTransferCommandResult(
                transaction.getId(),
                transaction.getReferenceNumber(),
                transaction.getStatus().name(),
                calculateFee(transaction.getType()),
                getEstimatedCompletionTime(transaction.getType())
        );
    }

    private void processBiFastTransfer(Transaction transaction, InitiateTransferCommand command) {
        try {
            BifastTransferRequest bifastRequest = BifastTransferRequest.builder()
                    .referenceNumber(transaction.getReferenceNumber())
                    .amount(command.amount().getAmount())
                    .currency(command.amount().getCurrency().getCurrencyCode())
                    .beneficiaryAccountNumber(command.recipientAccountNumber())
                    .beneficiaryBankCode("014")
                    .beneficiaryAccountName("Beneficiary")
                    .senderAccountNumber(command.senderAccountId().toString())
                    .senderAccountName("Sender")
                    .purposeCode("OTHR")
                    .build();

            bifastServicePort.initiateTransfer(bifastRequest);
            transaction.setStatus(Transaction.TransactionStatus.PENDING);
        } catch (Exception e) {
            // SAGA COMPENSATION: Release reserved balance on BiFast failure
            log.error("BiFast transfer failed, initiating compensation. Transaction: {}, Error: {}",
                    transaction.getId(), e.getMessage());

            // Compensate: Release the reserved balance back to wallet
            try {
                walletServicePort.releaseBalance(
                        command.senderAccountId(),
                        transaction.getId().toString(),
                        command.amount().getAmount()
                );
                log.info("Balance released successfully for transaction: {}", transaction.getId());
            } catch (Exception compensationError) {
                log.error("Failed to release balance during compensation for transaction: {}. Error: {}",
                        transaction.getId(), compensationError.getMessage());
                // Continue with failure marking even if compensation fails
                // In production, this should trigger an alert for manual intervention
            }

            transaction.setStatus(Transaction.TransactionStatus.FAILED);
            transaction.setFailureReason("BiFast transfer failed: " + e.getMessage());
            eventPublisherPort.publishTransactionFailed(transaction, e.getMessage());
        } finally {
            transactionPersistencePort.save(transaction);
        }
    }

    private String generateReferenceNumber() {
        return "TXN" + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }

    private BigDecimal calculateFee(Transaction.TransactionType type) {
        return switch (type) {
            case INTERNAL_TRANSFER -> BigDecimal.ZERO;
            case BIFAST_TRANSFER -> new BigDecimal("2500");
            case SKN_TRANSFER -> new BigDecimal("5000");
            case RTGS_TRANSFER -> new BigDecimal("25000");
            default -> BigDecimal.ZERO;
        };
    }

    private String getEstimatedCompletionTime(Transaction.TransactionType type) {
        return switch (type) {
            case INTERNAL_TRANSFER, BIFAST_TRANSFER -> "2 seconds";
            case SKN_TRANSFER -> "Same day";
            case RTGS_TRANSFER -> "Real-time";
            default -> "Unknown";
        };
    }
}
