package id.payu.transaction.domain.port.out;

import id.payu.transaction.dto.QrisPaymentRequest;
import id.payu.transaction.dto.QrisPaymentResponse;

public interface QrisServicePort {
    QrisPaymentResponse processPayment(QrisPaymentRequest request);
    QrisPaymentResponse checkStatus(String transactionId);
}
