package id.payu.api.common.exception;

import id.payu.api.common.response.FieldError;

import java.util.List;

/**
 * Exception thrown when request validation fails.
 * Used for field-level validation errors with specific field information.
 */
public class ValidationException extends BusinessException {

    private final List<FieldError> fieldErrors;

    /**
     * Creates a ValidationException with field errors.
     *
     * @param fieldErrors List of field-specific validation errors
     */
    public ValidationException(List<FieldError> fieldErrors) {
        super("VALIDATION_ERROR", "Request validation failed");
        this.fieldErrors = fieldErrors;
    }

    /**
     * Creates a ValidationException with code and field errors.
     *
     * @param code        Unique error code
     * @param fieldErrors List of field-specific validation errors
     */
    public ValidationException(String code, List<FieldError> fieldErrors) {
        super(code, "Request validation failed");
        this.fieldErrors = fieldErrors;
    }

    /**
     * Creates a ValidationException with code, message, and field errors.
     *
     * @param code        Unique error code
     * @param message     Human-readable error message
     * @param fieldErrors List of field-specific validation errors
     */
    public ValidationException(String code, String message, List<FieldError> fieldErrors) {
        super(code, message);
        this.fieldErrors = fieldErrors;
    }

    public List<FieldError> getFieldErrors() {
        return fieldErrors;
    }
}
