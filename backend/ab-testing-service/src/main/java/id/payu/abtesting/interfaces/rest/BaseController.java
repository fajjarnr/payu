package id.payu.abtesting.interfaces.rest;

import org.springframework.web.bind.annotation.RestController;

/**
 * Base controller for A/B Testing Service providing common API response functionality.
 * Extends the shared api-commons BaseController.
 */
@RestController
public abstract class BaseController extends id.payu.api.common.controller.BaseController {
    // All common functionality inherited from api-commons BaseController
}
