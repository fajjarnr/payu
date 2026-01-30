package id.payu.saga.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the result of a saga step execution.
 *
 * @param <T> The saga context type
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StepResult<T> {

    private boolean success;
    private T context;
    private String message;
    private Throwable error;

    /**
     * Additional metadata about the step execution.
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * Whether this step should trigger compensation.
     */
    @Builder.Default
    private boolean triggerCompensation = false;

    /**
     * Whether to retry this step.
     */
    @Builder.Default
    private boolean retryable = false;

    /**
     * Create a successful result.
     */
    public static <T> StepResult<T> success(T context) {
        return StepResult.<T>builder()
                .success(true)
                .context(context)
                .build();
    }

    /**
     * Create a successful result with message.
     */
    public static <T> StepResult<T> success(T context, String message) {
        return StepResult.<T>builder()
                .success(true)
                .context(context)
                .message(message)
                .build();
    }

    /**
     * Create a successful result with metadata.
     */
    public static <T> StepResult<T> success(T context, Map<String, Object> metadata) {
        return StepResult.<T>builder()
                .success(true)
                .context(context)
                .metadata(metadata)
                .build();
    }

    /**
     * Create a failure result.
     */
    public static <T> StepResult<T> failure(T context, String message) {
        return StepResult.<T>builder()
                .success(false)
                .context(context)
                .message(message)
                .triggerCompensation(true)
                .build();
    }

    /**
     * Create a failure result with error.
     */
    public static <T> StepResult<T> failure(T context, String message, Throwable error) {
        return StepResult.<T>builder()
                .success(false)
                .context(context)
                .message(message)
                .error(error)
                .triggerCompensation(true)
                .build();
    }

    /**
     * Create a failure result that can be retried.
     */
    public static <T> StepResult<T> retryableFailure(T context, String message, Throwable error) {
        return StepResult.<T>builder()
                .success(false)
                .context(context)
                .message(message)
                .error(error)
                .retryable(true)
                .build();
    }

    /**
     * Create a result that skips compensation (for non-critical failures).
     */
    public static <T> StepResult<T> nonCriticalFailure(T context, String message) {
        return StepResult.<T>builder()
                .success(false)
                .context(context)
                .message(message)
                .triggerCompensation(false)
                .build();
    }

    /**
     * Add metadata to the result.
     */
    public StepResult<T> withMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        this.metadata.put(key, value);
        return this;
    }
}
