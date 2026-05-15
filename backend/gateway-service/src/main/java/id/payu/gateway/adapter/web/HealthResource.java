package id.payu.gateway.adapter.web;

import id.payu.gateway.application.service.CircuitBreakerService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import id.payu.gateway.application.service.State;

/**
 * Health and status endpoints for the gateway.
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Health", description = "Health check and status endpoints")
public class HealthResource {

    private static final Instant START_TIME = Instant.now();

    @Inject
    CircuitBreakerService circuitBreakerService;

    /**
     * Root health check.
     */
    @GET
    @Path("/health")
    @Operation(summary = "Health check", description = "Returns the health status of the gateway service including circuit breaker states")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Service is healthy or degraded",
            content = @Content(schema = @Schema(implementation = Map.class))),
        @APIResponse(responseCode = "503", description = "Service is unavailable")
    })
    public Response health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "gateway-service");
        response.put("version", "1.0.0");
        response.put("timestamp", Instant.now());

        // Include circuit breaker states per service
        Map<String, CircuitBreakerService.CircuitBreakerInfo> circuitStates =
                circuitBreakerService.getCircuitStates();
        if (!circuitStates.isEmpty()) {
            boolean anyOpen = false;
            Map<String, Object> cbSummary = new HashMap<>();
            for (var entry : circuitStates.entrySet()) {
                var info = entry.getValue();
                Map<String, Object> serviceState = new HashMap<>();
                serviceState.put("state", info.state().name());
                serviceState.put("failureCount", info.failureCount());
                serviceState.put("successCount", info.successCount());
                serviceState.put("totalCount", info.totalCount());
                if (info.lastFailureTime() != null) {
                    serviceState.put("lastFailureTime", info.lastFailureTime().toString());
                }
                if (info.state() == State.OPEN) {
                    anyOpen = true;
                    serviceState.put("retryAfterSeconds", info.retryAfterSeconds());
                    if (info.openedAt() != null) {
                        serviceState.put("openedAt", info.openedAt().toString());
                    }
                }
                cbSummary.put(entry.getKey(), serviceState);
            }
            response.put("circuitBreakers", cbSummary);
            // Degrade overall status if any circuit is OPEN
            if (anyOpen) {
                response.put("status", "DEGRADED");
            }
        }

        return Response.ok(response).build();
    }

    /**
     * Detailed status information.
     */
    @GET
    @Path("/status")
    @Operation(summary = "Service status", description = "Returns detailed status information including uptime, memory, and system info")
    @APIResponse(responseCode = "200", description = "Status information retrieved successfully",
        content = @Content(schema = @Schema(implementation = Map.class)))
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
    @Operation(summary = "API version", description = "Returns the API version and build information")
    @APIResponse(responseCode = "200", description = "Version information retrieved successfully",
        content = @Content(schema = @Schema(implementation = Map.class)))
    public Response version() {
        return Response.ok(Map.of(
            "service", "gateway-service",
            "version", "1.0.0",
            "apiVersion", "v1",
            "buildTime", "2026-01-18T00:00:00Z"
        )).build();
    }

    /**
     * Circuit breaker states for all downstream services.
     * Returns per-service state, failure counts, and retry-after info.
     */
    @GET
    @Path("/health/circuits")
    @Operation(summary = "Circuit breaker states", description = "Returns circuit breaker states for all downstream services")
    @APIResponse(responseCode = "200", description = "Circuit breaker states retrieved successfully",
        content = @Content(schema = @Schema(implementation = Map.class)))
    public Response circuitBreakers() {
        Map<String, CircuitBreakerService.CircuitBreakerInfo> states =
                circuitBreakerService.getCircuitStates();

        Map<String, Object> result = new HashMap<>();
        result.put("timestamp", Instant.now());

        if (states.isEmpty()) {
            result.put("services", Map.of());
            result.put("summary", "No circuit breakers initialized yet");
        } else {
            Map<String, Object> services = new HashMap<>();
            int openCount = 0;
            int halfOpenCount = 0;
            int closedCount = 0;

            for (var entry : states.entrySet()) {
                var info = entry.getValue();
                Map<String, Object> svcInfo = new HashMap<>();
                svcInfo.put("state", info.state().name());
                svcInfo.put("failureCount", info.failureCount());
                svcInfo.put("successCount", info.successCount());
                svcInfo.put("totalCount", info.totalCount());
                if (info.lastFailureTime() != null) {
                    svcInfo.put("lastFailureTime", info.lastFailureTime().toString());
                }
                if (info.state() == State.OPEN) {
                    openCount++;
                    svcInfo.put("retryAfterSeconds", info.retryAfterSeconds());
                    if (info.openedAt() != null) {
                        svcInfo.put("openedAt", info.openedAt().toString());
                    }
                } else if (info.state() == State.HALF_OPEN) {
                    halfOpenCount++;
                } else {
                    closedCount++;
                }
                services.put(entry.getKey(), svcInfo);
            }

            result.put("services", services);
            result.put("summary", Map.of(
                "total", states.size(),
                "closed", closedCount,
                "open", openCount,
                "halfOpen", halfOpenCount
            ));
        }

        return Response.ok(result).build();
    }

    /**
     * Circuit breaker state for a specific service.
     */
    @GET
    @Path("/health/circuits/{serviceName}")
    @Operation(summary = "Service circuit breaker state", description = "Returns circuit breaker state for a specific downstream service")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Circuit breaker state retrieved successfully",
            content = @Content(schema = @Schema(implementation = Map.class))),
        @APIResponse(responseCode = "404", description = "Service not found")
    })
    public Response circuitBreakerForService(@PathParam("serviceName") String serviceName) {
        CircuitBreakerService.CircuitBreakerInfo info =
                circuitBreakerService.getCircuitState(serviceName);

        Map<String, Object> result = new HashMap<>();
        result.put("service", serviceName);
        result.put("state", info.state().name());
        result.put("failureCount", info.failureCount());
        result.put("successCount", info.successCount());
        result.put("totalCount", info.totalCount());
        if (info.lastFailureTime() != null) {
            result.put("lastFailureTime", info.lastFailureTime().toString());
        }
        if (info.state() == State.OPEN) {
            result.put("retryAfterSeconds", info.retryAfterSeconds());
            if (info.openedAt() != null) {
                result.put("openedAt", info.openedAt().toString());
            }
        }
        result.put("timestamp", Instant.now());

        return Response.ok(result).build();
    }

    /**
     * Admin endpoint to reset circuit breaker for a specific service.
     */
    @POST
    @Path("/health/circuits/{serviceName}/reset")
    @Operation(summary = "Reset circuit breaker", description = "Resets the circuit breaker for a specific downstream service (admin only)")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Circuit breaker reset successfully",
            content = @Content(schema = @Schema(implementation = Map.class))),
        @APIResponse(responseCode = "404", description = "Service not found"),
        @APIResponse(responseCode = "403", description = "Forbidden - admin access required")
    })
    public Response resetCircuitBreaker(@PathParam("serviceName") String serviceName) {
        circuitBreakerService.reset(serviceName);
        return Response.ok(Map.of(
            "service", serviceName,
            "state", "CLOSED",
            "message", "Circuit breaker reset successfully",
            "timestamp", Instant.now()
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
