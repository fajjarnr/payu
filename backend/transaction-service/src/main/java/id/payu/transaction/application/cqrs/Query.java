package id.payu.transaction.application.cqrs;

/**
 * Marker interface for queries that read state.
 * Queries return DTOs and should not modify system state.
 *
 * @param <R> the result type (typically a DTO or collection of DTOs)
 */
public interface Query<R> {
}
