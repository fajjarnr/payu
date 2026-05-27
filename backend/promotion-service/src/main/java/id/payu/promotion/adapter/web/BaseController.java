package id.payu.promotion.adapter.web;

import id.payu.api.common.response.ApiResponse;
import id.payu.api.common.response.MetaInfo;
import id.payu.api.common.response.PaginationInfo;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Base controller for PromotionEntity Service providing common API response functionality.
 * Spring Boot-specific implementation using Spring MVC.
 */
public abstract class BaseController extends id.payu.api.common.controller.BaseController {

    /**
     * Creates a successful API response with data and pagination.
     */
    protected <T> ResponseEntity<ApiResponse<T>> ok(T data, long totalElements, int page, int size, String basePath) {
        PaginationInfo pagination = PaginationInfo.builder()
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages((int) Math.ceil((double) totalElements / size))
                .build();

        ApiResponse<T> response = ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .meta(MetaInfo.now())
                .pagination(pagination)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Creates a 201 Created response with location header.
     */
    protected <T> ResponseEntity<ApiResponse<T>> created(T data, String locationPath, Object... pathParams) {
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .replacePath(locationPath)
                .buildAndExpand(pathParams)
                .toUri();

        ApiResponse<T> response = ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .meta(MetaInfo.now())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).location(location).body(response);
    }


    /**
     * Creates a 202 Accepted response.
     */
    protected <T> ResponseEntity<ApiResponse<T>> accepted(T data) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.success(data));
    }

    /**
     * Creates an error response.
     */
    protected ResponseEntity<ApiResponse<Void>> error(int statusCode, String code, String message) {
        return ResponseEntity.status(statusCode)
                .body(ApiResponse.error(code, message));
    }

    /**
     * Creates a 404 Not Found response.
     */
    protected ResponseEntity<ApiResponse<Void>> notFound(String code, String message) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(code, message));
    }

    /**
     * Creates a 400 Bad Request response.
     */
    protected ResponseEntity<ApiResponse<Void>> badRequest(String code, String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(code, message));
    }

    /**
     * Generates a unique request ID.
     */
    protected String generateRequestId() {
        return "req-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Generates a correlation ID.
     */
    protected String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }
}
