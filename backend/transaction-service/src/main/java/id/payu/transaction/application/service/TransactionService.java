package id.payu.transaction.application.service;

import id.payu.transaction.application.cqrs.command.InitiateTransferCommand;
import id.payu.transaction.application.cqrs.command.InitiateTransferCommandHandler;
import id.payu.transaction.application.cqrs.command.InitiateTransferCommandResult;
import id.payu.transaction.application.cqrs.command.ProcessQrisPaymentCommand;
import id.payu.transaction.application.cqrs.command.ProcessQrisPaymentCommandHandler;
import id.payu.transaction.application.cqrs.query.GetAccountTransactionsQuery;
import id.payu.transaction.application.cqrs.query.GetAccountTransactionsQueryHandler;
import id.payu.transaction.application.cqrs.query.GetTransactionQuery;
import id.payu.transaction.application.cqrs.query.GetTransactionQueryHandler;
import id.payu.transaction.adapter.persistence.entity.TransactionEntity;
import id.payu.transaction.domain.port.in.TransactionUseCase;
import id.payu.transaction.dto.InitiateTransferRequest;
import id.payu.transaction.dto.ProcessQrisPaymentRequest;
import id.payu.transaction.dto.TransactionRefundDetailsResponse;
import id.payu.transaction.domain.port.out.TransactionPersistencePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

/**
 * TransactionEntity service implementing CQRS pattern.
 *
 * <p>This service acts as a facade that delegates to specialized command and query handlers.
 * It maintains backward compatibility through deprecated methods while encouraging
 * the use of explicit Command and Query objects.</p>
 *
 * <p>CQRS Architecture:</p>
 * <ul>
 *   <li><b>Command Side:</b> Handlers for write operations (InitiateTransfer, ProcessQrisPayment)</li>
 *   <li><b>Query Side:</b> Handlers for read operations (GetTransaction, GetAccountTransactions)</li>
 *   <li><b>Benefits:</b> Independent optimization, clear intent, better testability</li>
 * </ul>
 */
