package id.payu.integration.adapter.camel.transformer;

import id.payu.integration.domain.model.IntegrationMessage;
import id.payu.integration.domain.model.MessageType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Transformer for SWIFT MT messages.
 * Converts between SWIFT format and internal JSON format.
 */
@Component
@Slf4j
public class SwiftTransformer {

    // SWIFT field patterns
    private static final Pattern FIELD_20_PATTERN = Pattern.compile(":20:([^\\n]+)");
    private static final Pattern FIELD_32A_PATTERN = Pattern.compile(":32A:(\\d{6})([A-Z]{3})([\\d,]+)");
    private static final Pattern FIELD_50_PATTERN = Pattern.compile(":50[AK]?:([^:]+)(?::|$)");
    private static final Pattern FIELD_59_PATTERN = Pattern.compile(":59:([^:]+)(?::|$)");
    private static final Pattern FIELD_71A_PATTERN = Pattern.compile(":71A:(SHA|BEN|OUR)");

    /**
     * Transform SWIFT message to internal JSON format.
     */
    public String toInternalFormat(IntegrationMessage message) {
        log.debug("Transforming SWIFT message to internal format: {}", message.getMessageId());

        String swiftPayload = message.getRawPayload();
        Map<String, Object> internalFormat = new HashMap<>();

        internalFormat.put("messageId", message.getMessageId());
        internalFormat.put("messageType", message.getType().name());
        internalFormat.put("direction", message.getDirection().name());
        internalFormat.put("sourceSystem", message.getSourceSystem());
        internalFormat.put("targetSystem", message.getTargetSystem());

        // Parse SWIFT fields
        internalFormat.put("transactionReference", extractField(swiftPayload, FIELD_20_PATTERN));

        Map<String, Object> amountInfo = parseAmountField(swiftPayload);
        if (amountInfo != null) {
            internalFormat.put("valueDate", amountInfo.get("valueDate"));
            internalFormat.put("currency", amountInfo.get("currency"));
            internalFormat.put("amount", amountInfo.get("amount"));
        }

        internalFormat.put("orderingCustomer", extractField(swiftPayload, FIELD_50_PATTERN));
        internalFormat.put("beneficiaryCustomer", extractField(swiftPayload, FIELD_59_PATTERN));
        internalFormat.put("charges", extractField(swiftPayload, FIELD_71A_PATTERN));

        // Convert to JSON
        return toJson(internalFormat);
    }

    /**
     * Transform internal format to SWIFT message.
     */
    public String fromInternalFormat(Map<String, Object> internalData) {
        log.debug("Transforming internal format to SWIFT message");

        StringBuilder swiftMessage = new StringBuilder();
        String messageType = (String) internalData.get("messageType");

        if ("SWIFT_MT103".equals(messageType)) {
            swiftMessage.append("{1:F01PAYUIDJAAXXX0000000000}{2:I103PAYUIDJAXXXXN}{4:\n");
        } else if ("SWIFT_MT202".equals(messageType)) {
            swiftMessage.append("{1:F01PAYUIDJAAXXX0000000000}{2:I202PAYUIDJAXXXXN}{4:\n");
        }

        // Transaction Reference (Field 20)
        swiftMessage.append(":20:").append(internalData.get("transactionReference")).append("\n");

        // Amount (Field 32A)
        String valueDate = (String) internalData.get("valueDate");
        String currency = (String) internalData.get("currency");
        String amount = (String) internalData.get("amount");
        if (valueDate != null && currency != null && amount != null) {
            swiftMessage.append(":32A:")
                    .append(valueDate)
                    .append(currency)
                    .append(amount)
                    .append("\n");
        }

        // Ordering Customer (Field 50)
        String orderingCustomer = (String) internalData.get("orderingCustomer");
        if (orderingCustomer != null) {
            swiftMessage.append(":50K:").append(orderingCustomer).append("\n");
        }

        // Beneficiary Customer (Field 59)
        String beneficiary = (String) internalData.get("beneficiaryCustomer");
        if (beneficiary != null) {
            swiftMessage.append(":59:").append(beneficiary).append("\n");
        }

        // Charges (Field 71A)
        String charges = (String) internalData.get("charges");
        if (charges != null) {
            swiftMessage.append(":71A:").append(charges).append("\n");
        }

        swiftMessage.append("-}{5:{CHK:000000000000}}");

        return swiftMessage.toString();
    }

    private String extractField(String payload, Pattern pattern) {
        if (payload == null) return null;
        Matcher matcher = pattern.matcher(payload);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    private Map<String, Object> parseAmountField(String payload) {
        if (payload == null) return null;

        Matcher matcher = FIELD_32A_PATTERN.matcher(payload);
        if (matcher.find()) {
            Map<String, Object> result = new HashMap<>();

            // Parse date (YYMMDD format)
            String dateStr = matcher.group(1);
            LocalDate valueDate = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyMMdd"));
            result.put("valueDate", valueDate.toString());

            // Currency
            result.put("currency", matcher.group(2));

            // Amount (replace comma with dot for decimal)
            String amountStr = matcher.group(3).replace(",", ".");
            result.put("amount", new BigDecimal(amountStr).toPlainString());

            return result;
        }
        return null;
    }

    private String toJson(Map<String, Object> data) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (!first) json.append(",");
            first = false;

            json.append("\"").append(entry.getKey()).append("\":");
            Object value = entry.getValue();

            if (value == null) {
                json.append("null");
            } else if (value instanceof String) {
                json.append("\"").append(escapeJson((String) value)).append("\"");
            } else if (value instanceof Number || value instanceof Boolean) {
                json.append(value);
            } else {
                json.append("\"").append(escapeJson(value.toString())).append("\"");
            }
        }

        json.append("}");
        return json.toString();
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
