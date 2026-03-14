package id.payu.promotion.domain.port.out;

import java.math.BigDecimal;

public interface WalletServicePort {
    boolean creditWallet(String accountId, BigDecimal amount, String referenceId, String description);
}
