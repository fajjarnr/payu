package id.payu.api.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;

/**
 * Abstract base class for amount validators.
 */
public abstract class AbstractAmountValidator<A extends java.lang.annotation.Annotation>
        implements ConstraintValidator<A, BigDecimal> {

    @Override
    public boolean isValid(BigDecimal value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Let @NotNull handle null
        }
        return isValidAmount(value, context);
    }

    /**
     * Validates if the amount is within the allowed range.
     *
     * @param value   The amount to validate (guaranteed to be non-null)
     * @param context The constraint validator context
     * @return true if valid, false otherwise
     */
    protected abstract boolean isValidAmount(BigDecimal value, ConstraintValidatorContext context);
}
