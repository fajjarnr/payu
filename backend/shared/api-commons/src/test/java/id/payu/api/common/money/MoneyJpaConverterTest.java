package id.payu.api.common.money;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link MoneyJpaConverter}.
 *
 * @author PayU Digital Banking Platform
 * @since 1.0.0
 */
@DisplayName("MoneyJpaConverter Tests")
class MoneyJpaConverterTest {

    private final MoneyJpaConverter converter = new MoneyJpaConverter();

    @Test
    @DisplayName("Should convert Money to database column")
    void shouldConvertMoneyToDatabaseColumn() {
        Money money = Money.of(new BigDecimal("100.50"), "IDR");

        String dbData = converter.convertToDatabaseColumn(money);

        assertThat(dbData).isEqualTo("IDR|100.50");
    }

    @Test
    @DisplayName("Should convert Money with USD to database column")
    void shouldConvertMoneyWithUsdToDatabaseColumn() {
        Money money = Money.of(new BigDecimal("99.99"), "USD");

        String dbData = converter.convertToDatabaseColumn(money);

        assertThat(dbData).isEqualTo("USD|99.99");
    }

    @Test
    @DisplayName("Should convert null Money to null")
    void shouldConvertNullMoneyToNull() {
        String dbData = converter.convertToDatabaseColumn(null);

        assertThat(dbData).isNull();
    }

    @Test
    @DisplayName("Should convert database column to Money")
    void shouldConvertDatabaseColumnToMoney() {
        String dbData = "IDR|100.50";

        Money money = converter.convertToEntityAttribute(dbData);

        assertThat(money.getAmount()).isEqualByComparingTo(new BigDecimal("100.50"));
        assertThat(money.getCurrencyCode()).isEqualTo("IDR");
    }

    @Test
    @DisplayName("Should convert database column with USD to Money")
    void shouldConvertDatabaseColumnWithUsdToMoney() {
        String dbData = "USD|99.99";

        Money money = converter.convertToEntityAttribute(dbData);

        assertThat(money.getAmount()).isEqualByComparingTo(new BigDecimal("99.99"));
        assertThat(money.getCurrencyCode()).isEqualTo("USD");
    }

    @Test
    @DisplayName("Should convert null database column to null")
    void shouldConvertNullDatabaseColumnToNull() {
        Money money = converter.convertToEntityAttribute(null);

        assertThat(money).isNull();
    }

    @Test
    @DisplayName("Should convert empty database column to null")
    void shouldConvertEmptyDatabaseColumnToNull() {
        Money money = converter.convertToEntityAttribute("");

        assertThat(money).isNull();
    }

    @Test
    @DisplayName("Should throw exception for invalid format")
    void shouldThrowExceptionForInvalidFormat() {
        String dbData = "invalid-format";

        assertThatThrownBy(() -> converter.convertToEntityAttribute(dbData))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid Money format");
    }

    @Test
    @DisplayName("Should throw exception for missing delimiter")
    void shouldThrowExceptionForMissingDelimiter() {
        String dbData = "IDR100.50";

        assertThatThrownBy(() -> converter.convertToEntityAttribute(dbData))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid Money format");
    }

    @Test
    @DisplayName("Should handle large amounts")
    void shouldHandleLargeAmounts() {
        Money money = Money.of(new BigDecimal("999999999999999999.99"), "IDR");

        String dbData = converter.convertToDatabaseColumn(money);
        Money converted = converter.convertToEntityAttribute(dbData);

        assertThat(converted).isEqualTo(money);
    }

    @Test
    @DisplayName("Should round-trip conversion correctly")
    void shouldRoundTripConversionCorrectly() {
        Money original = Money.of(new BigDecimal("1234567.89"), "IDR");

        String dbData = converter.convertToDatabaseColumn(original);
        Money converted = converter.convertToEntityAttribute(dbData);

        assertThat(converted).isEqualTo(original);
    }
}
