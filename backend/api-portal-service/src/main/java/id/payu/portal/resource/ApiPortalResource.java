package id.payu.portal.resource;

import id.payu.portal.dto.AggregatedOpenApiResponse;
import id.payu.portal.dto.OpenApiSpec;
import id.payu.portal.dto.ServiceListResponse;
import id.payu.portal.service.ApiPortalService;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/v1/portal")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "API Portal", description = "Centralized API documentation portal")
public class ApiPortalResource {

    @Inject
    ApiPortalService apiPortalService;

    @GET
    @Path("/services")
    @Operation(summary = "List all registered services")
    public Uni<ServiceListResponse> listServices() {
        return apiPortalService.listServices();
    }

    @GET
    @Path("/services/{serviceId}/openapi")
    @Operation(summary = "Get OpenAPI spec for a specific service")
    public Uni<Response> getServiceSpec(@PathParam("serviceId") String serviceId) {
        return apiPortalService.getServiceSpec(serviceId)
            .onItem().transform(spec -> {
                if (spec != null) {
                    return Response.ok(spec).build();
                } else {
                    return Response.status(Response.Status.NOT_FOUND)
                        .entity("OpenAPI spec not found for service: " + serviceId)
                        .build();
                }
            })
            .onFailure().recoverWithItem(t -> {
                Log.errorf("Error fetching OpenAPI spec for %s: %s", serviceId, t.getMessage());
                return Response.serverError()
                    .entity("Error fetching OpenAPI spec")
                    .build();
            });
    }

    @GET
    @Path("/openapi")
    @Operation(summary = "Get aggregated OpenAPI specs for all services")
    public Uni<AggregatedOpenApiResponse> getAggregatedSpecs(@QueryParam("refresh") @DefaultValue("false") boolean refresh) {
        if (refresh) {
            return apiPortalService.refreshCache();
        }
        return apiPortalService.getAggregatedSpecs();
    }

    @POST
    @Path("/refresh")
    @Operation(summary = "Refresh all OpenAPI spec caches")
    public Uni<AggregatedOpenApiResponse> refreshSpecs() {
        return apiPortalService.refreshCache();
    }
}
