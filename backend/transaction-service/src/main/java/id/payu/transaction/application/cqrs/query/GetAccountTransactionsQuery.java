package id.payu.transaction.application.cqrs.query;

import id.payu.transaction.application.cqrs.Query;
import id.payu.transaction.domain.model.Transaction;

import java.util.List;

/**
 * Query to get transactions for an account with pagination.
 * This is a read operation that does not modify state.
 */
public record GetAccountTransactionsQuery(
        String accountId,
        String userId,
        int page,
        int size
) implements Query<List<Transaction>> {
}
