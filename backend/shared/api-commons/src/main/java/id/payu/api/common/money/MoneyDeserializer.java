package id.payu.api.common.money;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * Jackson deserializer for {@link Money} objects.
 * <p>
 * Deserializes Money from JSON. Supports the following formats:
 * <ul>
 *   <li>Object format: {"amount": "100.50", "currencyCode": "IDR"}</li>
 *   <li>String format: "IDR 100.50" or "100.50 IDR"</li>
 *   <li>Number format (assumes default currency): 100.50</li>
 * </ul>
 *
 * @author PayU Digital Banking Platform
 * @since 1.0.0
 */
public class MoneyDeserializer extends JsonDeserializer<Money> {

    @Override
    public Money deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.currentToken() == JsonToken.VALUE_NULL) {
            return null;
        }

        // Handle object format: {"amount": "100.50", "currencyCode": "IDR"}
        if (p.currentToken() == JsonToken.START_OBJECT) {
            return deserializeObjectFormat(p, ctxt);
        }

        // Handle string format: "IDR 100.50"
        if (p.currentToken() == JsonToken.VALUE_STRING) {
            return deserializeStringFormat(p.getText());
        }

        // Handle number format (assumes default currency): 100.50
        if (p.currentToken() == JsonToken.VALUE_NUMBER_FLOAT ||
            p.currentToken() == JsonToken.VALUE_NUMBER_INT) {
            return Money.of(p.getDecimalValue());
        }

        throw new IOException("Cannot deserialize Money from token: " + p.currentToken());
    }

    /**
     * Deserializes Money from object format.
     */
    private Money deserializeObjectFormat(JsonParser p, DeserializationContext ctxt) throws IOException {
        BigDecimal amount = null;
        String currencyCode = Money.DEFAULT_CURRENCY_CODE;

        while (p.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = p.getCurrentName();
            p.nextToken();

            if ("amount".equals(fieldName)) {
                if (p.currentToken() == JsonToken.VALUE_STRING) {
                    amount = new BigDecimal(p.getText());
                } else if (p.currentToken() == JsonToken.VALUE_NUMBER_FLOAT ||
                           p.currentToken() == JsonToken.VALUE_NUMBER_INT) {
                    amount = p.getDecimalValue();
                }
            } else if ("currencyCode".equals(fieldName) || "currency".equals(fieldName)) {
                currencyCode = p.getText();
            }
        }

        if (amount == null) {
            throw new IOException("Amount is required for Money deserialization");
        }

        return Money.of(amount, currencyCode);
    }

    /**
     * Deserializes Money from string format.
     * Supports: "IDR 100.50", "100.50 IDR", "100.50"
     */
    private Money deserializeStringFormat(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Money string cannot be null or empty");
        }

        String trimmed = text.trim();
        String[] parts = trimmed.split("\\s+");

        if (parts.length == 1) {
            // Only amount provided, use default currency
            return Money.of(new BigDecimal(parts[0]));
        } else if (parts.length == 2) {
            // Format: "IDR 100.50" or "100.50 IDR"
            if (isCurrencyCode(parts[0])) {
                return Money.of(new BigDecimal(parts[1]), parts[0]);
            } else if (isCurrencyCode(parts[1])) {
                return Money.of(new BigDecimal(parts[0]), parts[1]);
            }
        }

        throw new IllegalArgumentException("Invalid Money format: " + text);
    }

    /**
     * Checks if the string looks like a currency code (3 letters).
     */
    private boolean isCurrencyCode(String str) {
        return str != null && str.length() == 3 && str.matches("[A-Za-z]{3}");
    }
}
