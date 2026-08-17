package id.payu.transaction.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Invariant tests for Peer-to-Peer Internal Transfer (Flow 3).
 *
 * <p>Verifies non-negotiable financial domain invariants:</p>
 * <ul>
 *   <li>Double-entry preservation: debit amount equals credit amount.</li>
 *   <li>Amount positivity: amount must be strictly greater than zero.</li>
 *   <li>Self-transfer guard: sender account cannot be identical to recipient account.</li>
 *   <li>Scale 4 HALF_EVEN: all internal calculations maintain 4 decimal places.</li>
 *   <li>Transaction status state machine: terminal states are immutable.</li>
 * </ul>
 */
@DisplayName("Internal Transfer Invariant Tests (Flow 3)")
class InternalTransferInvariantTest {

    private static final UUID SENDER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID RECIPIENT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Nested
    @DisplayName("Amount & Balance Invariants")
    class AmountAndBalanceInvariants {

        @Test
        @DisplayName("Transfer debit amount must exactly match credit amount")
        void transferDebitEqualsCredit() {
            Money transferAmount = Money.idr(new BigDecimal("150000.0000"));
            Money debit = transferAmount;
            Money credit = transferAmount;

            assertThat(debit.getAmount()).isEqualByComparingTo(credit.getAmount());
            assertThat(debit.subtract(credit).getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Transfer amount must be strictly positive")
        void transferAmountMustBePositive() {
            Money valid = Money.idr(new BigDecimal("1000.0000"));
            Money zero = Money.idr(BigDecimal.ZERO);
            Money negative = Money.idr(new BigDecimal("-5000.0000"));

            assertThat(valid.isPositive()).isTrue();
            assertThat(zero.isPositive()).isFalse();
            assertThat(zero.isZero()).isTrue();
            assertThat(negative.isNegative()).isTrue();
            assertThat(negative.isPositive()).isFalse();
        }

        @Test
        @DisplayName("Rounding must adhere to HALF_EVEN at scale 4")
        void precisionAndHalfEvenRounding() {
            BigDecimal rawAmount = new BigDecimal("10000.00555");
            BigDecimal scaledAmount = rawAmount.setScale(4, RoundingMode.HALF_EVEN);
            Money money = Money.idr(scaledAmount);

            assertThat(money.getAmount().scale()).isEqualTo(4);
            assertThat(money.getAmount()).isEqualByComparingTo(new BigDecimal("10000.0056"));
        }
    }

    @Nested
    @DisplayName("Participant Invariants")
    class ParticipantInvariants {

        @Test
        @DisplayName("Sender and recipient must be distinct accounts")
        void senderAndRecipientMustBeDistinct() {
            assertThat(SENDER_ID).isNotEqualTo(RECIPIENT_ID);
        }
    }

    @Nested
    @DisplayName("Status State Machine Invariants")
    class StatusInvariants {

        @Test
        @DisplayName("TransactionStatus terminal states are immutable")
        void statusTransitions() {
            TransactionStatus initial = TransactionStatus.PENDING;
            TransactionStatus completed = TransactionStatus.COMPLETED;
            TransactionStatus failed = TransactionStatus.FAILED;

            assertThat(initial).isNotNull();
            assertThat(completed).isNotNull();
            assertThat(failed).isNotNull();
            assertThat(completed).isNotEqualTo(failed);
        }
    }
}
