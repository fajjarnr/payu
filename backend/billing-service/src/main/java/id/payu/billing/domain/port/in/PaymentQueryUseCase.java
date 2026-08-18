package id.payu.billing.domain.port.in;

import id.payu.billing.domain.model.BillPayment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Inbound port for payment query use cases.
 */
public interface PaymentQueryUseCase {

    /**
     * Find a payment by its ID.
     */
    Optional<BillPayment> getPayment(UUID id);

    /**
     * Find a payment by its reference number.
     */
    Optional<BillPayment> getPaymentByReference(String referenceNumber);

    /**
     * Find the paginated payment history of an account.
     */
    PaymentPage getPaymentHistory(String accountId, int page, int size);

    /**
     * Plain page slice of payments, independent of any persistence framework.
     */
    record PaymentPage(List<BillPayment> content, long totalElements) {
    }
}
