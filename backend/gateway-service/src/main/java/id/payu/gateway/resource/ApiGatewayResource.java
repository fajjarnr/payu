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
    @POST @Path("/accounts/{path: .*}")
    public Uni<Response> accountPost(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("account-service", "/api/v1/accounts/" + path, "POST", body, headers);
    }
    @GET @Path("/accounts/{path: .*}")
    public Uni<Response> accountGet(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("account-service", "/api/v1/accounts/" + path, "GET", body, headers);
    }
    @PUT @Path("/accounts/{path: .*}")
    public Uni<Response> accountPut(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("account-service", "/api/v1/accounts/" + path, "PUT", body, headers);
    }
    @DELETE @Path("/accounts/{path: .*}")
    public Uni<Response> accountDelete(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("account-service", "/api/v1/accounts/" + path, "DELETE", body, headers);
    }

    @POST @Path("/accounts")
    public Uni<Response> accountRootPost(String body, @Context HttpHeaders headers) {
        return proxy("account-service", "/api/v1/accounts", "POST", body, headers);
    }
    @GET @Path("/accounts")
    public Uni<Response> accountRootGet(String body, @Context HttpHeaders headers) {
        return proxy("account-service", "/api/v1/accounts", "GET", body, headers);
    }

    // ==================== Wallet Service ====================
    @GET @Path("/wallets/{path: .*}")
    public Uni<Response> walletGet(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("wallet-service", "/api/v1/wallets/" + path, "GET", body, headers);
    }
    @POST @Path("/wallets/{path: .*}")
    public Uni<Response> walletPost(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("wallet-service", "/api/v1/wallets/" + path, "POST", body, headers);
    }
    @PUT @Path("/wallets/{path: .*}")
    public Uni<Response> walletPut(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("wallet-service", "/api/v1/wallets/" + path, "PUT", body, headers);
    }
    // Wallet service typically doesn't have root /wallets endpoint access directly in this design (usually under accounts), but if needed:
    // Skipping wallet root for now as not in original code explicitly (wait, original code didn't have wallet root)

    // ==================== Transaction Service ====================
    @GET @Path("/transactions/{path: .*}")
    public Uni<Response> transactionGet(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("transaction-service", "/api/v1/transactions/" + path, "GET", body, headers);
    }
    @POST @Path("/transactions/{path: .*}")
    public Uni<Response> transactionPost(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("transaction-service", "/api/v1/transactions/" + path, "POST", body, headers);
    }

    @GET @Path("/transactions")
    public Uni<Response> transactionRootGet(String body, @Context HttpHeaders headers) {
        return proxy("transaction-service", "/api/v1/transactions", "GET", body, headers);
    }
    @POST @Path("/transactions")
    public Uni<Response> transactionRootPost(String body, @Context HttpHeaders headers) {
        return proxy("transaction-service", "/api/v1/transactions", "POST", body, headers);
    }

    // ==================== Billing Service ====================
    @GET @Path("/billers/{path: .*}")
    public Uni<Response> billerGet(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("billing-service", "/api/v1/billers/" + path, "GET", body, headers);
    }
    @GET @Path("/billers")
    public Uni<Response> billerRootGet(String body, @Context HttpHeaders headers) {
        return proxy("billing-service", "/api/v1/billers", "GET", body, headers);
    }

    @GET @Path("/payments/{path: .*}")
    public Uni<Response> paymentGet(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("billing-service", "/api/v1/payments/" + path, "GET", body, headers);
    }
    @POST @Path("/payments/{path: .*}")
    public Uni<Response> paymentPost(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("billing-service", "/api/v1/payments/" + path, "POST", body, headers);
    }
    @POST @Path("/payments")
    public Uni<Response> paymentRootPost(String body, @Context HttpHeaders headers) {
        return proxy("billing-service", "/api/v1/payments", "POST", body, headers);
    }

    // ==================== Notification Service ====================
    @GET @Path("/notifications/{path: .*}")
    public Uni<Response> notificationGet(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("notification-service", "/api/v1/notifications/" + path, "GET", body, headers);
    }
    @POST @Path("/notifications/{path: .*}")
    public Uni<Response> notificationPost(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("notification-service", "/api/v1/notifications/" + path, "POST", body, headers);
    }

    // ==================== Card Service ====================
    @GET @Path("/cards/{path: .*}")
    public Uni<Response> cardGet(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("wallet-service", "/api/v1/cards/" + path, "GET", body, headers);
    }
    @POST @Path("/cards/{path: .*}")
    public Uni<Response> cardPost(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("wallet-service", "/api/v1/cards/" + path, "POST", body, headers);
    }
    @PUT @Path("/cards/{path: .*}")
    public Uni<Response> cardPut(@PathParam("path") String path, String body, @Context HttpHeaders headers) {
        return proxy("wallet-service", "/api/v1/cards/" + path, "PUT", body, headers);
    }

    @GET @Path("/cards")
    public Uni<Response> cardRootGet(String body, @Context HttpHeaders headers) {
        return proxy("wallet-service", "/api/v1/cards", "GET", body, headers);
    }
    @POST @Path("/cards")
    public Uni<Response> cardRootPost(String body, @Context HttpHeaders headers) {
        return proxy("wallet-service", "/api/v1/cards", "POST", body, headers);
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

        // Forward matching headers (simple version)
        if (headers != null) {
            headers.getRequestHeaders().forEach((k, v) -> {
                if (!k.equalsIgnoreCase("Host") && !k.equalsIgnoreCase("Content-Length")) {
                    request.putHeader(k, v);
                }
            });
        }
        
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
