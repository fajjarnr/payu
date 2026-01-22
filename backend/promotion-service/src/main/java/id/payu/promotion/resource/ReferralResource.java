package id.payu.promotion.resource;

import id.payu.promotion.domain.Referral;
import id.payu.promotion.dto.*;
import id.payu.promotion.service.ReferralService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;

@Path("/api/v1/referrals")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ReferralResource {

    @Inject
    ReferralService referralService;

    @POST
    public Response createReferral(@Valid CreateReferralRequest request) {
        try {
            Referral referral = referralService.createReferral(request);
            return Response.status(Response.Status.CREATED)
                .entity(ReferralResponse.from(referral))
                .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(e.getMessage()))
                .build();
        }
    }

    @POST
    @Path("/complete")
    public Response completeReferral(@Valid CompleteReferralRequest request) {
        try {
            Referral referral = referralService.completeReferral(request);
            return Response.ok(ReferralResponse.from(referral)).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(e.getMessage()))
                .build();
        }
    }

    @GET
    @Path("/{id}")
    public Response getReferral(@PathParam("id") UUID id) {
        return referralService.getReferral(id)
            .map(referral -> Response.ok(ReferralResponse.from(referral)).build())
            .orElse(Response.status(Response.Status.NOT_FOUND)
                .entity(new ErrorResponse("Referral not found"))
                .build());
    }

    @GET
    @Path("/code/{code}")
    public Response getReferralByCode(@PathParam("code") String code) {
        return referralService.getReferralByCode(code)
            .map(referral -> Response.ok(ReferralResponse.from(referral)).build())
            .orElse(Response.status(Response.Status.NOT_FOUND)
                .entity(new ErrorResponse("Referral code not found"))
                .build());
    }

    @GET
    @Path("/referrer/{referrerAccountId}")
    public Response getReferralsByReferrer(@PathParam("referrerAccountId") String referrerAccountId) {
        List<Referral> referrals = referralService.getReferralsByReferrer(referrerAccountId);
        return Response.ok(referrals.stream().map(ReferralResponse::from).toList()).build();
    }

    @GET
    @Path("/referrer/{referrerAccountId}/summary")
    public Response getReferralSummary(@PathParam("referrerAccountId") String referrerAccountId) {
        ReferralSummaryResponse summary = referralService.getReferralSummary(referrerAccountId);
        return Response.ok(summary).build();
    }

    record ErrorResponse(String message) {}
}
