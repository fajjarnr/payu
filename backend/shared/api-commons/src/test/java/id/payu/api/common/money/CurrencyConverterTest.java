package id.payu.api.common.money;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link CurrencyConverter} interface.
 *
 * @author PayU Digital Banking Platform
 * @since 1.0.0
 */
@DisplayName("CurrencyConverter Tests")
class CurrencyConverterTest {

    @Test
    @DisplayName("Should convert with rate using default implementation")
    void shouldConvertWithRate() {
        CurrencyConverter converter = new CurrencyConverter() {
            @Override
            public BigDecimal convert(BigDecimal amount, String fromCurrency, String toCurrency) {
                return amount; // Simplified implementation
            }
        };

        BigDecimal result = converter.convertWithRate(new BigDecimal("100.00"), new BigDecimal("15000"));

        assertThat(result).isEqualByComparingTo(new BigDecimal("1500000.00"));
    }

    @Test
    @DisplayName("Should throw exception when rate is null")
    void shouldThrowExceptionWhenRateIsNull() {
        CurrencyConverter converter = (amount, from, to) -> amount;

        assertThatThrownBy(() -> converter.convertWithRate(new BigDecimal("100.00"), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Exchange rate must not be null");
    }

    @Test
    @DisplayName("Should throw exception when rate is zero")
    void shouldThrowExceptionWhenRateIsZero() {
        CurrencyConverter converter = (amount, from, to) -> amount;

        assertThatThrownBy(() -> converter.convertWithRate(new BigDecimal("100.00"), BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Exchange rate must be positive");
    }

    @Test
    @DisplayName("Should throw exception when rate is negative")
    void shouldThrowExceptionWhenRateIsNegative() {
        CurrencyConverter converter = (amount, from, to) -> amount;

        assertThatThrownBy(() -> converter.convertWithRate(new BigDecimal("100.00"), new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Exchange rate must be positive");
    }

    @Test
    @DisplayName("Should get rate using default implementation")
    void shouldGetRate() {
        CurrencyConverter converter = new CurrencyConverter() {
            @Override
            public BigDecimal convert(BigDecimal amount, String fromCurrency, String toCurrency) {
                // Mock rate: IDR to USD = 0.000065
                return amount.multiply(new BigDecimal("0.000065"));
            }
        };

        BigDecimal rate = converter.getRate("IDR", "USD");

        assertThat(rate).isEqualByComparingTo(new BigDecimal("0.000065"));
    }
}
