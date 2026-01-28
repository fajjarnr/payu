package id.payu.transaction.application.cqrs.query;

import id.payu.transaction.application.cqrs.Query;
import id.payu.transaction.domain.model.Transaction;

import java.util.UUID;

/**
 * Query to get transaction details by ID.
 * This is a read operation that does not modify state.
 */
public record GetTransactionQuery(
        UUID transactionId,
        String userId
) implements Query<Transaction> {
}
