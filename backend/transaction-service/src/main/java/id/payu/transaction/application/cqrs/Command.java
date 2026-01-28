package id.payu.transaction.application.cqrs;

/**
 * Marker interface for commands that modify state.
 * Commands represent intent to change system state and return void or an identifier.
 *
 * @param <R> the result type (typically Void, UUID, or a status enum)
 */
public interface Command<R> {
}
