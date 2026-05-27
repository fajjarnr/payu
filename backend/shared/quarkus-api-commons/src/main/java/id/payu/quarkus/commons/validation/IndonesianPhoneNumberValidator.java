package id.payu.quarkus.commons.validation;

import id.payu.quarkus.commons.constant.ApiConstants;

import java.util.regex.Pattern;

public class IndonesianPhoneNumberValidator extends AbstractRegexValidator<ValidIndonesianPhoneNumber> {

    private static final Pattern PHONE_PATTERN = Pattern.compile(ApiConstants.PHONE_NUMBER_PATTERN);
    private static final Pattern PHONE_ALT_PATTERN = Pattern.compile(ApiConstants.PHONE_NUMBER_PATTERN_ALT);
    private static final Pattern MOBILE_PATTERN = Pattern.compile(ApiConstants.MOBILE_NUMBER_PATTERN);

    @Override
    protected boolean isValid(String value) {
        return PHONE_PATTERN.matcher(value).matches()
                || PHONE_ALT_PATTERN.matcher(value).matches()
                || MOBILE_PATTERN.matcher(value).matches();
    }
}
