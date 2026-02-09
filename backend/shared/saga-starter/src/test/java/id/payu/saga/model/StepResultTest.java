package id.payu.saga.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link StepResult}.
 * Validates factory methods and result classification.
 */
@DisplayName("StepResult")
class StepResultTest {

    @Nested
    @DisplayName("success() factory methods")
    class SuccessTests {

        @Test
        @DisplayName("should create success result with context")
        void shouldCreateSuccessWithContext() {
            StepResult<String> result = StepResult.success("ctx");

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getContext()).isEqualTo("ctx");
            assertThat(result.isTriggerCompensation()).isFalse();
            assertThat(result.isRetryable()).isFalse();
        }

        @Test
        @DisplayName("should create success result with message")
        void shouldCreateSuccessWithMessage() {
            StepResult<String> result = StepResult.success("ctx", "Step completed");

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getMessage()).isEqualTo("Step completed");
        }

        @Test
        @DisplayName("should create success result with metadata")
        void shouldCreateSuccessWithMetadata() {
            Map<String, Object> meta = Map.of("duration", 150);
            StepResult<String> result = StepResult.success("ctx", meta);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getMetadata()).containsEntry("duration", 150);
        }
    }

    @Nested
    @DisplayName("failure() factory methods")
    class FailureTests {

        @Test
        @DisplayName("should create failure with message and trigger compensation")
        void shouldCreateFailureWithMessage() {
            StepResult<String> result = StepResult.failure("ctx", "Timed out");

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getContext()).isEqualTo("ctx");
            assertThat(result.getMessage()).isEqualTo("Timed out");
            assertThat(result.isTriggerCompensation()).isTrue();
        }

        @Test
        @DisplayName("should create failure with error and trigger compensation")
        void shouldCreateFailureWithError() {
            RuntimeException ex = new RuntimeException("DB error");
            StepResult<String> result = StepResult.failure("ctx", "DB error", ex);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getError()).isEqualTo(ex);
            assertThat(result.isTriggerCompensation()).isTrue();
        }
    }

    @Nested
    @DisplayName("retryableFailure()")
    class RetryableTests {

        @Test
        @DisplayName("should create retryable failure")
        void shouldCreateRetryableFailure() {
            RuntimeException ex = new RuntimeException("Connection reset");
            StepResult<String> result = StepResult.retryableFailure("ctx", "Retry needed", ex);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.isRetryable()).isTrue();
            assertThat(result.getError()).isEqualTo(ex);
        }
    }

    @Nested
    @DisplayName("nonCriticalFailure()")
    class NonCriticalTests {

        @Test
        @DisplayName("should create non-critical failure without compensation trigger")
        void shouldCreateNonCriticalFailure() {
            StepResult<String> result = StepResult.nonCriticalFailure("ctx", "Optional step failed");

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.isTriggerCompensation()).isFalse();
            assertThat(result.getMessage()).isEqualTo("Optional step failed");
        }
    }
}
