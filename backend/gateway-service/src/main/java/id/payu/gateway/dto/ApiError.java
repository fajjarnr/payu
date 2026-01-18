package id.payu.gateway.dto;

import java.time.Instant;

/**
 * Standard API error response.
 */
public record ApiError(
    String error,
    String message,
    String path,
    int status,
    Instant timestamp,
    String correlationId,
    String requestId
) {
    public static ApiError of(String error, String message, String path, int status) {
        return new ApiError(
            error,
            message,
            path,
            status,
            Instant.now(),
            null,
            null
        );
    }

    public static ApiError of(String error, String message, String path, int status,
                               String correlationId, String requestId) {
        return new ApiError(
            error,
            message,
            path,
            status,
            Instant.now(),
            correlationId,
            requestId
        );
    }
}
