package id.payu.partner.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Standard API response envelope for all PayU API endpoints.
 * Provides consistent structure for both success and error responses.
 */
@Schema(description = "Standard API response envelope")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    @Schema(description = "Indicates if the request was successful", example = "true")
    private boolean success;

    @Schema(description = "Response data payload")
    private T data;

    @Schema(description = "Error information (present only when success=false)")
    private ErrorInfo error;

    @Schema(description = "Response metadata including request ID and timestamp")
    private MetaInfo meta;

    @Schema(description = "Pagination information (present only for paginated responses)")
    private PaginationInfo pagination;

    public ApiResponse() {
    }

    public ApiResponse(boolean success, T data, ErrorInfo error, MetaInfo meta, PaginationInfo pagination) {
        this.success = success;
        this.data = data;
        this.error = error;
        this.meta = meta;
        this.pagination = pagination;
    }

    /**
     * Creates a successful response with data.
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(
            true,
            data,
            null,
            MetaInfo.now(),
            null
        );
    }

    /**
     * Creates a successful response with data and pagination.
     */
    public static <T> ApiResponse<T> success(T data, PaginationInfo pagination) {
        return new ApiResponse<>(
            true,
            data,
            null,
            MetaInfo.now(),
            pagination
        );
    }

    /**
     * Creates an error response with code and message.
     */
    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(
            false,
            null,
            ErrorInfo.of(code, message),
            MetaInfo.now(),
            null
        );
    }

    /**
     * Creates an error response with code, message, and details.
     */
    public static <T> ApiResponse<T> error(String code, String message, List<FieldError> details) {
        return new ApiResponse<>(
            false,
            null,
            ErrorInfo.of(code, message, details),
            MetaInfo.now(),
            null
        );
    }

    /**
     * Creates an error response from ErrorInfo.
     */
    public static <T> ApiResponse<T> error(ErrorInfo errorInfo) {
        return new ApiResponse<>(
            false,
            null,
            errorInfo,
            MetaInfo.now(),
            null
        );
    }

    /**
     * Creates a not found error response.
     */
    public static <T> ApiResponse<T> notFound(String resource, Object identifier) {
        return error("NOT_FOUND", String.format("%s with identifier '%s' not found", resource, identifier));
    }

    /**
     * Creates a bad request error response.
     */
    public static <T> ApiResponse<T> badRequest(String code, String message) {
        return error(code, message);
    }

    /**
     * Creates an unauthorized error response.
     */
    public static <T> ApiResponse<T> unauthorized(String message) {
        return error("UNAUTHORIZED", message);
    }

    /**
     * Creates a forbidden error response.
     */
    public static <T> ApiResponse<T> forbidden(String message) {
        return error("FORBIDDEN", message);
    }

    /**
     * Creates an internal server error response.
     */
    public static <T> ApiResponse<T> internalError(String message) {
        return error("INTERNAL_ERROR", message);
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public ErrorInfo getError() {
        return error;
    }

    public void setError(ErrorInfo error) {
        this.error = error;
    }

    public MetaInfo getMeta() {
        return meta;
    }

    public void setMeta(MetaInfo meta) {
        this.meta = meta;
    }

    public PaginationInfo getPagination() {
        return pagination;
    }

    public void setPagination(PaginationInfo pagination) {
        this.pagination = pagination;
    }
}
