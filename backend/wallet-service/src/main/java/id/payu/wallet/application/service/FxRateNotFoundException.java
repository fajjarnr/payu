package id.payu.wallet.application.service;

import id.payu.api.common.exception.BusinessException;

/**
 * BUG-ARCH-002 FIX: Migrated to extend BusinessException with proper error code WAL_006.
 * Thrown when an FX rate is not found.
 */
public class FxRateNotFoundException extends BusinessException {
    public FxRateNotFoundException(String message) {
        super("WAL_006", message);
    }

    public FxRateNotFoundException(String message, Throwable cause) {
        super("WAL_006", message, cause);
    }
}
