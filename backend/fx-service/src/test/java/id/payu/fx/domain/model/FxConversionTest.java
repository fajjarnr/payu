package id.payu.fx.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for FxConversion domain model.
 * Validates status transitions, effective amount calculation, and builder.
 */
@DisplayName("FxConversion — Domain Model")
class FxConversionTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2025, 6, 15, 14, 30, 0);

    private FxConversion buildConversion(ConversionStatus status, BigDecimal fee) {
        return FxConversion.builder()
                .id(UUID.randomUUID())
                .accountId("ACC-001")
                .fromCurrency("USD")
                .toCurrency("IDR")
                .fromAmount(new BigDecimal("100"))
                .toAmount(new BigDecimal("1580000"))
                .exchangeRate(new BigDecimal("15800"))
                .fee(fee)
                .conversionDate(NOW)
                .status(status)
                .build();
    }

    // ========================================================================
    // Status transitions
    // ========================================================================
    @Nested
    @DisplayName("Status transitions")
    class StatusTransitions {

        @Test
        @DisplayName("markCompleted sets status to COMPLETED")
        void markCompleted() {
            FxConversion conv = buildConversion(ConversionStatus.PENDING, BigDecimal.ZERO);
            conv.markCompleted();
            assertThat(conv.getStatus()).isEqualTo(ConversionStatus.COMPLETED);
        }

        @Test
        @DisplayName("markFailed sets status to FAILED")
        void markFailed() {
            FxConversion conv = buildConversion(ConversionStatus.PENDING, BigDecimal.ZERO);
            conv.markFailed();
            assertThat(conv.getStatus()).isEqualTo(ConversionStatus.FAILED);
        }

        @Test
        @DisplayName("markReversed sets status to REVERSED")
        void markReversed() {
            FxConversion conv = buildConversion(ConversionStatus.COMPLETED, BigDecimal.ZERO);
            conv.markReversed();
            assertThat(conv.getStatus()).isEqualTo(ConversionStatus.REVERSED);
        }

        @Test
        @DisplayName("PENDING → COMPLETED → REVERSED lifecycle")
        void fullLifecycle() {
            FxConversion conv = buildConversion(ConversionStatus.PENDING, BigDecimal.ZERO);
            assertThat(conv.getStatus()).isEqualTo(ConversionStatus.PENDING);

            conv.markCompleted();
            assertThat(conv.getStatus()).isEqualTo(ConversionStatus.COMPLETED);

            conv.markReversed();
            assertThat(conv.getStatus()).isEqualTo(ConversionStatus.REVERSED);
        }

        @Test
        @DisplayName("PENDING → FAILED transition")
        void pendingToFailed() {
            FxConversion conv = buildConversion(ConversionStatus.PENDING, BigDecimal.ZERO);
            conv.markFailed();
            assertThat(conv.getStatus()).isEqualTo(ConversionStatus.FAILED);
        }
    }

    // ========================================================================
    // getEffectiveAmount
    // ========================================================================
    @Nested
    @DisplayName("getEffectiveAmount")
    class EffectiveAmount {

        @Test
        @DisplayName("returns toAmount - fee when fee is present")
        void withFee() {
            FxConversion conv = buildConversion(
                    ConversionStatus.COMPLETED,
                    new BigDecimal("5000")
            );
            // toAmount = 1,580,000, fee = 5,000 → effective = 1,575,000
            assertThat(conv.getEffectiveAmount()).isEqualByComparingTo(new BigDecimal("1575000"));
        }

        @Test
        @DisplayName("returns toAmount when fee is null")
        void withNullFee() {
            FxConversion conv = buildConversion(ConversionStatus.COMPLETED, null);
            assertThat(conv.getEffectiveAmount()).isEqualByComparingTo(new BigDecimal("1580000"));
        }

        @Test
        @DisplayName("returns toAmount when fee is zero")
        void withZeroFee() {
            FxConversion conv = buildConversion(ConversionStatus.COMPLETED, BigDecimal.ZERO);
            assertThat(conv.getEffectiveAmount()).isEqualByComparingTo(new BigDecimal("1580000"));
        }

        @Test
        @DisplayName("handles large fee reducing effective amount significantly")
        void largeFee() {
            FxConversion conv = FxConversion.builder()
                    .id(UUID.randomUUID())
                    .toAmount(new BigDecimal("1000000"))
                    .fee(new BigDecimal("999999"))
                    .build();
            assertThat(conv.getEffectiveAmount()).isEqualByComparingTo(BigDecimal.ONE);
        }
    }

    // ========================================================================
    // ConversionStatus enum
    // ========================================================================
    @Nested
    @DisplayName("ConversionStatus enum")
    class ConversionStatusEnum {

        @Test
        @DisplayName("has exactly 4 values")
        void fourValues() {
            assertThat(ConversionStatus.values()).hasSize(4);
        }

        @ParameterizedTest
        @EnumSource(ConversionStatus.class)
        @DisplayName("all enum values have correct names")
        void allValuesExist(ConversionStatus status) {
            assertThat(status.name()).isIn("PENDING", "COMPLETED", "FAILED", "REVERSED");
        }
    }

    // ========================================================================
    // Builder
    // ========================================================================
    @Nested
    @DisplayName("Builder")
    class Builder {

        @Test
        @DisplayName("builds FxConversion with all fields set correctly")
        void allFieldsSet() {
            UUID id = UUID.randomUUID();
            FxConversion conv = FxConversion.builder()
                    .id(id)
                    .accountId("ACC-999")
                    .fromCurrency("EUR")
                    .toCurrency("IDR")
                    .fromAmount(new BigDecimal("250"))
                    .toAmount(new BigDecimal("4375000"))
                    .exchangeRate(new BigDecimal("17500"))
                    .fee(new BigDecimal("10000"))
                    .conversionDate(NOW)
                    .status(ConversionStatus.PENDING)
                    .build();

            assertThat(conv.getId()).isEqualTo(id);
            assertThat(conv.getAccountId()).isEqualTo("ACC-999");
            assertThat(conv.getFromCurrency()).isEqualTo("EUR");
            assertThat(conv.getToCurrency()).isEqualTo("IDR");
            assertThat(conv.getFromAmount()).isEqualByComparingTo(new BigDecimal("250"));
            assertThat(conv.getToAmount()).isEqualByComparingTo(new BigDecimal("4375000"));
            assertThat(conv.getExchangeRate()).isEqualByComparingTo(new BigDecimal("17500"));
            assertThat(conv.getFee()).isEqualByComparingTo(new BigDecimal("10000"));
            assertThat(conv.getConversionDate()).isEqualTo(NOW);
            assertThat(conv.getStatus()).isEqualTo(ConversionStatus.PENDING);
        }

        @Test
        @DisplayName("default constructor creates empty conversion")
        void defaultConstructor() {
            FxConversion conv = new FxConversion();
            assertThat(conv.getId()).isNull();
            assertThat(conv.getStatus()).isNull();
            assertThat(conv.getFromCurrency()).isNull();
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
            FxConversion conv = new FxConversion();
            UUID id = UUID.randomUUID();
            conv.setId(id);
            conv.setAccountId("ACC-123");
            conv.setFromCurrency("SGD");
            conv.setToCurrency("IDR");
            conv.setFromAmount(new BigDecimal("500"));
            conv.setToAmount(new BigDecimal("5750000"));
            conv.setExchangeRate(new BigDecimal("11500"));
            conv.setFee(new BigDecimal("1000"));
            conv.setConversionDate(NOW);
            conv.setStatus(ConversionStatus.COMPLETED);

            assertThat(conv.getId()).isEqualTo(id);
            assertThat(conv.getAccountId()).isEqualTo("ACC-123");
            assertThat(conv.getFromCurrency()).isEqualTo("SGD");
            assertThat(conv.getToCurrency()).isEqualTo("IDR");
            assertThat(conv.getFromAmount()).isEqualByComparingTo(new BigDecimal("500"));
            assertThat(conv.getToAmount()).isEqualByComparingTo(new BigDecimal("5750000"));
            assertThat(conv.getExchangeRate()).isEqualByComparingTo(new BigDecimal("11500"));
            assertThat(conv.getFee()).isEqualByComparingTo(new BigDecimal("1000"));
            assertThat(conv.getConversionDate()).isEqualTo(NOW);
            assertThat(conv.getStatus()).isEqualTo(ConversionStatus.COMPLETED);
        }
    }
}
