package id.payu.security.masking;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.regex.Pattern;

/**
 * Logback PatternLayout that masks sensitive data (PII) in log output.
 * <p>
 * BUG-SHARED-001 FIX: Previously this was a {@code Filter<ILoggingEvent>} which cannot modify
 * log messages — the masking result was computed but discarded (complete no-op).
 * Converted to {@link PatternLayout} which owns the formatted output and can apply
 * regex replacements before the appender writes to its destination.
 * <p>
 * Usage in logback.xml:
 * <pre>{@code
 * <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
 *   <encoder class="ch.qos.logback.core.encoder.LayoutWrappingEncoder">
 *     <layout class="id.payu.security.masking.LogbackMaskingFilter">
 *       <pattern>%d{ISO8601} %-5level [%thread] %logger{36} - %msg%n</pattern>
 *     </layout>
 *   </encoder>
 * </appender>
 * }</pre>
 *
 * @author PayU Digital Banking Platform
 * @since 1.0.0
 */
public class LogbackMaskingFilter extends PatternLayout {

    public LogbackMaskingFilter() {
        setPattern("%msg%n");
    }

    // Patterns for sensitive data
    private static final Pattern NIK_PATTERN = Pattern.compile("(\\d{3})\\d{10}(\\d{3})");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("(\\w{1})[\\w.]+@([\\w.]+)");
    private static final Pattern PHONE_PATTERN = Pattern.compile("(\\d{3})\\d{4,}(\\d{3})");
    private static final Pattern CARD_PATTERN = Pattern.compile("(\\d{4})\\d{8,}(\\d{4})");
    private static final Pattern SSN_PATTERN = Pattern.compile("(\\d{3})-?(\\d{2})-?(\\d{4})");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("(\"password\"\\s*:\\s*\")([^\"]+)(\")", Pattern.CASE_INSENSITIVE);
    private static final Pattern TOKEN_PATTERN = Pattern.compile("(\"token\"\\s*:\\s*\")([^\"]{20,})(\")", Pattern.CASE_INSENSITIVE);
    private static final Pattern API_KEY_PATTERN = Pattern.compile("(\"api[_-]?key\"\\s*:\\s*\")([^\"]{20,})(\")", Pattern.CASE_INSENSITIVE);

    /**
     * Formats the logging event using the parent PatternLayout, then applies PII masking
     * to the resulting string before it reaches the appender.
     */
    @Override
    public String doLayout(ILoggingEvent event) {
        String formatted = super.doLayout(event);
        return maskSensitiveData(formatted);
    }

    /**
     * Mask sensitive data in a string.
     * <p>
     * Replaces PII patterns (NIK, emails, phone numbers, card numbers, SSN,
     * passwords, tokens, API keys) with partially masked equivalents.
     *
     * @param input the raw string potentially containing PII
     * @return the masked string with sensitive data redacted
     */
    public String maskSensitiveData(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        String result = input;

        // Mask passwords (full redaction)
        result = PASSWORD_PATTERN.matcher(result).replaceAll("$1****$3");

        // Mask tokens (full redaction)
        result = TOKEN_PATTERN.matcher(result).replaceAll("$1****$3");

        // Mask API keys (full redaction)
        result = API_KEY_PATTERN.matcher(result).replaceAll("$1****$3");

        // Mask emails (keep first char + domain)
        result = EMAIL_PATTERN.matcher(result).replaceAll("$1***@$2");

        // Mask NIK (16-digit Indonesian ID: keep first 3 + last 3)
        result = NIK_PATTERN.matcher(result).replaceAll("$1**********$2");

        // Mask phone numbers (keep first 3 + last 3)
        result = PHONE_PATTERN.matcher(result).replaceAll("$1****$2");

        // Mask credit card numbers (keep first 4 + last 4)
        result = CARD_PATTERN.matcher(result).replaceAll("$1********$2");

        // Mask SSN
        result = SSN_PATTERN.matcher(result).replaceAll("$1-**-****");

        return result;
    }
}
