package id.payu.gateway.adapter.web;

import io.quarkus.logging.Log;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Global exception handler for the gateway.
 *
 * <p>Forwards upstream 4xx/5xx responses verbatim per READY-025.
 * Only transforms gateway-native errors (route not found, circuit‑breaker, etc.)
 * into a consistent RFC 9457 {@code application/problem+json} body.
 * Catastrophic failures (non‑WebApplicationException) produce a generic 500.
 */
@Provider
public class GlobalExceptionHandler implements ExceptionMapper<Throwable> {

    @Override
    public Response toResponse(Throwable exception) {
        // WebApplicationException — forward the upstream response verbatim.
        // The original status code, headers, and body are preserved so the
        // client sees exactly what the backend service returned.
        if (exception instanceof WebApplicationException wae) {
            return wae.getResponse();
        }

        // Catastrophic failure (connection error, timeout, etc.) — return
        // a standardised 500 so the client always gets parseable JSON.
        Log.errorf(exception, "Catastrophic failure in gateway");
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(problemDetail(500, "Internal server error",
                        "An unexpected error occurred. Please try again later.",
                        "INTERNAL_ERROR"))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    private static Map<String, Object> problemDetail(int status, String title,
                                                     String detail, String errorCode) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "about:blank");
        body.put("title", title);
        body.put("status", status);
        body.put("detail", detail);
        body.put("error_code", errorCode);
        body.put("timestamp", Instant.now().toString());
        return body;
    }
}
