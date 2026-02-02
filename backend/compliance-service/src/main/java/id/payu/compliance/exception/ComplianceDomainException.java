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
public abstract class ComplianceDomainException extends BusinessException {

    protected ComplianceDomainException(String code, String message) {
        super(code, message);
    }

    protected ComplianceDomainException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }

    protected ComplianceDomainException(String code, String message, Object... args) {
        super(code, message, args);
    }
}
