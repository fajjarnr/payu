package id.payu.api.common.validation;

import id.payu.api.common.constant.ApiConstants;

import java.util.regex.Pattern;

/**
 * Validator for email addresses.
 */
public class EmailValidator extends AbstractRegexValidator<ValidEmail> {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(ApiConstants.EMAIL_PATTERN);

    private boolean indonesianDomainsOnly;

    @Override
    public void initialize(ValidEmail constraintAnnotation) {
        this.indonesianDomainsOnly = constraintAnnotation.indonesianDomainsOnly();
    }

    @Override
    protected boolean isValid(String value) {
        if (!EMAIL_PATTERN.matcher(value).matches()) {
            return false;
        }

        if (indonesianDomainsOnly) {
            String domain = value.substring(value.indexOf('@') + 1).toLowerCase();
            // Common Indonesian email providers
            return domain.endsWith(".id") ||
                    domain.equals("gmail.com") ||
                    domain.equals("yahoo.com") ||
                    domain.equals("outlook.com") ||
                    domain.equals("icloud.com");
        }

        return true;
    }
}
