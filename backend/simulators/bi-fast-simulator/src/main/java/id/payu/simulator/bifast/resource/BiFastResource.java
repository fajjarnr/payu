package id.payu.simulator.bifast.resource;

import id.payu.simulator.bifast.dto.*;
import id.payu.simulator.bifast.service.BiFastService;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST API resource for BI-FAST simulation.
 * 
 * Endpoints:
 * - POST /api/v1/inquiry     - Account inquiry
 * - POST /api/v1/transfer    - Fund transfer
 * - GET  /api/v1/status/{ref} - Transfer status
 */
@Path("/api/v1")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BiFastResource {

    @Inject
    BiFastService biFastService;

    /**
     * Account inquiry - Get account holder name and status.
     * 
     * @param request Inquiry request with bank code and account number
     * @return Account holder information or error
     */
    @POST
    @Path("/inquiry")
    public Response inquiry(@Valid InquiryRequest request) {
        Log.infof("Received inquiry request: bank=%s, account=%s", 
                  request.bankCode(), request.accountNumber());
        
        try {
            InquiryResponse response = biFastService.inquiry(request);
            
            int statusCode = switch (response.responseCode()) {
                case "00" -> 200;
                case "14" -> 404;
                case "62" -> 403;
                case "68" -> 504;
                default -> 500;
            };
            
            return Response.status(statusCode).entity(response).build();
        } catch (Exception e) {
            Log.errorf(e, "Error processing inquiry");
            return Response.serverError()
                    .entity(InquiryResponse.error(e.getMessage()))
                    .build();
        }
    }

    /**
     * Fund transfer - Initiate BI-FAST transfer.
     * 
     * @param request Transfer request with source, destination, and amount
     * @return Transfer response with reference number and status
     */
    @POST
    @Path("/transfer")
    public Response transfer(@Valid TransferRequest request) {
        Log.infof("Received transfer request: %s-%s to %s-%s, amount=%s",
                  request.sourceBankCode(), request.sourceAccountNumber(),
                  request.destinationBankCode(), request.destinationAccountNumber(),
                  request.amount());
        
        try {
            TransferResponse response = biFastService.transfer(request);
            
            int statusCode = switch (response.responseCode()) {
                case "00" -> 200;
                case "09" -> 202; // Accepted, processing
                case "51" -> 400; // Failed
                case "68" -> 504; // Timeout
                default -> 500;
            };
            
            return Response.status(statusCode).entity(response).build();
        } catch (Exception e) {
            Log.errorf(e, "Error processing transfer");
            return Response.serverError()
                    .entity(TransferResponse.error(e.getMessage()))
                    .build();
        }
    }

    /**
     * Get transfer status by reference number.
     * 
     * @param referenceNumber The transfer reference number
     * @return Transfer status
     */
    @GET
    @Path("/status/{referenceNumber}")
    public Response getStatus(@PathParam("referenceNumber") String referenceNumber) {
        Log.infof("Received status request: ref=%s", referenceNumber);
        
        try {
            TransferResponse response = biFastService.getStatus(referenceNumber);
            
            if (response.responseCode().equals("96") && 
                response.responseMessage().startsWith("Transfer not found")) {
                return Response.status(404).entity(response).build();
            }
            
            return Response.ok(response).build();
        } catch (Exception e) {
            Log.errorf(e, "Error getting status");
            return Response.serverError()
                    .entity(TransferResponse.error(e.getMessage()))
                    .build();
        }
    }

    /**
     * Health check endpoint specific to BI-FAST.
     */
    @GET
    @Path("/health")
    public Response health() {
        return Response.ok()
                .entity(new HealthResponse("UP", "bi-fast-simulator", "1.0.0"))
                .build();
    }

    public record HealthResponse(String status, String service, String version) {}
}
