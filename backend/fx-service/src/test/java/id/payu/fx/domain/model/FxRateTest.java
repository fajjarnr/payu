package id.payu.fx.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for FxRate domain model.
 * Validates temporal validity checks, currency conversion, and builder.
 */
@DisplayName("FxRate — Domain Model")
class FxRateTest {

    private static final LocalDateTime BASE = LocalDateTime.of(2025, 6, 15, 10, 0, 0);

    private FxRate buildRate(LocalDateTime validFrom, LocalDateTime validUntil, BigDecimal rate) {
        return FxRate.builder()
                .id(UUID.randomUUID())
                .fromCurrency("USD")
                .toCurrency("IDR")
                .rate(rate)
                .inverseRate(BigDecimal.ONE.divide(rate, 10, java.math.RoundingMode.HALF_EVEN))
                .validFrom(validFrom)
                .validUntil(validUntil)
                .version(1L)
                .createdAt(BASE)
                .build();
    }

    // ========================================================================
    // isValidAt
    // ========================================================================
    @Nested
    @DisplayName("isValidAt")
    class IsValidAt {

        @Test
        @DisplayName("returns true at validFrom (inclusive lower bound)")
        void trueAtValidFrom() {
            FxRate rate = buildRate(BASE, BASE.plusMinutes(15), new BigDecimal("15800"));
            assertThat(rate.isValidAt(BASE)).isTrue();
        }

        @Test
        @DisplayName("returns true at validUntil (inclusive upper bound)")
        void trueAtValidUntil() {
            FxRate rate = buildRate(BASE, BASE.plusMinutes(15), new BigDecimal("15800"));
            assertThat(rate.isValidAt(BASE.plusMinutes(15))).isTrue();
        }

        @Test
        @DisplayName("returns true in the middle of the validity window")
        void trueInMiddle() {
            FxRate rate = buildRate(BASE, BASE.plusMinutes(15), new BigDecimal("15800"));
            assertThat(rate.isValidAt(BASE.plusMinutes(7))).isTrue();
        }

        @Test
        @DisplayName("returns false before validFrom")
        void falseBeforeValidFrom() {
            FxRate rate = buildRate(BASE, BASE.plusMinutes(15), new BigDecimal("15800"));
            assertThat(rate.isValidAt(BASE.minusSeconds(1))).isFalse();
        }

        @Test
        @DisplayName("returns false after validUntil")
        void falseAfterValidUntil() {
            FxRate rate = buildRate(BASE, BASE.plusMinutes(15), new BigDecimal("15800"));
            assertThat(rate.isValidAt(BASE.plusMinutes(15).plusSeconds(1))).isFalse();
        }
    }

    // ========================================================================
    // isExpired
    // ========================================================================
    @Nested
    @DisplayName("isExpired")
    class IsExpired {

        @Test
        @DisplayName("returns false when validUntil is in the future")
        void notExpired() {
            FxRate rate = buildRate(BASE, LocalDateTime.now().plusHours(1), new BigDecimal("15800"));
            assertThat(rate.isExpired()).isFalse();
        }

        @Test
        @DisplayName("returns true when validUntil is in the past")
        void expired() {
            FxRate rate = buildRate(BASE, LocalDateTime.now().minusSeconds(1), new BigDecimal("15800"));
            assertThat(rate.isExpired()).isTrue();
        }
    }

    // ========================================================================
    // convert
    // ========================================================================
    @Nested
    @DisplayName("convert")
    class Convert {

        @Test
        @DisplayName("multiplies amount by rate: 100 USD × 15800 = 1,580,000 IDR")
        void basicConversion() {
            FxRate rate = buildRate(BASE, BASE.plusMinutes(15), new BigDecimal("15800"));
            BigDecimal result = rate.convert(new BigDecimal("100"));
            assertThat(result).isEqualByComparingTo(new BigDecimal("1580000"));
        }

