package id.payu.wallet.domain.port.out;

import id.payu.wallet.domain.model.Pocket;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port for pocket persistence operations.
 * Pocket is a sub-wallet feature allowing users to create multiple currency pockets.
 */
public interface PocketPersistencePort {

    /**
     * Save a pocket (create or update).
     *
     * @param pocket the pocket to save
     * @return the saved pocket
     */
    Pocket save(Pocket pocket);

    /**
     * Find a pocket by its ID.
     *
     * @param pocketId the pocket ID
     * @return Optional containing the pocket if found
     */
    Optional<Pocket> findById(UUID pocketId);

    /**
     * Find all pockets for a given account ID.
     *
     * @param accountId the account ID
     * @return list of pockets for the account
     */
    List<Pocket> findByAccountId(String accountId);

    /**
     * Find pockets for a given account ID and currency.
     *
     * @param accountId the account ID
     * @param currency the currency code (e.g., "USD", "IDR")
     * @return list of matching pockets
     */
    List<Pocket> findByAccountIdAndCurrency(String accountId, String currency);

    /**
     * Find all active pockets across all accounts.
     *
     * @return list of active pockets
     */
    List<Pocket> findAllActive();
}
