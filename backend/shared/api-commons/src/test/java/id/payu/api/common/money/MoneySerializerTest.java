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
 * Unit tests for {@link MoneySerializer}.
 *
 * @author PayU Digital Banking Platform
 * @since 1.0.0
 */
@DisplayName("MoneySerializer Tests")
class MoneySerializerTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(Money.class, new MoneySerializer());
        objectMapper.registerModule(module);
    }

    @Test
    @DisplayName("Should serialize Money to JSON object")
    void shouldSerializeMoneyToJsonObject() throws JsonProcessingException {
        Money money = Money.of(new BigDecimal("100.50"), "IDR");

        String json = objectMapper.writeValueAsString(money);

        assertThat(json).contains("\"amount\":\"100.5000\"");
        assertThat(json).contains("\"currencyCode\":\"IDR\"");
        assertThat(json).contains("\"formatted\":\"IDR 100.5000\"");
    }

    @Test
    @DisplayName("Should serialize Money with USD currency")
    void shouldSerializeMoneyWithUsdCurrency() throws JsonProcessingException {
        Money money = Money.of(new BigDecimal("99.99"), "USD");

        String json = objectMapper.writeValueAsString(money);

        assertThat(json).contains("\"amount\":\"99.9900\"");
        assertThat(json).contains("\"currencyCode\":\"USD\"");
        assertThat(json).contains("\"formatted\":\"USD 99.9900\"");
    }

    @Test
    @DisplayName("Should serialize zero amount")
    void shouldSerializeZeroAmount() throws JsonProcessingException {
        Money money = Money.of(BigDecimal.ZERO, "IDR");

        String json = objectMapper.writeValueAsString(money);

        assertThat(json).contains("\"amount\":\"0.0000\"");
    }

    @Test
    @DisplayName("Should serialize null as null")
    void shouldSerializeNullAsNull() throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(null);

        assertThat(json).isEqualTo("null");
    }

    @Test
    @DisplayName("Should serialize Money within a wrapper object")
    void shouldSerializeMoneyWithinWrapperObject() throws JsonProcessingException {
        record Transaction(Money amount, String description) {}

        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(Money.class, new MoneySerializer());
        mapper.registerModule(module);

        Transaction transaction = new Transaction(
                Money.of(new BigDecimal("50000.00"), "IDR"),
                "Test transaction"
        );

        String json = mapper.writeValueAsString(transaction);

        assertThat(json).contains("\"amount\"");
        assertThat(json).contains("\"50000.0000\"");
        assertThat(json).contains("\"description\":\"Test transaction\"");
    }
}
