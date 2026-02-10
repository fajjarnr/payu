package id.payu.partner.adapter.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * Base controller providing common response building methods for all REST controllers.
 * Implements PayU's standard Response Envelope pattern using {@link ApiResponse}.
 */
public class BaseController {

    protected <T> ResponseEntity<ApiResponse<T>> ok(T data) {
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    protected <T> ResponseEntity<ApiResponse<T>> created(T data) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(data));
    }

    protected ResponseEntity<Void> noContent() {
        return ResponseEntity.noContent().build();
    }

    protected ResponseEntity<ApiResponse<Object>> badRequest(String code, String message) {
        return ResponseEntity.badRequest().body(ApiResponse.badRequest(code, message));
    }

    protected ResponseEntity<ApiResponse<Object>> badRequest(String code, String message, List<FieldError> details) {
        return ResponseEntity.badRequest().body(ApiResponse.error(code, message, details));
    }

    protected ResponseEntity<ApiResponse<Object>> unauthorized(String message) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.unauthorized(message));
    }

    protected ResponseEntity<ApiResponse<Object>> forbidden(String message) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.forbidden(message));
    }

    protected <T> ResponseEntity<ApiResponse<T>> notFound(String resource, Object identifier) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.notFound(resource, identifier));
    }

    protected ResponseEntity<ApiResponse<Object>> notFound(String message) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("NOT_FOUND", message));
    }

    protected ResponseEntity<ApiResponse<Object>> conflict(String code, String message) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(code, message));
    }

    protected ResponseEntity<ApiResponse<Object>> internalError(String message) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.internalError(message));
    }

    protected ResponseEntity<ApiResponse<Object>> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(ApiResponse.error(code, message));
    }
}
