package id.payu.lending.domain.port.out;

import id.payu.lending.domain.model.InstallmentCheckout;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port for InstallmentCheckout persistence operations.
 */
public interface InstallmentCheckoutPersistencePort {
    InstallmentCheckout save(InstallmentCheckout checkout);
    Optional<InstallmentCheckout> findById(UUID id);
    List<InstallmentCheckout> findByUserId(UUID userId);
    Optional<InstallmentCheckout> findByExternalOrderId(String externalOrderId);
}
