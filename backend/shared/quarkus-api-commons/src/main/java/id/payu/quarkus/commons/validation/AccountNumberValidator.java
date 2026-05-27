package id.payu.quarkus.commons.validation;

import id.payu.quarkus.commons.constant.ApiConstants;

import java.util.regex.Pattern;

public class AccountNumberValidator extends AbstractRegexValidator<ValidAccountNumber> {

    private static final Pattern ACCOUNT_PATTERN = Pattern.compile(ApiConstants.ACCOUNT_NUMBER_PATTERN);

    @Override
    protected boolean isValid(String value) {
        return ACCOUNT_PATTERN.matcher(value).matches();
    }
}
