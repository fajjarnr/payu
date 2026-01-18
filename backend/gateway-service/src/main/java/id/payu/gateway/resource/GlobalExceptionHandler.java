package id.payu.gateway.resource;

import id.payu.gateway.dto.ApiError;
import io.quarkus.logging.Log;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Global exception handler for the gateway.
 */
@Provider
public class GlobalExceptionHandler implements ExceptionMapper<Throwable> {

    @Override
    public Response toResponse(Throwable exception) {
        Log.errorf(exception, "Unhandled exception in gateway");

        if (exception instanceof WebApplicationException wae) {
            Response response = wae.getResponse();
            return Response.status(response.getStatus())
                .entity(ApiError.of(
                    getErrorCode(response.getStatus()),
                    exception.getMessage(),
                    null,
                    response.getStatus()
                ))
                .type("application/json")
                .build();
        }

        // Default to 500 Internal Server Error
        return Response.status(500)
            .entity(ApiError.of(
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred",
                null,
                500
            ))
            .type("application/json")
            .build();
    }

    private String getErrorCode(int status) {
        return switch (status) {
            case 400 -> "BAD_REQUEST";
            case 401 -> "UNAUTHORIZED";
            case 403 -> "FORBIDDEN";
            case 404 -> "NOT_FOUND";
            case 405 -> "METHOD_NOT_ALLOWED";
            case 408 -> "REQUEST_TIMEOUT";
            case 409 -> "CONFLICT";
            case 429 -> "TOO_MANY_REQUESTS";
            case 500 -> "INTERNAL_SERVER_ERROR";
            case 502 -> "BAD_GATEWAY";
            case 503 -> "SERVICE_UNAVAILABLE";
            case 504 -> "GATEWAY_TIMEOUT";
            default -> "ERROR";
        };
    }
}
