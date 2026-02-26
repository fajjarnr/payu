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
}