@Service
public class TransactionService implements TransactionUseCase {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TransactionService.class);



    private final InitiateTransferCommandHandler initiateTransferHandler;
    private final ProcessQrisPaymentCommandHandler processQrisPaymentHandler;
    private final GetTransactionQueryHandler getTransactionHandler;
    private final GetAccountTransactionsQueryHandler getAccountTransactionsQueryHandler;
    private final TransactionPersistencePort transactionPersistencePort;
    private final ObjectMapper objectMapper;

    public TransactionService(InitiateTransferCommandHandler initiateTransferHandler,
                              ProcessQrisPaymentCommandHandler processQrisPaymentHandler,
                              GetTransactionQueryHandler getTransactionHandler,
                              GetAccountTransactionsQueryHandler getAccountTransactionsQueryHandler,
                              TransactionPersistencePort transactionPersistencePort,
                              ObjectMapper objectMapper) {
        this.initiateTransferHandler = initiateTransferHandler;
        this.processQrisPaymentHandler = processQrisPaymentHandler;
        this.getTransactionHandler = getTransactionHandler;
        this.getAccountTransactionsQueryHandler = getAccountTransactionsQueryHandler;
        this.transactionPersistencePort = transactionPersistencePort;
        this.objectMapper = objectMapper;
    }

    // CQRS Methods - Command Side (Write Operations)

    @Override
    public InitiateTransferCommandResult initiateTransfer(InitiateTransferCommand command) {
        log.info("Delegating to InitiateTransferCommandHandler");
        return initiateTransferHandler.handle(command);
    }

    @Override
    public TransactionEntity settleInterbankTransfer(String referenceNumber, String status, String failureReason) {
        return initiateTransferHandler.settleInterbankTransfer(referenceNumber, status, failureReason);
    }

    @Override
    public void processQrisPayment(ProcessQrisPaymentCommand command) {
        log.info("Delegating to ProcessQrisPaymentCommandHandler");
        processQrisPaymentHandler.handle(command);
    }

    // CQRS Methods - Query Side (Read Operations)

    @Override
    public TransactionEntity getTransaction(GetTransactionQuery query) {
        log.info("Delegating to GetTransactionQueryHandler");
        return getTransactionHandler.handle(query);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionRefundDetailsResponse getTransactionRefundDetails(UUID transactionId) {
        TransactionEntity transaction = transactionPersistencePort.findById(transactionId)
                .orElseThrow(() -> new id.payu.api.common.exception.BusinessException(
                        "TXN_404", "TransactionEntity not found: " + transactionId));
        var money = transaction.getAmount();
        var amount = money != null ? money.getAmount() : transaction.getAmountValue();
        var currency = money != null
                ? money.getCurrency().getCurrencyCode()
                : transaction.getCurrencyCode();
        if (amount == null || amount.signum() <= 0 || currency == null || currency.isBlank()) {
            throw new id.payu.api.common.exception.BusinessException(
                    "TXN_422", "Transaction has invalid refund details: " + transactionId);
        }
        return new TransactionRefundDetailsResponse(amount, currency);
    }

    @Override
    public List<TransactionEntity> getAccountTransactions(GetAccountTransactionsQuery query) {
        log.info("Delegating to GetAccountTransactionsQueryHandler");
        return getAccountTransactionsQueryHandler.handle(query);
    }

    // Legacy Methods - Deprecated, Use CQRS Methods Instead

    @Override
    @Deprecated
    public InitiateTransferCommandResult initiateTransfer(InitiateTransferRequest request, String userId) {
        log.warn("Using deprecated initiateTransfer method - consider using InitiateTransferCommand");
        InitiateTransferCommand command = InitiateTransferCommand.from(request, userId);
        return initiateTransfer(command);
    }

    @Override
    @Deprecated
    public void processQrisPayment(ProcessQrisPaymentRequest request, String userId) {
        log.warn("Using deprecated processQrisPayment method - consider using ProcessQrisPaymentCommand");
        ProcessQrisPaymentCommand command = ProcessQrisPaymentCommand.from(request, userId);
        processQrisPayment(command);
    }

    @Override
    @Deprecated
    public TransactionEntity getTransaction(UUID transactionId, String userId) {
        log.warn("Using deprecated getTransaction method - consider using GetTransactionQuery");
        GetTransactionQuery query = new GetTransactionQuery(transactionId, userId);
        return getTransaction(query);
    }

    @Override
    @Deprecated
    public List<TransactionEntity> getAccountTransactions(UUID accountId, String userId, int page, int size) {
        log.warn("Using deprecated getAccountTransactions method - consider using GetAccountTransactionsQuery");
        GetAccountTransactionsQuery query = new GetAccountTransactionsQuery(
                accountId.toString(), userId, page, size);
        return getAccountTransactions(query);
    }

    @Override
    @Transactional
    public TransactionEntity updateTransactionTags(UUID transactionId, String userId, List<String> tags) {
        log.info("Updating tags for transaction: {}", transactionId);

        // Verify ownership and get transaction
        GetTransactionQuery query = new GetTransactionQuery(transactionId, userId);
        TransactionEntity transaction = getTransaction(query);

        // Convert tags to JSON
        try {
            String tagsJson = objectMapper.writeValueAsString(tags);
            transaction.setTags(tagsJson);
            transaction.setUpdatedAt(java.time.Instant.now());

            // Save updated transaction
            return transactionPersistencePort.save(transaction);
        } catch (Exception e) {
            log.error("Failed to update transaction tags: {}", e.getMessage());
            throw new RuntimeException("Failed to update transaction tags", e);
        }
    }
    @Override
    public long countAccountTransactions(UUID accountId, String userId) {
        return transactionPersistencePort.countByAccountId(accountId);
    }
}
