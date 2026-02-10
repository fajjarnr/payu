package id.payu.support.adapter.web;

import id.payu.api.common.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Base controller for Support Service providing common API response functionality.
 * Extends the shared api-commons BaseController.
 */
@RestController
public abstract class BaseController extends id.payu.api.common.controller.BaseController {

    /**
     * Creates a 404 Not Found response.
     */
    protected <T> ResponseEntity<ApiResponse<T>> notFound(String code, String message) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(code, message));
    }
}
