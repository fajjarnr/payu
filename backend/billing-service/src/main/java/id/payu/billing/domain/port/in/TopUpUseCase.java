package id.payu.billing.domain.port.in;

import id.payu.billing.adapter.persistence.entity.BillPaymentEntity;
import id.payu.billing.dto.TopUpRequest;

/**
 * Inbound port for e-wallet top-up use case.
 */
public interface TopUpUseCase {

    /**
     * Create and process an e-wallet top-up.
     */
    BillPaymentEntity createTopUp(TopUpRequest request);
}