        @Test
        @DisplayName("converting zero returns zero")
        void zeroAmount() {
            FxRate rate = buildRate(BASE, BASE.plusMinutes(15), new BigDecimal("15800"));
            assertThat(rate.convert(BigDecimal.ZERO)).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("fractional amounts: 0.50 USD × 15800 = 7,900")
        void fractionalAmount() {
            FxRate rate = buildRate(BASE, BASE.plusMinutes(15), new BigDecimal("15800"));
            BigDecimal result = rate.convert(new BigDecimal("0.50"));
            assertThat(result).isEqualByComparingTo(new BigDecimal("7900"));
        }

        @Test
        @DisplayName("fractional rate: 100 USD × 15832.75 = 1,583,275.00")
        void fractionalRate() {
            FxRate rate = buildRate(BASE, BASE.plusMinutes(15), new BigDecimal("15832.75"));
            BigDecimal result = rate.convert(new BigDecimal("100"));
            assertThat(result).isEqualByComparingTo(new BigDecimal("1583275.00"));
        }
    }

    // ========================================================================
    // Builder
    // ========================================================================
    @Nested
    @DisplayName("Builder")
    class Builder {

        @Test
        @DisplayName("builds FxRate with all fields set correctly")
        void allFieldsSet() {
            UUID id = UUID.randomUUID();
            FxRate rate = FxRate.builder()
                    .id(id)
                    .fromCurrency("EUR")
                    .toCurrency("IDR")
                    .rate(new BigDecimal("17500.50"))
                    .inverseRate(new BigDecimal("0.0000571413"))
                    .validFrom(BASE)
                    .validUntil(BASE.plusMinutes(15))
                    .version(42L)
                    .createdAt(BASE.minusHours(1))
                    .build();

            assertThat(rate.getId()).isEqualTo(id);
            assertThat(rate.getFromCurrency()).isEqualTo("EUR");
            assertThat(rate.getToCurrency()).isEqualTo("IDR");
            assertThat(rate.getRate()).isEqualByComparingTo(new BigDecimal("17500.50"));
            assertThat(rate.getInverseRate()).isEqualByComparingTo(new BigDecimal("0.0000571413"));
            assertThat(rate.getValidFrom()).isEqualTo(BASE);
            assertThat(rate.getValidUntil()).isEqualTo(BASE.plusMinutes(15));
            assertThat(rate.getVersion()).isEqualTo(42L);
            assertThat(rate.getCreatedAt()).isEqualTo(BASE.minusHours(1));
        }

        @Test
        @DisplayName("default constructor creates empty rate")
        void defaultConstructor() {
            FxRate rate = new FxRate();
            assertThat(rate.getId()).isNull();
            assertThat(rate.getRate()).isNull();
            assertThat(rate.getFromCurrency()).isNull();
        }
    }

    // ========================================================================
    // Setters
    // ========================================================================
    @Nested
    @DisplayName("Setters")
    class Setters {

        @Test
        @DisplayName("setters update fields correctly")
        void settersWork() {
            FxRate rate = new FxRate();
            UUID id = UUID.randomUUID();
            rate.setId(id);
            rate.setFromCurrency("GBP");
            rate.setToCurrency("JPY");
            rate.setRate(new BigDecimal("190.50"));
            rate.setInverseRate(new BigDecimal("0.00525"));
            rate.setValidFrom(BASE);
            rate.setValidUntil(BASE.plusMinutes(30));
            rate.setVersion(5L);
            rate.setCreatedAt(BASE);

            assertThat(rate.getId()).isEqualTo(id);
            assertThat(rate.getFromCurrency()).isEqualTo("GBP");
            assertThat(rate.getToCurrency()).isEqualTo("JPY");
            assertThat(rate.getRate()).isEqualByComparingTo(new BigDecimal("190.50"));
            assertThat(rate.getVersion()).isEqualTo(5L);
        }
    }
}
