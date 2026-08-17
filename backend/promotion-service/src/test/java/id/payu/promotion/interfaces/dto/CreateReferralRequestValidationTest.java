package id.payu.promotion.interfaces.dto;

import id.payu.promotion.domain.ReferralRewardType;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CreateReferralRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsNegativeOrOverScaleRewards() {
        CreateReferralRequest request = new CreateReferralRequest(
            "account-1", new BigDecimal("-0.0001"), new BigDecimal("1.23456"),
            ReferralRewardType.CASHBACK, LocalDateTime.now().plusDays(1));

        assertEquals(2, validator.validate(request).size());
    }
}
