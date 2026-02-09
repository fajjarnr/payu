package id.payu.saga.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SagaStep}.
 * Validates factory methods, defaults, and precondition logic.
 */
@DisplayName("SagaStep")
class SagaStepTest {

    @Nested
    @DisplayName("of() factory")
    class OfFactoryTests {

        @Test
        @DisplayName("should create step with name and action")
        void shouldCreateStepWithNameAndAction() {
            SagaStep<String> step = SagaStep.of("validate", data -> StepResult.success(data));

            assertThat(step.getName()).isEqualTo("validate");
            assertThat(step.getAction()).isNotNull();
            assertThat(step.getCompensation()).isNull();
            assertThat(step.isCritical()).isFalse();
            assertThat(step.getMaxRetries()).isZero();
            assertThat(step.getRetryDelay()).isEqualTo(Duration.ofSeconds(1));
            assertThat(step.getTimeout()).isEqualTo(Duration.ofSeconds(30));
            assertThat(step.isContinueOnFailure()).isFalse();
        }
    }

    @Nested
    @DisplayName("withCompensation() factory")
    class WithCompensationFactoryTests {

        @Test
        @DisplayName("should create step with action and compensation")
        void shouldCreateStepWithCompensation() {
            SagaStep<String> step = SagaStep.withCompensation(
                    "debit",
                    data -> StepResult.success(data),
                    data -> StepResult.success(data));

            assertThat(step.getName()).isEqualTo("debit");
            assertThat(step.getAction()).isNotNull();
            assertThat(step.getCompensation()).isNotNull();
        }
    }

    @Nested
    @DisplayName("hasCompensation()")
    class HasCompensationTests {

        @Test
        @DisplayName("should return true when compensation is set")
        void shouldReturnTrueWhenSet() {
            SagaStep<String> step = SagaStep.withCompensation(
                    "debit",
                    data -> StepResult.success(data),
                    data -> StepResult.success(data));

            assertThat(step.hasCompensation()).isTrue();
        }

        @Test
        @DisplayName("should return false when compensation is null")
        void shouldReturnFalseWhenNull() {
            SagaStep<String> step = SagaStep.of("validate", data -> StepResult.success(data));

            assertThat(step.hasCompensation()).isFalse();
        }
    }

    @Nested
    @DisplayName("canExecute()")
    class CanExecuteTests {

        @Test
        @DisplayName("should return true when no precondition")
        void shouldReturnTrueWhenNoPrecondition() {
            SagaStep<String> step = SagaStep.of("validate", data -> StepResult.success(data));

            assertThat(step.canExecute("anything")).isTrue();
        }

        @Test
        @DisplayName("should evaluate precondition when set")
        void shouldEvaluatePrecondition() {
            SagaStep<String> step = SagaStep.<String>builder()
                    .name("conditionalStep")
                    .action(data -> StepResult.success(data))
                    .precondition(data -> data.startsWith("VALID"))
                    .build();

            assertThat(step.canExecute("VALID_data")).isTrue();
            assertThat(step.canExecute("INVALID_data")).isFalse();
        }
    }

    @Nested
    @DisplayName("Action execution")
    class ActionExecutionTests {

        @Test
        @DisplayName("should execute action and return result")
        void shouldExecuteAction() {
            SagaStep<String> step = SagaStep.of("uppercase",
                    data -> StepResult.success(data.toUpperCase()));

            StepResult<String> result = step.getAction().apply("hello");

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getContext()).isEqualTo("HELLO");
        }

        @Test
        @DisplayName("should execute compensation and return result")
        void shouldExecuteCompensation() {
            SagaStep<String> step = SagaStep.withCompensation(
                    "debit",
                    data -> StepResult.success(data + "_debited"),
                    data -> StepResult.success(data + "_refunded"));

            StepResult<String> compensationResult = step.getCompensation().apply("account");

            assertThat(compensationResult.isSuccess()).isTrue();
            assertThat(compensationResult.getContext()).isEqualTo("account_refunded");
        }
    }
}
