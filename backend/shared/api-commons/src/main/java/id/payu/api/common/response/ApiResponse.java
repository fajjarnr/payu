package id.payu.api.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Standard API response envelope for all PayU API endpoints.
 * Provides consistent structure for both success and error responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard API response envelope")
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

    /**
     * Creates a successful response with data.
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .meta(MetaInfo.now())
                .build();
    }

    /**
     * Creates a successful response with data and pagination.
     */
    public static <T> ApiResponse<T> success(T data, PaginationInfo pagination) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .meta(MetaInfo.now())
                .pagination(pagination)
                .build();
    }

    /**
     * Creates an error response with code and message.
     */
    public static <T> ApiResponse<T> error(String code, String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(ErrorInfo.of(code, message))
                .meta(MetaInfo.now())
                .build();
    }

    /**
     * Creates an error response with code, message, and details.
     */
    public static <T> ApiResponse<T> error(String code, String message, java.util.List<FieldError> details) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(ErrorInfo.of(code, message, details))
                .meta(MetaInfo.now())
                .build();
    }

    /**
     * Creates an error response from ErrorInfo.
     */
    public static <T> ApiResponse<T> error(ErrorInfo errorInfo) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(errorInfo)
                .meta(MetaInfo.now())
                .build();
    }
}
