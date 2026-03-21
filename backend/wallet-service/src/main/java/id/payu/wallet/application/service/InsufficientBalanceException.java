package id.payu.wallet.application.service;

import java.math.BigDecimal;

// TODO BUG-ARCH-002: Migrate to extend BusinessException with proper error codes
public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException(String accountId, BigDecimal amount, BigDecimal availableBalance) {
        super(String.format("Insufficient balance for account %s. Required: %s, Available: %s", accountId, amount, availableBalance));
    }
}
