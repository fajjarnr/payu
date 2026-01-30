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

    /**
     * Execute the compensation action.
     *
     * @param context The context containing data needed for compensation
     * @return The result of the compensation action
     */
    StepResult<T> compensate(T context);

    /**
     * Get the name of this compensation action.
     * Default implementation returns the simple class name.
     *
     * @return The compensation action name
     */
    default String getName() {
        return this.getClass().getSimpleName();
    }

    /**
     * Check if this compensation action is applicable for the given context.
     * Default implementation always returns true.
     *
     * @param context The compensation context
     * @return true if compensation should be executed
     */
    default boolean isApplicable(T context) {
        return true;
    }

    /**
     * Get the order/priority of this compensation action.
     * Lower values indicate higher priority (executed first during compensation).
     * Default is 0.
     *
     * @return The compensation order
     */
    default int getOrder() {
        return 0;
    }

    /**
     * Create a compensating action from a lambda.
     *
     * @param action The compensation logic
     * @param <T> The context type
     * @return A CompensatingAction instance
     */
    static <T> CompensatingAction<T> of(java.util.function.Function<T, StepResult<T>> action) {
        return new CompensatingAction<>() {
            @Override
            public StepResult<T> compensate(T context) {
                return action.apply(context);
            }
        };
    }

    /**
     * Create a compensating action with a name.
     *
     * @param name The action name
     * @param action The compensation logic
     * @param <T> The context type
     * @return A CompensatingAction instance
     */
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
