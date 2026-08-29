package id.payu.gateway.adapter.web;

import id.payu.gateway.application.service.GatewayDispatchService;
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
 * API Gateway Resource - single catch-all dispatcher.
 *
 * <p>All incoming requests under {@code /api/v1} are matched against the
 * {@code RouteRegistry} (via {@link GatewayDispatcher}) which resolves the target
 * backend service via longest prefix match. This design avoids the Quarkus
 * RESTeasy Reactive exact-vs-greedy @Path conflict that occurs when both
 * {@code @Path("/foo")} and {@code @Path("/foo/{path: .*}")} are declared in
 * the same resource.
 *
 * <p>Per Quarkus best practice:
 * <ul>
 *   <li>Single catch-all per HTTP method, no exact @Path duplicates</li>
 *   <li>Routing logic lives in {@code RouteRegistry}, not in @Path annotations</li>
 *   <li>Adding a new route = update {@code gateway.routes} config, no code change</li>
 * </ul>
 *
 * <p>The public SNAP-BI contract {@code /v1/partner/**} is served by
 * {@link PartnerContractResource} which shares the same dispatcher, so both
 * entry points route identically (PARTNER-001).
 */
@Path("/api/v1")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class ApiGatewayResource {

    @Inject
    GatewayDispatchService dispatcher;

    @Context
    UriInfo uriInfo;

    // ==================== Single Catch-All Dispatcher ====================

    @GET
    @Path("/{path: .*}")
    public Uni<Response> get(@PathParam("path") String path,
                              @Context HttpHeaders headers) {
        return dispatcher.dispatch(path, "GET", null, headers, uriInfo);
    }

    @POST
    @Path("/{path: .*}")
    public Uni<Response> post(@PathParam("path") String path,
                               String body,
                               @Context HttpHeaders headers) {
        return dispatcher.dispatch(path, "POST", body, headers, uriInfo);
    }

    @PUT
    @Path("/{path: .*}")
    public Uni<Response> put(@PathParam("path") String path,
                              String body,
                              @Context HttpHeaders headers) {
        return dispatcher.dispatch(path, "PUT", body, headers, uriInfo);
    }

    @DELETE
    @Path("/{path: .*}")
    public Uni<Response> delete(@PathParam("path") String path,
                                 String body,
                                 @Context HttpHeaders headers) {
        return dispatcher.dispatch(path, "DELETE", body, headers, uriInfo);
    }

    @PATCH
    @Path("/{path: .*}")
    public Uni<Response> patch(@PathParam("path") String path,
                                String body,
                                @Context HttpHeaders headers) {
        return dispatcher.dispatch(path, "PATCH", body, headers, uriInfo);
    }
}
