package id.payu.billing.domain.port.out;

import id.payu.billing.domain.model.BillPayment;

import java.util.Optional;
import java.util.UUID;

public interface BillPaymentPersistencePort {
    BillPayment save(BillPayment payment);
    Optional<BillPayment> findById(UUID id);
    Optional<BillPayment> findByReferenceNumber(String referenceNumber);
}
