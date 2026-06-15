package id.payu.gateway.adapter.web;

import id.payu.gateway.application.service.CircuitBreakerService;
import id.payu.gateway.application.service.RetryAndTimeoutService;
import id.payu.gateway.application.service.RouteRegistry;
import id.payu.gateway.config.GatewayConfig;
import id.payu.gateway.adapter.filter.TenantFilter;
import io.quarkus.logging.Log;
import io.smallrye.common.annotation.Blocking;
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
import jakarta.ws.rs.core.UriInfo;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;

/**
 * API Gateway Resource - single catch-all dispatcher.
 *
 * <p>All incoming requests under {@code /api/v1} are matched against the
 * {@link RouteRegistry} which resolves the target backend service via longest
 * prefix match. This design avoids the Quarkus RESTeasy Reactive
 * exact-vs-greedy @Path conflict that occurs when both {@code @Path("/foo")}
 * and {@code @Path("/foo/{path: .*}")} are declared in the same resource.
 *
 * <p>Per Quarkus best practice:
 * <ul>
 *   <li>Single catch-all per HTTP method, no exact @Path duplicates</li>
 *   <li>Routing logic lives in {@link RouteRegistry}, not in @Path annotations</li>
 *   <li>Adding a new route = update {@code gateway.routes} config, no code change</li>
 * </ul>
 */
@Path("/api/v1")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Blocking
public class ApiGatewayResource {

    @Inject
    GatewayConfig config;

    @Inject
    Vertx vertx;

    @Context
    UriInfo uriInfo;

    @Inject
    CircuitBreakerService circuitBreakerService;

    @Inject
    RetryAndTimeoutService retryAndTimeoutService;

    @Inject
    RouteRegistry routeRegistry;

    private WebClient webClient;

    @PostConstruct
    void init() {
        this.webClient = WebClient.create(vertx);
    }

    // ==================== Single Catch-All Dispatcher ====================
    // All HTTP methods delegate to RouteRegistry which handles prefix matching,
    // method allow-list enforcement, and target path construction.
    // This eliminates the exact-vs-greedy @Path conflict that drops @Path("/foo")
    // methods when @Path("/foo/{path: .*}") is also declared.

    @GET
    @Path("/{path: .*}")
    public Uni<Response> get(@PathParam("path") String path,
                              String body,
                              @Context HttpHeaders headers) {
        return dynamicRoute(path, "GET", body, headers);
    }

    @POST
    @Path("/{path: .*}")
    public Uni<Response> post(@PathParam("path") String path,
                               String body,
                               @Context HttpHeaders headers) {
        return dynamicRoute(path, "POST", body, headers);
    }

    @PUT
    @Path("/{path: .*}")
    public Uni<Response> put(@PathParam("path") String path,
                              String body,
                              @Context HttpHeaders headers) {
        return dynamicRoute(path, "PUT", body, headers);
    }

    @DELETE
    @Path("/{path: .*}")
    public Uni<Response> delete(@PathParam("path") String path,
                                 String body,
                                 @Context HttpHeaders headers) {
        return dynamicRoute(path, "DELETE", body, headers);
    }

    @PATCH
    @Path("/{path: .*}")
    public Uni<Response> patch(@PathParam("path") String path,
                                String body,
                                @Context HttpHeaders headers) {
        return dynamicRoute(path, "PATCH", body, headers);
    }

    // ==================== Dynamic Route (uses RouteRegistry) ====================

    /**
     * Dynamic route resolution for any path registered in RouteRegistry.
     * Falls back to 404 if no route matches, 405 if method not allowed.
     */
    private Uni<Response> dynamicRoute(String path, String method, String body, HttpHeaders headers) {
        Optional<RouteRegistry.ResolvedRoute> resolved = routeRegistry.resolve(path);
        if (resolved.isEmpty()) {
            return Uni.createFrom().item(
                    Response.status(404)
                            .entity("{\"error\":\"NOT_FOUND\",\"message\":\"No route found for path: /api/v1/" + path + "\",\"status\":404}")
                            .type(MediaType.APPLICATION_JSON)
                            .build()
            );
        }

        RouteRegistry.ResolvedRoute route = resolved.get();

        // Check if HTTP method is allowed
        if (!route.definition().methods().contains(method)) {
            return Uni.createFrom().item(
                    Response.status(405)
                            .entity("{\"error\":\"METHOD_NOT_ALLOWED\",\"message\":\"Method " + method + " not allowed\",\"status\":405}")
                            .type(MediaType.APPLICATION_JSON)
                            .build()
            );
        }

        return proxy(route.serviceName(), route.targetPath(), method, body, headers);
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

        // Capture query string from the incoming request and append to downstream path
        String queryString = (uriInfo != null) ? uriInfo.getRequestUri().getRawQuery() : null;
        String fullPath = (queryString != null && !queryString.isEmpty())
                ? path + "?" + queryString
                : path;

        // Wrap the actual call with circuit breaker + retry + timeout
        return circuitBreakerService.execute(serviceName, () -> {
            Uni<Response> call = doProxy(serviceName, serviceConfig, fullPath, method, body, headers);

            // Apply retry with backoff
            call = retryAndTimeoutService.executeWithRetry(serviceName, call);

            // Apply timeout
            Duration timeout = retryAndTimeoutService.getTimeout(serviceName);
            call = call.ifNoItem().after(timeout).fail();

            return call;
        });
    }

    /**
     * Performs the actual HTTP proxy call via Vert.x WebClient.
     */
    private Uni<Response> doProxy(String serviceName, GatewayConfig.ServiceConfig serviceConfig,
                                   String path, String method, String body, HttpHeaders headers) {

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

        // Forward tenant ID from filter context
        String tenantId = headers.getHeaderString(TenantFilter.TENANT_ID_HEADER);
        if (tenantId != null && !tenantId.isBlank()) {
            request.putHeader(TenantFilter.TENANT_ID_HEADER, tenantId);
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
