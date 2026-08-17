package id.payu.transaction.application.cqrs.command;

import id.payu.transaction.application.cqrs.CommandHandler;
import id.payu.transaction.application.service.AuthorizationService;
import id.payu.transaction.adapter.persistence.entity.TransactionEntity;
import id.payu.transaction.domain.port.out.*;
import id.payu.transaction.interfaces.dto.BifastTransferRequest;
import id.payu.transaction.interfaces.dto.InitiateTransferRequest;
import id.payu.transaction.interfaces.dto.QrisPaymentRequest;
import id.payu.transaction.interfaces.dto.QrisPaymentResponse;
import id.payu.transaction.interfaces.dto.RgsTransferRequest;
import id.payu.transaction.interfaces.dto.ReserveBalanceResponse;
import id.payu.transaction.interfaces.dto.SknTransferRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import id.payu.transaction.domain.model.TransactionStatus;
import id.payu.transaction.domain.model.TransactionType;

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
        TransactionEntity transaction = createTransaction(command);
        transaction = transactionPersistencePort.save(transaction);
        eventPublisherPort.publishTransactionInitiated(transaction);

        // IMP-1: internal transfers are atomic 1-hop on the wallet side (debit+credit in one
        // transaction, idempotent by reference) — no reservation and no saga compensation needed.
        if (command.type() == id.payu.transaction.interfaces.dto.TransactionType.INTERNAL_TRANSFER) {
            processInternalTransfer(transaction, command);
            log.info("Transfer initiated successfully: {}", transaction.getId());
            return buildResult(transaction);
        }

        // Reserve balance from wallet
        ReserveBalanceResponse balanceResponse = walletServicePort.reserveBalance(
                command.senderAccountId(),
                transaction.getId().toString(),
                command.amount().getAmount()
        );

        if (!balanceResponse.isSuccess()) {
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setFailureReason("Insufficient balance");
            transactionPersistencePort.save(transaction);
            eventPublisherPort.publishTransactionFailed(transaction, "Insufficient balance");
            throw new IllegalStateException("Insufficient balance");
        }

        String reservationId = balanceResponse.getReservationId();

        transaction.setReservationId(reservationId);
        transaction.setStatus(TransactionStatus.VALIDATING);
        transactionPersistencePort.save(transaction);

        // Process based on transfer type (BUG-BE-007: handle all types, not just BIFAST)
        switch (command.type()) {
            case BIFAST_TRANSFER -> processBiFastTransfer(transaction, command, reservationId);
            case INTERNAL_TRANSFER -> throw new IllegalStateException(
                    "INTERNAL_TRANSFER must take the atomic 1-hop path, not the reservation path");
            case SKN_TRANSFER, RTGS_TRANSFER -> processInterBankTransfer(transaction, command, reservationId);
            default -> {
                transaction.setStatus(TransactionStatus.FAILED);
                transaction.setFailureReason("Unsupported transfer type: " + command.type());
                transactionPersistencePort.save(transaction);
                eventPublisherPort.publishTransactionFailed(transaction, "Unsupported transfer type");
                throw new IllegalArgumentException("Unsupported transfer type: " + command.type());
            }
        }

        log.info("Transfer initiated successfully: {}", transaction.getId());
        return buildResult(transaction);
    }

    private TransactionEntity createTransaction(InitiateTransferCommand command) {
        String referenceNumber = generateReferenceNumber();

        return TransactionEntity.builder()
                .referenceNumber(referenceNumber)
                .senderAccountId(command.senderAccountId())
                .amount(command.amount())
                .description(command.description())
                .type(TransactionType.valueOf(command.type().name()))
                .status(TransactionStatus.PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .idempotencyKey(command.idempotencyKey())
                .metadata("{\"recipientAccountNumber\":\""
                        + command.recipientAccountNumber().replace("\\", "\\\\").replace("\"", "\\\"")
                        + "\"}")
                .build();
    }

    private InitiateTransferCommandResult findByIdempotencyKey(String idempotencyKey) {
        return transactionPersistencePort.findByIdempotencyKey(idempotencyKey)
                .map(this::buildResult)
                .orElse(null);
    }

    private InitiateTransferCommandResult buildResult(TransactionEntity transaction) {
        return new InitiateTransferCommandResult(
                transaction.getId(),
                transaction.getReferenceNumber(),
                transaction.getStatus().name(),
                BigDecimal.ZERO,
                getEstimatedCompletionTime(transaction.getType())
        );
    }

    @Transactional
    public TransactionEntity settleInterbankTransfer(String referenceNumber, String status, String failureReason) {
        // IMP-5: lock the row FOR UPDATE so concurrent callbacks serialize; the
        // terminal check below is then race-free (exactly one callback mutates).
        TransactionEntity transaction = transactionPersistencePort.findByReferenceNumberForUpdate(referenceNumber)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + referenceNumber));

        if (transaction.getType() != TransactionType.BIFAST_TRANSFER
                && transaction.getType() != TransactionType.SKN_TRANSFER
                && transaction.getType() != TransactionType.RTGS_TRANSFER) {
            throw new IllegalArgumentException("Transaction is not an interbank transfer: " + referenceNumber);
        }

        if (transaction.getStatus() == TransactionStatus.COMPLETED
                || transaction.getStatus() == TransactionStatus.FAILED
                || transaction.getStatus() == TransactionStatus.CANCELLED) {
            return transaction;
        }

        String normalizedStatus = status.toUpperCase(Locale.ROOT);
        switch (normalizedStatus) {
            case "COMPLETED", "SUCCESS", "SETTLED" -> {
                requireReservation(transaction);
                walletServicePort.commitBalance(
                        transaction.getSenderAccountId(),
                        transaction.getId().toString(),
                        transaction.getReservationId(),
                        transaction.getAmount().getAmount());
                transaction.setStatus(TransactionStatus.COMPLETED);
                transaction.setCompletedAt(Instant.now());
                eventPublisherPort.publishTransactionCompleted(transaction);
            }
            case "FAILED", "REJECTED", "CANCELLED" -> {
                requireReservation(transaction);
                walletServicePort.releaseBalance(
                        transaction.getSenderAccountId(),
                        transaction.getId().toString(),
                        transaction.getReservationId(),
                        transaction.getAmount().getAmount());
                transaction.setStatus(TransactionStatus.FAILED);
                transaction.setFailureReason(failureReason != null ? failureReason : "Interbank transfer failed");
                eventPublisherPort.publishTransactionFailed(transaction, transaction.getFailureReason());
            }
            case "PENDING", "PROCESSING", "ACCEPTED" -> transaction.setStatus(TransactionStatus.PENDING);
            default -> throw new IllegalArgumentException("Unsupported interbank status: " + status);
        }

        return transactionPersistencePort.save(transaction);
    }

    private void requireReservation(TransactionEntity transaction) {
        if (transaction.getReservationId() == null || transaction.getReservationId().isBlank()) {
            throw new IllegalStateException("Missing wallet reservation for transaction: " + transaction.getReferenceNumber());
        }
    }

    private void processBiFastTransfer(TransactionEntity transaction, InitiateTransferCommand command, String reservationId) {
        try {
            BifastTransferRequest bifastRequest = BifastTransferRequest.builder()
                    .referenceNumber(transaction.getReferenceNumber())
                    .amount(command.amount().getAmount())
                    .currency(command.amount().getCurrency().getCurrencyCode())
                    .beneficiaryAccountNumber(command.recipientAccountNumber())
                    .beneficiaryBankCode(resolveBankCode(command))
                    .beneficiaryAccountName("Beneficiary")
                    .senderAccountNumber(command.senderAccountId().toString())
                    .senderAccountName("Sender")
                    .purposeCode("OTHR")
                    .build();

            bifastServicePort.initiateTransfer(bifastRequest);
            transaction.setStatus(TransactionStatus.PENDING);
        } catch (Exception e) {
            // SAGA COMPENSATION: Release reserved balance on BiFast failure
            log.error("BiFast transfer failed, initiating compensation. TransactionEntity: {}, Error: {}",
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

            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setFailureReason("BiFast transfer failed: " + e.getMessage());
            eventPublisherPort.publishTransactionFailed(transaction, e.getMessage());
        } finally {
            transactionPersistencePort.save(transaction);
        }
    }

    /**
     * Process internal transfer — atomic 1-hop (IMP-1): wallet debits the sender and
     * credits the recipient in one transaction, idempotent by reference, so a failure
     * leaves no money moved and a replay cannot double-mutate. The pre-IMP-1
     * reserve→commit→credit saga (with :REFUND compensation) is no longer needed.
     */
    private void processInternalTransfer(TransactionEntity transaction, InitiateTransferCommand command) {
        try {
            walletServicePort.transferBalance(
                    command.senderAccountId().toString(),
                    command.recipientAccountNumber(),
                    command.amount().getAmount(),
                    transaction.getId().toString()
            );
            transaction.setStatus(TransactionStatus.COMPLETED);
            transaction.setCompletedAt(java.time.Instant.now());
            eventPublisherPort.publishTransactionCompleted(transaction);
        } catch (Exception e) {
            log.error("Internal transfer failed, no compensation needed (atomic 1-hop). TransactionEntity: {}, Error: {}",
                    transaction.getId(), e.getMessage());
            transaction.setStatus(TransactionStatus.FAILED);
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
    private void processInterBankTransfer(TransactionEntity transaction, InitiateTransferCommand command, String reservationId) {
        try {
            if (command.type() == id.payu.transaction.interfaces.dto.TransactionType.SKN_TRANSFER) {
                sknServicePort.initiateTransfer(SknTransferRequest.builder()
                        .referenceNumber(transaction.getReferenceNumber())
                        .amount(command.amount().getAmount())
                        .currency(command.amount().getCurrency().getCurrencyCode())
                        .beneficiaryAccountNumber(command.recipientAccountNumber())
                        .beneficiaryBankCode(resolveBankCode(command))
                        .beneficiaryAccountName("Beneficiary")
                        .senderAccountNumber(command.senderAccountId().toString())
                        .senderAccountName("Sender")
                        .beneficiaryBankName("Bank")
                        .purposeCode("OTHR")
                        .beneficiaryTypeCode("001")
                        .beneficiaryResidentCode("001")
                        .build());
            } else {
                RgsTransferRequest request = new RgsTransferRequest();
                request.setReferenceNumber(transaction.getReferenceNumber());
                request.setAmount(command.amount().getAmount());
                request.setCurrency(command.amount().getCurrency().getCurrencyCode());
                request.setBeneficiaryAccountNumber(command.recipientAccountNumber());
                request.setBeneficiaryBankCode(resolveBankCode(command));
                request.setBeneficiaryAccountName("Beneficiary");
                request.setBeneficiaryBankName("Bank");
                request.setSenderAccountNumber(command.senderAccountId().toString());
                request.setSenderAccountName("Sender");
                request.setPurposeCode("OTHR");
                request.setBeneficiaryTypeCode("001");
                request.setBeneficiaryResidentCode("001");
                rgsServicePort.initiateTransfer(request);
            }

            transaction.setStatus(TransactionStatus.PENDING);
            log.info("{} transfer submitted for clearing: {}", command.type(), transaction.getId());
        } catch (Exception e) {
            try {
                walletServicePort.releaseBalance(
                        command.senderAccountId(),
                        transaction.getId().toString(),
                        reservationId,
                        command.amount().getAmount());
            } catch (Exception compensationError) {
                log.error("Failed to release balance for interbank transfer: {}", transaction.getId(), compensationError);
            }
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setFailureReason("Interbank transfer failed: " + e.getMessage());
            eventPublisherPort.publishTransactionFailed(transaction, e.getMessage());
        } finally {
            transactionPersistencePort.save(transaction);
        }
    }
    private String resolveBankCode(InitiateTransferCommand command) {
        return command.bankCode() != null && !command.bankCode().isBlank() ? command.bankCode() : "014";
    }

    private String generateReferenceNumber() {
        return "TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    private String getEstimatedCompletionTime(TransactionType type) {
        return switch (type) {
            case INTERNAL_TRANSFER, BIFAST_TRANSFER -> "2 seconds";
            case SKN_TRANSFER -> "Same day";
            case RTGS_TRANSFER -> "Real-time";
            default -> "Unknown";
        };
    }
}
