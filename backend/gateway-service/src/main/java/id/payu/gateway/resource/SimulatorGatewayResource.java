package id.payu.gateway.resource;

import id.payu.gateway.config.GatewayConfig;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import java.net.URI;
import java.time.temporal.ChronoUnit;

/**
 * Gateway proxy for simulator services.
 * Routes requests to BI-FAST, Dukcapil, and QRIS simulators.
 */
@Path("/api/v1/simulator")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class SimulatorGatewayResource {

    @Inject
    GatewayConfig config;

    @Inject
    Vertx vertx;

    private WebClient webClient;

    @PostConstruct
    void init() {
        this.webClient = WebClient.create(vertx);
    }

    // ==================== BI-FAST Simulator ====================

    @POST
    @Path("/bifast/inquiry")
    @Timeout(value = 30, unit = ChronoUnit.SECONDS)
    @Retry(maxRetries = 2, delay = 500)
    @CircuitBreaker(requestVolumeThreshold = 10, failureRatio = 0.5, delay = 30000)
    public Uni<Response> bifastInquiry(String body, @Context HttpHeaders headers) {
        return proxy("bi-fast", "/api/v1/inquiry", "POST", body, headers);
    }

    @POST
    @Path("/bifast/transfer")
    @Timeout(value = 30, unit = ChronoUnit.SECONDS)
    @Retry(maxRetries = 1, delay = 1000)
    @CircuitBreaker(requestVolumeThreshold = 10, failureRatio = 0.5, delay = 30000)
    public Uni<Response> bifastTransfer(String body, @Context HttpHeaders headers) {
        return proxy("bi-fast", "/api/v1/transfer", "POST", body, headers);
    }

    @GET
    @Path("/bifast/status/{ref}")
    @Timeout(value = 15, unit = ChronoUnit.SECONDS)
    @Retry(maxRetries = 3, delay = 500)
    public Uni<Response> bifastStatus(@PathParam("ref") String ref, @Context HttpHeaders headers) {
        return proxy("bi-fast", "/api/v1/status/" + ref, "GET", null, headers);
    }

    // ==================== Dukcapil Simulator ====================

    @POST
    @Path("/dukcapil/verify")
    @Timeout(value = 30, unit = ChronoUnit.SECONDS)
    @Retry(maxRetries = 2, delay = 500)
    @CircuitBreaker(requestVolumeThreshold = 10, failureRatio = 0.5, delay = 30000)
    public Uni<Response> dukcapilVerify(String body, @Context HttpHeaders headers) {
        return proxy("dukcapil", "/api/v1/verify", "POST", body, headers);
    }

    @POST
    @Path("/dukcapil/match-photo")
    @Timeout(value = 60, unit = ChronoUnit.SECONDS)
    @Retry(maxRetries = 1, delay = 1000)
    @CircuitBreaker(requestVolumeThreshold = 10, failureRatio = 0.5, delay = 30000)
    public Uni<Response> dukcapilMatchPhoto(String body, @Context HttpHeaders headers) {
        return proxy("dukcapil", "/api/v1/match-photo", "POST", body, headers);
    }

    @GET
    @Path("/dukcapil/nik/{nik}")
    @Timeout(value = 15, unit = ChronoUnit.SECONDS)
    @Retry(maxRetries = 3, delay = 500)
    public Uni<Response> dukcapilGetNik(@PathParam("nik") String nik, @Context HttpHeaders headers) {
        return proxy("dukcapil", "/api/v1/nik/" + nik, "GET", null, headers);
    }

    // ==================== QRIS Simulator ====================

    @POST
    @Path("/qris/generate")
    @Timeout(value = 15, unit = ChronoUnit.SECONDS)
    @Retry(maxRetries = 2, delay = 500)
    @CircuitBreaker(requestVolumeThreshold = 10, failureRatio = 0.5, delay = 30000)
    public Uni<Response> qrisGenerate(String body, @Context HttpHeaders headers) {
        return proxy("qris", "/api/v1/generate", "POST", body, headers);
    }

    @POST
    @Path("/qris/pay")
    @Timeout(value = 15, unit = ChronoUnit.SECONDS)
    @Retry(maxRetries = 1, delay = 1000)
    @CircuitBreaker(requestVolumeThreshold = 10, failureRatio = 0.5, delay = 30000)
    public Uni<Response> qrisPay(String body, @Context HttpHeaders headers) {
        return proxy("qris", "/api/v1/pay", "POST", body, headers);
    }

    @GET
    @Path("/qris/status/{qrId}")
    @Timeout(value = 10, unit = ChronoUnit.SECONDS)
    @Retry(maxRetries = 3, delay = 500)
    public Uni<Response> qrisStatus(@PathParam("qrId") String qrId, @Context HttpHeaders headers) {
        return proxy("qris", "/api/v1/status/" + qrId, "GET", null, headers);
    }

    // ==================== Proxy Logic ====================

    private Uni<Response> proxy(String simulator, String path, String method, 
                                 String body, HttpHeaders headers) {
        GatewayConfig.ServiceConfig simulatorConfig = config.simulators().get(simulator);
        if (simulatorConfig == null) {
            Log.errorf("Simulator not configured: %s", simulator);
            return Uni.createFrom().item(
                Response.status(503)
                    .entity("{\"error\":\"SERVICE_UNAVAILABLE\",\"message\":\"Simulator not configured\"}")
                    .build()
            );
        }

        String baseUrl = simulatorConfig.url();
        URI targetUri = URI.create(baseUrl);
        String fullPath = path;

        Log.debugf("Proxying to %s: %s %s%s", simulator, method, baseUrl, fullPath);

        var request = webClient.request(
            io.vertx.core.http.HttpMethod.valueOf(method),
            targetUri.getPort() != -1 ? targetUri.getPort() : 80,
            targetUri.getHost(),
            fullPath
        );

        // Forward headers
        String correlationId = headers.getHeaderString("X-Correlation-Id");
        if (correlationId != null) {
            request.putHeader("X-Correlation-Id", correlationId);
        }
        request.putHeader("Content-Type", "application/json");
        request.putHeader("Accept", "application/json");

        // Send request
        Uni<HttpResponse<Buffer>> responseUni;
        if (body != null && !body.isBlank()) {
            responseUni = request.sendBuffer(Buffer.buffer(body));
        } else {
            responseUni = request.send();
        }

        return responseUni.map(response -> {
            Log.debugf("Response from %s: status=%d", simulator, response.statusCode());
            
            Response.ResponseBuilder builder = Response.status(response.statusCode());
            
            String responseBody = response.bodyAsString();
            if (responseBody != null) {
                builder.entity(responseBody);
            }
            
            builder.type(MediaType.APPLICATION_JSON);
            
            // Forward correlation ID
            String respCorrelationId = response.getHeader("X-Correlation-Id");
            if (respCorrelationId != null) {
                builder.header("X-Correlation-Id", respCorrelationId);
            }

            return builder.build();
        }).onFailure().recoverWithItem(throwable -> {
            Log.errorf(throwable, "Error proxying to %s", simulator);
            return Response.status(503)
                .entity("{\"error\":\"SERVICE_UNAVAILABLE\",\"message\":\"" + 
                        throwable.getMessage() + "\"}")
                .type(MediaType.APPLICATION_JSON)
                .build();
        });
    }
}
