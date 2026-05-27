package id.payu.quarkus.commons.validation;

import id.payu.quarkus.commons.constant.ApiConstants;

import java.util.regex.Pattern;

public class NIKValidator extends AbstractRegexValidator<ValidNIK> {

    private static final Pattern NIK_PATTERN = Pattern.compile(ApiConstants.NIK_PATTERN);

    @Override
    protected boolean isValid(String value) {
        return NIK_PATTERN.matcher(value).matches();
    }
}
