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
 * Validates Indonesian phone number format.
 * Accepts formats:
 * - +628123456789
 * - 628123456789
 * - 08123456789
 */
@Target({FIELD, PARAMETER})
@Retention(RUNTIME)
@Constraint(validatedBy = IndonesianPhoneNumberValidator.class)
@Documented
public @interface ValidIndonesianPhoneNumber {

    String message() default "Invalid Indonesian phone number format. Use +628xxxxxxxxx or 08xxxxxxxxx";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * Whether to allow the 08xx format without country code.
     */
    boolean allowMobileFormat() default true;
}
