package id.payu.billing.domain.port.out;

import id.payu.billing.adapter.persistence.entity.BillPaymentEntity;

import java.util.Optional;
import java.util.UUID;

public interface BillPaymentPersistencePort {
    BillPaymentEntity save(BillPaymentEntity payment);
    Optional<BillPaymentEntity> findById(UUID id);
    Optional<BillPaymentEntity> findByReferenceNumber(String referenceNumber);
}
