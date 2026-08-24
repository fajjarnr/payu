package id.payu.simulator.biller.resource;

import id.payu.simulator.biller.interfaces.dto.*;
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
    public Response inquiry(@Valid InquiryRequest request, @HeaderParam("X-Simulate") String simulate) {
        if (simulate != null && !simulate.isBlank()) {
            String m = simulate.trim().toLowerCase();
            if ("rate-limit".equals(m)) return Response.status(429).entity(InquiryResponse.error("Rate limit exceeded")).header("X-Simulate", simulate).build();
            if ("5xx".equals(m)) return Response.status(500).entity(InquiryResponse.error("Simulated internal error")).header("X-Simulate", simulate).build();
        }
        InquiryResponse response = billerService.inquiry(request, simulate);
        int status = switch (response.responseCode()) { case "00" -> 200; case "62" -> 403; case "42" -> 429; case "96" -> 500; default -> 400; };
        return Response.status(status).entity(response).header("X-Simulate", simulate != null ? simulate : "success").build();
    }

    @POST
    @Path("/pay")
    public Response pay(@Valid PaymentRequest request, @HeaderParam("X-Simulate") String simulate, @HeaderParam("X-Idempotency-Key") String idempotencyKey, @HeaderParam("X-External-Id") String externalId) {
        if (simulate != null && !simulate.isBlank()) {
            String m = simulate.trim().toLowerCase();
            if ("rate-limit".equals(m)) return Response.status(429).entity(PaymentResponse.error("Rate limit exceeded")).header("X-Simulate", simulate).build();
            if ("5xx".equals(m)) return Response.status(500).entity(PaymentResponse.error("Simulated internal error")).header("X-Simulate", simulate).build();
        }
        PaymentResponse response = billerService.pay(request, simulate);
        int status = switch (response.responseCode()) {
            case "00" -> 200;
            case "94" -> 409;
            case "42" -> 429;
            case "96" -> 500;
            default -> 400;
        };
        return Response.status(status).entity(response).header("X-Simulate", simulate != null ? simulate : "success").header("X-Idempotency-Key", idempotencyKey != null ? idempotencyKey : externalId).build();
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
