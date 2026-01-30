package id.payu.api.common.money;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * Jackson serializer for {@link Money} objects.
 * <p>
 * Serializes Money to a JSON object with the following structure:
 * <pre>
 * {
 *   "amount": "100.50",
 *   "currencyCode": "IDR",
 *   "formatted": "IDR 100.50"
 * }
 * </pre>
 *
 * @author PayU Digital Banking Platform
 * @since 1.0.0
 */
public class MoneySerializer extends JsonSerializer<Money> {

    @Override
    public void serialize(Money value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }

        gen.writeStartObject();
        gen.writeStringField("amount", value.getAmount().toPlainString());
        gen.writeStringField("currencyCode", value.getCurrencyCode());
        gen.writeStringField("formatted", formatMoney(value));
        gen.writeEndObject();
    }

    /**
     * Formats the money for display purposes.
     *
     * @param money the money to format
     * @return formatted string
     */
    private String formatMoney(Money money) {
        return String.format("%s %s", money.getCurrencyCode(), money.getAmount().toPlainString());
    }
}
