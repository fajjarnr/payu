package id.payu.api.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Validates monetary amount for transactions.
 * Ensures amount is positive and within allowed range.
 */
@Target({FIELD, PARAMETER})
@Retention(RUNTIME)
@Constraint(validatedBy = AmountValidator.class)
@Documented
public @interface ValidAmount {

    String message() default "Invalid amount";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * Minimum allowed amount (inclusive).
     */
    long min() default 1000;

    /**
     * Maximum allowed amount (inclusive).
     * Use Long.MAX_VALUE for no upper limit.
     */
    long max() default 250000000;

    /**
     * Currency code for the amount.
     */
    String currency() default "IDR";
}
