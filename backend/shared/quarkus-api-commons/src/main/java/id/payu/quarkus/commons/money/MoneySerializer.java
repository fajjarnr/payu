package id.payu.quarkus.commons.money;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.math.BigDecimal;

public class MoneySerializer extends JsonSerializer<Money> {

    @Override
    public void serialize(Money money, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("currency", money.getCurrencyCode());
        gen.writeNumberField("amount", money.getAmount());
        gen.writeStringField("formatted", formatMoney(money));
        gen.writeEndObject();
    }

    private String formatMoney(Money money) {
        if ("IDR".equals(money.getCurrencyCode())) {
            return "Rp " + formatIndonesianCurrency(money.getAmount());
        }
        return money.getCurrencyCode() + " " + money.getAmount().toPlainString();
    }

    private String formatIndonesianCurrency(BigDecimal amount) {
        String[] parts = amount.toPlainString().split("\\.");
        String integerPart = parts[0];
        StringBuilder formatted = new StringBuilder();
        int count = 0;
        for (int i = integerPart.length() - 1; i >= 0; i--) {
            if (count > 0 && count % 3 == 0) {
                formatted.insert(0, '.');
            }
            formatted.insert(0, integerPart.charAt(i));
            count++;
        }
        if (parts.length > 1 && !"00".equals(parts[1])) {
            formatted.append(',').append(parts[1]);
        }
        return formatted.toString();
    }
}
