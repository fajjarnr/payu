package id.payu.gateway.adapter.web;

import id.payu.gateway.application.service.GatewayDispatchService;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

/**
 * Public SNAP-BI v1.0 contract entry point.
 *
 * <p>SNAP-PATH-001: the standard SNAP-BI taxonomy path {@code /v1.0/**} (e.g.
 * {@code /v1.0/access-token/b2b}, {@code /v1.0/transfer-va/payment}) must reach
 * partner-service without a doubled prefix, exactly like the legacy
 * {@code /v1/partner/**} entry ({@link PartnerContractResource}). Both entry
 * points share the same {@link GatewayDispatchService}, so routing can never
 * drift.
 */
@Path("/v1.0")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Blocking
public class PartnerV10ContractResource {

    @Inject
    GatewayDispatchService dispatcher;

    @Context
    UriInfo uriInfo;

    @GET
    @Path("/{path: .*}")
    public Uni<Response> get(@PathParam("path") String path,
                             @Context HttpHeaders headers) {
        return dispatcher.dispatch("v1.0/" + path, "GET", null, headers, uriInfo);
    }

    @POST
    @Path("/{path: .*}")
    public Uni<Response> post(@PathParam("path") String path,
                              String body,
                              @Context HttpHeaders headers) {
        return dispatcher.dispatch("v1.0/" + path, "POST", body, headers, uriInfo);
    }

    @PUT
    @Path("/{path: .*}")
    public Uni<Response> put(@PathParam("path") String path,
                             String body,
                             @Context HttpHeaders headers) {
        return dispatcher.dispatch("v1.0/" + path, "PUT", body, headers, uriInfo);
    }

    @DELETE
    @Path("/{path: .*}")
    public Uni<Response> delete(@PathParam("path") String path,
                                String body,
                                @Context HttpHeaders headers) {
        return dispatcher.dispatch("v1.0/" + path, "DELETE", body, headers, uriInfo);
    }

    @PATCH
    @Path("/{path: .*}")
    public Uni<Response> patch(@PathParam("path") String path,
                               String body,
                               @Context HttpHeaders headers) {
        return dispatcher.dispatch("v1.0/" + path, "PATCH", body, headers, uriInfo);
    }
}
