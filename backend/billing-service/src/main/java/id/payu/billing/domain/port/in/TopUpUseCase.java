package id.payu.billing.domain.port.in;

import id.payu.billing.domain.model.BillPayment;
import id.payu.billing.interfaces.dto.TopUpRequest;

/**
 * Inbound port for e-wallet top-up use case.
 */
public interface TopUpUseCase {

    BillPayment createTopUp(TopUpRequest request);
}
