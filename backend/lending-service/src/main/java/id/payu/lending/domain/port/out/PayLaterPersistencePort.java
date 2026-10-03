package id.payu.lending.domain.port.out;

import id.payu.lending.domain.model.PayLater;

import java.util.Optional;
import java.util.UUID;

/**
 * Output port for PayLater account persistence operations.
 * Manages PayLater credit accounts for users.
 */
public interface PayLaterPersistencePort {

    PayLater save(PayLater payLater);

    Optional<PayLater> findByUserId(UUID userId);

    Optional<PayLater> findByUserIdForUpdate(UUID userId);

    Optional<PayLater> findById(UUID id);
}
