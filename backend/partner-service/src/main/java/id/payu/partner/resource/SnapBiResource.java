package id.payu.partner.resource;

import id.payu.partner.domain.Partner;
import id.payu.partner.dto.snap.PaymentRequest;
import id.payu.partner.dto.snap.PaymentResponse;
import id.payu.partner.dto.snap.PaymentStatusResponse;
import id.payu.partner.dto.snap.TokenRequest;
import id.payu.partner.dto.snap.TokenResponse;
import id.payu.partner.repository.PartnerRepository;
import id.payu.partner.service.SnapBiPaymentService;
import id.payu.partner.service.SnapBiSignatureService;
import id.payu.partner.service.SnapBiTokenService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import com.fasterxml.jackson.databind.ObjectMapper;

@Path("/v1/partner")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SnapBiResource {

    @Inject
    PartnerRepository partnerRepository;

    @Inject
    SnapBiSignatureService signatureService;

    @Inject
    SnapBiTokenService tokenService;

    @Inject
    SnapBiPaymentService paymentService;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @POST
    @Path("/auth/token")
    public Response getAccessToken(@HeaderParam("X-CLIENT-KEY") String clientKey, 
                                   @HeaderParam("X-TIMESTAMP") String timestamp,
                                   @HeaderParam("X-SIGNATURE") String signature,
                                   TokenRequest request) {
        
        if (clientKey == null) {
             return Response.status(Response.Status.UNAUTHORIZED)
                 .entity(createErrorResponse("4012501", "Missing X-CLIENT-KEY")).build();
        }

        Partner partner = partnerRepository.find("clientId", clientKey).firstResult();
        if (partner == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity(createErrorResponse("4012502", "Invalid Client Key")).build();
        }

        if (!partner.active) {
             return Response.status(Response.Status.UNAUTHORIZED)
                 .entity(createErrorResponse("4012503", "Partner is inactive")).build();
        }

        try {
            String requestBody = objectMapper.writeValueAsString(request);
            boolean signatureValid = signatureService.validateSignatureWithClientKey(
                partner.clientSecret, 
                "POST", 
                "/v1/partner/auth/token", 
                timestamp, 
                requestBody, 
                signature
            );

            if (!signatureValid) {
                return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(createErrorResponse("4012504", "Invalid Signature")).build();
            }
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(createErrorResponse("4002501", "Invalid Request Body")).build();
        }

        String accessToken = tokenService.generateAccessToken(
            partner.clientId, 
            partner.id.toString(), 
            partner.name
        );

        TokenResponse response = new TokenResponse(accessToken, "Bearer", "900");
        return Response.ok(response).build();
    }

    @POST
    @Path("/payments")
    public Uni<Response> createPayment(@HeaderParam("Authorization") String authorization,
                                       @HeaderParam("X-EXTERNAL-ID") String externalId,
                                       @HeaderParam("X-TIMESTAMP") String timestamp,
                                       @HeaderParam("X-SIGNATURE") String signature,
                                       PaymentRequest request) {
        
        if (authorization == null || !authorization.startsWith("Bearer ")) {
             return Uni.createFrom().item(Response.status(Response.Status.UNAUTHORIZED)
                 .entity(createErrorResponse("4012505", "Missing or Invalid Authorization Header")).build());
        }

        String token = authorization.substring(7);
        String clientId = tokenService.getClientIdFromToken(token);
        
        if (clientId == null) {
            return Uni.createFrom().item(Response.status(Response.Status.UNAUTHORIZED)
                .entity(createErrorResponse("4012506", "Invalid or Expired Token")).build());
        }

        Partner partner = partnerRepository.find("clientId", clientId).firstResult();
        if (partner == null || !partner.active) {
            return Uni.createFrom().item(Response.status(Response.Status.UNAUTHORIZED)
                .entity(createErrorResponse("4012507", "Partner not found or inactive")).build());
        }

        try {
            String requestBody = objectMapper.writeValueAsString(request);
            boolean signatureValid = signatureService.validateSignature(
                partner.clientSecret, 
                "POST", 
                "/v1/partner/payments", 
                token, 
                requestBody, 
                timestamp, 
                signature
            );

            if (!signatureValid) {
                return Uni.createFrom().item(Response.status(Response.Status.UNAUTHORIZED)
                    .entity(createErrorResponse("4012504", "Invalid Signature")).build());
            }
        } catch (Exception e) {
            return Uni.createFrom().item(Response.status(Response.Status.BAD_REQUEST)
                .entity(createErrorResponse("4002501", "Invalid Request Body")).build());
        }

        return paymentService.createPayment(partner.id.toString(), request)
            .onItem().transform(response -> Response.ok(response).build());
    }

    @GET
    @Path("/payments/{id}")
    public Uni<Response> getPaymentStatus(@HeaderParam("Authorization") String authorization,
                                          @HeaderParam("X-TIMESTAMP") String timestamp,
                                          @HeaderParam("X-SIGNATURE") String signature,
                                          @PathParam("id") String referenceNo) {
        
        if (authorization == null || !authorization.startsWith("Bearer ")) {
             return Uni.createFrom().item(Response.status(Response.Status.UNAUTHORIZED)
                 .entity(createErrorResponse("4012505", "Missing or Invalid Authorization Header")).build());
        }

        String token = authorization.substring(7);
        String clientId = tokenService.getClientIdFromToken(token);
        
        if (clientId == null) {
            return Uni.createFrom().item(Response.status(Response.Status.UNAUTHORIZED)
                .entity(createErrorResponse("4012506", "Invalid or Expired Token")).build());
        }

        Partner partner = partnerRepository.find("clientId", clientId).firstResult();
        if (partner == null || !partner.active) {
            return Uni.createFrom().item(Response.status(Response.Status.UNAUTHORIZED)
                .entity(createErrorResponse("4012507", "Partner not found or inactive")).build());
        }

        try {
            String requestBody = "";
            boolean signatureValid = signatureService.validateSignature(
                partner.clientSecret, 
                "GET", 
                "/v1/partner/payments/" + referenceNo, 
                token, 
                requestBody, 
                timestamp, 
                signature
            );

            if (!signatureValid) {
                return Uni.createFrom().item(Response.status(Response.Status.UNAUTHORIZED)
                    .entity(createErrorResponse("4012504", "Invalid Signature")).build());
            }
        } catch (Exception e) {
            return Uni.createFrom().item(Response.status(Response.Status.BAD_REQUEST)
                .entity(createErrorResponse("4002501", "Invalid Request")).build());
        }

        return paymentService.getPaymentStatus(partner.id.toString(), referenceNo)
            .onItem().transform(response -> Response.ok(response).build());
    }

    private String createErrorResponse(String responseCode, String responseMessage) {
        return String.format("{\"responseCode\":\"%s\",\"responseMessage\":\"%s\"}", responseCode, responseMessage);
    }
}