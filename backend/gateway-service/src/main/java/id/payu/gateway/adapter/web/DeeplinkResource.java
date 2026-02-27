package id.payu.gateway.adapter.web;

import id.payu.gateway.application.service.DeeplinkService;
import id.payu.gateway.dto.ApiResponse;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Resource for generating checkout deep links for the PayU mobile app.
 * URL scheme: payu://pay, payu://topup, payu://transfer
 *
 * Part of E-15 IMP-046: Checkout Deeplink
 */
@Path("/api/v1/deeplinks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class DeeplinkResource {

    @Inject
    DeeplinkService deeplinkService;

    /**
     * Generate a signed deeplink URL for the PayU mobile app.
     */
    @POST
    public Response generateDeeplink(DeeplinkService.DeeplinkRequest request) {
        Log.infof("POST /deeplinks action=%s token=%s", request.action(), request.token());

        DeeplinkService.DeeplinkResult result = deeplinkService.generateDeeplink(request);

        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.success(result))
                .build();
    }
}
