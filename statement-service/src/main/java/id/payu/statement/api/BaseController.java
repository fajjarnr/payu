package id.payu.statement.api;

import id.payu.api.common.controller.BaseController as ApiCommonBaseController;
import org.springframework.web.bind.annotation.RestController;

/**
 * Base controller for Statement Service providing common API response functionality.
 * Extends the shared api-commons BaseController.
 */
@RestController
public abstract class BaseController extends ApiCommonBaseController {
    // All common functionality inherited from api-commons BaseController
}
