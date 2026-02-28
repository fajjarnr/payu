package id.payu.transaction.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for Disbursement Aggregate Root.
 *
 * <p>P0 Critical Tests - These tests verify the core disbursement lifecycle
 * and state transitions that must be correct for financial integrity.</p>
 *
 * <p>Test Categories:</p>
 * <ul>
 *   <li>Factory Methods - create(), createWithIdempotencyKey()</li>
 *   <li>State Transitions - process(), complete(), fail()</li>
 *   <li>Validation - amount limits, bank code validation</li>
 *   <li>Idempotency - key generation and uniqueness</li>
 *   <li>Edge Cases - invalid transitions, null checks</li>
 * </ul>
 *
 * @see Disbursement
 */
@Execution(ExecutionMode.CONCURRENT)
@DisplayName("Disbursement Aggregate Root Tests")
class DisbursementTest {

    private static final UUID SOURCE_ACCOUNT_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final String BANK_CODE = "014"; // BCA
    private static final String ACCOUNT_NUMBER = "1234567890";
    private static final String ACCOUNT_NAME = "John Doe";
    private static final BigDecimal AMOUNT = new BigDecimal("100000.00");
    private static final String CURRENCY = "IDR";
    private static final String IDEMPOTENCY_KEY = "idem-key-123";

