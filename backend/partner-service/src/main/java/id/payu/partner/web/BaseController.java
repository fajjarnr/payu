package id.payu.partner.web;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;

import java.util.List;

/**
 * Base controller providing common response building methods for all REST resources.
 * Implements PayU's standard Response Envelope pattern using {@link ApiResponse}.
 *
 * <p>This class provides utility methods for creating consistent HTTP responses
 * following PayU's API standards. All resource classes should extend or delegate
 * to this base controller for response building.</p>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Standardized success responses with data payload</li>
 *   <li>Standardized error responses with error codes and messages</li>
 *   <li>Response envelope pattern with metadata</li>
 *   <li>Consistent HTTP status code mapping</li>
 * </ul>
 */
public class BaseController {

    /**
     * Creates a successful OK (200) response with data.
     *
     * @param data the response data payload
     * @param <T>  the type of data
     * @return JAX-RS Response with 200 status and ApiResponse envelope
     */
    protected <T> Response ok(T data) {
        return Response.status(Status.OK)
            .entity(ApiResponse.success(data))
            .build();
    }

    /**
     * Creates a CREATED (201) response with data.
     *
     * @param data the response data payload
     * @param <T>  the type of data
     * @return JAX-RS Response with 201 status and ApiResponse envelope
     */
    protected <T> Response created(T data) {
        return Response.status(Status.CREATED)
            .entity(ApiResponse.success(data))
            .build();
    }

    /**
     * Creates a NO CONTENT (204) response.
     * Used for successful DELETE operations or updates with no return data.
     *
     * @return JAX-Rs Response with 204 status
     */
    protected Response noContent() {
        return Response.status(Status.NO_CONTENT)
            .build();
    }

    /**
     * Creates a BAD REQUEST (400) error response.
     *
     * @param code    the error code
     * @param message the error message
     * @return JAX-RS Response with 400 status and ApiResponse error envelope
     */
    protected Response badRequest(String code, String message) {
        return Response.status(Status.BAD_REQUEST)
            .entity(ApiResponse.badRequest(code, message))
            .build();
    }

    /**
     * Creates a BAD REQUEST (400) error response with field validation details.
     *
     * @param code    the error code
     * @param message the error message
     * @param details list of field validation errors
     * @return JAX-RS Response with 400 status and ApiResponse error envelope
     */
    protected Response badRequest(String code, String message, List<FieldError> details) {
        return Response.status(Status.BAD_REQUEST)
            .entity(ApiResponse.error(code, message, details))
            .build();
    }

    /**
     * Creates an UNAUTHORIZED (401) error response.
     *
     * @param message the error message
     * @return JAX-RS Response with 401 status and ApiResponse error envelope
     */
    protected Response unauthorized(String message) {
        return Response.status(Status.UNAUTHORIZED)
            .entity(ApiResponse.unauthorized(message))
            .build();
    }

    /**
     * Creates a FORBIDDEN (403) error response.
     *
     * @param message the error message
     * @return JAX-RS Response with 403 status and ApiResponse error envelope
     */
    protected Response forbidden(String message) {
        return Response.status(Status.FORBIDDEN)
            .entity(ApiResponse.forbidden(message))
            .build();
    }

    /**
     * Creates a NOT FOUND (404) error response.
     *
     * @param resource  the resource type name
     * @param identifier the resource identifier
     * @return JAX-RS Response with 404 status and ApiResponse error envelope
     */
    protected Response notFound(String resource, Object identifier) {
        return Response.status(Status.NOT_FOUND)
            .entity(ApiResponse.notFound(resource, identifier))
            .build();
    }

    /**
     * Creates a NOT FOUND (404) error response with a custom message.
     *
     * @param message the error message
     * @return JAX-RS Response with 404 status and ApiResponse error envelope
     */
    protected Response notFound(String message) {
        return Response.status(Status.NOT_FOUND)
            .entity(ApiResponse.error("NOT_FOUND", message))
            .build();
    }

    /**
     * Creates a CONFLICT (409) error response.
     * Used when the request conflicts with the current state of the server.
     *
     * @param code    the error code
     * @param message the error message
     * @return JAX-RS Response with 409 status and ApiResponse error envelope
     */
    protected Response conflict(String code, String message) {
        return Response.status(Status.CONFLICT)
            .entity(ApiResponse.error(code, message))
            .build();
    }

    /**
     * Creates an INTERNAL SERVER ERROR (500) error response.
     *
     * @param message the error message
     * @return JAX-RS Response with 500 status and ApiResponse error envelope
     */
    protected Response internalError(String message) {
        return Response.status(Status.INTERNAL_SERVER_ERROR)
            .entity(ApiResponse.internalError(message))
            .build();
    }

    /**
     * Creates a response with custom status and error information.
     *
     * @param status  the HTTP status
     * @param code    the error code
     * @param message the error message
     * @return JAX-RS Response with custom status and ApiResponse error envelope
     */
    protected Response error(Status status, String code, String message) {
        return Response.status(status)
            .entity(ApiResponse.error(code, message))
            .build();
    }
}
