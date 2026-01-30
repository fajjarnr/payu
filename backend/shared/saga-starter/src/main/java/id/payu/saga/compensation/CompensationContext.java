package id.payu.saga.compensation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Context object passed to compensation actions.
 * Contains information about the original step execution and saga state.
 *
 * @param <T> The saga data type
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompensationContext<T> {

    private String sagaId;
    private String sagaType;
    private String stepName;
    private T sagaData;

    /**
     * The result of the original step execution.
     */
    private Object originalResult;

    /**
     * The error that triggered compensation.
     */
    private Throwable error;

    /**
     * When the original step was executed.
     */
    private Instant originalStepTimestamp;

    /**
     * Additional context for compensation.
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * Whether this is a partial compensation (some steps already compensated).
     */
    @Builder.Default
    private boolean partialCompensation = false;

    /**
     * List of steps already compensated.
     */
    @Builder.Default
    private java.util.List<String> compensatedSteps = new java.util.ArrayList<>();

    /**
     * Create a new context for a step compensation.
     */
    public static <T> CompensationContext<T> forStep(String sagaId, String sagaType,
                                                      String stepName, T sagaData) {
        return CompensationContext.<T>builder()
                .sagaId(sagaId)
                .sagaType(sagaType)
                .stepName(stepName)
                .sagaData(sagaData)
                .originalStepTimestamp(Instant.now())
                .build();
    }

    /**
     * Add metadata to the context.
     */
    public CompensationContext<T> withMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        this.metadata.put(key, value);
        return this;
    }

    /**
     * Mark a step as compensated.
     */
    public void markStepCompensated(String stepName) {
        if (this.compensatedSteps == null) {
            this.compensatedSteps = new java.util.ArrayList<>();
        }
        this.compensatedSteps.add(stepName);
        this.partialCompensation = true;
    }

    /**
     * Check if a step has already been compensated.
     */
    public boolean isStepCompensated(String stepName) {
        return this.compensatedSteps != null && this.compensatedSteps.contains(stepName);
    }
}
