package id.payu.gateway.adapter.web;

import id.payu.gateway.application.service.RouteRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Path("/q/openapi")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Tag(name = "OpenAPI Aggregation", description = "Unified API documentation across all PayU services")
public class GatewayOpenApiResource {

    @Inject
    RouteRegistry routeRegistry;

    @GET
    @Path("/services")
    @Operation(summary = "List all service OpenAPI endpoints", description = "Returns a map of all registered backend services and their OpenAPI spec URLs")
    public Map<String, Object> listServiceOpenApiEndpoints() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("gateway_openapi", "/q/openapi");
        result.put("gateway_swagger_ui", "/q/swagger-ui");
        result.put("service_count", routeRegistry.getRouteCount());

        Map<String, String> services = new LinkedHashMap<>();
        for (Map.Entry<String, RouteRegistry.RouteDefinition> entry : routeRegistry.getAllRoutes().entrySet()) {
            String serviceName = entry.getValue().serviceName();
            // Spring Boot services use /v3/api-docs, Quarkus services use /q/openapi
            String openApiUrl = switch (serviceName) {
                case "billing-service", "notification-service", "partner-service", "promotion-service",
                     "support-service", "backoffice-service", "api-portal-service" ->
                        "/q/openapi"; // Quarkus
                default -> "/v3/api-docs"; // Spring Boot
            };
            services.putIfAbsent(serviceName, openApiUrl);
        }
        result.put("backend_services", services);

        return result;
    }
}
