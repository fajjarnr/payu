package id.payu.billing.domain.port.out;

import id.payu.billing.domain.model.BillPayment;

public interface PaymentEventPort {
    void publishPaymentEvent(BillPayment payment);
}
