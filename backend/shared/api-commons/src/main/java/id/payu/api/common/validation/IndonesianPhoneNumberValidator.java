package id.payu.api.common.validation;

import id.payu.api.common.constant.ApiConstants;

import java.util.regex.Pattern;

/**
 * Validator for Indonesian phone numbers.
 */
public class IndonesianPhoneNumberValidator extends AbstractRegexValidator<ValidIndonesianPhoneNumber> {

    private static final Pattern INTERNATIONAL_PATTERN = Pattern.compile(ApiConstants.PHONE_NUMBER_PATTERN);
    private static final Pattern INTERNATIONAL_PATTERN_ALT = Pattern.compile(ApiConstants.PHONE_NUMBER_PATTERN_ALT);
    private static final Pattern MOBILE_PATTERN = Pattern.compile(ApiConstants.MOBILE_NUMBER_PATTERN);

    private boolean allowMobileFormat;

    @Override
    public void initialize(ValidIndonesianPhoneNumber constraintAnnotation) {
        this.allowMobileFormat = constraintAnnotation.allowMobileFormat();
    }

    @Override
    protected boolean isValid(String value) {
        if (allowMobileFormat) {
            return INTERNATIONAL_PATTERN.matcher(value).matches()
                    || INTERNATIONAL_PATTERN_ALT.matcher(value).matches()
                    || MOBILE_PATTERN.matcher(value).matches();
        }
        return INTERNATIONAL_PATTERN.matcher(value).matches()
                || INTERNATIONAL_PATTERN_ALT.matcher(value).matches();
    }
}
