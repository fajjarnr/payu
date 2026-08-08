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
 * Public SNAP-BI partner contract entry point.
 *
 * <p>PARTNER-001: the contract path {@code /v1/partner/**} (e.g.
 * {@code /v1/partner/auth/token}) must reach partner-service without the
 * {@code /api/v1} prefix and without a doubled {@code /v1}. This resource
 * owns the exact {@code /v1/partner} prefix and hands the remainder to the
 * same {@link GatewayDispatcher} used by the {@code /api/v1} catch-all.
 *
 * <p>The gateway {@link jakarta.ws.rs.container.ContainerRequestFilter}s
 * (authorization, idempotency, rate limit) already treat {@code /v1/partner}
 * as the public SNAP-BI boundary, so requests to this path bypass platform JWT
 * auth and are authenticated by the partner-service client-key HMAC flow.
 */
@Path("/v1/partner")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Blocking
public class PartnerContractResource {

    @Inject
    GatewayDispatchService dispatcher;

    @Context
    UriInfo uriInfo;

    @GET
    @Path("/{path: .*}")
    public Uni<Response> get(@PathParam("path") String path,
                             @Context HttpHeaders headers) {
        return dispatcher.dispatch("v1/partner/" + path, "GET", null, headers, uriInfo);
    }

    @POST
    @Path("/{path: .*}")
    public Uni<Response> post(@PathParam("path") String path,
                              String body,
                              @Context HttpHeaders headers) {
        return dispatcher.dispatch("v1/partner/" + path, "POST", body, headers, uriInfo);
    }

    @PUT
    @Path("/{path: .*}")
    public Uni<Response> put(@PathParam("path") String path,
                             String body,
                             @Context HttpHeaders headers) {
        return dispatcher.dispatch("v1/partner/" + path, "PUT", body, headers, uriInfo);
    }

    @DELETE
    @Path("/{path: .*}")
    public Uni<Response> delete(@PathParam("path") String path,
                                String body,
                                @Context HttpHeaders headers) {
        return dispatcher.dispatch("v1/partner/" + path, "DELETE", body, headers, uriInfo);
    }

    @PATCH
    @Path("/{path: .*}")
    public Uni<Response> patch(@PathParam("path") String path,
                               String body,
                               @Context HttpHeaders headers) {
        return dispatcher.dispatch("v1/partner/" + path, "PATCH", body, headers, uriInfo);
    }
}
