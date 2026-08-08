package id.payu.gateway.application.service;

import id.payu.gateway.config.GatewayConfig;
import id.payu.gateway.adapter.filter.TenantFilter;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;

/**
 * Shared dynamic-route dispatch service behind both the {@code /api/v1}
 * catch-all and the public {@code /v1/partner} contract entry point.
 *
 * <p>PARTNER-001: the SNAP-BI contract path {@code /v1/partner/**} must reach
 * partner-service without a doubled prefix. Both resources resolve through the
 * same {@link RouteRegistry}, so the two entry points can never drift.
 */
@ApplicationScoped
public class GatewayDispatchService {

    @Inject
    GatewayConfig config;

    @Inject
    Vertx vertx;

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

    /**
     * Resolve and proxy a request whose path-after-entry is {@code pathAfterEntry}
     * (e.g. {@code "v1/partner/auth/token"} for both entry points).
     */
    public Uni<Response> dispatch(String pathAfterEntry, String method, String body,
                                  HttpHeaders headers, UriInfo uriInfo) {
        Optional<RouteRegistry.ResolvedRoute> resolved = routeRegistry.resolve(pathAfterEntry);
        if (resolved.isEmpty()) {
            return Uni.createFrom().item(
                    Response.status(404)
                            .entity("{\"error\":\"NOT_FOUND\",\"message\":\"No route found for path: " + pathAfterEntry + "\",\"status\":404}")
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

        return proxy(route.serviceName(), route.targetPath(), method, body, headers, uriInfo);
    }

    private Uni<Response> proxy(String serviceName, String path, String method,
                                String body, HttpHeaders headers, UriInfo uriInfo) {

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
        String tenantId = headers != null ? headers.getHeaderString(TenantFilter.TENANT_ID_HEADER) : null;
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
