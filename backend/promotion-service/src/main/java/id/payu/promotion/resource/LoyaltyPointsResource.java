package id.payu.promotion.resource;

import id.payu.promotion.domain.LoyaltyPoints;
import id.payu.promotion.dto.*;
import id.payu.promotion.service.LoyaltyPointsService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;

@Path("/api/v1/loyalty-points")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LoyaltyPointsResource {

    @Inject
    LoyaltyPointsService loyaltyPointsService;

    @POST
    public Response addPoints(@Valid CreateLoyaltyPointsRequest request) {
        try {
            LoyaltyPoints loyaltyPoints = loyaltyPointsService.addPoints(request);
            return Response.status(Response.Status.CREATED)
                .entity(LoyaltyPointsResponse.from(loyaltyPoints))
                .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(e.getMessage()))
                .build();
        }
    }

    @POST
    @Path("/redeem")
    public Response redeemPoints(@Valid RedeemLoyaltyPointsRequest request) {
        try {
            LoyaltyPoints loyaltyPoints = loyaltyPointsService.redeemPoints(request);
            return Response.ok(LoyaltyPointsResponse.from(loyaltyPoints)).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(e.getMessage()))
                .build();
        }
    }

    @GET
    @Path("/{id}")
    public Response getLoyaltyPoints(@PathParam("id") UUID id) {
        return loyaltyPointsService.getLoyaltyPoints(id)
            .map(loyaltyPoints -> Response.ok(LoyaltyPointsResponse.from(loyaltyPoints)).build())
            .orElse(Response.status(Response.Status.NOT_FOUND)
                .entity(new ErrorResponse("Loyalty points record not found"))
                .build());
    }

    @GET
    @Path("/account/{accountId}")
    public Response getLoyaltyPointsByAccount(@PathParam("accountId") String accountId) {
        List<LoyaltyPoints> loyaltyPoints = loyaltyPointsService.getLoyaltyPointsByAccount(accountId);
        return Response.ok(loyaltyPoints.stream().map(LoyaltyPointsResponse::from).toList()).build();
    }

    @GET
    @Path("/account/{accountId}/balance")
    public Response getBalance(@PathParam("accountId") String accountId) {
        LoyaltyBalanceResponse balance = loyaltyPointsService.getBalance(accountId);
        return Response.ok(balance).build();
    }

    record ErrorResponse(String message) {}
}
