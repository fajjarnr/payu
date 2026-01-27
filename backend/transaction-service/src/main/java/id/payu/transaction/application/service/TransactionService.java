package id.payu.transaction.application.service;

import id.payu.transaction.domain.model.Transaction;
import id.payu.transaction.domain.port.in.TransactionUseCase;
import id.payu.transaction.domain.port.out.*;
import id.payu.transaction.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService implements TransactionUseCase {

    private final TransactionPersistencePort transactionPersistencePort;
    private final WalletServicePort walletServicePort;
    private final BifastServicePort bifastServicePort;
    private final SknServicePort sknServicePort;
    private final RgsServicePort rgsServicePort;
    private final QrisServicePort qrisServicePort;
    private final TransactionEventPublisherPort eventPublisherPort;

    @Override
    @Transactional
    public InitiateTransferResponse initiateTransfer(InitiateTransferRequest request) {
        if (request.getIdempotencyKey() != null) {
            return transactionPersistencePort.findByIdempotencyKey(request.getIdempotencyKey())
                    .map(this::mapToInitiateTransferResponse)
                    .orElseGet(() -> createNewTransaction(request));
        }
        return createNewTransaction(request);
    }

    private InitiateTransferResponse createNewTransaction(InitiateTransferRequest request) {
        String referenceNumber = generateReferenceNumber();

        Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID())
                .referenceNumber(referenceNumber)
                .senderAccountId(request.getSenderAccountId())
                .amount(request.getAmount())
                .currency(request.getCurrency() != null ? request.getCurrency() : "IDR")
                .description(request.getDescription())
                .type(Transaction.TransactionType.valueOf(request.getType().name()))
                .status(Transaction.TransactionStatus.PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .idempotencyKey(request.getIdempotencyKey())
                .build();

        transaction = transactionPersistencePort.save(transaction);
        eventPublisherPort.publishTransactionInitiated(transaction);

        ReserveBalanceResponse balanceResponse = walletServicePort.reserveBalance(
                request.getSenderAccountId(),
                transaction.getId().toString(),
                request.getAmount()
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

        if (request.getType() == InitiateTransferRequest.TransactionType.BIFAST_TRANSFER) {
            processBiFastTransfer(transaction, request);
        }

        return InitiateTransferResponse.builder()
                .transactionId(transaction.getId())
                .referenceNumber(referenceNumber)
                .status(transaction.getStatus().name())
                .fee(calculateFee(request.getType(), request.getAmount()))
                .estimatedCompletionTime(getEstimatedCompletionTime(request.getType()))
                .build();
    }

    @Override
    public Transaction getTransaction(UUID transactionId) {
        return transactionPersistencePort.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));
    }

    @Override
    public List<Transaction> getAccountTransactions(UUID accountId, int page, int size) {
        return transactionPersistencePort.findByAccountId(accountId, page, size);
    }

    @Override
    @Transactional
    public void processQrisPayment(ProcessQrisPaymentRequest request) {
        String referenceNumber = generateReferenceNumber();

        Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID())
                .referenceNumber(referenceNumber)
                .amount(request.getAmount())
                .currency(request.getCurrency() != null ? request.getCurrency() : "IDR")
                .type(Transaction.TransactionType.QRIS_PAYMENT)
                .status(Transaction.TransactionStatus.PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        transaction = transactionPersistencePort.save(transaction);
        eventPublisherPort.publishTransactionInitiated(transaction);

        QrisPaymentRequest qrisRequest = QrisPaymentRequest.builder()
                .qrisCode(request.getQrisCode())
                .amount(request.getAmount())
                .currency(transaction.getCurrency())
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
    }

    private String generateReferenceNumber() {
        return "TXN" + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }

    private BigDecimal calculateFee(InitiateTransferRequest.TransactionType type, BigDecimal amount) {
        return switch (type) {
            case INTERNAL_TRANSFER -> BigDecimal.ZERO;
            case BIFAST_TRANSFER -> new BigDecimal("2500");
            case SKN_TRANSFER -> new BigDecimal("5000");
            case RTGS_TRANSFER -> new BigDecimal("25000");
        };
    }

    private String getEstimatedCompletionTime(InitiateTransferRequest.TransactionType type) {
        return switch (type) {
            case INTERNAL_TRANSFER, BIFAST_TRANSFER -> "2 seconds";
            case SKN_TRANSFER -> "Same day";
            case RTGS_TRANSFER -> "Real-time";
        };
    }

    private void processBiFastTransfer(Transaction transaction, InitiateTransferRequest request) {
        try {
            BifastTransferRequest bifastRequest = BifastTransferRequest.builder()
                    .referenceNumber(transaction.getReferenceNumber())
                    .amount(request.getAmount())
                    .currency(request.getCurrency())
                    .beneficiaryAccountNumber(request.getRecipientAccountNumber())
                    .beneficiaryBankCode("014") // BCA dummy for now
                    .beneficiaryAccountName("Beneficiary") // Dummy
                    .senderAccountNumber(request.getSenderAccountId().toString())
                    .senderAccountName("Sender") // Dummy
                    .purposeCode("OTHR")
                    .build();

            bifastServicePort.initiateTransfer(bifastRequest);
            // If success, it remains PENDING/VALIDATING until callback or subsequent status check
            // Or if synchronous:
            transaction.setStatus(Transaction.TransactionStatus.PENDING);
        } catch (java.util.concurrent.TimeoutException e) {
            transaction.setStatus(Transaction.TransactionStatus.FAILED);
            transaction.setFailureReason("BI-FAST Timeout");
            eventPublisherPort.publishTransactionFailed(transaction, "BI-FAST Timeout");
        } catch (Exception e) {
            transaction.setStatus(Transaction.TransactionStatus.FAILED);
            transaction.setFailureReason(e.getMessage());
            eventPublisherPort.publishTransactionFailed(transaction, e.getMessage());
        } finally {
            transactionPersistencePort.save(transaction);
        }
    }

    private InitiateTransferResponse mapToInitiateTransferResponse(Transaction transaction) {
        return InitiateTransferResponse.builder()
                .transactionId(transaction.getId())
                .referenceNumber(transaction.getReferenceNumber())
                .status(transaction.getStatus().name())
                .fee(calculateFee(toRequestType(transaction.getType()), transaction.getAmount()))
                .estimatedCompletionTime(getEstimatedCompletionTime(toRequestType(transaction.getType())))
                .build();
    }

    private InitiateTransferRequest.TransactionType toRequestType(Transaction.TransactionType type) {
        try {
            return InitiateTransferRequest.TransactionType.valueOf(type.name());
        } catch (IllegalArgumentException e) {
            return InitiateTransferRequest.TransactionType.INTERNAL_TRANSFER; // Default fallback
        }
    }
}
