package id.payu.saga.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SagaState} enum.
 * Validates state classification methods.
 */
@DisplayName("SagaState")
class SagaStateTest {

    @Nested
    @DisplayName("isTerminal()")
    class IsTerminalTests {

        @ParameterizedTest
        @EnumSource(value = SagaState.class, names = {"COMPLETED", "FAILED", "COMPENSATED", "COMPENSATION_FAILED", "CANCELLED"})
        @DisplayName("should return true for terminal states")
        void shouldReturnTrueForTerminalStates(SagaState state) {
            assertThat(state.isTerminal()).isTrue();
        }

        @ParameterizedTest
        @EnumSource(value = SagaState.class, names = {"PENDING", "STARTED", "IN_PROGRESS", "WAITING_FOR_RESPONSE", "COMPENSATING", "TIMED_OUT", "RETRYING", "PAUSED"})
        @DisplayName("should return false for non-terminal states")
        void shouldReturnFalseForNonTerminalStates(SagaState state) {
            assertThat(state.isTerminal()).isFalse();
        }
    }

    @Nested
    @DisplayName("isCompensating()")
    class IsCompensatingTests {

        @ParameterizedTest
        @EnumSource(value = SagaState.class, names = {"COMPENSATING", "COMPENSATED", "COMPENSATION_FAILED"})
        @DisplayName("should return true for compensation-related states")
        void shouldReturnTrueForCompensationStates(SagaState state) {
            assertThat(state.isCompensating()).isTrue();
        }

        @ParameterizedTest
        @EnumSource(value = SagaState.class, names = {"PENDING", "STARTED", "IN_PROGRESS", "COMPLETED", "FAILED", "TIMED_OUT", "RETRYING", "PAUSED", "CANCELLED"})
        @DisplayName("should return false for non-compensation states")
        void shouldReturnFalseForNonCompensationStates(SagaState state) {
            assertThat(state.isCompensating()).isFalse();
        }
    }

    @Nested
    @DisplayName("isRetryable()")
    class IsRetryableTests {

        @ParameterizedTest
        @EnumSource(value = SagaState.class, names = {"FAILED", "TIMED_OUT", "RETRYING"})
        @DisplayName("should return true for retryable states")
        void shouldReturnTrueForRetryableStates(SagaState state) {
            assertThat(state.isRetryable()).isTrue();
        }

        @ParameterizedTest
        @EnumSource(value = SagaState.class, names = {"PENDING", "STARTED", "IN_PROGRESS", "COMPLETED", "COMPENSATING", "COMPENSATED", "COMPENSATION_FAILED", "PAUSED", "CANCELLED"})
        @DisplayName("should return false for non-retryable states")
        void shouldReturnFalseForNonRetryableStates(SagaState state) {
            assertThat(state.isRetryable()).isFalse();
        }
    }

    @Nested
    @DisplayName("Enum properties")
    class EnumPropertyTests {

        @Test
        @DisplayName("should have 13 states total")
        void shouldHave13States() {
            assertThat(SagaState.values()).hasSize(13);
        }

        @Test
        @DisplayName("each state should have a description")
        void eachStateShouldHaveDescription() {
            for (SagaState state : SagaState.values()) {
                assertThat(state.getDescription()).isNotBlank();
            }
        }
    }
}
