package id.payu.backoffice.resource;

import id.payu.backoffice.dto.ApiResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.jboss.logging.Logger;

import java.net.URI;
import java.util.UUID;

/**
 * Base resource providing common functionality for all backoffice controllers.
 * Includes utility methods for response building and request handling.
 */
public abstract class BackofficeBaseResource {

    protected final Logger LOG = Logger.getLogger(getClass());

    /**
     * Creates a successful API response with data.
     */
    protected <T> Response ok(T data) {
        return Response.ok(ApiResponse.success(data)).build();
    }

    /**
     * Creates a successful API response with data and custom request ID.
     */
    protected <T> Response ok(T data, String requestId) {
        return Response.ok(ApiResponse.success(data, requestId)).build();
    }

    /**
     * Creates a 201 Created response with location header.
     */
    protected <T> Response created(T data, String location) {
        return Response
                .created(URI.create(location))
                .entity(ApiResponse.success(data))
                .build();
    }

    /**
     * Creates a 204 No Content response.
     */
    protected Response noContent() {
        return Response.noContent().build();
    }

    /**
     * Creates a 404 Not Found response.
     */
    protected <T> Response notFound(String resourceName) {
        return Response
                .status(Response.Status.NOT_FOUND)
                .entity(ApiResponse.notFound(resourceName))
                .build();
    }

    /**
     * Creates a 400 Bad Request response.
     */
    protected <T> Response badRequest(String message) {
        return Response
                .status(Response.Status.BAD_REQUEST)
                .entity(ApiResponse.badRequest(message))
                .build();
    }

    /**
     * Generates a unique request ID.
     */
    protected String generateRequestId() {
        return "req-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Gets the base URL from UriInfo.
     */
    protected String getBaseUrl(UriInfo uriInfo) {
        if (uriInfo == null) {
            return "";
        }
        return uriInfo.getBaseUri().toString();
    }
}
