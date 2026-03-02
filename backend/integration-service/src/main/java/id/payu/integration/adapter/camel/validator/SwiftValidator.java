package id.payu.integration.adapter.camel.validator;

import id.payu.integration.domain.model.IntegrationMessage;
import id.payu.integration.domain.model.MessageType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Validator for SWIFT MT messages.
 * Validates message structure and required fields.
 */
@Component
@Slf4j
public class SwiftValidator {

    // SWIFT message block patterns
    private static final Pattern BASIC_HEADER_PATTERN = Pattern.compile("\\{1:[FAL]\\d{2}\\w{12}\\d{4}\\d{6}\\}");
    private static final Pattern APPLICATION_HEADER_PATTERN = Pattern.compile("\\{2:[IEO]\\d{3}\\w{12}[\\dN]\\d{4}\\d{6}\\}");
    private static final Pattern USER_HEADER_PATTERN = Pattern.compile("\\{3:\\{\\d{3}:[^}]+\\}*\\}");
    private static final Pattern TEXT_BLOCK_PATTERN = Pattern.compile("\\{4:[^}]+\\}");
    private static final Pattern TRAILER_PATTERN = Pattern.compile("\\{5:\\{[^}]+\\}\\}");

    // Required field patterns for MT103
    private static final Pattern FIELD_20_PATTERN = Pattern.compile(":20:[^\n]+");
    private static final Pattern FIELD_23B_PATTERN = Pattern.compile(":23B:[^\n]+");
    private static final Pattern FIELD_32A_PATTERN = Pattern.compile(":32A:\\d{6}[A-Z]{3}[\\d,]+");
    private static final Pattern FIELD_50_PATTERN = Pattern.compile(":50[AKF]?:[^\n]+");
    private static final Pattern FIELD_59_PATTERN = Pattern.compile(":59[AF]?:[^\n]+");
    private static final Pattern FIELD_71A_PATTERN = Pattern.compile(":71A:(SHA|BEN|OUR)");

    /**
     * Validate SWIFT message structure and content.
     */
    public ValidationResult validate(IntegrationMessage message) {
        log.debug("Validating SWIFT message: {}", message.getMessageId());

        List<String> errors = new ArrayList<>();
        String payload = message.getRawPayload();

        if (payload == null || payload.trim().isEmpty()) {
            errors.add("Message payload is empty");
            return new ValidationResult(false, errors);
        }

        // Validate message blocks
        validateMessageBlocks(payload, errors);

        // Validate required fields based on message type
        validateRequiredFields(message.getType(), payload, errors);

        // Validate field formats
        validateFieldFormats(payload, errors);

        boolean valid = errors.isEmpty();
        if (valid) {
            log.debug("SWIFT message {} is valid", message.getMessageId());
        } else {
            log.warn("SWIFT message {} validation failed: {}", message.getMessageId(), errors);
        }

        return new ValidationResult(valid, errors);
    }

    /**
     * Quick validation for routing purposes.
     */
    public boolean isValidFormat(String swiftMessage) {
        if (swiftMessage == null || swiftMessage.trim().isEmpty()) {
            return false;
        }

        // Check for basic SWIFT structure
        return swiftMessage.contains("{1:") && swiftMessage.contains("{2:") && swiftMessage.contains("{4:");
    }

    /**
     * Detect SWIFT message type from content.
     */
    public String detectMessageType(String swiftMessage) {
        if (swiftMessage == null) return "UNKNOWN";

        // Look for message type in application header (block 2)
        java.util.regex.Matcher matcher = Pattern.compile("\\{2:[IEO](\\d{3})").matcher(swiftMessage);
        if (matcher.find()) {
            return "MT" + matcher.group(1);
        }

        return "UNKNOWN";
    }

    private void validateMessageBlocks(String payload, List<String> errors) {
        // Basic header (block 1) - optional for some message types
        if (!BASIC_HEADER_PATTERN.matcher(payload).find() && !payload.startsWith("{4:")) {
            errors.add("Missing or invalid basic header (block 1)");
        }

        // Application header (block 2) - required
        if (!APPLICATION_HEADER_PATTERN.matcher(payload).find()) {
            errors.add("Missing or invalid application header (block 2)");
        }

        // Text block (block 4) - required
        if (!TEXT_BLOCK_PATTERN.matcher(payload).find() && !payload.contains(":20:")) {
            errors.add("Missing or invalid text block (block 4)");
        }
    }

    private void validateRequiredFields(MessageType messageType, String payload, List<String> errors) {
        switch (messageType) {
            case SWIFT_MT103 -> validateMT103Fields(payload, errors);
            case SWIFT_MT202 -> validateMT202Fields(payload, errors);
            case SWIFT_MT940 -> validateMT940Fields(payload, errors);
            default -> log.warn("Unknown message type for validation: {}", messageType);
        }
    }

