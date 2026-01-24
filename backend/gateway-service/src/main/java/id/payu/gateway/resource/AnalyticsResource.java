package id.payu.gateway.resource;

import id.payu.gateway.service.ApiAnalyticsService;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;

/**
 * REST resource to expose API analytics.
 */
@Path("/gateway/analytics")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AnalyticsResource {

    @Inject
    ApiAnalyticsService analyticsService;

    @GET
    @Path("/metrics")
    public Uni<Response> getMetrics(
            @QueryParam("path") String path,
            @QueryParam("method") String method) {

        if (path == null || path.isBlank()) {
            return Uni.createFrom().item(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "path parameter is required"))
                    .build()
            );
        }

        return analyticsService.getMetrics(path, method)
            .onItem().transform(metrics -> {
                if (metrics.isEmpty()) {
                    return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of(
                            "error", "METRICS_NOT_FOUND",
                            "message", "No metrics found for the specified endpoint"
                        ))
                        .build();
                }
                return Response.ok(metrics).build();
            })
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

    @GET
    @Path("/health")
    public Response health() {
        return Response.ok(Map.of(
            "status", "UP",
            "service", "analytics"
        )).build();
    }
}
