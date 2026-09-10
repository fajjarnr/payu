package id.payu.transaction.domain.port.out;

import java.util.List;
import java.util.UUID;

/**
 * Output port for account service interactions.
 * Used for authorization checks (verifying user owns the account).
 */
public interface AccountServicePort {

    /**
     * Retrieves account IDs associated with a user.
     *
     * @param userId the user ID to look up
     * @return list of account UUIDs belonging to the user
     */
    List<UUID> getAccountIdsByUserId(String userId);

    /**
     * Resolves an account number (e.g. {@code 1001002001}) to its account UUID.
     *
     * @param accountNumber the human-readable account number
     * @return the account UUID, or empty when unknown (fail-safe: caller rejects)
     */
    java.util.Optional<UUID> getAccountIdByNumber(String accountNumber);
}
