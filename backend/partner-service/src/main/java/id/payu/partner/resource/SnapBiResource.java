package id.payu.partner.resource;

import id.payu.partner.domain.Partner;
import id.payu.partner.dto.snap.PaymentRequest;
import id.payu.partner.dto.snap.PaymentResponse;
import id.payu.partner.dto.snap.PaymentStatusResponse;
import id.payu.partner.dto.snap.RefundRequest;
import id.payu.partner.dto.snap.RefundResponse;
import id.payu.partner.dto.snap.TokenRequest;
import id.payu.partner.dto.snap.TokenResponse;
import id.payu.partner.repository.PartnerRepository;
import id.payu.partner.service.SnapBiPaymentService;
import id.payu.partner.service.SnapBiSignatureService;
import id.payu.partner.service.SnapBiTokenService;
import id.payu.partner.web.OpenApiConstants;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.headers.Header;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * REST resource for SNAP BI (BI-FAST) payment integration.
 * Implements the SNAP BI API specification for payment processing.
 *
 * <p>This resource handles:</p>
 * <ul>
 *   <li>OAuth2 token generation for partner authentication</li>
 *   <li>Payment creation and processing</li>
 *   <li>Payment status inquiry</li>
 *   <li>Refund processing</li>
 * </ul>
 *
 * <p>All endpoints require signature-based authentication using HMAC SHA-512.</p>
 */
