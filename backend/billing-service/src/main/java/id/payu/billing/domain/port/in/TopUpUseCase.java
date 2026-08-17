package id.payu.billing.domain.port.in;

import id.payu.billing.domain.model.BillPayment;
import id.payu.billing.interfaces.dto.TopUpRequest;

/**
 * Inbound port for e-wallet top-up use case.
 */
public interface TopUpUseCase {

    /**
     * Create and process an e-wallet top-up.
     */
    BillPayment createTopUp(TopUpRequest request);
}
