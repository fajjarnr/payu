package id.payu.investment.adapter.web;

import id.payu.api.common.response.ApiResponse;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Base controller for investment-service providing common response utilities.
 * Extends api-commons BaseController for standard PayU API patterns.
 */
public abstract class BaseController extends id.payu.api.common.controller.BaseController {

    /**
     * Creates a successful API response with data.
     */
    protected <T> ResponseEntity<ApiResponse<T>> ok(T data) {
        return ResponseEntity.ok(ApiResponse.success(data));
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
     * Creates a successful API response for list data.
     */
    protected <T> ResponseEntity<ApiResponse<List<T>>> okList(List<T> data) {
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * Wraps a CompletableFuture result in ApiResponse.
     */
    protected <T> CompletableFuture<ResponseEntity<ApiResponse<T>>> okAsync(CompletableFuture<T> future) {
        return future.thenApply(this::ok);
    }
}
