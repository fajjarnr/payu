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
 * Validates Indonesian NIK (Nomor Induk Kependudukan) format.
 * NIK must be exactly 16 digits.
 */
@Target({FIELD, PARAMETER})
@Retention(RUNTIME)
@Constraint(validatedBy = NIKValidator.class)
@Documented
public @interface ValidNIK {

    String message() default "NIK must be exactly 16 digits";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
