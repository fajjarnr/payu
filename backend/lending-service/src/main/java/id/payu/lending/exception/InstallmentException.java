package id.payu.lending.exception;

/**
 * Exception for installment/PayLater checkout domain errors.
 * 
 * Error codes:
 * - INST_001: Insufficient PayLater credit for tenor options
 * - INST_002: Insufficient PayLater credit for checkout
 * - INST_003: Invalid tenor
 * - INST_004: Duplicate external order
 * - INST_005: Checkout not found
 * - INST_006: No PayLater account found
 * - INST_007: PayLater account not active
 */
public class InstallmentException extends LendingDomainException {

    public InstallmentException(String code, String message) {
        super(code, message);
    }

    public InstallmentException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
