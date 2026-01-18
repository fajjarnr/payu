package id.payu.gateway.resource;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.Map;

/**
 * Health and status endpoints for the gateway.
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class HealthResource {

    private static final Instant START_TIME = Instant.now();

    /**
     * Root health check.
     */
    @GET
    @Path("/health")
    public Response health() {
        return Response.ok(Map.of(
            "status", "UP",
            "service", "gateway-service",
            "version", "1.0.0",
            "timestamp", Instant.now()
        )).build();
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
