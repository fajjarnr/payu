package id.payu.saga.entity;

import id.payu.saga.model.SagaState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SagaInstance} entity.
 * Validates state transitions, step tracking, retry logic, and compensation ordering.
 */
@DisplayName("SagaInstance Entity")
class SagaInstanceTest {

    @Nested
    @DisplayName("create()")
    class CreateTests {

        @Test
        @DisplayName("should create instance with generated UUID")
        void shouldCreateWithGeneratedUuid() {
            SagaInstance instance = SagaInstance.create("TransferSaga", SagaState.STARTED.name(), Map.of("amount", 100));

            assertThat(instance.getSagaId()).isNotNull();
            assertThat(instance.getSagaId()).hasSize(36); // UUID format
            assertThat(instance.getSagaType()).isEqualTo("TransferSaga");
            assertThat(instance.getCurrentState()).isEqualTo("STARTED");
            assertThat(instance.getPayload()).containsEntry("amount", 100);
        }

        @Test
        @DisplayName("should handle null payload by using empty map")
        void shouldHandleNullPayload() {
            SagaInstance instance = SagaInstance.create("TestSaga", SagaState.PENDING.name(), null);

            assertThat(instance.getPayload()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("should initialize with defaults")
        void shouldInitializeDefaults() {
            SagaInstance instance = SagaInstance.create("TestSaga", SagaState.STARTED.name(), Map.of());

            assertThat(instance.getRetryCount()).isZero();
            assertThat(instance.getMaxRetries()).isEqualTo(3);
            assertThat(instance.getCompletedSteps()).isNotNull().isEmpty();
            assertThat(instance.getStepContext()).isNotNull().isEmpty();
        }
    }

    @Nested
    @DisplayName("transitionTo()")
    class TransitionTests {

        @Test
        @DisplayName("should update state and preserve previous state")
        void shouldUpdateStateAndPreservePrevious() {
            SagaInstance instance = SagaInstance.create("TestSaga", SagaState.STARTED.name(), Map.of());

            instance.transitionTo(SagaState.IN_PROGRESS.name());

            assertThat(instance.getCurrentState()).isEqualTo("IN_PROGRESS");
            assertThat(instance.getPreviousState()).isEqualTo("STARTED");
        }

        @Test
        @DisplayName("should update lastUpdatedAt on transition")
        void shouldUpdateTimestamp() {
            SagaInstance instance = SagaInstance.create("TestSaga", SagaState.STARTED.name(), Map.of());
            Instant before = Instant.now();

            instance.transitionTo(SagaState.IN_PROGRESS.name());

            assertThat(instance.getLastUpdatedAt()).isAfterOrEqualTo(before);
        }

        @Test
        @DisplayName("should support multiple transitions")
        void shouldSupportMultipleTransitions() {
            SagaInstance instance = SagaInstance.create("TestSaga", SagaState.STARTED.name(), Map.of());

            instance.transitionTo(SagaState.IN_PROGRESS.name());
            instance.transitionTo(SagaState.COMPENSATING.name());
            instance.transitionTo(SagaState.COMPENSATED.name());

            assertThat(instance.getCurrentState()).isEqualTo("COMPENSATED");
            assertThat(instance.getPreviousState()).isEqualTo("COMPENSATING");
        }
    }

    @Nested
    @DisplayName("recordStepCompletion()")
    class RecordStepTests {

        @Test
        @DisplayName("should add step to completedSteps and stepContext")
        void shouldRecordStep() {
            SagaInstance instance = SagaInstance.create("TestSaga", SagaState.STARTED.name(), Map.of());

            instance.recordStepCompletion("validateInput", Map.of("valid", true));

            assertThat(instance.getCompletedSteps()).containsExactly("validateInput");
            assertThat(instance.getStepContext()).containsKey("validateInput");
        }

        @Test
        @DisplayName("should record multiple steps in order")
        void shouldRecordMultipleStepsInOrder() {
            SagaInstance instance = SagaInstance.create("TestSaga", SagaState.STARTED.name(), Map.of());

            instance.recordStepCompletion("step1", Map.of("result", "ok1"));
            instance.recordStepCompletion("step2", Map.of("result", "ok2"));
            instance.recordStepCompletion("step3", Map.of("result", "ok3"));

            assertThat(instance.getCompletedSteps()).containsExactly("step1", "step2", "step3");
            assertThat(instance.getStepContext()).hasSize(3);
        }

        @Test
        @DisplayName("should initialize lists if null")
        void shouldInitializeListsIfNull() {
            SagaInstance instance = new SagaInstance();
            instance.setCompletedSteps(null);
            instance.setStepContext(null);

            instance.recordStepCompletion("step1", "result");

            assertThat(instance.getCompletedSteps()).containsExactly("step1");
            assertThat(instance.getStepContext()).containsEntry("step1", "result");
        }
    }

    @Nested
    @DisplayName("complete()")
    class CompleteTests {

        @Test
        @DisplayName("should set completedAt and lastUpdatedAt")
        void shouldSetCompletedAt() {
            SagaInstance instance = SagaInstance.create("TestSaga", SagaState.STARTED.name(), Map.of());
            Instant before = Instant.now();

            instance.complete();

            assertThat(instance.getCompletedAt()).isAfterOrEqualTo(before);
            assertThat(instance.getLastUpdatedAt()).isAfterOrEqualTo(before);
        }
    }

    @Nested
    @DisplayName("recordError()")
    class RecordErrorTests {

        @Test
        @DisplayName("should record error step and message")
        void shouldRecordError() {
            SagaInstance instance = SagaInstance.create("TestSaga", SagaState.STARTED.name(), Map.of());

            instance.recordError("debitAccount", "Insufficient funds");

            assertThat(instance.getErrorStep()).isEqualTo("debitAccount");
            assertThat(instance.getErrorMessage()).isEqualTo("Insufficient funds");
        }
    }

    @Nested
    @DisplayName("Retry logic")
    class RetryLogicTests {

        @Test
        @DisplayName("incrementRetry should increment from 0")
        void shouldIncrementFromZero() {
            SagaInstance instance = SagaInstance.create("TestSaga", SagaState.STARTED.name(), Map.of());

            instance.incrementRetry();

            assertThat(instance.getRetryCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("incrementRetry should handle null retryCount")
        void shouldHandleNullRetryCount() {
            SagaInstance instance = new SagaInstance();
            instance.setRetryCount(null);

            instance.incrementRetry();

            assertThat(instance.getRetryCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("isMaxRetriesExceeded should return false when under limit")
        void shouldReturnFalseWhenUnderLimit() {
            SagaInstance instance = SagaInstance.create("TestSaga", SagaState.STARTED.name(), Map.of());
            instance.setRetryCount(2);
            instance.setMaxRetries(3);

            assertThat(instance.isMaxRetriesExceeded()).isFalse();
        }

        @Test
        @DisplayName("isMaxRetriesExceeded should return true when at limit")
        void shouldReturnTrueWhenAtLimit() {
            SagaInstance instance = SagaInstance.create("TestSaga", SagaState.STARTED.name(), Map.of());
            instance.setRetryCount(3);
            instance.setMaxRetries(3);

            assertThat(instance.isMaxRetriesExceeded()).isTrue();
        }

        @Test
        @DisplayName("isMaxRetriesExceeded should return true when over limit")
        void shouldReturnTrueWhenOverLimit() {
            SagaInstance instance = SagaInstance.create("TestSaga", SagaState.STARTED.name(), Map.of());
            instance.setRetryCount(5);
            instance.setMaxRetries(3);

            assertThat(instance.isMaxRetriesExceeded()).isTrue();
        }

        @Test
        @DisplayName("isMaxRetriesExceeded should handle null retryCount")
        void shouldHandleNullRetryCountInCheck() {
            SagaInstance instance = new SagaInstance();
            instance.setRetryCount(null);
            instance.setMaxRetries(3);

            assertThat(instance.isMaxRetriesExceeded()).isFalse();
        }

        @Test
        @DisplayName("isMaxRetriesExceeded should handle null maxRetries")
        void shouldHandleNullMaxRetries() {
            SagaInstance instance = new SagaInstance();
            instance.setRetryCount(5);
            instance.setMaxRetries(null);

            assertThat(instance.isMaxRetriesExceeded()).isFalse();
        }
    }

    @Nested
    @DisplayName("getStepsForCompensation()")
    class CompensationOrderTests {

        @Test
        @DisplayName("should return steps in reverse order (LIFO)")
        void shouldReturnStepsInReverseOrder() {
            SagaInstance instance = SagaInstance.create("TestSaga", SagaState.STARTED.name(), Map.of());
            instance.recordStepCompletion("step1", "result1");
            instance.recordStepCompletion("step2", "result2");
            instance.recordStepCompletion("step3", "result3");

            List<String> compensationOrder = instance.getStepsForCompensation();

            assertThat(compensationOrder).containsExactly("step3", "step2", "step1");
        }

        @Test
        @DisplayName("should return empty list when no completed steps")
        void shouldReturnEmptyWhenNoSteps() {
            SagaInstance instance = SagaInstance.create("TestSaga", SagaState.STARTED.name(), Map.of());

            assertThat(instance.getStepsForCompensation()).isEmpty();
        }

        @Test
        @DisplayName("should return empty list when completedSteps is null")
        void shouldReturnEmptyWhenNull() {
            SagaInstance instance = new SagaInstance();
            instance.setCompletedSteps(null);

            assertThat(instance.getStepsForCompensation()).isEmpty();
        }

        @Test
        @DisplayName("should not modify original completedSteps list")
        void shouldNotModifyOriginal() {
            SagaInstance instance = SagaInstance.create("TestSaga", SagaState.STARTED.name(), Map.of());
            instance.recordStepCompletion("step1", "r1");
            instance.recordStepCompletion("step2", "r2");

            instance.getStepsForCompensation(); // Should not mutate

            assertThat(instance.getCompletedSteps()).containsExactly("step1", "step2");
        }
    }
}
