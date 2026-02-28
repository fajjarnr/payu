package id.payu.gateway.adapter.web;

import id.payu.gateway.application.service.PersistentAnalyticsService;
import id.payu.gateway.domain.repository.ApiAnalyticsRepository;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * REST resource to expose API analytics with persistent storage.
 *
 * <p>
 * Provides endpoints for:
 * - Per-partner metrics
 * - Per-endpoint metrics
 * - Top endpoints by usage
 * - Retention configuration
 *
 * <p>
 * This resource implements IMP-016: Persistent API Analytics.
 */
@Path("/gateway/analytics")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AnalyticsResource {

    @Inject
    PersistentAnalyticsService analyticsService;

    /**
     * Get metrics for a specific endpoint.
     */
    @GET
    @Path("/metrics")
    @RolesAllowed({"admin", "analytics-reader"})
    public Uni<Response> getMetrics(
            @QueryParam("path") String path,
            @QueryParam("method") @DefaultValue("GET") String method,
            @QueryParam("from") Long fromTimestamp,
            @QueryParam("to") Long toTimestamp) {

        if (path == null || path.isBlank()) {
            return Uni.createFrom().item(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "path parameter is required"))
                    .build()
            );
        }

        Instant from = fromTimestamp != null
            ? Instant.ofEpochMilli(fromTimestamp)
            : Instant.now().minus(1, ChronoUnit.HOURS);
        Instant to = toTimestamp != null
            ? Instant.ofEpochMilli(toTimestamp)
            : Instant.now();

        return analyticsService.getEndpointMetrics(path, method, from, to)
            .onItem().transform(metrics -> Response.ok(Map.of(
                "endpoint", metrics.endpoint(),
                "method", metrics.method().name(),
                "totalRequests", metrics.totalRequests(),
                "successfulRequests", metrics.successfulRequests(),
                "errorRequests", metrics.errorRequests(),
                "avgResponseTime", metrics.avgResponseTime(),
                "minResponseTime", metrics.minResponseTime(),
                "maxResponseTime", metrics.maxResponseTime(),
                "statusCodeDistribution", metrics.statusCodeDistribution()
            )).build())
            .onFailure().recoverWithItem(throwable -> {
                Log.errorf(throwable, "Failed to retrieve analytics metrics");
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(
                        "error", "ANALYTICS_ERROR",
                        "message", "Failed to retrieve metrics"
                    ))
                    .build();
            });
    }

    /**
     * Get metrics for a specific partner.
     */
    @GET
    @Path("/partners/{partnerId}/metrics")
    @RolesAllowed({"admin", "analytics-reader"})
    public Uni<Response> getPartnerMetrics(
            @PathParam("partnerId") String partnerId,
            @QueryParam("from") Long fromTimestamp,
            @QueryParam("to") Long toTimestamp) {

        Instant from = fromTimestamp != null
            ? Instant.ofEpochMilli(fromTimestamp)
            : Instant.now().minus(24, ChronoUnit.HOURS);
        Instant to = toTimestamp != null
            ? Instant.ofEpochMilli(toTimestamp)
            : Instant.now();

        return analyticsService.getPartnerMetrics(partnerId, from, to)
            .onItem().transform(metrics -> Response.ok(Map.of(
                "partnerId", metrics.partnerId(),
                "totalRequests", metrics.totalRequests(),
                "successfulRequests", metrics.successfulRequests(),
                "errorRequests", metrics.errorRequests(),
                "serverErrors", metrics.serverErrors(),
                "avgResponseTime", metrics.avgResponseTime(),
                "minResponseTime", metrics.minResponseTime(),
                "maxResponseTime", metrics.maxResponseTime(),
                "statusCodeDistribution", metrics.statusCodeDistribution()
            )).build())
            .onFailure().recoverWithItem(throwable -> {
                Log.errorf(throwable, "Failed to retrieve partner metrics");
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(
                        "error", "ANALYTICS_ERROR",
                        "message", "Failed to retrieve partner metrics"
                    ))
                    .build();
            });
    }

    /**
     * Get top endpoints by usage.
     */
    @GET
    @Path("/top-endpoints")
    @RolesAllowed({"admin", "analytics-reader"})
    public Uni<Response> getTopEndpoints(
            @QueryParam("limit") @DefaultValue("10") int limit,
            @QueryParam("from") Long fromTimestamp,
            @QueryParam("to") Long toTimestamp) {

        Instant from = fromTimestamp != null
            ? Instant.ofEpochMilli(fromTimestamp)
            : Instant.now().minus(24, ChronoUnit.HOURS);
        Instant to = toTimestamp != null
            ? Instant.ofEpochMilli(toTimestamp)
            : Instant.now();

        return analyticsService.getTopEndpoints(limit, from, to)
            .collect().asList()
            .onItem().transform(endpoints -> Response.ok(Map.of(
                "endpoints", endpoints,
                "from", from.toEpochMilli(),
                "to", to.toEpochMilli()
            )).build())
            .onFailure().recoverWithItem(throwable -> {
                Log.errorf(throwable, "Failed to retrieve top endpoints");
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(
                        "error", "ANALYTICS_ERROR",
                        "message", "Failed to retrieve top endpoints"
                    ))
                    .build();
            });
    }

    /**
     * Get retention configuration.
     */
    @GET
    @Path("/config")
    @RolesAllowed("admin")
    public Response getConfig() {
        return Response.ok(Map.of(
            "retention", analyticsService.getRetentionConfig(),
            "bufferSize", analyticsService.getBufferSize()
        )).build();
    }

    /**
     * Health check endpoint.
     */
    @GET
    @Path("/health")
    public Response health() {
        return Response.ok(Map.of(
            "status", "UP",
            "service", "analytics",
            "bufferSize", analyticsService.getBufferSize()
        )).build();
    }
}
