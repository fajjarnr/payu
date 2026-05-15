package id.payu.billing.domain.port.in;

import id.payu.billing.adapter.persistence.entity.BillPaymentEntity;
import id.payu.billing.dto.CreatePaymentRequest;

/**
 * Inbound port for bill payment use case.
 */
public interface PayBillUseCase {

    /**
     * Create and process a bill payment.
     */
    BillPaymentEntity createPayment(CreatePaymentRequest request);
}
