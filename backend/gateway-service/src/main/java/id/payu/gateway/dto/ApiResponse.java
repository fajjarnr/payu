package id.payu.gateway.dto;

import java.time.Instant;

/**
 * Standard API response wrapper.
 */
public record ApiResponse<T>(
    boolean success,
    T data,
    ApiError error,
    Instant timestamp,
    String correlationId
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, Instant.now(), null);
    }

    public static <T> ApiResponse<T> success(T data, String correlationId) {
        return new ApiResponse<>(true, data, null, Instant.now(), correlationId);
    }

    public static <T> ApiResponse<T> error(ApiError error) {
        return new ApiResponse<>(false, null, error, Instant.now(), null);
    }

    public static <T> ApiResponse<T> error(ApiError error, String correlationId) {
        return new ApiResponse<>(false, null, error, Instant.now(), correlationId);
    }
}
