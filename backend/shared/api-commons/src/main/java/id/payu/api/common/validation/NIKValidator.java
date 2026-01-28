package id.payu.api.common.validation;

import id.payu.api.common.constant.ApiConstants;

import java.util.regex.Pattern;

/**
 * Validator for Indonesian NIK (Nomor Induk Kependudukan).
 */
public class NIKValidator extends AbstractRegexValidator<ValidNIK> {

    private static final Pattern NIK_PATTERN = Pattern.compile(ApiConstants.NIK_PATTERN);

    @Override
    protected boolean isValid(String value) {
        return NIK_PATTERN.matcher(value).matches();
    }
}
