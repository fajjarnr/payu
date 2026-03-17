package id.payu.api.common.validation;

import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

/**
 * Validator for monetary amounts.
 */
@Slf4j
public class AmountValidator extends AbstractAmountValidator<ValidAmount> {

    private long min;
    private long max;
    private String currency;

    @Override
    public void initialize(ValidAmount constraintAnnotation) {
        this.min = constraintAnnotation.min();
        this.max = constraintAnnotation.max();
        this.currency = constraintAnnotation.currency();
    }

    @Override
    protected boolean isValidAmount(BigDecimal value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Let @NotNull handle null
        }

        // Use BigDecimal.compareTo for lossless comparison — avoids longValue() truncation
        // where e.g. 1000.99 would be truncated to 1000 and incorrectly pass validation
        BigDecimal minAmount = BigDecimal.valueOf(min).movePointLeft(2);
        BigDecimal maxAmount = BigDecimal.valueOf(max).movePointLeft(2);

        if (value.compareTo(minAmount) < 0) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    String.format("Amount must be at least %,.2f %s", min / 100.0, currency)
            ).addConstraintViolation();
            return false;
        }

        if (max != Long.MAX_VALUE && value.compareTo(maxAmount) > 0) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    String.format("Amount must not exceed %,.2f %s", max / 100.0, currency)
            ).addConstraintViolation();
            return false;
        }

        return true;
    }
}
