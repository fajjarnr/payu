package id.payu.billing.domain.port.out;

import id.payu.billing.domain.model.BillPayment;
import id.payu.billing.domain.model.PaymentStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BillPaymentPersistencePort {
    BillPayment save(BillPayment payment);
    Optional<BillPayment> findById(UUID id);
    Optional<BillPayment> findByReferenceNumber(String referenceNumber);
    Optional<BillPayment> findByIdempotencyKey(String idempotencyKey);
    List<BillPayment> findByStatusIn(Collection<PaymentStatus> statuses);
}
