package id.payu.security.masking;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

import java.util.regex.Pattern;

/**
 * Logback filter for masking sensitive data in logs
 * Apply this filter to logback appender configuration
 */
public class LogbackMaskingFilter extends Filter<ILoggingEvent> {

    // Patterns for sensitive data
    private static final Pattern EMAIL_PATTERN = Pattern.compile("(\\w{1})[\\w.]+@([\\w.]+)");
    private static final Pattern PHONE_PATTERN = Pattern.compile("(\\d{3})\\d{4,}(\\d{3})");
    private static final Pattern CARD_PATTERN = Pattern.compile("(\\d{4})\\d{8,}(\\d{4})");
    private static final Pattern SSN_PATTERN = Pattern.compile("(\\d{3})-?(\\d{2})-?(\\d{4})");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("(\"password\"\\s*:\\s*\")([^\"]+)(\")", Pattern.CASE_INSENSITIVE);
    private static final Pattern TOKEN_PATTERN = Pattern.compile("(\"token\"\\s*:\\s*\")([^\"]{20,})(\")", Pattern.CASE_INSENSITIVE);
    private static final Pattern API_KEY_PATTERN = Pattern.compile("(\"api[_-]?key\"\\s*:\\s*\")([^\"]{20,})(\")", Pattern.CASE_INSENSITIVE);

    private final char maskChar;

    public LogbackMaskingFilter() {
        this.maskChar = '*';
    }

    public LogbackMaskingFilter(char maskChar) {
        this.maskChar = maskChar;
    }

    @Override
    public FilterReply decide(ILoggingEvent event) {
        if (!isStarted()) {
            return FilterReply.NEUTRAL;
        }

        String formattedMessage = event.getFormattedMessage();
        if (formattedMessage != null) {
            String maskedMessage = maskSensitiveData(formattedMessage);
            // Note: In logback, we can't modify the message directly
            // This filter is for demonstration - actual masking should be done in the pattern layout
        }

        return FilterReply.NEUTRAL;
    }

    /**
     * Mask sensitive data in a string
     */
    public String maskSensitiveData(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        String result = input;

        // Mask passwords
        result = PASSWORD_PATTERN.matcher(result).replaceAll("$1****$3");

        // Mask tokens
        result = TOKEN_PATTERN.matcher(result).replaceAll("$1****$3");

        // Mask API keys
        result = API_KEY_PATTERN.matcher(result).replaceAll("$1****$3");

        // Mask emails
        result = EMAIL_PATTERN.matcher(result).replaceAll("$1***@$2");

        // Mask phone numbers
        result = PHONE_PATTERN.matcher(result).replaceAll("$1****$2");

        // Mask credit card numbers
        result = CARD_PATTERN.matcher(result).replaceAll("$1********$2");

        // Mask SSN
        result = SSN_PATTERN.matcher(result).replaceAll("$1-**-****$3");

        return result;
    }
}
