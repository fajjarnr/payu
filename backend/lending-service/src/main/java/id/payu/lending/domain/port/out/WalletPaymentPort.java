package id.payu.lending.domain.port.out;

import java.math.BigDecimal;
import java.util.UUID;

public interface WalletPaymentPort {
    String collectRepayment(UUID loanId, UUID userId, BigDecimal amount, String currency,
                            String referenceId, String description);
}
