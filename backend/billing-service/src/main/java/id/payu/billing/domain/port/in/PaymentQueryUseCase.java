package id.payu.billing.domain.port.in;

import id.payu.billing.domain.model.BillPayment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Inbound port for payment query use cases.
 */
public interface PaymentQueryUseCase {

    Optional<BillPayment> getPayment(UUID id);

    Optional<BillPayment> getPaymentByReference(String referenceNumber);

    PaymentPage getPaymentHistory(String accountId, int page, int size);

    /**
     * Plain page slice of payments, independent of any persistence framework.
     */
    record PaymentPage(List<BillPayment> content, long totalElements) {
    }
}
