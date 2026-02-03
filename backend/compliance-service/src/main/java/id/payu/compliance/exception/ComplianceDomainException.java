package id.payu.compliance.exception;

import id.payu.api.common.exception.BusinessException;

/**
 * Base exception for Compliance Service domain errors.
 *
 * Error Code Structure: COMPLIANCE_[CATEGORY]_[SPECIFIC]
 *
 * Categories:
 * - VAL: Validation errors
 * - BUS: Business rule violations
 * - EXT: External service errors
 * - SYS: System/technical errors
 */
public class ComplianceDomainException extends BusinessException {

    public ComplianceDomainException(String message) {
        super("COMPLIANCE_GENERIC_ERROR", message);
    }

    public ComplianceDomainException(String message, Throwable cause) {
        super("COMPLIANCE_GENERIC_ERROR", message, cause);
    }

    public ComplianceDomainException(String code, String message) {
        super(code, message);
    }

    public ComplianceDomainException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }

    public ComplianceDomainException(String code, String message, Object... args) {
        super(code, message, args);
    }
}
