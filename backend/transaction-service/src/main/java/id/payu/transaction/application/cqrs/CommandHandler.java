package id.payu.transaction.application.cqrs;

/**
 * Interface for handling commands.
 * Command handlers execute business logic and persist state changes.
 *
 * @param <C> the command type
 * @param <R> the result type
 */
public interface CommandHandler<C extends Command<R>, R> {
    /**
     * Handles the command.
     *
     * @param command the command to handle
     * @return the result of command execution
     * @throws Exception if command execution fails
     */
    R handle(C command) throws Exception;
}
