package id.payu.gateway.adapter.web;

import id.payu.gateway.application.service.CircuitBreakerService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Health and status endpoints for the gateway.
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class HealthResource {

    private static final Instant START_TIME = Instant.now();

    @Inject
    CircuitBreakerService circuitBreakerService;

    /**
     * Root health check.
     */
    @GET
    @Path("/health")
    public Response health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "gateway-service");
        response.put("version", "1.0.0");
        response.put("timestamp", Instant.now());

        // Include circuit breaker states
        Map<String, CircuitBreakerService.CircuitBreakerInfo> circuitStates =
                circuitBreakerService.getCircuitStates();
        if (!circuitStates.isEmpty()) {
            Map<String, Object> cbSummary = circuitStates.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> Map.of(
                                    "state", e.getValue().state().name(),
                                    "failureCount", e.getValue().failureCount(),
                                    "totalCount", e.getValue().totalCount()
                            )
                    ));
            response.put("circuitBreakers", cbSummary);
        }

        return Response.ok(response).build();
    }

    /**
     * Detailed status information.
     */
    @GET
    @Path("/status")
    public Response status() {
        Runtime runtime = Runtime.getRuntime();
        long uptime = Instant.now().toEpochMilli() - START_TIME.toEpochMilli();
        
        return Response.ok(Map.of(
            "status", "UP",
            "service", "gateway-service",
            "version", "1.0.0",
            "uptime", formatUptime(uptime),
            "uptimeMs", uptime,
            "startTime", START_TIME,
            "timestamp", Instant.now(),
            "memory", Map.of(
                "total", runtime.totalMemory(),
                "free", runtime.freeMemory(),
                "used", runtime.totalMemory() - runtime.freeMemory(),
                "max", runtime.maxMemory()
            ),
            "processors", runtime.availableProcessors()
        )).build();
    }

    /**
     * API version information.
     */
    @GET
    @Path("/version")
    public Response version() {
        return Response.ok(Map.of(
            "service", "gateway-service",
            "version", "1.0.0",
            "apiVersion", "v1",
            "buildTime", "2026-01-18T00:00:00Z"
        )).build();
    }

    private String formatUptime(long uptimeMs) {
        long seconds = uptimeMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return String.format("%dd %dh %dm", days, hours % 24, minutes % 60);
        } else if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes % 60, seconds % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }
}
