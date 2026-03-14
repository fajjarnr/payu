package id.payu.statement.domain.port.out;

import java.math.BigDecimal;

public interface WalletServicePort {
    BigDecimal getCurrentBalance(String customerId);
}
