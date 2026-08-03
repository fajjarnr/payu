package id.payu.transaction.domain.port.in;

import id.payu.transaction.application.cqrs.command.InitiateTransferCommand;
import id.payu.transaction.application.cqrs.command.InitiateTransferCommandResult;
import id.payu.transaction.application.cqrs.command.ProcessQrisPaymentCommand;
import id.payu.transaction.application.cqrs.query.GetAccountTransactionsQuery;
import id.payu.transaction.application.cqrs.query.GetTransactionQuery;
import id.payu.transaction.adapter.persistence.entity.TransactionEntity;
import id.payu.transaction.dto.TransactionRefundDetailsResponse;

import java.util.List;
import java.util.UUID;

/**
 * Use Case interface for TransactionEntity operations following CQRS pattern.
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

    TransactionEntity settleInterbankTransfer(String referenceNumber, String status, String failureReason);

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
    TransactionEntity getTransaction(GetTransactionQuery query);

    /**
     * Gets the amount and currency needed by dispute-service to create a refund.
     */
    TransactionRefundDetailsResponse getTransactionRefundDetails(UUID transactionId);

    /**
     * Gets transactions for an account with pagination.
     *
     * @param query the account transactions query
     * @return list of transactions
     */
    List<TransactionEntity> getAccountTransactions(GetAccountTransactionsQuery query);

    /**
     * Updates tags for a transaction (IMP-037).
     *
     * @param transactionId the transaction ID
     * @param userId the user ID for ownership validation
     * @param tags the list of tags to set
     * @return the updated transaction
     */
    TransactionEntity updateTransactionTags(UUID transactionId, String userId, List<String> tags);

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
    default TransactionEntity getTransaction(java.util.UUID transactionId, String userId) {
        GetTransactionQuery query = new GetTransactionQuery(transactionId, userId);
        return getTransaction(query);
    }

    /**
     * @deprecated Use {@link #getAccountTransactions(GetAccountTransactionsQuery)} instead
     */
    @Deprecated
    default List<TransactionEntity> getAccountTransactions(
            java.util.UUID accountId, String userId, int page, int size) {
        GetAccountTransactionsQuery query = new GetAccountTransactionsQuery(
                accountId.toString(), userId, page, size);
        return getAccountTransactions(query);
    }
    long countAccountTransactions(UUID accountId, String userId);
}
