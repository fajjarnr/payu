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
import id.payu.transaction.domain.model.Transaction;
import id.payu.transaction.domain.port.in.TransactionUseCase;
import id.payu.transaction.dto.InitiateTransferRequest;
import id.payu.transaction.dto.ProcessQrisPaymentRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Transaction service implementing CQRS pattern.
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
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService implements TransactionUseCase {

    private final InitiateTransferCommandHandler initiateTransferHandler;
    private final ProcessQrisPaymentCommandHandler processQrisPaymentHandler;
    private final GetTransactionQueryHandler getTransactionHandler;
    private final GetAccountTransactionsQueryHandler getAccountTransactionsQueryHandler;

    // CQRS Methods - Command Side (Write Operations)

    @Override
    public InitiateTransferCommandResult initiateTransfer(InitiateTransferCommand command) {
        log.info("Delegating to InitiateTransferCommandHandler");
        return initiateTransferHandler.handle(command);
    }

    @Override
    public void processQrisPayment(ProcessQrisPaymentCommand command) {
        log.info("Delegating to ProcessQrisPaymentCommandHandler");
        processQrisPaymentHandler.handle(command);
    }

    // CQRS Methods - Query Side (Read Operations)

    @Override
    public Transaction getTransaction(GetTransactionQuery query) {
        log.info("Delegating to GetTransactionQueryHandler");
        return getTransactionHandler.handle(query);
    }

    @Override
    public List<Transaction> getAccountTransactions(GetAccountTransactionsQuery query) {
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
    public Transaction getTransaction(UUID transactionId, String userId) {
        log.warn("Using deprecated getTransaction method - consider using GetTransactionQuery");
        GetTransactionQuery query = new GetTransactionQuery(transactionId, userId);
        return getTransaction(query);
    }

    @Override
    @Deprecated
    public List<Transaction> getAccountTransactions(UUID accountId, String userId, int page, int size) {
        log.warn("Using deprecated getAccountTransactions method - consider using GetAccountTransactionsQuery");
        GetAccountTransactionsQuery query = new GetAccountTransactionsQuery(
                accountId.toString(), userId, page, size);
        return getAccountTransactions(query);
    }
}
