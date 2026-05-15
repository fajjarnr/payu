package id.payu.billing.domain.port.in;

import id.payu.billing.adapter.persistence.entity.BillPaymentEntity;

import java.util.Optional;
import java.util.UUID;

/**
 * Inbound port for payment query use cases.
 */
public interface PaymentQueryUseCase {

    /**
     * Find a payment by its ID.
     */
    Optional<BillPaymentEntity> getPayment(UUID id);

    /**
     * Find a payment by its reference number.
     */
    Optional<BillPaymentEntity> getPaymentByReference(String referenceNumber);
}
