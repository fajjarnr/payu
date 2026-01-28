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
 * Validates PayU account number format.
 * Account number must be exactly 10 digits.
 */
@Target({FIELD, PARAMETER})
@Retention(RUNTIME)
@Constraint(validatedBy = AccountNumberValidator.class)
@Documented
public @interface ValidAccountNumber {

    String message() default "Account number must be exactly 10 digits";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
