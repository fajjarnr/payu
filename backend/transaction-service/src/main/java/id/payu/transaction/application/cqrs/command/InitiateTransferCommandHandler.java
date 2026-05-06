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
@Component
public class InitiateTransferCommandHandler implements CommandHandler<InitiateTransferCommand, InitiateTransferCommandResult> {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(InitiateTransferCommandHandler.class);



    private final TransactionPersistencePort transactionPersistencePort;
    private final WalletServicePort walletServicePort;
    private final BifastServicePort bifastServicePort;
    private final SknServicePort sknServicePort;
    private final RgsServicePort rgsServicePort;
    private final TransactionEventPublisherPort eventPublisherPort;
    private final AuthorizationService authorizationService;

    public InitiateTransferCommandHandler(TransactionPersistencePort transactionPersistencePort,
                                          WalletServicePort walletServicePort,
                                          BifastServicePort bifastServicePort,
                                          SknServicePort sknServicePort,
                                          RgsServicePort rgsServicePort,
                                          TransactionEventPublisherPort eventPublisherPort,
                                          AuthorizationService authorizationService) {
        this.transactionPersistencePort = transactionPersistencePort;
        this.walletServicePort = walletServicePort;
        this.bifastServicePort = bifastServicePort;
        this.sknServicePort = sknServicePort;
        this.rgsServicePort = rgsServicePort;
        this.eventPublisherPort = eventPublisherPort;
        this.authorizationService = authorizationService;
    }

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

        String reservationId = balanceResponse.getReservationId();

        transaction.setStatus(Transaction.TransactionStatus.VALIDATING);
        transactionPersistencePort.save(transaction);

        // Process based on transfer type (BUG-BE-007: handle all types, not just BIFAST)
        switch (command.type()) {
            case BIFAST_TRANSFER -> processBiFastTransfer(transaction, command, reservationId);
            case INTERNAL_TRANSFER -> processInternalTransfer(transaction, command, reservationId);
            case SKN_TRANSFER, RTGS_TRANSFER -> processInterBankTransfer(transaction, command, reservationId);
            default -> {
                transaction.setStatus(Transaction.TransactionStatus.FAILED);
                transaction.setFailureReason("Unsupported transfer type: " + command.type());
                transactionPersistencePort.save(transaction);
                eventPublisherPort.publishTransactionFailed(transaction, "Unsupported transfer type");
                throw new IllegalArgumentException("Unsupported transfer type: " + command.type());
            }
        }

        log.info("Transfer initiated successfully: {}", transaction.getId());
        return buildResult(transaction);
    }

    private Transaction createTransaction(InitiateTransferCommand command) {
        String referenceNumber = generateReferenceNumber();

        return Transaction.builder()
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

    private void processBiFastTransfer(Transaction transaction, InitiateTransferCommand command, String reservationId) {
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
                        reservationId,
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

    /**
     * Process internal transfer — immediately completes by committing the reserved balance.
     * BUG-BE-007 fix: Previously, INTERNAL_TRANSFER was left stuck in VALIDATING status.
     */
    private void processInternalTransfer(Transaction transaction, InitiateTransferCommand command, String reservationId) {
        try {
            // For internal transfers, commit the reservation immediately
            walletServicePort.commitBalance(
                    command.senderAccountId(),
                    transaction.getId().toString(),
                    reservationId,
                    command.amount().getAmount()
            );

            // Credit the recipient
            walletServicePort.creditBalance(
                    command.recipientAccountNumber(),
                    transaction.getId().toString(),
                    command.amount().getAmount()
            );

            transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
            transaction.setCompletedAt(java.time.Instant.now());
            eventPublisherPort.publishTransactionCompleted(transaction);
        } catch (Exception e) {
            log.error("Internal transfer failed, initiating compensation. Transaction: {}, Error: {}",
                    transaction.getId(), e.getMessage());

            try {
                walletServicePort.releaseBalance(
                        command.senderAccountId(),
                        transaction.getId().toString(),
                        reservationId,
                        command.amount().getAmount()
                );
                log.info("Balance released successfully for transaction: {}", transaction.getId());
            } catch (Exception compensationError) {
                log.error("Failed to release balance during compensation for transaction: {}. Error: {}",
                        transaction.getId(), compensationError.getMessage());
            }

            transaction.setStatus(Transaction.TransactionStatus.FAILED);
            transaction.setFailureReason("Internal transfer failed: " + e.getMessage());
            eventPublisherPort.publishTransactionFailed(transaction, e.getMessage());
        } finally {
            transactionPersistencePort.save(transaction);
        }
    }

    /**
     * Process inter-bank transfer (SKN/RTGS) — sets to PENDING for downstream clearing.
     * BUG-BE-007 fix: Previously, SKN and RTGS were left stuck in VALIDATING status.
     */
    private void processInterBankTransfer(Transaction transaction, InitiateTransferCommand command, String reservationId) {
        // SKN/RTGS transfers are queued for batch/real-time clearing
        // The actual clearing is handled by the downstream clearing system
        transaction.setStatus(Transaction.TransactionStatus.PENDING);
        transactionPersistencePort.save(transaction);
        log.info("{} transfer queued for clearing: {}", command.type(), transaction.getId());
    }
    private String generateReferenceNumber() {
        return "TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
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
