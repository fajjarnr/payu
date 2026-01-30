package id.payu.saga.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.util.function.Function;

/**
 * Represents a single step in a saga orchestration.
 *
 * @param <T> The saga context type
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SagaStep<T> {

    private String name;
    private String description;

    /**
     * The action to execute for this step.
     */
    private Function<T, StepResult<T>> action;

    /**
     * The compensation action to execute if this step needs to be rolled back.
     */
    private Function<T, StepResult<T>> compensation;

    /**
     * Whether this step is critical (cannot be skipped during compensation).
     */
    @Builder.Default
    private boolean critical = false;

    /**
     * Maximum number of retries for this step.
     */
    @Builder.Default
    private int maxRetries = 0;

    /**
     * Delay between retries.
     */
    @Builder.Default
    private Duration retryDelay = Duration.ofSeconds(1);

    /**
     * Timeout for this step execution.
     */
    @Builder.Default
    private Duration timeout = Duration.ofSeconds(30);

    /**
     * Whether to continue on failure (for non-critical steps).
     */
    @Builder.Default
    private boolean continueOnFailure = false;

    /**
     * Pre-condition check before executing this step.
     */
    private Function<T, Boolean> precondition;

    /**
     * Creates a simple step with just an action.
     */
    public static <T> SagaStep<T> of(String name, Function<T, StepResult<T>> action) {
        return SagaStep.<T>builder()
                .name(name)
                .action(action)
                .build();
    }

    /**
     * Creates a step with action and compensation.
     */
    public static <T> SagaStep<T> withCompensation(String name,
                                                    Function<T, StepResult<T>> action,
                                                    Function<T, StepResult<T>> compensation) {
        return SagaStep.<T>builder()
                .name(name)
                .action(action)
                .compensation(compensation)
                .build();
    }

    /**
     * Check if this step has a compensation defined.
     */
    public boolean hasCompensation() {
        return compensation != null;
    }

    /**
     * Check if pre-condition is met for this step.
     */
    public boolean canExecute(T context) {
        if (precondition == null) {
            return true;
        }
        return precondition.apply(context);
    }
}
