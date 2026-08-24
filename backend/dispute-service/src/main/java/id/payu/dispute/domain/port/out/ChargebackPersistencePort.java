package id.payu.dispute.domain.port.out;

import id.payu.dispute.domain.model.Chargeback;
import id.payu.dispute.domain.model.ChargebackStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChargebackPersistencePort {
    Chargeback save(Chargeback chargeback);
    Optional<Chargeback> findById(UUID id);
    List<Chargeback> findAll();
    List<Chargeback> findByStatus(ChargebackStatus status);
    List<Chargeback> findByCustomerId(UUID customerId);
}
