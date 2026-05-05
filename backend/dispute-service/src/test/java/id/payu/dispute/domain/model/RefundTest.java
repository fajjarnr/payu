package id.payu.dispute.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for Refund Aggregate Root.
 *
 * <p>P0 Critical Tests - These tests verify the core refund state machine
 * that must be correct for financial compliance.</p>
 *
 * <p>Test Categories:</p>
 * <ul>
 *   <li>Creation - Factory methods and initial state</li>
 *   <li>State Transitions - process(), complete(), fail()</li>
 *   <li>Invalid Transitions - Illegal state changes</li>
 *   <li>Full Refund - Complete amount refund</li>
 *   <li>Partial Refund - Partial amount refund</li>
 * </ul>
 *
 * @see Refund
 */
@Execution(ExecutionMode.CONCURRENT)
@DisplayName("Refund Aggregate Root Tests")
class RefundTest {

    private static final UUID TRANSACTION_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final BigDecimal AMOUNT = new BigDecimal("100000.00");
    private static final String REASON = "Customer request";
    private static final String CURRENCY = "IDR";

    // ==================== CREATION TESTS ====================

    @Nested
    @DisplayName("Creation")
    class CreationTests {

        @Test
        @DisplayName("Should create refund with pending status")
        void shouldCreateRefundWithPendingStatus() {
            Refund refund = Refund.create(TRANSACTION_ID, AMOUNT, CURRENCY, REASON);

            assertThat(refund.getStatus()).isEqualTo(RefundStatus.PENDING);
            assertThat(refund.getTransactionId()).isEqualTo(TRANSACTION_ID);
            assertThat(refund.getAmount()).isEqualByComparingTo(AMOUNT);
            assertThat(refund.getCurrency()).isEqualTo(CURRENCY);
            assertThat(refund.getReason()).isEqualTo(REASON);
            assertThat(refund.getId()).isNotNull();
            assertThat(refund.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should create full refund when amount equals transaction total")
        void shouldCreateFullRefundWhenAmountEqualsTransactionTotal() {
            BigDecimal fullAmount = new BigDecimal("500000.00");
            Refund refund = Refund.createFullRefund(TRANSACTION_ID, fullAmount, CURRENCY, REASON);

            assertThat(refund.getAmount()).isEqualByComparingTo(fullAmount);
            assertThat(refund.getStatus()).isEqualTo(RefundStatus.PENDING);
        }

        @Test
        @DisplayName("Should create partial refund")
        void shouldCreatePartialRefund() {
            BigDecimal partialAmount = new BigDecimal("50000.00");
            Refund refund = Refund.createPartialRefund(TRANSACTION_ID, partialAmount, CURRENCY, REASON);

            assertThat(refund.getAmount()).isEqualByComparingTo(partialAmount);
            assertThat(refund.getStatus()).isEqualTo(RefundStatus.PENDING);
        }

        @Test
        @DisplayName("Should throw exception when transactionId is null")
        void shouldThrowExceptionWhenTransactionIdIsNull() {
            assertThatThrownBy(() -> Refund.create(null, AMOUNT, CURRENCY, REASON))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Transaction ID cannot be null");
        }

        @Test
        @DisplayName("Should throw exception when amount is null")
        void shouldThrowExceptionWhenAmountIsNull() {
            assertThatThrownBy(() -> Refund.create(TRANSACTION_ID, null, CURRENCY, REASON))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Amount cannot be null");
        }

        @Test
        @DisplayName("Should throw exception when amount is zero or negative")
        void shouldThrowExceptionWhenAmountIsZeroOrNegative() {
            assertThatThrownBy(() -> Refund.create(TRANSACTION_ID, BigDecimal.ZERO, CURRENCY, REASON))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Amount must be positive");

            assertThatThrownBy(() -> Refund.create(TRANSACTION_ID, new BigDecimal("-100"), CURRENCY, REASON))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Amount must be positive");
        }

        @Test
        @DisplayName("Should throw exception when currency is null or empty")
        void shouldThrowExceptionWhenCurrencyIsNullOrEmpty() {
            assertThatThrownBy(() -> Refund.create(TRANSACTION_ID, AMOUNT, null, REASON))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Currency cannot be null or empty");

            assertThatThrownBy(() -> Refund.create(TRANSACTION_ID, AMOUNT, "", REASON))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Currency cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw exception when reason is null or empty")
        void shouldThrowExceptionWhenReasonIsNullOrEmpty() {
            assertThatThrownBy(() -> Refund.create(TRANSACTION_ID, AMOUNT, CURRENCY, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Reason cannot be null or empty");

            assertThatThrownBy(() -> Refund.create(TRANSACTION_ID, AMOUNT, CURRENCY, ""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Reason cannot be null or empty");
        }
    }

    // ==================== STATE TRANSITION TESTS ====================

    @Nested
    @DisplayName("State Transitions")
    class StateTransitionTests {

        @Test
        @DisplayName("Should process refund from pending to processing")
        void shouldProcessRefund() {
            Refund refund = Refund.create(TRANSACTION_ID, AMOUNT, CURRENCY, REASON);

            refund.process();

            assertThat(refund.getStatus()).isEqualTo(RefundStatus.PROCESSING);
            assertThat(refund.getProcessedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should complete refund from processing to completed")
        void shouldCompleteRefund() {
            Refund refund = Refund.create(TRANSACTION_ID, AMOUNT, CURRENCY, REASON);
            refund.process();

            refund.complete();

            assertThat(refund.getStatus()).isEqualTo(RefundStatus.COMPLETED);
            assertThat(refund.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should fail refund from processing to failed")
        void shouldFailRefund() {
            Refund refund = Refund.create(TRANSACTION_ID, AMOUNT, CURRENCY, REASON);
            refund.process();
            String failureReason = "Insufficient funds";

            refund.fail(failureReason);

            assertThat(refund.getStatus()).isEqualTo(RefundStatus.FAILED);
            assertThat(refund.getFailureReason()).isEqualTo(failureReason);
            assertThat(refund.getFailedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should cancel refund from pending to cancelled")
        void shouldCancelRefund() {
            Refund refund = Refund.create(TRANSACTION_ID, AMOUNT, CURRENCY, REASON);

            refund.cancel("Customer changed mind");

            assertThat(refund.getStatus()).isEqualTo(RefundStatus.CANCELLED);
            assertThat(refund.getCancelledAt()).isNotNull();
        }
    }

    // ==================== INVALID TRANSITION TESTS ====================

    @Nested
    @DisplayName("Invalid State Transitions")
    class InvalidTransitionTests {

        @Test
        @DisplayName("Should not process already processed refund")
        void shouldNotProcessAlreadyProcessedRefund() {
            Refund refund = Refund.create(TRANSACTION_ID, AMOUNT, CURRENCY, REASON);
            refund.process();

            assertThatThrownBy(refund::process)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot process refund in status");
        }

        @Test
        @DisplayName("Should not process completed refund")
        void shouldNotProcessCompletedRefund() {
            Refund refund = Refund.create(TRANSACTION_ID, AMOUNT, CURRENCY, REASON);
            refund.process();
            refund.complete();

            assertThatThrownBy(refund::process)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot process refund in status");
        }

        @Test
        @DisplayName("Should not process failed refund")
        void shouldNotProcessFailedRefund() {
            Refund refund = Refund.create(TRANSACTION_ID, AMOUNT, CURRENCY, REASON);
            refund.process();
            refund.fail("Error");

            assertThatThrownBy(refund::process)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot process refund in status");
        }

        @Test
        @DisplayName("Should not complete refund without processing")
        void shouldNotCompleteRefundWithoutProcessing() {
            Refund refund = Refund.create(TRANSACTION_ID, AMOUNT, CURRENCY, REASON);

            assertThatThrownBy(refund::complete)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot complete refund in status");
        }

        @Test
        @DisplayName("Should not fail refund without processing")
        void shouldNotFailRefundWithoutProcessing() {
            Refund refund = Refund.create(TRANSACTION_ID, AMOUNT, CURRENCY, REASON);

            assertThatThrownBy(() -> refund.fail("Error"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot fail refund in status");
        }

        @Test
        @DisplayName("Should not complete already completed refund")
        void shouldNotCompleteAlreadyCompletedRefund() {
            Refund refund = Refund.create(TRANSACTION_ID, AMOUNT, CURRENCY, REASON);
            refund.process();
            refund.complete();

            assertThatThrownBy(refund::complete)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot complete refund in status");
        }

        @Test
        @DisplayName("Should not cancel processing refund")
        void shouldNotCancelProcessingRefund() {
            Refund refund = Refund.create(TRANSACTION_ID, AMOUNT, CURRENCY, REASON);
            refund.process();

            assertThatThrownBy(() -> refund.cancel("Changed mind"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot cancel refund in status");
        }

        @Test
        @DisplayName("Should not cancel completed refund")
        void shouldNotCancelCompletedRefund() {
            Refund refund = Refund.create(TRANSACTION_ID, AMOUNT, CURRENCY, REASON);
            refund.process();
            refund.complete();

            assertThatThrownBy(() -> refund.cancel("Changed mind"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot cancel refund in status");
        }

        @Test
        @DisplayName("Should not cancel already cancelled refund")
        void shouldNotCancelAlreadyCancelledRefund() {
            Refund refund = Refund.create(TRANSACTION_ID, AMOUNT, CURRENCY, REASON);
            refund.cancel("Changed mind");

            assertThatThrownBy(() -> refund.cancel("Changed mind again"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot cancel refund in status");
        }
    }

    // ==================== BUSINESS RULE TESTS ====================

    @Nested
    @DisplayName("Business Rules")
    class BusinessRuleTests {

        @Test
        @DisplayName("Should be in terminal state when completed")
        void shouldBeInTerminalStateWhenCompleted() {
            Refund refund = Refund.create(TRANSACTION_ID, AMOUNT, CURRENCY, REASON);
            refund.process();
            refund.complete();

            assertThat(refund.isInTerminalState()).isTrue();
        }

        @Test
        @DisplayName("Should be in terminal state when failed")
        void shouldBeInTerminalStateWhenFailed() {
            Refund refund = Refund.create(TRANSACTION_ID, AMOUNT, CURRENCY, REASON);
            refund.process();
            refund.fail("Error");

            assertThat(refund.isInTerminalState()).isTrue();
        }

        @Test
        @DisplayName("Should be in terminal state when cancelled")
        void shouldBeInTerminalStateWhenCancelled() {
            Refund refund = Refund.create(TRANSACTION_ID, AMOUNT, CURRENCY, REASON);
            refund.cancel("Changed mind");

            assertThat(refund.isInTerminalState()).isTrue();
        }

        @Test
        @DisplayName("Should not be in terminal state when pending")
        void shouldNotBeInTerminalStateWhenPending() {
            Refund refund = Refund.create(TRANSACTION_ID, AMOUNT, CURRENCY, REASON);

            assertThat(refund.isInTerminalState()).isFalse();
        }

        @Test
        @DisplayName("Should not be in terminal state when processing")
        void shouldNotBeInTerminalStateWhenProcessing() {
            Refund refund = Refund.create(TRANSACTION_ID, AMOUNT, CURRENCY, REASON);
            refund.process();

            assertThat(refund.isInTerminalState()).isFalse();
        }

        @Test
        @DisplayName("Should require failure reason when failing")
        void shouldRequireFailureReasonWhenFailing() {
            Refund refund = Refund.create(TRANSACTION_ID, AMOUNT, CURRENCY, REASON);
            refund.process();

            assertThatThrownBy(() -> refund.fail(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Failure reason cannot be null or empty");

            assertThatThrownBy(() -> refund.fail(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Failure reason cannot be null or empty");
        }

        @Test
        @DisplayName("Should require cancellation reason when cancelling")
        void shouldRequireCancellationReasonWhenCancelling() {
            Refund refund = Refund.create(TRANSACTION_ID, AMOUNT, CURRENCY, REASON);

            assertThatThrownBy(() -> refund.cancel(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cancellation reason cannot be null or empty");

            assertThatThrownBy(() -> refund.cancel(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cancellation reason cannot be null or empty");
        }
    }

    // ==================== EQUALITY TESTS ====================

    @Nested
    @DisplayName("Equality and Identity")
    class EqualityTests {

        @Test
        @DisplayName("Should be equal when same ID")
        void shouldBeEqualWhenSameId() {
            UUID id = UUID.randomUUID();
            Instant now = Instant.now();
            Refund refund1 = Refund.builder()
                    .id(id)
                    .transactionId(TRANSACTION_ID)
                    .amount(AMOUNT)
                    .currency(CURRENCY)
                    .reason(REASON)
                    .status(RefundStatus.PENDING)
                    .createdAt(now)
                    .build();

            Refund refund2 = Refund.builder()
                    .id(id)
                    .transactionId(TRANSACTION_ID)
                    .amount(AMOUNT)
                    .currency(CURRENCY)
                    .reason(REASON)
                    .status(RefundStatus.PENDING)
                    .createdAt(now)
                    .build();

            assertThat(refund1).isEqualTo(refund2);
        }

        @Test
        @DisplayName("Should not be equal when different ID")
        void shouldNotBeEqualWhenDifferentId() {
            Refund refund1 = Refund.create(TRANSACTION_ID, AMOUNT, CURRENCY, REASON);
            Refund refund2 = Refund.create(TRANSACTION_ID, AMOUNT, CURRENCY, REASON);

            assertThat(refund1).isNotEqualTo(refund2);
        }
    }
}
