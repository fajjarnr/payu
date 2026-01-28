package id.payu.api.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Abstract base class for regex-based validators.
 * Provides common validation logic for pattern-based validation.
 */
public abstract class AbstractRegexValidator<A extends java.lang.annotation.Annotation>
        implements ConstraintValidator<A, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return true; // Let @NotNull/@NotBlank handle null/empty
        }
        return isValid(value);
    }

    /**
     * Validates if the value matches the required pattern.
     *
     * @param value The value to validate (guaranteed to be non-null and non-empty)
     * @return true if valid, false otherwise
     */
    protected abstract boolean isValid(String value);
}
