package id.payu.billing.domain.port.out;

import id.payu.billing.adapter.persistence.entity.BillPaymentEntity;

public interface PaymentEventPort {
    void publishPaymentEvent(BillPaymentEntity payment);
}
