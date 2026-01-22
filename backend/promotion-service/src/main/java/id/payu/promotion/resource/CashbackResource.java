package id.payu.promotion.resource;

import id.payu.promotion.domain.Cashback;
import id.payu.promotion.dto.*;
import id.payu.promotion.service.CashbackService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;

@Path("/api/v1/cashbacks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CashbackResource {

    @Inject
    CashbackService cashbackService;

    @POST
    public Response createCashback(@Valid CreateCashbackRequest request) {
        try {
            Cashback cashback = cashbackService.createCashback(request);
            return Response.status(Response.Status.CREATED)
                .entity(CashbackResponse.from(cashback))
                .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(e.getMessage()))
                .build();
        }
    }

    @GET
    @Path("/{id}")
    public Response getCashback(@PathParam("id") UUID id) {
        return cashbackService.getCashback(id)
            .map(cashback -> Response.ok(CashbackResponse.from(cashback)).build())
            .orElse(Response.status(Response.Status.NOT_FOUND)
                .entity(new ErrorResponse("Cashback not found"))
                .build());
    }

    @GET
    @Path("/account/{accountId}")
    public Response getCashbacksByAccount(@PathParam("accountId") String accountId) {
        List<Cashback> cashbacks = cashbackService.getCashbacksByAccount(accountId);
        return Response.ok(cashbacks.stream().map(CashbackResponse::from).toList()).build();
    }

    @GET
    @Path("/account/{accountId}/summary")
    public Response getCashbackSummary(@PathParam("accountId") String accountId) {
        CashbackSummaryResponse summary = cashbackService.getCashbackSummary(accountId);
        return Response.ok(summary).build();
    }

    record ErrorResponse(String message) {}
}
