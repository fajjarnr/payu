package id.payu.api.common.validation;

import id.payu.api.common.constant.ApiConstants;

import java.util.regex.Pattern;

/**
 * Validator for PayU account numbers.
 */
public class AccountNumberValidator extends AbstractRegexValidator<ValidAccountNumber> {

    private static final Pattern ACCOUNT_NUMBER_PATTERN = Pattern.compile(ApiConstants.ACCOUNT_NUMBER_PATTERN);

    @Override
    protected boolean isValid(String value) {
        return ACCOUNT_NUMBER_PATTERN.matcher(value).matches();
    }
}
