package id.payu.backoffice.adapter.web;

import id.payu.backoffice.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.util.UUID;

/**
 * Base controller providing common functionality for all backoffice controllers.
 * Includes utility methods for response building and request handling.
 */
public abstract class BaseController {

    protected final Logger LOG = LoggerFactory.getLogger(getClass());

    /**
     * Creates a successful API response with data.
     */
    protected <T> ResponseEntity<ApiResponse<T>> ok(T data) {
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * Creates a successful API response with data and custom request ID.
     */
    protected <T> ResponseEntity<ApiResponse<T>> ok(T data, String requestId) {
        return ResponseEntity.ok(ApiResponse.success(data, requestId));
    }

    /**
     * Creates a 201 Created response with location header.
     */
    protected <T> ResponseEntity<ApiResponse<T>> created(T data, String location) {
        return ResponseEntity
                .created(URI.create(location))
                .body(ApiResponse.success(data));
    }

    /**
     * Creates a 204 No Content response.
     */
    protected ResponseEntity<Void> noContent() {
        return ResponseEntity.noContent().build();
    }

    /**
     * Creates a 404 Not Found response.
     */
    protected <T> ResponseEntity<ApiResponse<T>> notFound(String resourceName) {
        return ResponseEntity
                .status(404)
                .body(ApiResponse.notFound(resourceName));
    }

    /**
     * Creates a 400 Bad Request response.
     */
    protected <T> ResponseEntity<ApiResponse<T>> badRequest(String message) {
        return ResponseEntity
                .status(400)
                .body(ApiResponse.badRequest(message));
    }

    /**
     * Generates a unique request ID.
     */
    protected String generateRequestId() {
        return "req-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
