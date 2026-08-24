package id.payu.dispute.domain.port.in;

import id.payu.dispute.domain.model.Chargeback;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChargebackUseCase {
    Chargeback create(UUID transactionId, UUID customerId, UUID merchantId, BigDecimal amount, String currency, String reason);
    Chargeback submit(UUID chargebackId, String schemeCaseId);
    Chargeback startReview(UUID chargebackId);
    Chargeback accept(UUID chargebackId);
    Chargeback reject(UUID chargebackId, String rejectionReason);
    Chargeback reverse(UUID chargebackId);
    Chargeback close(UUID chargebackId);
    Optional<Chargeback> getById(UUID id);
    List<Chargeback> getAll();
    List<Chargeback> getByStatus(String status);
    List<Chargeback> getByCustomer(UUID customerId);
}
