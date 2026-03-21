package id.payu.billing.exception;

import id.payu.api.common.exception.BusinessException;

/**
 * Exception thrown when a subscription or plan is not found.
 */
// BUG-ARCH-002 FIX: Migrated from RuntimeException to BusinessException with proper error code
public class SubscriptionNotFoundException extends BusinessException {

    private static final String ERROR_CODE = "BILL_SUB_404";

    public SubscriptionNotFoundException(String message) {
        super(ERROR_CODE, message);
    }
}
