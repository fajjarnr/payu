package id.payu.transaction.domain.port.in;

import id.payu.transaction.application.cqrs.command.InitiateTransferCommand;
import id.payu.transaction.application.cqrs.command.InitiateTransferCommandResult;
import id.payu.transaction.application.cqrs.command.ProcessQrisPaymentCommand;
import id.payu.transaction.application.cqrs.query.GetAccountTransactionsQuery;
import id.payu.transaction.application.cqrs.query.GetTransactionQuery;
import id.payu.transaction.domain.model.Transaction;

import java.util.List;

/**
 * Use Case interface for Transaction operations following CQRS pattern.
 *
 * <p>This interface defines the contract for transaction operations,
 separating Commands (write operations) from Queries (read operations).</p>
 *
 * <p>CQRS Benefits:</p>
 * <ul>
 *   <li>Separation of concerns: read and write models are independent</li>
 *   <li>Optimized queries: read models can be denormalized for performance</li>
 *   <li>Clear intent: Commands modify state, Queries read state</li>
 *   <li>Independent scaling: read and write sides can scale independently</li>
 * </ul>
 */
public interface TransactionUseCase {

    // Command Methods (Write Operations)

    /**
     * Initiates a fund transfer.
     *
     * @param command the transfer command
     * @return the result containing transaction ID and status
     */
    InitiateTransferCommandResult initiateTransfer(InitiateTransferCommand command);

    /**
     * Processes a QRIS payment.
     *
     * @param command the QRIS payment command
     */
    void processQrisPayment(ProcessQrisPaymentCommand command);

    // Query Methods (Read Operations)

    /**
     * Gets a transaction by ID.
     *
     * @param query the transaction query
     * @return the transaction
     */
    Transaction getTransaction(GetTransactionQuery query);

    /**
     * Gets transactions for an account with pagination.
     *
     * @param query the account transactions query
     * @return list of transactions
     */
    List<Transaction> getAccountTransactions(GetAccountTransactionsQuery query);

    // Legacy Methods (Deprecated - Will be removed)

    /**
     * @deprecated Use {@link #initiateTransfer(InitiateTransferCommand)} instead
     */
    @Deprecated
    default InitiateTransferCommandResult initiateTransfer(
            id.payu.transaction.dto.InitiateTransferRequest request, String userId) {
        InitiateTransferCommand command = InitiateTransferCommand.from(request, userId);
        return initiateTransfer(command);
    }

    /**
     * @deprecated Use {@link #processQrisPayment(ProcessQrisPaymentCommand)} instead
     */
    @Deprecated
    default void processQrisPayment(
            id.payu.transaction.dto.ProcessQrisPaymentRequest request, String userId) {
        ProcessQrisPaymentCommand command = ProcessQrisPaymentCommand.from(request, userId);
        processQrisPayment(command);
    }

    /**
     * @deprecated Use {@link #getTransaction(GetTransactionQuery)} instead
     */
    @Deprecated
    default Transaction getTransaction(java.util.UUID transactionId, String userId) {
        GetTransactionQuery query = new GetTransactionQuery(transactionId, userId);
        return getTransaction(query);
    }

    /**
     * @deprecated Use {@link #getAccountTransactions(GetAccountTransactionsQuery)} instead
     */
    @Deprecated
    default List<Transaction> getAccountTransactions(
            java.util.UUID accountId, String userId, int page, int size) {
        GetAccountTransactionsQuery query = new GetAccountTransactionsQuery(
                accountId.toString(), userId, page, size);
        return getAccountTransactions(query);
    }
}
