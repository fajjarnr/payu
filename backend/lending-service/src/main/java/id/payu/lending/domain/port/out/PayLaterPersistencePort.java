package id.payu.lending.domain.port.out;

import id.payu.lending.domain.model.PayLater;

import java.util.Optional;
import java.util.UUID;

/**
 * Output port for PayLater account persistence operations.
 * Manages PayLater credit accounts for users.
 */
public interface PayLaterPersistencePort {

    /**
     * Save a PayLater account.
     *
     * @param payLater the PayLater account to save
     * @return the saved PayLater account
     */
    PayLater save(PayLater payLater);

    /**
     * Find PayLater account by user ID.
     *
     * @param userId the user ID
     * @return optional containing the PayLater account if found
     */
    Optional<PayLater> findByUserId(UUID userId);

    /**
     * Find PayLater account by ID.
     *
     * @param id the PayLater account ID
     * @return optional containing the PayLater account if found
     */
    Optional<PayLater> findById(UUID id);
}
