package id.payu.simulator.qris.resource;

import id.payu.simulator.qris.interfaces.dto.*;
import id.payu.simulator.qris.service.QrisService;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST API resource for QRIS simulation.
 * 
 * Endpoints:
 * - POST /api/v1/generate       - Generate QRIS code
 * - POST /api/v1/pay            - Simulate payment
 * - GET  /api/v1/status/{qrId}  - Get payment status
 */
@Path("/api/v1")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class QrisResource {

    @Inject
    QrisService qrisService;

    /**
     * Generate QRIS code for a merchant.
     * Returns QR code image as base64 string.
     * 
     * @param request QR generation request with merchant and amount
     * @return Generated QR code with image and reference number
     */
    @POST
    @Path("/generate")
    public Response generateQr(@Valid GenerateQrRequest request, @HeaderParam("X-Simulate") String simulate) {
        Log.infof("Received QR generation request for merchant=%s", request.merchantId());
        
        try {
            if (simulate != null && !simulate.isBlank()) {
                String m = simulate.trim().toLowerCase();
                if ("rate-limit".equals(m)) return Response.status(429).entity(GenerateQrResponse.error("Rate limit exceeded")).header("X-Simulate", simulate).build();
                if ("5xx".equals(m)) return Response.status(500).entity(GenerateQrResponse.error("Simulated internal error")).header("X-Simulate", simulate).build();
            }
            GenerateQrResponse response = qrisService.generateQr(request, simulate);
            
            int statusCode = switch (response.responseCode()) {
                case "00" -> 201; // Created
                case "14" -> 404; // Merchant not found
                case "62" -> 403; // Merchant blocked
                default -> 500;
            };
            
            return Response.status(statusCode).entity(response).header("X-Simulate", simulate != null ? simulate : "success").build();
        } catch (Exception e) {
            Log.errorf(e, "Error generating QR code");
            return Response.serverError()
                    .entity(GenerateQrResponse.error(e.getMessage()))
                    .build();
        }
    }

    /**
     * Simulate payment for a QRIS code.
     * This endpoint simulates a customer scanning and paying the QR.
     * 
     * @param request Payment request with payer details
     * @return Payment result
     */
    @POST
    @Path("/pay")
    public Response payQr(@Valid PayQrRequest request, @HeaderParam("X-Simulate") String simulate, @HeaderParam("X-Idempotency-Key") String idempotencyKey) {
        Log.infof("Received payment request for qrId=%s", request.qrId());
        
        try {
            if (simulate != null && "5xx".equals(simulate.trim().toLowerCase())) return Response.status(500).entity(PaymentResponse.error("Simulated internal error")).header("X-Simulate", simulate).build();
            if (simulate != null && "rate-limit".equals(simulate.trim().toLowerCase())) return Response.status(429).entity(PaymentResponse.error("Rate limit exceeded")).header("X-Simulate", simulate).build();
            PaymentResponse response = qrisService.payQr(request, simulate);
            
            int statusCode = switch (response.responseCode()) {
                case "00" -> 200; // Success
                case "14" -> 404; // QR not found
                case "51" -> 400; // Payment failed
                case "54" -> 410; // Expired (Gone)
                case "55" -> 409; // Already paid (Conflict)
                default -> 500;
            };
            
            return Response.status(statusCode).entity(response).header("X-Simulate", simulate != null ? simulate : "success").header("X-Idempotency-Key", idempotencyKey).build();
        } catch (Exception e) {
            Log.errorf(e, "Error processing payment");
            return Response.serverError()
                    .entity(PaymentResponse.error(e.getMessage()))
                    .build();
        }
    }

    /**
     * Get payment status by QR ID or reference number.
     * 
     * @param qrId The QR ID or reference number
     * @return Payment status details
     */
    @GET
    @Path("/status/{qrId}")
    public Response getStatus(@PathParam("qrId") String qrId) {
        Log.infof("Received status request for qrId=%s", qrId);
        
        try {
            PaymentStatusResponse response = qrisService.getStatus(qrId);
            
            int statusCode = switch (response.responseCode()) {
                case "00", "09" -> 200; // Success or pending
                case "14" -> 404; // Not found
                case "54" -> 200; // Expired (still return 200)
                case "51" -> 200; // Failed (still return 200)
                default -> 500;
            };
            
            return Response.status(statusCode).entity(response).build();
        } catch (Exception e) {
            Log.errorf(e, "Error getting status");
            return Response.serverError()
                    .entity(PaymentStatusResponse.error(e.getMessage()))
                    .build();
        }
    }

    /**
     * Health check endpoint.
     */
    @GET
    @Path("/health")
    public Response health() {
        return Response.ok()
                .entity(new HealthResponse("UP", "qris-simulator", "1.0.0"))
                .build();
    }

    public record HealthResponse(String status, String service, String version) {}
}
