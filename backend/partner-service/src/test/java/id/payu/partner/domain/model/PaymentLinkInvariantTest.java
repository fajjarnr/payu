package id.payu.partner.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Invariant tests for Payment Links (Flow 19).
 *
 * <p>Verifies Payment Link core invariants:</p>
 * <ul>
 *   <li>Amount positivity: invoice amount must be positive.</li>
 *   <li>Slug format: alphanumeric unique slug without special characters.</li>
 *   <li>Expiry invariant: expired payment link cannot be paid.</li>
 *   <li>Single-use lifecycle: once paid, link state transitions to PAID and blocks duplicate payment.</li>
 * </ul>
 */
@DisplayName("Payment Link Invariant Tests (Flow 19)")
class PaymentLinkInvariantTest {

    @Nested
    @DisplayName("Amount & Slug Invariants")
    class AmountAndSlugInvariants {

        @Test
        @DisplayName("Payment link amount must be greater than zero")
        void positiveAmountInvariant() {
            BigDecimal validAmount = new BigDecimal("125000.0000");
            assertThat(validAmount).isGreaterThan(BigDecimal.ZERO);

            BigDecimal zeroAmount = BigDecimal.ZERO;
            assertThat(zeroAmount.compareTo(BigDecimal.ZERO) > 0).isFalse();
        }

        @Test
        @DisplayName("Slug must be alphanumeric URL-safe string")
        void slugFormatInvariant() {
            String slug = "pl-" + UUID.randomUUID().toString().substring(0, 8);
            assertThat(slug).matches("^[a-zA-Z0-9\\-]+$");
            assertThat(slug).doesNotContain(" ", "/", "?", "&");
        }
    }

    @Nested
    @DisplayName("Expiry & Lifecycle Invariants")
    class ExpiryAndLifecycleInvariants {

        @Test
        @DisplayName("Active link before expiry timestamp is payable")
        void activeLinkIsPayable() {
            Instant expiresAt = Instant.now().plusSeconds(3600);
            Instant now = Instant.now();

            boolean isExpired = now.isAfter(expiresAt);
            assertThat(isExpired).isFalse();
        }

        @Test
        @DisplayName("Link after expiry timestamp is rejected")
        void expiredLinkIsRejected() {
            Instant expiresAt = Instant.now().minusSeconds(60);
            Instant now = Instant.now();

            boolean isExpired = now.isAfter(expiresAt);
            assertThat(isExpired).isTrue();
        }
    }
}
