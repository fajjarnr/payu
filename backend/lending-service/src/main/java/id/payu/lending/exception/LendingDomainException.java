package id.payu.lending.exception;

import id.payu.api.common.exception.BusinessException;

/**
 * Base exception for Lending Service domain errors.
 *
 * Error Code Structure: LENDING_[CATEGORY]_[SPECIFIC]
 *
 * Categories:
 * - VAL: Validation errors
 * - BUS: Business rule violations
 * - EXT: External service errors
 * - SYS: System/technical errors
 */
public abstract class LendingDomainException extends BusinessException {

    protected LendingDomainException(String code, String message) {
        super(code, message);
    }

    protected LendingDomainException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }

    protected LendingDomainException(String code, String message, Object... args) {
        super(code, message, args);
    }
}
