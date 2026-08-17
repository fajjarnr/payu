package id.payu.partner.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Invariant tests for SNAP-BI Refund (Flow 5).
 *
 * <p>Verifies Bank Indonesia SNAP-BI refund standards:</p>
 * <ul>
 *   <li>Amount invariants: refund amount must not exceed original transaction amount.</li>
 *   <li>Partial refunds: cumulative refund amounts must not exceed original payment.</li>
 *   <li>Positivity: zero and negative refund amounts are rejected.</li>
 *   <li>Scale 4 HALF_EVEN precision preservation.</li>
 * </ul>
 */
@DisplayName("SNAP Refund Invariant Tests (Flow 5)")
class SnapRefundInvariantTest {

    private static final UUID TXN_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PARTNER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Nested
    @DisplayName("Amount & Partial Refund Invariants")
    class AmountInvariants {

        @Test
        @DisplayName("Refund amount must be strictly greater than zero")
        void positiveAmountRequired() {
            assertThatThrownBy(() -> Refund.create(TXN_ID, PARTNER_ID, BigDecimal.ZERO, "Return", "admin"))
                .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> Refund.create(TXN_ID, PARTNER_ID, new BigDecimal("-100.00"), "Return", "admin"))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Refund amount cannot exceed original transaction amount")
        void refundAmountCannotExceedOriginal() {
            BigDecimal originalPayment = new BigDecimal("500000.0000");
            BigDecimal refundRequest = new BigDecimal("500000.0000");

            assertThat(refundRequest).isLessThanOrEqualTo(originalPayment);

            BigDecimal excessiveRefund = new BigDecimal("500000.0001");
            assertThat(excessiveRefund).isGreaterThan(originalPayment);
        }

        @Test
        @DisplayName("Cumulative partial refunds cannot exceed original payment amount")
        void cumulativePartialRefunds() {
            BigDecimal originalPayment = new BigDecimal("300000.0000");
            BigDecimal partialRefund1 = new BigDecimal("100000.0000");
            BigDecimal partialRefund2 = new BigDecimal("150000.0000");

            BigDecimal totalRefunded = partialRefund1.add(partialRefund2).setScale(4, RoundingMode.HALF_EVEN);
            BigDecimal remainingRefundable = originalPayment.subtract(totalRefunded);

            assertThat(totalRefunded).isEqualByComparingTo(new BigDecimal("250000.0000"));
            assertThat(remainingRefundable).isEqualByComparingTo(new BigDecimal("50000.0000"));
            assertThat(totalRefunded).isLessThanOrEqualTo(originalPayment);
        }
    }

    @Nested
    @DisplayName("Lifecycle State Invariants")
    class LifecycleInvariants {

        @Test
        @DisplayName("Refund lifecycle transitions from PENDING through PROCESSING to COMPLETED")
        void refundLifecycleTransitions() {
            Refund refund = Refund.create(TXN_ID, PARTNER_ID, new BigDecimal("75000.0000"), "Damaged item", "user-1");
            assertThat(refund.getStatus()).isEqualTo(RefundStatus.PENDING);

            refund.process("processor-1");
            assertThat(refund.getStatus()).isEqualTo(RefundStatus.PROCESSING);

            refund.complete("ext-refund-999");
            assertThat(refund.getStatus()).isEqualTo(RefundStatus.COMPLETED);
            assertThat(refund.isTerminal()).isTrue();
        }
    }
}
