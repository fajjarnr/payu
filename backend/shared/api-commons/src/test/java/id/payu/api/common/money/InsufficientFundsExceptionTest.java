package id.payu.api.common.money;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link InsufficientFundsException}.
 *
 * @author PayU Digital Banking Platform
 * @since 1.0.0
 */
@DisplayName("InsufficientFundsException Tests")
class InsufficientFundsExceptionTest {

    @Test
    @DisplayName("Should create exception with message only")
    void shouldCreateExceptionWithMessageOnly() {
        InsufficientFundsException exception = new InsufficientFundsException("Insufficient funds");

        assertThat(exception.getMessage()).isEqualTo("Insufficient funds");
        assertThat(exception.getCode()).isEqualTo("MONEY_001");
        assertThat(exception.getAvailable()).isNull();
        assertThat(exception.getRequested()).isNull();
    }

    @Test
    @DisplayName("Should create exception with available and requested amounts")
    void shouldCreateExceptionWithAvailableAndRequested() {
        Money available = Money.of(new BigDecimal("100.00"), "IDR");
        Money requested = Money.of(new BigDecimal("150.00"), "IDR");

        InsufficientFundsException exception = new InsufficientFundsException(
                available, requested, "Insufficient funds"
        );

        assertThat(exception.getAvailable()).isEqualTo(available);
        assertThat(exception.getRequested()).isEqualTo(requested);
        assertThat(exception.getShortfall()).isEqualTo(Money.of(new BigDecimal("50.00"), "IDR"));
    }

    @Test
    @DisplayName("Should create exception with cause")
    void shouldCreateExceptionWithCause() {
        Throwable cause = new RuntimeException("Root cause");
        Money available = Money.of(new BigDecimal("100.00"), "IDR");
        Money requested = Money.of(new BigDecimal("150.00"), "IDR");

        InsufficientFundsException exception = new InsufficientFundsException(
                available, requested, "Insufficient funds", cause
        );

        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    @DisplayName("Should return null shortfall when amounts not provided")
    void shouldReturnNullShortfallWhenAmountsNotProvided() {
        InsufficientFundsException exception = new InsufficientFundsException("Insufficient funds");

        assertThat(exception.getShortfall()).isNull();
    }
}
