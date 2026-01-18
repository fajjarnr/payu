package id.payu.simulator.dukcapil.resource;

import id.payu.simulator.dukcapil.dto.*;
import id.payu.simulator.dukcapil.service.DukcapilService;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST API resource for Dukcapil simulation.
 * 
 * Endpoints:
 * - POST /api/v1/verify        - NIK verification with data comparison
 * - POST /api/v1/match-photo   - Face matching (KTP vs Selfie)
 * - GET  /api/v1/nik/{nik}     - Get citizen data by NIK
 */
@Path("/api/v1")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DukcapilResource {

    @Inject
    DukcapilService dukcapilService;

    /**
     * Verify NIK and compare with provided data.
     * Used for eKYC name matching.
     * 
     * @param request Verification request with NIK and personal data
     * @return Verification result with match status
     */
    @POST
    @Path("/verify")
    public Response verifyNik(@Valid VerifyNikRequest request) {
        Log.infof("Received NIK verification request for: %s****", 
                  request.nik().substring(0, 4));
        
        try {
            VerifyNikResponse response = dukcapilService.verifyNik(request);
            
            int statusCode = switch (response.responseCode()) {
                case "00" -> response.verified() ? 200 : 200; // Still 200, just not verified
                case "14" -> 404; // Not found
                case "30" -> 400; // Invalid
                case "62" -> 403; // Blocked
                default -> 500;
            };
            
            return Response.status(statusCode).entity(response).build();
        } catch (Exception e) {
            Log.errorf(e, "Error processing NIK verification");
            return Response.serverError()
                    .entity(VerifyNikResponse.error(null, e.getMessage()))
                    .build();
        }
    }

    /**
     * Face matching between KTP photo and selfie.
     * Used for eKYC biometric verification.
     * 
     * @param request Face match request with photos in base64
     * @return Match result with score and threshold
     */
    @POST
    @Path("/match-photo")
    public Response matchFace(@Valid FaceMatchRequest request) {
        Log.infof("Received face match request for NIK: %s****", 
                  request.nik().substring(0, 4));
        
        try {
            FaceMatchResponse response = dukcapilService.matchFace(request);
            
            int statusCode = switch (response.responseCode()) {
                case "00" -> 200; // Matched
                case "51" -> 200; // Not matched (still valid response)
                case "52" -> 400; // Liveness failed
                case "14" -> 404; // NIK not found
                case "62" -> 403; // Blocked
                default -> 500;
            };
            
            return Response.status(statusCode).entity(response).build();
        } catch (Exception e) {
            Log.errorf(e, "Error processing face match");
            return Response.serverError()
                    .entity(FaceMatchResponse.error(null, e.getMessage()))
                    .build();
        }
    }

    /**
     * Get citizen data by NIK.
     * Returns full citizen information for verified requests.
     * 
     * @param nik The 16-digit NIK
     * @return Citizen data
     */
    @GET
    @Path("/nik/{nik}")
    public Response getCitizenData(
            @PathParam("nik") 
            @Pattern(regexp = "^[0-9]{16}$", message = "NIK must be exactly 16 digits")
            String nik) {
        Log.infof("Received citizen data request for NIK: %s****", nik.substring(0, 4));
        
        try {
            CitizenDataResponse response = dukcapilService.getCitizenData(nik);
            
            int statusCode = switch (response.responseCode()) {
                case "00" -> 200;
                case "14" -> 404;
                case "62" -> 403;
                default -> 500;
            };
            
            return Response.status(statusCode).entity(response).build();
        } catch (Exception e) {
            Log.errorf(e, "Error retrieving citizen data");
            return Response.serverError()
                    .entity(CitizenDataResponse.error(null, e.getMessage()))
                    .build();
        }
    }

    /**
     * Health check endpoint specific to Dukcapil simulator.
     */
    @GET
    @Path("/health")
    public Response health() {
        return Response.ok()
                .entity(new HealthResponse("UP", "dukcapil-simulator", "1.0.0"))
                .build();
    }

    public record HealthResponse(String status, String service, String version) {}
}
