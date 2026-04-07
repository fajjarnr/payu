package id.payu.wallet.application.service;

import id.payu.api.common.exception.BusinessException;

/**
 * BUG-ARCH-002 FIX: Migrated to extend BusinessException with proper error code WAL_007.
 * Thrown when a pocket is not found.
 */
public class PocketNotFoundException extends BusinessException {
    public PocketNotFoundException(String message) {
        super("WAL_007", message);
    }

    public PocketNotFoundException(String message, Throwable cause) {
        super("WAL_007", message, cause);
    }
}
