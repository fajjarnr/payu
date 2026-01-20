package id.payu.wallet.application.service;

import java.math.BigDecimal;

public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException(String accountId, BigDecimal amount, BigDecimal availableBalance) {
        super(String.format("Insufficient balance for account %s. Required: %s, Available: %s", accountId, amount, availableBalance));
    }
}
