package id.payu.promotion.resource;

import id.payu.api.common.response.ApiResponse;
import id.payu.api.common.response.MetaInfo;
import id.payu.api.common.response.PaginationInfo;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Base controller for Promotion Service providing common API response functionality.
 * Quarkus-specific implementation using JAX-RS.
 */
public abstract class BaseController {

    /**
     * Creates a successful API response with data.
     */
    protected <T> Response ok(T data) {
        return Response.ok(ApiResponse.success(data)).build();
    }

    /**
     * Creates a successful API response with data and pagination.
     */
    protected <T> Response ok(T data, long totalElements, int page, int size, String basePath) {
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

        return Response.ok(response).build();
    }

    /**
     * Creates a 201 Created response with location header.
     */
    protected <T> Response created(T data, String locationPath, Object... pathParams) {
        URI location = UriBuilder.fromPath(locationPath).build(pathParams);

        ApiResponse<T> response = ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .meta(MetaInfo.now())
                .build();

        return Response.created(location).entity(response).build();
    }

    /**
     * Creates a 204 No Content response.
     */
    protected Response noContent() {
        return Response.noContent().build();
    }

    /**
     * Creates a 202 Accepted response.
     */
    protected <T> Response accepted(T data) {
        return Response.accepted(ApiResponse.success(data)).build();
    }

    /**
     * Creates an error response.
     */
    protected Response error(int statusCode, String code, String message) {
        return Response.status(statusCode)
                .entity(ApiResponse.error(code, message))
                .build();
    }

    /**
     * Creates a 404 Not Found response.
     */
    protected Response notFound(String code, String message) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(ApiResponse.error(code, message))
                .build();
    }

    /**
     * Creates a 400 Bad Request response.
     */
    protected Response badRequest(String code, String message) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(ApiResponse.error(code, message))
                .build();
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
