package id.payu.billing.domain.port.in;

import id.payu.billing.domain.model.BillPayment;
import id.payu.billing.interfaces.dto.CreatePaymentRequest;

/**
 * Inbound port for bill payment use case.
 */
public interface PayBillUseCase {

    /**
     * Create and process a bill payment.
     */
    BillPayment createPayment(CreatePaymentRequest request);
}
