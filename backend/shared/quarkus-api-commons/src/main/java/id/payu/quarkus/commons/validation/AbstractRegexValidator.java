package id.payu.quarkus.commons.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public abstract class AbstractRegexValidator<A extends java.lang.annotation.Annotation>
        implements ConstraintValidator<A, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return true;
        }
        return isValid(value);
    }

    protected abstract boolean isValid(String value);
}
