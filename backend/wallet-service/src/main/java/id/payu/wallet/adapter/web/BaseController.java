package id.payu.wallet.adapter.web;

import id.payu.api.common.response.ApiResponse;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.util.List;

/**
 * Base controller for wallet-service providing common response utilities.
 * Extends api-commons BaseController for standard PayU API patterns.
 */
public abstract class BaseController extends id.payu.api.common.controller.BaseController {

    /**
     * Creates a successful API response for list data.
     */
    protected <T> ResponseEntity<ApiResponse<List<T>>> okList(List<T> data) {
        return ok(data);
    }
}