    // ==================== FACTORY METHOD TESTS ====================

    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethodsTests {

        @Test
        @DisplayName("Should create disbursement with PENDING status")
        void shouldCreateDisbursementWithPendingStatus() {
            Disbursement disbursement = Disbursement.create(
                    SOURCE_ACCOUNT_ID,
                    Money.idr(AMOUNT),
                    BANK_CODE,
                    ACCOUNT_NUMBER,
                    ACCOUNT_NAME
            );

            assertThat(disbursement.getStatus()).isEqualTo(DisbursementStatus.PENDING);
            assertThat(disbursement.getSourceAccountId()).isEqualTo(SOURCE_ACCOUNT_ID);
            assertThat(disbursement.getAmount()).isEqualTo(Money.idr(AMOUNT));
            assertThat(disbursement.getBankCode()).isEqualTo(BANK_CODE);
            assertThat(disbursement.getAccountNumber()).isEqualTo(ACCOUNT_NUMBER);
            assertThat(disbursement.getAccountName()).isEqualTo(ACCOUNT_NAME);
            assertThat(disbursement.getCreatedAt()).isNotNull();
            assertThat(disbursement.getId()).isNotNull();
        }

        @Test
        @DisplayName("Should create disbursement with idempotency key")
        void shouldCreateDisbursementWithIdempotencyKey() {
            Disbursement disbursement = Disbursement.createWithIdempotencyKey(
                    SOURCE_ACCOUNT_ID,
                    Money.idr(AMOUNT),
                    BANK_CODE,
                    ACCOUNT_NUMBER,
                    ACCOUNT_NAME,
                    IDEMPOTENCY_KEY
            );

            assertThat(disbursement.getIdempotencyKey()).isEqualTo(IDEMPOTENCY_KEY);
            assertThat(disbursement.getStatus()).isEqualTo(DisbursementStatus.PENDING);
        }

        @Test
        @DisplayName("Should generate idempotency key if not provided")
        void shouldGenerateIdempotencyKeyIfNotProvided() {
            Disbursement disbursement = Disbursement.create(
                    SOURCE_ACCOUNT_ID,
                    Money.idr(AMOUNT),
                    BANK_CODE,
                    ACCOUNT_NUMBER,
                    ACCOUNT_NAME
            );

            assertThat(disbursement.getIdempotencyKey()).isNotNull();
            assertThat(disbursement.getIdempotencyKey()).isNotEmpty();
        }

        @Test
        @DisplayName("Should throw exception when source account ID is null")
        void shouldThrowExceptionWhenSourceAccountIdIsNull() {
            assertThatThrownBy(() -> Disbursement.create(
                    null,
                    Money.idr(AMOUNT),
                    BANK_CODE,
                    ACCOUNT_NUMBER,
                    ACCOUNT_NAME
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Source account ID cannot be null");
        }

        @Test
        @DisplayName("Should throw exception when amount is null")
        void shouldThrowExceptionWhenAmountIsNull() {
            assertThatThrownBy(() -> Disbursement.create(
                    SOURCE_ACCOUNT_ID,
                    null,
                    BANK_CODE,
                    ACCOUNT_NUMBER,
                    ACCOUNT_NAME
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Amount cannot be null");
        }

        @Test
        @DisplayName("Should throw exception when amount is zero or negative")
        void shouldThrowExceptionWhenAmountIsZeroOrNegative() {
            assertThatThrownBy(() -> Disbursement.create(
                    SOURCE_ACCOUNT_ID,
                    Money.idr(BigDecimal.ZERO),
                    BANK_CODE,
                    ACCOUNT_NUMBER,
                    ACCOUNT_NAME
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Amount must be positive");
        }

        @Test
        @DisplayName("Should throw exception when bank code is null or empty")
        void shouldThrowExceptionWhenBankCodeIsNullOrEmpty() {
            assertThatThrownBy(() -> Disbursement.create(
                    SOURCE_ACCOUNT_ID,
                    Money.idr(AMOUNT),
                    null,
                    ACCOUNT_NUMBER,
                    ACCOUNT_NAME
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Bank code cannot be null or empty");

            assertThatThrownBy(() -> Disbursement.create(
                    SOURCE_ACCOUNT_ID,
                    Money.idr(AMOUNT),
                    "",
                    ACCOUNT_NUMBER,
                    ACCOUNT_NAME
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Bank code cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw exception when account number is null or empty")
        void shouldThrowExceptionWhenAccountNumberIsNullOrEmpty() {
            assertThatThrownBy(() -> Disbursement.create(
                    SOURCE_ACCOUNT_ID,
                    Money.idr(AMOUNT),
                    BANK_CODE,
                    null,
                    ACCOUNT_NAME
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Account number cannot be null or empty");

            assertThatThrownBy(() -> Disbursement.create(
                    SOURCE_ACCOUNT_ID,
                    Money.idr(AMOUNT),
                    BANK_CODE,
                    "",
                    ACCOUNT_NAME
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Account number cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw exception when account name is null or empty")
        void shouldThrowExceptionWhenAccountNameIsNullOrEmpty() {
            assertThatThrownBy(() -> Disbursement.create(
                    SOURCE_ACCOUNT_ID,
                    Money.idr(AMOUNT),
                    BANK_CODE,
                    ACCOUNT_NUMBER,
                    null
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Account name cannot be null or empty");

            assertThatThrownBy(() -> Disbursement.create(
                    SOURCE_ACCOUNT_ID,
                    Money.idr(AMOUNT),
                    BANK_CODE,
                    ACCOUNT_NUMBER,
                    ""
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Account name cannot be null or empty");
        }
    }

    // ==================== STATE TRANSITION TESTS ====================

    @Nested
    @DisplayName("State Transitions")
    class StateTransitionTests {

        @Test
        @DisplayName("Should transition from PENDING to PROCESSING")
        void shouldTransitionFromPendingToProcessing() {
            Disbursement disbursement = createSampleDisbursement();

            disbursement.process();

            assertThat(disbursement.getStatus()).isEqualTo(DisbursementStatus.PROCESSING);
            assertThat(disbursement.getProcessedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should transition from PROCESSING to COMPLETED")
        void shouldTransitionFromProcessingToCompleted() {
            Disbursement disbursement = createSampleDisbursement();
            disbursement.process();

            disbursement.complete("REF12345");

            assertThat(disbursement.getStatus()).isEqualTo(DisbursementStatus.COMPLETED);
            assertThat(disbursement.getBankReference()).isEqualTo("REF12345");
            assertThat(disbursement.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should transition from PROCESSING to FAILED")
        void shouldTransitionFromProcessingToFailed() {
            Disbursement disbursement = createSampleDisbursement();
            disbursement.process();

            disbursement.fail("Invalid account number");

            assertThat(disbursement.getStatus()).isEqualTo(DisbursementStatus.FAILED);
            assertThat(disbursement.getFailureReason()).isEqualTo("Invalid account number");
            assertThat(disbursement.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should throw exception when processing non-PENDING disbursement")
        void shouldThrowExceptionWhenProcessingNonPendingDisbursement() {
            Disbursement disbursement = createSampleDisbursement();
            disbursement.process();

            assertThatThrownBy(disbursement::process)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot process disbursement in status");
        }

        @Test
        @DisplayName("Should throw exception when completing non-PROCESSING disbursement")
        void shouldThrowExceptionWhenCompletingNonProcessingDisbursement() {
            Disbursement disbursement = createSampleDisbursement();

            assertThatThrownBy(() -> disbursement.complete("REF12345"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot complete disbursement in status");
        }

        @Test
        @DisplayName("Should throw exception when failing non-PROCESSING disbursement")
        void shouldThrowExceptionWhenFailingNonProcessingDisbursement() {
            Disbursement disbursement = createSampleDisbursement();

            assertThatThrownBy(() -> disbursement.fail("Some reason"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot fail disbursement in status");
        }

        @Test
        @DisplayName("Should throw exception when completing with null bank reference")
        void shouldThrowExceptionWhenCompletingWithNullBankReference() {
            Disbursement disbursement = createSampleDisbursement();
            disbursement.process();

            assertThatThrownBy(() -> disbursement.complete(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Bank reference cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw exception when failing with null reason")
        void shouldThrowExceptionWhenFailingWithNullReason() {
            Disbursement disbursement = createSampleDisbursement();
            disbursement.process();

            assertThatThrownBy(() -> disbursement.fail(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Failure reason cannot be null or empty");
        }
    }

    // ==================== IDEMPOTENCY TESTS ====================

    @Nested
    @DisplayName("Idempotency")
    class IdempotencyTests {

        @Test
        @DisplayName("Should return true for matching idempotency key")
        void shouldReturnTrueForMatchingIdempotencyKey() {
            Disbursement disbursement = Disbursement.createWithIdempotencyKey(
                    SOURCE_ACCOUNT_ID,
                    Money.idr(AMOUNT),
                    BANK_CODE,
                    ACCOUNT_NUMBER,
                    ACCOUNT_NAME,
                    IDEMPOTENCY_KEY
            );

            assertThat(disbursement.matchesIdempotencyKey(IDEMPOTENCY_KEY)).isTrue();
        }

        @Test
        @DisplayName("Should return false for non-matching idempotency key")
        void shouldReturnFalseForNonMatchingIdempotencyKey() {
            Disbursement disbursement = Disbursement.createWithIdempotencyKey(
                    SOURCE_ACCOUNT_ID,
                    Money.idr(AMOUNT),
                    BANK_CODE,
                    ACCOUNT_NUMBER,
                    ACCOUNT_NAME,
                    IDEMPOTENCY_KEY
            );

            assertThat(disbursement.matchesIdempotencyKey("different-key")).isFalse();
        }

        @Test
        @DisplayName("Should return false for null idempotency key check")
        void shouldReturnFalseForNullIdempotencyKeyCheck() {
            Disbursement disbursement = createSampleDisbursement();

            assertThat(disbursement.matchesIdempotencyKey(null)).isFalse();
        }
    }

    // ==================== STATUS CHECK TESTS ====================

    @Nested
    @DisplayName("Status Checks")
    class StatusCheckTests {

        @Test
        @DisplayName("Should return true for isPending when status is PENDING")
        void shouldReturnTrueForIsPendingWhenStatusIsPending() {
            Disbursement disbursement = createSampleDisbursement();

            assertThat(disbursement.isPending()).isTrue();
            assertThat(disbursement.isProcessing()).isFalse();
            assertThat(disbursement.isCompleted()).isFalse();
            assertThat(disbursement.isFailed()).isFalse();
        }

        @Test
        @DisplayName("Should return true for isProcessing when status is PROCESSING")
        void shouldReturnTrueForIsProcessingWhenStatusIsProcessing() {
            Disbursement disbursement = createSampleDisbursement();
            disbursement.process();

            assertThat(disbursement.isPending()).isFalse();
            assertThat(disbursement.isProcessing()).isTrue();
            assertThat(disbursement.isCompleted()).isFalse();
            assertThat(disbursement.isFailed()).isFalse();
        }

        @Test
        @DisplayName("Should return true for isCompleted when status is COMPLETED")
        void shouldReturnTrueForIsCompletedWhenStatusIsCompleted() {
            Disbursement disbursement = createSampleDisbursement();
            disbursement.process();
            disbursement.complete("REF123");

            assertThat(disbursement.isPending()).isFalse();
            assertThat(disbursement.isProcessing()).isFalse();
            assertThat(disbursement.isCompleted()).isTrue();
            assertThat(disbursement.isFailed()).isFalse();
        }

        @Test
        @DisplayName("Should return true for isFailed when status is FAILED")
        void shouldReturnTrueForIsFailedWhenStatusIsFailed() {
            Disbursement disbursement = createSampleDisbursement();
            disbursement.process();
            disbursement.fail("Some error");

            assertThat(disbursement.isPending()).isFalse();
            assertThat(disbursement.isProcessing()).isFalse();
            assertThat(disbursement.isCompleted()).isFalse();
            assertThat(disbursement.isFailed()).isTrue();
        }

        @Test
        @DisplayName("Should return true for isTerminal when status is COMPLETED or FAILED")
        void shouldReturnTrueForIsTerminalWhenStatusIsCompletedOrFailed() {
            Disbursement completed = createSampleDisbursement();
            completed.process();
            completed.complete("REF123");

            Disbursement failed = createSampleDisbursement();
            failed.process();
            failed.fail("Some error");

            Disbursement pending = createSampleDisbursement();
            Disbursement processing = createSampleDisbursement();
            processing.process();

            assertThat(completed.isTerminal()).isTrue();
            assertThat(failed.isTerminal()).isTrue();
            assertThat(pending.isTerminal()).isFalse();
            assertThat(processing.isTerminal()).isFalse();
        }
    }

    // ==================== DESCRIPTION TESTS ====================

    @Nested
    @DisplayName("Description")
    class DescriptionTests {

        @Test
        @DisplayName("Should set and get description")
        void shouldSetAndGetDescription() {
            Disbursement disbursement = Disbursement.create(
                    SOURCE_ACCOUNT_ID,
                    Money.idr(AMOUNT),
                    BANK_CODE,
                    ACCOUNT_NUMBER,
                    ACCOUNT_NAME
            );

            disbursement.setDescription("Monthly salary payment");

            assertThat(disbursement.getDescription()).isEqualTo("Monthly salary payment");
        }

        @Test
        @DisplayName("Should allow null description")
        void shouldAllowNullDescription() {
            Disbursement disbursement = Disbursement.create(
                    SOURCE_ACCOUNT_ID,
                    Money.idr(AMOUNT),
                    BANK_CODE,
                    ACCOUNT_NUMBER,
                    ACCOUNT_NAME
            );

            disbursement.setDescription(null);

            assertThat(disbursement.getDescription()).isNull();
        }
    }

    // ==================== HELPER METHODS ====================

    private Disbursement createSampleDisbursement() {
        return Disbursement.create(
                SOURCE_ACCOUNT_ID,
                Money.idr(AMOUNT),
                BANK_CODE,
                ACCOUNT_NUMBER,
                ACCOUNT_NAME
        );
    }
}
