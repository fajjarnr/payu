package id.payu.wallet.application.service;

import id.payu.api.common.exception.BusinessException;
import java.math.BigDecimal;

/**
 * BUG-ARCH-002 FIX: Migrated to extend BusinessException with proper error code WAL_002.
 * Thrown when a wallet operation requires more balance than is available.
 */
public class InsufficientBalanceException extends BusinessException {
    public InsufficientBalanceException(String accountId, BigDecimal amount, BigDecimal availableBalance) {
        super("WAL_002", String.format("Insufficient balance for account %s. Required: %s, Available: %s", accountId, amount, availableBalance));
    }
}
