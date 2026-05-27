package id.payu.quarkus.commons.exception;

import id.payu.quarkus.commons.response.FieldError;

import java.util.List;

public class ValidationException extends BusinessException {

    private final List<FieldError> fieldErrors;

    public ValidationException(List<FieldError> fieldErrors) {
        super("VALIDATION_ERROR", "Request validation failed");
        this.fieldErrors = fieldErrors;
    }

    public ValidationException(String code, List<FieldError> fieldErrors) {
        super(code, "Request validation failed");
        this.fieldErrors = fieldErrors;
    }

    public ValidationException(String code, String message, List<FieldError> fieldErrors) {
        super(code, message);
        this.fieldErrors = fieldErrors;
    }

    public List<FieldError> getFieldErrors() {
        return fieldErrors;
    }
}
