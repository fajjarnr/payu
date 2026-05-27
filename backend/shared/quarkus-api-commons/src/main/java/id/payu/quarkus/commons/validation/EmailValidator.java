package id.payu.quarkus.commons.validation;

import id.payu.quarkus.commons.constant.ApiConstants;

import java.util.regex.Pattern;

public class EmailValidator extends AbstractRegexValidator<ValidEmail> {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(ApiConstants.EMAIL_PATTERN);

    @Override
    protected boolean isValid(String value) {
        return EMAIL_PATTERN.matcher(value).matches();
    }
}
