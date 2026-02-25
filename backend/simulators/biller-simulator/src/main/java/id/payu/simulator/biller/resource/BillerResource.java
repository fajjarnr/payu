package id.payu.simulator.biller.resource;

import id.payu.simulator.biller.dto.*;
import id.payu.simulator.biller.service.BillerService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST API for biller simulation.
 * 
 * Endpoints:
 *   POST /api/v1/biller/inquiry    — Check customer & outstanding bill
 *   POST /api/v1/biller/pay        — Process a bill payment
 *   GET  /api/v1/biller/status/{ref} — Check payment status
 *   GET  /api/v1/biller/health      — Health check
 */
@Path("/api/v1/biller")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BillerResource {

    @Inject
    BillerService billerService;

    @POST
    @Path("/inquiry")
    public Response inquiry(@Valid InquiryRequest request) {
        InquiryResponse response = billerService.inquiry(request);
        int status = "00".equals(response.responseCode()) ? 200 : 400;
        return Response.status(status).entity(response).build();
    }

    @POST
    @Path("/pay")
    public Response pay(@Valid PaymentRequest request) {
        PaymentResponse response = billerService.pay(request);
        int status = switch (response.responseCode()) {
            case "00" -> 200;
            case "94" -> 409; // Duplicate
            default -> 400;
        };
        return Response.status(status).entity(response).build();
    }

    @GET
    @Path("/status/{referenceNumber}")
    public Response status(@PathParam("referenceNumber") String referenceNumber) {
        PaymentResponse response = billerService.status(referenceNumber);
        int status = "00".equals(response.responseCode()) ? 200 : 404;
        return Response.status(status).entity(response).build();
    }

    @GET
    @Path("/health")
    public Response health() {
        return Response.ok(new HealthResponse("UP", "biller-simulator")).build();
    }

    public record HealthResponse(String status, String service) {}
}