    private void validateMT103Fields(String payload, List<String> errors) {
        // Field 20 - Transaction Reference Number (mandatory)
        if (!FIELD_20_PATTERN.matcher(payload).find()) {
            errors.add("Missing required field 20 (Transaction Reference)");
        }

        // Field 23B - Bank Operation Code (mandatory)
        if (!FIELD_23B_PATTERN.matcher(payload).find()) {
            errors.add("Missing required field 23B (Bank Operation Code)");
        }

        // Field 32A - Value Date/Currency/Interbank Settled Amount (mandatory)
        if (!FIELD_32A_PATTERN.matcher(payload).find()) {
            errors.add("Missing or invalid field 32A (Value Date/Currency/Amount)");
        }

        // Field 50a - Ordering Customer (mandatory)
        if (!FIELD_50_PATTERN.matcher(payload).find()) {
            errors.add("Missing required field 50 (Ordering Customer)");
        }

        // Field 59a - Beneficiary Customer (mandatory)
        if (!FIELD_59_PATTERN.matcher(payload).find()) {
            errors.add("Missing required field 59 (Beneficiary Customer)");
        }

        // Field 71A - Details of Charges (mandatory)
        if (!FIELD_71A_PATTERN.matcher(payload).find()) {
            errors.add("Missing required field 71A (Details of Charges)");
        }
    }

    private void validateMT202Fields(String payload, List<String> errors) {
        // Field 20 - Transaction Reference Number (mandatory)
        if (!FIELD_20_PATTERN.matcher(payload).find()) {
            errors.add("Missing required field 20 (Transaction Reference)");
        }

        // Field 21 - Related Reference (mandatory)
        if (!Pattern.compile(":21:[^\n]+").matcher(payload).find()) {
            errors.add("Missing required field 21 (Related Reference)");
        }

        // Field 32A - Value Date/Currency/Interbank Settled Amount (mandatory)
        if (!FIELD_32A_PATTERN.matcher(payload).find()) {
            errors.add("Missing or invalid field 32A (Value Date/Currency/Amount)");
        }

        // Field 58a - Beneficiary Institution (mandatory)
        if (!Pattern.compile(":58[AD]?:[^\n]+").matcher(payload).find()) {
            errors.add("Missing required field 58 (Beneficiary Institution)");
        }
    }

    private void validateMT940Fields(String payload, List<String> errors) {
        // Field 20 - Transaction Reference Number (mandatory)
        if (!FIELD_20_PATTERN.matcher(payload).find()) {
            errors.add("Missing required field 20 (Transaction Reference)");
        }

        // Field 25 - Account Identification (mandatory)
        if (!Pattern.compile(":25:[^\n]+").matcher(payload).find()) {
            errors.add("Missing required field 25 (Account Identification)");
        }

        // Field 28C - Statement Number/Sequence Number (mandatory)
        if (!Pattern.compile(":28C:[^\n]+").matcher(payload).find()) {
            errors.add("Missing required field 28C (Statement Number)");
        }
    }

    private void validateFieldFormats(String payload, List<String> errors) {
        // Validate date format in field 32A (YYMMDD)
        java.util.regex.Matcher dateMatcher = Pattern.compile(":32A:(\\d{6})").matcher(payload);
        if (dateMatcher.find()) {
            String dateStr = dateMatcher.group(1);
            try {
                int year = Integer.parseInt(dateStr.substring(0, 2));
                int month = Integer.parseInt(dateStr.substring(2, 4));
                int day = Integer.parseInt(dateStr.substring(4, 6));

                if (month < 1 || month > 12 || day < 1 || day > 31) {
                    errors.add("Invalid date format in field 32A: " + dateStr);
                }
            } catch (NumberFormatException e) {
                errors.add("Invalid date format in field 32A: " + dateStr);
            }
        }

        // Validate currency code (3 uppercase letters)
        java.util.regex.Matcher currencyMatcher = Pattern.compile(":32A:\\d{6}([A-Z]{3})").matcher(payload);
        if (currencyMatcher.find()) {
            String currency = currencyMatcher.group(1);
            // Basic check for valid currency format
            if (!currency.matches("[A-Z]{3}")) {
                errors.add("Invalid currency code in field 32A: " + currency);
            }
        }
    }

    /**
     * Result of validation.
     */
    public record ValidationResult(boolean valid, List<String> errors) {
        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        public String getErrorMessage() {
            return String.join("; ", errors);
        }
    }
}
