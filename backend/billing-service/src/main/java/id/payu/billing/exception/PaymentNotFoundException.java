package id.payu.billing.exception;

import id.payu.api.common.exception.BusinessException;

/**
 * Exception thrown when a payment is not found.
 */
// BUG-ARCH-002 FIX: Migrated from RuntimeException to BusinessException with proper error code
public class PaymentNotFoundException extends BusinessException {

    private static final String ERROR_CODE = "BILL_PAY_404";

    public PaymentNotFoundException(String message) {
        super(ERROR_CODE, message);
    }
}