@Path("/v1/partner")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = OpenApiConstants.TAG_SNAP_BI, description = "SNAP BI payment integration operations")
@ApplicationScoped
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

    /**
     * Generates an OAuth2 access token for partner authentication.
     * The token is required for all subsequent SNAP BI API calls.
     *
     * @param clientKey  The partner's client key (X-CLIENT-KEY header)
     * @param timestamp  Request timestamp in ISO 8601 format (X-TIMESTAMP header)
     * @param signature  HMAC signature for request authentication (X-SIGNATURE header)
     * @param request    Token request with grant type
     * @return Access token response with Bearer token
     */
    @POST
    @Path("/auth/token")
    @Operation(
        summary = "Get access token",
        description = "Obtains an OAuth2 access token for SNAP BI API authentication. " +
            "The access token is valid for 900 seconds (15 minutes) and must be included " +
            "in the Authorization header for all subsequent API calls. " +
            "This endpoint requires signature-based authentication using the partner's client secret."
    )
    @APIResponses(value = {
        @APIResponse(
            responseCode = "200",
            description = "Access token generated successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = TokenResponse.class)
            ),
            headers = {
                @Header(
                    name = "X-TIMESTAMP",
                    description = "Response timestamp",
                    schema = @Schema(type = SchemaType.STRING)
                ),
                @Header(
                    name = "X-SIGNATURE",
                    description = "Response signature",
                    schema = @Schema(type = SchemaType.STRING)
                )
            }
        ),
        @APIResponse(
            responseCode = "400",
            description = "Invalid request format or parameters",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = SnapErrorResponse.class)
            )
        ),
        @APIResponse(
            responseCode = "401",
            description = "Authentication failed - invalid client key or signature",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = SnapErrorResponse.class)
            )
        ),
        @APIResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = SnapErrorResponse.class)
            )
        )
    })
    public Response getAccessToken(
        @Parameter(
            description = "Partner client key obtained from partner registration",
            required = true,
            example = "client-key-123456"
        )
        @HeaderParam("X-CLIENT-KEY") String clientKey,
        @Parameter(
            description = "Request timestamp in ISO 8601 format (e.g., 2024-01-28T10:30:00+07:00)",
            required = true,
            example = "2024-01-28T10:30:00+07:00"
        )
        @HeaderParam("X-TIMESTAMP") String timestamp,
        @Parameter(
            description = "HMAC signature generated from client secret and request body",
            required = true,
            example = "signature-hmac-sha512"
        )
        @HeaderParam("X-SIGNATURE") String signature,
        @Valid
        @Schema(description = "Token request with grant type", implementation = TokenRequest.class)
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

    /**
     * Creates a new payment transaction through BI-FAST.
     *
     * @param authorization Bearer access token from token endpoint
     * @param externalId     External ID for idempotency (X-EXTERNAL-ID header)
     * @param timestamp      Request timestamp (X-TIMESTAMP header)
     * @param signature      HMAC signature (X-SIGNATURE header)
     * @param request        Payment request details
     * @return Payment response with reference number
     */
    @POST
    @Path("/payments")
    @Blocking
    @Operation(
        summary = "Create payment",
        description = "Initiates a new payment transaction through the BI-FAST network. " +
            "Supports various payment types including bank account transfer, virtual account, etc. " +
            "The payment will be processed asynchronously and a callback will be sent to " +
            "the registered webhook URL upon completion. " +
            "Use the external ID for idempotency - retrying with the same external ID " +
            "will return the same payment result."
    )
    @APIResponses(value = {
        @APIResponse(
            responseCode = "200",
            description = "Payment created/processed successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = PaymentResponse.class)
            ),
            headers = {
                @Header(
                    name = "X-TIMESTAMP",
                    description = "Response timestamp",
                    schema = @Schema(type = SchemaType.STRING)
                ),
                @Header(
                    name = "X-SIGNATURE",
                    description = "Response signature",
                    schema = @Schema(type = SchemaType.STRING)
                )
            }
        ),
        @APIResponse(
            responseCode = "400",
            description = "Invalid request parameters or validation error",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = SnapErrorResponse.class)
            )
        ),
        @APIResponse(
            responseCode = "401",
            description = "Authentication failed - invalid or expired token",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = SnapErrorResponse.class)
            )
        ),
        @APIResponse(
            responseCode = "500",
            description = "Internal server error during payment processing",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = SnapErrorResponse.class)
            )
        )
    })
    public Uni<Response> createPayment(
        @Parameter(
            description = "Bearer access token (format: Bearer {accessToken})",
            required = true,
            example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
        )
        @HeaderParam("Authorization") String authorization,
        @Parameter(
            description = "External ID for idempotency (must be unique per payment request)",
            required = true,
            example = "12345678901234567890"
        )
        @HeaderParam("X-EXTERNAL-ID") String externalId,
        @Parameter(
            description = "Request timestamp in ISO 8601 format",
            required = true,
            example = "2024-01-28T10:30:00+07:00"
        )
        @HeaderParam("X-TIMESTAMP") String timestamp,
        @Parameter(
            description = "HMAC signature generated from access token and request body",
            required = true
        )
        @HeaderParam("X-SIGNATURE") String signature,
        @Valid
        @Schema(description = "Payment request details", implementation = PaymentRequest.class)
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

    /**
     * Retrieves the status of an existing payment.
     *
     * @param authorization Bearer access token
     * @param timestamp      Request timestamp (X-TIMESTAMP header)
     * @param signature      HMAC signature (X-SIGNATURE header)
     * @param referenceNo    The payment reference number
     * @return Current payment status
     */
    @GET
    @Path("/payments/{id}")
    @Blocking
    @Operation(
        summary = "Get payment status",
        description = "Retrieves the current status of a payment transaction using " +
            "the partner reference number returned during payment creation. " +
            "Use this to check if a payment has been completed, is pending, or has failed. " +
            "The status is updated in real-time as the payment progresses through the BI-FAST network."
    )
    @APIResponses(value = {
        @APIResponse(
            responseCode = "200",
            description = "Payment status retrieved successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = PaymentStatusResponse.class)
            ),
            headers = {
                @Header(
                    name = "X-TIMESTAMP",
                    description = "Response timestamp",
                    schema = @Schema(type = SchemaType.STRING)
                ),
                @Header(
                    name = "X-SIGNATURE",
                    description = "Response signature",
                    schema = @Schema(type = SchemaType.STRING)
                )
            }
        ),
        @APIResponse(
            responseCode = "400",
            description = "Invalid request format",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = SnapErrorResponse.class)
            )
        ),
        @APIResponse(
            responseCode = "401",
            description = "Authentication failed - invalid or expired token",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = SnapErrorResponse.class)
            )
        ),
        @APIResponse(
            responseCode = "404",
            description = "Payment not found",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = SnapErrorResponse.class)
            )
        ),
        @APIResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = SnapErrorResponse.class)
            )
        )
    })
    public Uni<Response> getPaymentStatus(
        @Parameter(
            description = "Bearer access token",
            required = true,
            example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
        )
        @HeaderParam("Authorization") String authorization,
        @Parameter(
            description = "Request timestamp in ISO 8601 format",
            required = true,
            example = "2024-01-28T10:30:00+07:00"
        )
        @HeaderParam("X-TIMESTAMP") String timestamp,
        @Parameter(
            description = "HMAC signature",
            required = true
        )
        @HeaderParam("X-SIGNATURE") String signature,
        @Parameter(
            description = "Payment reference number from payment creation response",
            required = true,
            example = "PAY2024012812300012345"
        )
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

    /**
     * Processes a refund for an existing payment.
     *
     * @param authorization Bearer access token
     * @param timestamp      Request timestamp (X-TIMESTAMP header)
     * @param signature      HMAC signature (X-SIGNATURE header)
     * @param referenceNo    Original payment reference number
     * @param request        Refund request with amount and reason
     * @return Refund response with refund reference number
     */
    @POST
    @Path("/payments/{id}/refund")
    @Blocking
    @Operation(
        summary = "Create refund",
        description = "Initiates a refund for a previously completed payment. " +
            "The refund amount can be partial or full. " +
            "Refunds are processed asynchronously and a callback will be sent to " +
            "the registered webhook URL upon completion. " +
            "The original payment must be in a completed status before a refund can be processed. " +
            "Use the partnerRefundNo for idempotency - retrying with the same value " +
            "will return the same refund result."
    )
    @APIResponses(value = {
        @APIResponse(
            responseCode = "200",
            description = "Refund created/processed successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = RefundResponse.class)
            ),
            headers = {
                @Header(
                    name = "X-TIMESTAMP",
                    description = "Response timestamp",
                    schema = @Schema(type = SchemaType.STRING)
                ),
                @Header(
                    name = "X-SIGNATURE",
                    description = "Response signature",
                    schema = @Schema(type = SchemaType.STRING)
                )
            }
        ),
        @APIResponse(
            responseCode = "400",
            description = "Invalid request or refund amount exceeds original payment",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = SnapErrorResponse.class)
            )
        ),
        @APIResponse(
            responseCode = "401",
            description = "Authentication failed - invalid or expired token",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = SnapErrorResponse.class)
            )
        ),
        @APIResponse(
            responseCode = "404",
            description = "Original payment not found",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = SnapErrorResponse.class)
            )
        ),
        @APIResponse(
            responseCode = "500",
            description = "Internal server error during refund processing",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = SnapErrorResponse.class)
            )
        )
    })
    public Uni<Response> createRefund(
        @Parameter(
            description = "Bearer access token",
            required = true,
            example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
        )
        @HeaderParam("Authorization") String authorization,
        @Parameter(
            description = "Request timestamp in ISO 8601 format",
            required = true,
            example = "2024-01-28T10:30:00+07:00"
        )
        @HeaderParam("X-TIMESTAMP") String timestamp,
        @Parameter(
            description = "HMAC signature",
            required = true
        )
        @HeaderParam("X-SIGNATURE") String signature,
        @Parameter(
            description = "Original payment reference number",
            required = true,
            example = "PAY2024012812300012345"
        )
        @PathParam("id") String referenceNo,
        @Valid
        @Schema(description = "Refund request details", implementation = RefundRequest.class)
        RefundRequest request) {

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
                "/v1/partner/payments/" + referenceNo + "/refund",
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

        return paymentService.createRefund(partner.id.toString(), referenceNo, request)
            .onItem().transform(response -> Response.ok(response).build());
    }

    private String createErrorResponse(String responseCode, String responseMessage) {
        return String.format("{\"responseCode\":\"%s\",\"responseMessage\":\"%s\"}", responseCode, responseMessage);
    }

    // Schema class for SNAP BI error responses
    @Schema(name = "SnapErrorResponse", description = "SNAP BI error response format")
    private static class SnapErrorResponse {
        @Schema(description = "SNAP BI response code", example = "4012504")
        public String responseCode;

        @Schema(description = "Error response message", example = "Invalid Signature")
        public String responseMessage;
    }
}
