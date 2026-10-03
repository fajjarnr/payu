package id.payu.lending.domain.port.in;

import id.payu.lending.domain.model.InstallmentCheckout;
import id.payu.lending.domain.model.InstallmentOption;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Input port for installment checkout operations (GAP-012).
 * Gateway-facing use cases for PayLater installment payments.
 */
public interface InstallmentUseCase {

    List<InstallmentOption> getTenorOptions(UUID userId, BigDecimal amount);

    InstallmentCheckout checkout(UUID userId, String partnerId, String externalOrderId,
                                  BigDecimal amount, int tenor);

    InstallmentCheckout getCheckout(UUID checkoutId);

    List<InstallmentCheckout> getCheckoutsByUser(UUID userId);
}
