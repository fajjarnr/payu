package id.payu.lending.domain.port.out;

import id.payu.lending.domain.model.RepaymentPayment;
import id.payu.lending.domain.model.RepaymentPaymentStatus;

import java.util.List;
import java.util.Optional;

public interface RepaymentPaymentPersistencePort {
    RepaymentPayment save(RepaymentPayment payment);
    Optional<RepaymentPayment> findByIdempotencyKey(String idempotencyKey);
    List<RepaymentPayment> findByStatusIn(List<RepaymentPaymentStatus> statuses);
}
