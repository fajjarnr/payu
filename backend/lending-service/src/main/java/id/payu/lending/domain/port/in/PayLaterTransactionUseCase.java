package id.payu.lending.domain.port.in;

import id.payu.lending.domain.model.PayLaterTransaction;

import java.util.List;
import java.util.UUID;

public interface PayLaterTransactionUseCase {
    PayLaterTransaction recordPurchase(UUID userId, String merchantName, java.math.BigDecimal amount, String description);

    PayLaterTransaction recordPurchase(UUID userId, String merchantName, java.math.BigDecimal amount, String description, String externalId);
    PayLaterTransaction recordPayment(UUID userId, java.math.BigDecimal amount);

    PayLaterTransaction recordPayment(UUID userId, java.math.BigDecimal amount, String externalId);
    List<PayLaterTransaction> getTransactionHistory(UUID userId);
}
