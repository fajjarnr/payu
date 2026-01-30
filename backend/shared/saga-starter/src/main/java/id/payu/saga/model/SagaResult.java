package id.payu.saga.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Represents the result of a saga execution.
 *
 * @param <T> The type of the result data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SagaResult<T> {

    private String sagaId;
    private String sagaType;
    private SagaState finalState;
    private T data;
    private String errorMessage;
    private String errorStep;
    private Instant completedAt;
    private Map<String, Object> metadata;

    /**
     * Check if saga completed successfully.
     */
    public boolean isSuccess() {
        return finalState == SagaState.COMPLETED;
    }

    /**
     * Check if saga failed.
     */
    public boolean isFailure() {
        return finalState == SagaState.FAILED ||
               finalState == SagaState.COMPENSATION_FAILED ||
               finalState == SagaState.TIMED_OUT;
    }

    /**
     * Check if saga was compensated.
     */
    public boolean isCompensated() {
        return finalState == SagaState.COMPENSATED;
    }

    /**
     * Create a success result.
     */
    public static <T> SagaResult<T> success(String sagaId, String sagaType, T data) {
        return SagaResult.<T>builder()
                .sagaId(sagaId)
                .sagaType(sagaType)
                .finalState(SagaState.COMPLETED)
                .data(data)
                .completedAt(Instant.now())
                .build();
    }

    /**
     * Create a failure result.
     */
    public static <T> SagaResult<T> failure(String sagaId, String sagaType, String errorMessage, String errorStep) {
        return SagaResult.<T>builder()
                .sagaId(sagaId)
                .sagaType(sagaType)
                .finalState(SagaState.FAILED)
                .errorMessage(errorMessage)
                .errorStep(errorStep)
                .completedAt(Instant.now())
                .build();
    }

    /**
     * Create a compensated result.
     */
    public static <T> SagaResult<T> compensated(String sagaId, String sagaType) {
        return SagaResult.<T>builder()
                .sagaId(sagaId)
                .sagaType(sagaType)
                .finalState(SagaState.COMPENSATED)
                .completedAt(Instant.now())
                .build();
    }
}
