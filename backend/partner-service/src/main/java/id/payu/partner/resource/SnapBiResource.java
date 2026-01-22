package id.payu.partner.resource;

import id.payu.partner.domain.Partner;
import id.payu.partner.dto.snap.PaymentRequest;
import id.payu.partner.dto.snap.PaymentResponse;
import id.payu.partner.dto.snap.TokenRequest;
import id.payu.partner.dto.snap.TokenResponse;
import id.payu.partner.repository.PartnerRepository;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Base64;
import java.util.UUID;

@Path("/v1/partner")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SnapBiResource {

    @Inject
    PartnerRepository partnerRepository;

    @POST
    @Path("/auth/token")
    public Response getAccessToken(@HeaderParam("X-CLIENT-KEY") String clientKey, 
                                   @HeaderParam("X-TIMESTAMP") String timestamp,
                                   @HeaderParam("X-SIGNATURE") String signature,
                                   TokenRequest request) {
        
        if (clientKey == null) {
             return Response.status(Response.Status.UNAUTHORIZED).entity("Missing X-CLIENT-KEY").build();
        }

        Partner partner = partnerRepository.find("clientId", clientKey).firstResult();
        if (partner == null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Invalid Client Key").build();
        }

        if (!partner.active) {
             return Response.status(Response.Status.UNAUTHORIZED).entity("Partner is inactive").build();
        }

        // Mock Token Generation
        String accessToken = Base64.getEncoder().encodeToString((partner.clientId + ":" + System.currentTimeMillis()).getBytes());
        // In real world, we would store this token with expiry.

        TokenResponse response = new TokenResponse(accessToken, "Bearer", "900");
        return Response.ok(response).build();
    }

    @POST
    @Path("/payments")
    public Response createPayment(@HeaderParam("Authorization") String authorization,
                                  @HeaderParam("X-EXTERNAL-ID") String externalId,
                                  PaymentRequest request) {
        
        // Validate Token (Mock)
        if (authorization == null || !authorization.startsWith("Bearer ")) {
             return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        // Process Payment (Mock)
        // Ideally call Transaction Service via Kafka or REST
        
        PaymentResponse response = new PaymentResponse(
            "2002500", // SNAP Success Code
            "Successful", 
            request.partnerReferenceNo, 
            UUID.randomUUID().toString() // PayU Reference No
        );
        
        return Response.ok(response).build();
    }
}
