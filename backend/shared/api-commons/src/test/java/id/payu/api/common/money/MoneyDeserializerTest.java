package id.payu.api.common.money;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link MoneyDeserializer}.
 *
 * @author PayU Digital Banking Platform
 * @since 1.0.0
 */
@DisplayName("MoneyDeserializer Tests")
class MoneyDeserializerTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Money.class, new MoneyDeserializer());
        objectMapper.registerModule(module);
    }

    @Test
    @DisplayName("Should deserialize Money from object format")
    void shouldDeserializeMoneyFromObjectFormat() throws JsonProcessingException {
        String json = "{\"amount\":\"100.50\",\"currencyCode\":\"IDR\"}";

        Money money = objectMapper.readValue(json, Money.class);

        assertThat(money.getAmount()).isEqualByComparingTo(new BigDecimal("100.50"));
        assertThat(money.getCurrencyCode()).isEqualTo("IDR");
    }

    @Test
    @DisplayName("Should deserialize Money with 'currency' field")
    void shouldDeserializeMoneyWithCurrencyField() throws JsonProcessingException {
        String json = "{\"amount\":\"100.50\",\"currency\":\"USD\"}";

        Money money = objectMapper.readValue(json, Money.class);

        assertThat(money.getAmount()).isEqualByComparingTo(new BigDecimal("100.50"));
        assertThat(money.getCurrencyCode()).isEqualTo("USD");
    }

    @Test
    @DisplayName("Should deserialize Money with numeric amount")
    void shouldDeserializeMoneyWithNumericAmount() throws JsonProcessingException {
        String json = "{\"amount\":100.50,\"currencyCode\":\"IDR\"}";

        Money money = objectMapper.readValue(json, Money.class);

        assertThat(money.getAmount()).isEqualByComparingTo(new BigDecimal("100.50"));
    }

    @Test
    @DisplayName("Should deserialize Money with default currency when only amount provided")
    void shouldDeserializeWithDefaultCurrency() throws JsonProcessingException {
        String json = "{\"amount\":\"100.50\"}";

        Money money = objectMapper.readValue(json, Money.class);

        assertThat(money.getAmount()).isEqualByComparingTo(new BigDecimal("100.50"));
        assertThat(money.getCurrencyCode()).isEqualTo("IDR");
    }

    @Test
    @DisplayName("Should deserialize Money from string format with currency first")
    void shouldDeserializeFromStringFormatCurrencyFirst() throws JsonProcessingException {
        String json = "\"IDR 100.50\"";

        Money money = objectMapper.readValue(json, Money.class);

        assertThat(money.getAmount()).isEqualByComparingTo(new BigDecimal("100.50"));
        assertThat(money.getCurrencyCode()).isEqualTo("IDR");
    }

    @Test
    @DisplayName("Should deserialize Money from string format with amount first")
    void shouldDeserializeFromStringFormatAmountFirst() throws JsonProcessingException {
        String json = "\"100.50 USD\"";

        Money money = objectMapper.readValue(json, Money.class);

        assertThat(money.getAmount()).isEqualByComparingTo(new BigDecimal("100.50"));
        assertThat(money.getCurrencyCode()).isEqualTo("USD");
    }

    @Test
    @DisplayName("Should deserialize Money from number format with default currency")
    void shouldDeserializeFromNumberFormat() throws JsonProcessingException {
        String json = "100.50";

        Money money = objectMapper.readValue(json, Money.class);

        assertThat(money.getAmount()).isEqualByComparingTo(new BigDecimal("100.50"));
        assertThat(money.getCurrencyCode()).isEqualTo("IDR");
    }

    @Test
    @DisplayName("Should deserialize null as null")
    void shouldDeserializeNullAsNull() throws JsonProcessingException {
        String json = "null";

        Money money = objectMapper.readValue(json, Money.class);

        assertThat(money).isNull();
    }

    @Test
    @DisplayName("Should throw exception for invalid string format")
    void shouldThrowExceptionForInvalidStringFormat() {
        String json = "\"invalid format\"";

        assertThatThrownBy(() -> objectMapper.readValue(json, Money.class))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("Invalid Money format");
    }

    @Test
    @DisplayName("Should throw exception when amount is missing in object format")
    void shouldThrowExceptionWhenAmountMissing() {
        String json = "{\"currencyCode\":\"IDR\"}";

        assertThatThrownBy(() -> objectMapper.readValue(json, Money.class))
                .isInstanceOf(JsonProcessingException.class)
                .hasMessageContaining("Amount is required");
    }

    @Test
    @DisplayName("Should deserialize Money within a wrapper object")
    void shouldDeserializeMoneyWithinWrapperObject() throws JsonProcessingException {
        String json = "{\"amount\":{\"amount\":\"50000.00\",\"currencyCode\":\"IDR\"},\"description\":\"Test\"}";

        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Money.class, new MoneyDeserializer());
        mapper.registerModule(module);

        record Transaction(Money amount, String description) {}

        Transaction transaction = mapper.readValue(json, Transaction.class);

        assertThat(transaction.amount().getAmount()).isEqualByComparingTo(new BigDecimal("50000.00"));
        assertThat(transaction.amount().getCurrencyCode()).isEqualTo("IDR");
        assertThat(transaction.description()).isEqualTo("Test");
    }
}
