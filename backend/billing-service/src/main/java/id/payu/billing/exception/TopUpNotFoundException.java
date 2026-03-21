package id.payu.billing.exception;

import id.payu.api.common.exception.BusinessException;

/**
 * Exception thrown when a top-up transaction is not found.
 */
// BUG-ARCH-002 FIX: Migrated from RuntimeException to BusinessException with proper error code
public class TopUpNotFoundException extends BusinessException {

    private static final String ERROR_CODE = "BILL_TOPUP_404";

    public TopUpNotFoundException(String message) {
        super(ERROR_CODE, message);
    }
}
