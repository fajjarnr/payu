package id.payu.api.common.controller;

import id.payu.api.common.response.ApiResponse;
import id.payu.api.common.response.MetaInfo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;

import java.time.Instant;

/**
 * Base controller providing common functionality for all PayU API controllers.
 * Includes utility methods for pagination, response building, and metadata handling.
 */
public abstract class BaseController {

    /**
     * Creates a successful API response with data.
     */
    protected <T> ResponseEntity<ApiResponse<T>> ok(T data) {
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * Creates a successful API response with data and pagination.
     */
    protected <T> ResponseEntity<ApiResponse<T>> ok(T data, Page<?> page) {
        String baseUrl = getBaseUrl();
        return ResponseEntity.ok(ApiResponse.success(data,
                id.payu.api.common.response.PaginationInfo.from(page, baseUrl)));
    }

    /**
     * Creates a 201 Created response with location header.
     */
    protected <T> ResponseEntity<ApiResponse<T>> created(T data, String location) {
        return ResponseEntity
                .created(java.net.URI.create(location))
                .body(ApiResponse.success(data));
    }

    /**
     * Creates a 204 No Content response.
     */
    protected ResponseEntity<Void> noContent() {
        return ResponseEntity.noContent().build();
    }

    /**
     * Gets the base URL for building pagination links.
     * Subclasses can override this to customize the base URL.
     */
    protected String getBaseUrl() {
        // Default implementation - subclasses can override
        return "";
    }

    /**
     * Creates a Pageable object from request parameters.
     */
    protected Pageable createPageable(Integer page, Integer size, String sortBy, String sortDirection) {
        int pageNum = page != null ? page : id.payu.api.common.constant.ApiConstants.DEFAULT_PAGE;
        int sizeNum = size != null ? size : id.payu.api.common.constant.ApiConstants.DEFAULT_PAGE_SIZE;

        // Validate and clamp size
        sizeNum = Math.max(
                id.payu.api.common.constant.ApiConstants.MIN_PAGE_SIZE,
                Math.min(sizeNum, id.payu.api.common.constant.ApiConstants.MAX_PAGE_SIZE)
        );

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        String sortField = sortBy != null && !sortBy.isEmpty()
                ? sortBy
                : id.payu.api.common.constant.ApiConstants.DEFAULT_SORT_FIELD;

        return PageRequest.of(pageNum, sizeNum, Sort.by(direction, sortField));
    }

    /**
     * Creates a default Pageable object.
     */
    protected Pageable createPageable() {
        return PageRequest.of(
                id.payu.api.common.constant.ApiConstants.DEFAULT_PAGE,
                id.payu.api.common.constant.ApiConstants.DEFAULT_PAGE_SIZE,
                Sort.by(
                        Sort.Direction.DESC,
                        id.payu.api.common.constant.ApiConstants.DEFAULT_SORT_FIELD
                )
        );
    }

    /**
     * Extracts request ID from servlet request or generates a new one.
     */
    protected String getRequestId(HttpServletRequest request) {
        String requestId = request.getHeader(id.payu.api.common.constant.ApiConstants.REQUEST_ID_HEADER);
        return requestId != null ? requestId : "req-" + java.util.UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Extracts correlation ID from servlet request or generates a new one.
     */
    protected String getCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader(id.payu.api.common.constant.ApiConstants.CORRELATION_ID_HEADER);
        return correlationId != null ? correlationId : java.util.UUID.randomUUID().toString();
    }

    /**
     * Creates MetaInfo with request ID from servlet request.
     */
    protected MetaInfo createMetaInfo(HttpServletRequest request) {
        return MetaInfo.withRequestId(getRequestId(request));
    }
}
