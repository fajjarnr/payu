package id.payu.quarkus.commons.money;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.math.BigDecimal;

public class MoneyDeserializer extends JsonDeserializer<Money> {

    @Override
    public Money deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);

        if (node.isTextual()) {
            return Money.of(node.asText());
        }

        if (node.isNumber()) {
            return Money.of(node.decimalValue());
        }

        if (node.isObject()) {
            BigDecimal amount = node.has("amount")
                    ? node.get("amount").decimalValue()
                    : null;
            String currency = node.has("currency")
                    ? node.get("currency").asText()
                    : Money.DEFAULT_CURRENCY_CODE;

            if (amount == null) {
                throw new IllegalArgumentException("Money object must contain 'amount' field");
            }
            return Money.of(amount, currency);
        }

        throw new IllegalArgumentException("Cannot deserialize Money from: " + node.getNodeType());
    }
}
