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
import java.util.Map;

/**
 * Main API Gateway Resource.
 * Routes traffic to microservices.
 */
@Path("/api/v1")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class ApiGatewayResource {

    @Inject
    GatewayConfig config;

    @Inject
    Vertx vertx;

    private WebClient webClient;

    @PostConstruct
    void init() {
        this.webClient = WebClient.create(vertx);
    }

    // ==================== Account Service ====================
    @Path("/accounts/{path: .*}")
    @POST
    @GET
    @PUT
    @DELETE
    @Timeout(value = 30, unit = ChronoUnit.SECONDS)
    @Retry(maxRetries = 2, delay = 200)
    public Uni<Response> accountProxy(@PathParam("path") String path, String body, @Context HttpHeaders headers, @Context jakarta.ws.rs.core.Request request) {
        return proxy("account-service", "/api/v1/accounts/" + path, request.getMethod(), body, headers);
    }
    
    // Direct match for /api/v1/accounts (mostly POST)
    @Path("/accounts")
    @POST
    @GET
    public Uni<Response> accountRootProxy(String body, @Context HttpHeaders headers, @Context jakarta.ws.rs.core.Request request) {
        return proxy("account-service", "/api/v1/accounts", request.getMethod(), body, headers);
    }

    // ==================== Wallet Service ====================
    @Path("/wallets/{path: .*}")
    @GET
    @POST
    @PUT
    public Uni<Response> walletProxy(@PathParam("path") String path, String body, @Context HttpHeaders headers, @Context jakarta.ws.rs.core.Request request) {
        return proxy("wallet-service", "/api/v1/wallets/" + path, request.getMethod(), body, headers);
    }

    // ==================== Transaction Service ====================
    @Path("/transactions/{path: .*}")
    @GET
    @POST
    public Uni<Response> transactionProxy(@PathParam("path") String path, String body, @Context HttpHeaders headers, @Context jakarta.ws.rs.core.Request request) {
        return proxy("transaction-service", "/api/v1/transactions/" + path, request.getMethod(), body, headers);
    }
    
    @Path("/transactions")
    @GET
    @POST
    public Uni<Response> transactionRootProxy(String body, @Context HttpHeaders headers, @Context jakarta.ws.rs.core.Request request) {
        return proxy("transaction-service", "/api/v1/transactions", request.getMethod(), body, headers);
    }

    // ==================== Billing Service ====================
    @Path("/billers/{path: .*}")
    @GET
    public Uni<Response> billerProxy(@PathParam("path") String path, String body, @Context HttpHeaders headers, @Context jakarta.ws.rs.core.Request request) {
        return proxy("billing-service", "/api/v1/billers/" + path, request.getMethod(), body, headers);
    }
    
    @Path("/billers")
    @GET
    public Uni<Response> billerRootProxy(String body, @Context HttpHeaders headers, @Context jakarta.ws.rs.core.Request request) {
        return proxy("billing-service", "/api/v1/billers", request.getMethod(), body, headers);
    }

    @Path("/payments/{path: .*}")
    @GET
    @POST
    public Uni<Response> paymentProxy(@PathParam("path") String path, String body, @Context HttpHeaders headers, @Context jakarta.ws.rs.core.Request request) {
        return proxy("billing-service", "/api/v1/payments/" + path, request.getMethod(), body, headers);
    }
    
    @Path("/payments")
    @POST
    public Uni<Response> paymentRootProxy(String body, @Context HttpHeaders headers, @Context jakarta.ws.rs.core.Request request) {
        return proxy("billing-service", "/api/v1/payments", request.getMethod(), body, headers);
    }

    // ==================== Notification Service ====================
    @Path("/notifications/{path: .*}")
    @GET
    @POST
    public Uni<Response> notificationProxy(@PathParam("path") String path, String body, @Context HttpHeaders headers, @Context jakarta.ws.rs.core.Request request) {
        return proxy("notification-service", "/api/v1/notifications/" + path, request.getMethod(), body, headers);
    }


    // ==================== Proxy Logic ====================
    private Uni<Response> proxy(String serviceName, String path, String method, 
                                 String body, HttpHeaders headers) {
        
        GatewayConfig.ServiceConfig serviceConfig = config.services().get(serviceName);
        if (serviceConfig == null) {
            String errorMsg = String.format("Service %s not configured in gateway", serviceName);
            Log.error(errorMsg);
            return Uni.createFrom().item(Response.status(502).entity(errorMsg).build());
        }

        String baseUrl = serviceConfig.url();
        URI targetUri = URI.create(baseUrl);
        
        Log.infof("Proxying to %s: %s %s%s", serviceName, method, baseUrl, path);

        var request = webClient.request(
            io.vertx.core.http.HttpMethod.valueOf(method),
            targetUri.getPort() != -1 ? targetUri.getPort() : 80,
            targetUri.getHost(),
            path
        );

        // Forward matching headers
        headers.getRequestHeaders().forEach((k, v) -> {
            if (!k.equalsIgnoreCase("Host") && !k.equalsIgnoreCase("Content-Length")) {
                request.putHeader(k, v);
            }
        });
        
        request.putHeader("X-Forwarded-Host", "localhost:8080");

        Uni<HttpResponse<Buffer>> responseUni;
        if (body != null && !body.isBlank()) {
            responseUni = request.sendBuffer(Buffer.buffer(body));
        } else {
            responseUni = request.send();
        }

        return responseUni.map(response -> {
            Response.ResponseBuilder builder = Response.status(response.statusCode());
            
            if (response.body() != null) {
                builder.entity(response.bodyAsString());
            }

            response.headers().forEach(entry -> {
                 if (!entry.getKey().equalsIgnoreCase("Transfer-Encoding")) {
                    builder.header(entry.getKey(), entry.getValue());
                 }
            });

            return builder.build();
        }).onFailure().recoverWithItem(t -> {
            Log.errorf("Failed proxy to %s: %s", serviceName, t.getMessage());
            return Response.status(503).entity("Service Unavailable").build();
        });
    }
}
