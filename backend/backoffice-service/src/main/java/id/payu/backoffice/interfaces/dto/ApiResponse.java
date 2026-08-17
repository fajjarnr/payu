package id.payu.backoffice.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Standard API response envelope for all backoffice API endpoints.
 * Provides consistent structure for both success and error responses.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard API response envelope")
public record ApiResponse<T>(

        @Schema(description = "Indicates if the request was successful", example = "true")
        boolean success,

        @Schema(description = "Response data payload")
        T data,

        @Schema(description = "Error information (present only when success=false)")
        ErrorInfo error,

        @Schema(description = "Response metadata including request ID and timestamp")
        MetaInfo meta
) {
    /**
     * Creates a successful response with data.
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(
                true,
                data,
                null,
                new MetaInfo("req-" + UUID.randomUUID().toString().substring(0, 8), Instant.now())
        );
    }

    /**
     * Creates a successful response with data and custom request ID.
     */
    public static <T> ApiResponse<T> success(T data, String requestId) {
        return new ApiResponse<>(
                true,
                data,
                null,
                new MetaInfo(requestId, Instant.now())
        );
    }

    /**
     * Creates an error response with code and message.
     */
    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(
                false,
                null,
                new ErrorInfo(code, message, null),
                new MetaInfo("req-" + UUID.randomUUID().toString().substring(0, 8), Instant.now())
        );
    }

    /**
     * Creates an error response with code, message, and custom request ID.
     */
    public static <T> ApiResponse<T> error(String code, String message, String requestId) {
        return new ApiResponse<>(
                false,
                null,
                new ErrorInfo(code, message, null),
                new MetaInfo(requestId, Instant.now())
        );
    }

    /**
     * Creates a not found error response.
     */
    public static <T> ApiResponse<T> notFound(String resourceName) {
        return error("NOT_FOUND", resourceName + " not found");
    }

    /**
     * Creates a bad request error response.
     */
    public static <T> ApiResponse<T> badRequest(String message) {
        return error("BAD_REQUEST", message);
    }

    /**
     * Metadata information for API responses.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Response metadata")
    public record MetaInfo(
            @Schema(description = "Unique request identifier for tracing", example = "req-abc-123")
            String requestId,

            @Schema(description = "Response timestamp", example = "2026-01-31T10:30:00Z")
            Instant timestamp
    ) {}

    /**
     * Error information for failed responses.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Error information")
    public record ErrorInfo(
            @Schema(description = "Unique error code", example = "NOT_FOUND")
            String code,

            @Schema(description = "Human-readable error message", example = "Resource not found")
            String message,

            @Schema(description = "Field-level validation errors")
            String details
    ) {}
}
