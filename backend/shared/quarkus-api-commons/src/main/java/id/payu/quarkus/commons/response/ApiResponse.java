package id.payu.quarkus.commons.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private T data;
    private ErrorInfo error;
    private MetaInfo meta;
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

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, MetaInfo.now(), null);
    }

    public static <T> ApiResponse<T> success(T data, PaginationInfo pagination) {
        return new ApiResponse<>(true, data, null, MetaInfo.now(), pagination);
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(false, null, ErrorInfo.of(code, message), MetaInfo.now(), null);
    }

    public static <T> ApiResponse<T> error(String code, String message, java.util.List<FieldError> details) {
        return new ApiResponse<>(false, null, ErrorInfo.of(code, message, details), MetaInfo.now(), null);
    }

    public static <T> ApiResponse<T> error(ErrorInfo errorInfo) {
        return new ApiResponse<>(false, null, errorInfo, MetaInfo.now(), null);
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
    public ErrorInfo getError() { return error; }
    public void setError(ErrorInfo error) { this.error = error; }
    public MetaInfo getMeta() { return meta; }
    public void setMeta(MetaInfo meta) { this.meta = meta; }
    public PaginationInfo getPagination() { return pagination; }
    public void setPagination(PaginationInfo pagination) { this.pagination = pagination; }
}
