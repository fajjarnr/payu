package id.payu.saga.compensation;

import id.payu.saga.model.StepResult;

import java.util.Map;

/**
 * Interface for compensating actions in saga pattern.
 * Implementations should provide the logic to undo a specific saga step.
 *
 * @param <T> The type of context/data needed for compensation
 */
@FunctionalInterface
public interface CompensatingAction<T> {

    StepResult<T> compensate(T context);

    /**
     * Get the name of this compensation action.
     * Default implementation returns the simple class name.
     */
    default String getName() {
        return this.getClass().getSimpleName();
    }

    /**
     * Check if this compensation action is applicable for the given context.
     * Default implementation always returns true.
     */
    default boolean isApplicable(T context) {
        return true;
    }

    /**
     * Get the order/priority of this compensation action.
     * Lower values indicate higher priority (executed first during compensation).
     * Default is 0.
     */
    default int getOrder() {
        return 0;
    }

    static <T> CompensatingAction<T> of(java.util.function.Function<T, StepResult<T>> action) {
        return new CompensatingAction<>() {
            @Override
            public StepResult<T> compensate(T context) {
                return action.apply(context);
            }
        };
    }

    static <T> CompensatingAction<T> named(String name, java.util.function.Function<T, StepResult<T>> action) {
        return new CompensatingAction<>() {
            @Override
            public StepResult<T> compensate(T context) {
                return action.apply(context);
            }

            @Override
            public String getName() {
                return name;
            }
        };
    }
}
