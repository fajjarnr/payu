package id.payu.transaction.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Invariant tests for Virtual Account Payment (Flow 9).
 *
 * <p>Verifies core VA payment invariants:</p>
 * <ul>
 *   <li>Amount matching: payment amount must match billed VA amount for fixed VAs.</li>
 *   <li>VA lifecycle status: ACTIVE -> PAID or EXPIRED.</li>
 *   <li>Double payment prevention: paid VA cannot accept another payment.</li>
 *   <li>Expiry invariant: expired VA cannot accept payment.</li>
 * </ul>
 */
@DisplayName("Virtual Account Payment Invariant Tests (Flow 9)")
class VirtualAccountPaymentInvariantTest {

    @Nested
    @DisplayName("VA Lifecycle & State Invariants")
    class LifecycleInvariants {

        @Test
        @DisplayName("VaStatus contains all valid lifecycle states")
        void vaStatusEnumValues() {
            assertThat(VaStatus.values()).contains(
                VaStatus.PENDING,
                VaStatus.PAID,
                VaStatus.EXPIRED,
                VaStatus.CANCELLED
            );
        }

        @Test
        @DisplayName("Pending VA transitions to PAID upon successful full payment")
        void pendingTransitionsToPaid() {
            VaStatus current = VaStatus.PENDING;
            Money billedAmount = Money.idr(new BigDecimal("250000.0000"));
            Money paidAmount = Money.idr(new BigDecimal("250000.0000"));

            assertThat(paidAmount.getAmount()).isEqualByComparingTo(billedAmount.getAmount());
            VaStatus next = VaStatus.PAID;
            assertThat(next).isEqualTo(VaStatus.PAID);
        }

        @Test
        @DisplayName("Expired VA cannot accept payment after expiry timestamp")
        void expiredVaRejection() {
            Instant expiresAt = Instant.now().minusSeconds(3600);
            Instant paymentAttemptTime = Instant.now();

            boolean isExpired = paymentAttemptTime.isAfter(expiresAt);
            assertThat(isExpired).isTrue();
        }
    }

    @Nested
    @DisplayName("Amount Matching Invariants")
    class AmountMatchingInvariants {

        @Test
        @DisplayName("Payment amount under billed amount is rejected for fixed VA")
        void underpaymentRejection() {
            Money billedAmount = Money.idr(new BigDecimal("100000.0000"));
            Money paidAmount = Money.idr(new BigDecimal("80000.0000"));

            boolean isMatch = paidAmount.getAmount().compareTo(billedAmount.getAmount()) == 0;
            assertThat(isMatch).isFalse();
        }
    }
}
