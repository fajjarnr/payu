package id.payu.transaction.application.cqrs;

/**
 * Interface for handling queries.
 * Query handlers retrieve data and should not modify state.
 *
 * @param <Q> the query type
 * @param <R> the result type
 */
public interface QueryHandler<Q extends Query<R>, R> {
    /**
     * Handles the query.
     */
    R handle(Q query) throws Exception;
}
