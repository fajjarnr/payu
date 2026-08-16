package id.payu.partner.adapter.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.commons.idempotency.Idempotent;
import id.payu.partner.adapter.persistence.entity.PartnerEntity;
import id.payu.partner.dto.snap.*;
import id.payu.partner.application.service.PartnerService;
import id.payu.partner.application.service.SnapBiPaymentService;
import id.payu.partner.application.service.SnapBiSignatureService;
import id.payu.partner.application.service.SnapBiTokenService;
import id.payu.partner.adapter.web.OpenApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.time.Duration;
import java.time.format.DateTimeParseException;

/**
 * REST controller for SNAP BI (BI-FAST) payment integration.
 * Implements the SNAP BI API specification for payment processing.
 *
 * <p>SNAP-PATH-001: endpoints are exposed under BOTH the SNAP-BI v1.0
 * taxonomy ({@code /v1.0/access-token/b2b}, {@code /v1.0/transfer-va/payment},
 * {@code /v1.0/transfer-va/refund}) and the legacy PayU paths
 * ({@code /v1/partner/...}) so existing integrators keep working while new
 * ones use the standard. Signature validation always uses the actual request
 * path, matching what the caller signed.
 *
 * BUG-BE-138 FIX: Uses PartnerService instead of direct PartnerRepository access.
 * BUG-BE-139 FIX: Uses raw request body for signature validation instead of re-serialization.
 */
@RestController
@Tag(name = OpenApiConstants.TAG_SNAP_BI, description = "SNAP BI payment integration operations")
@RequiredArgsConstructor
public class SnapBiController {

    private static final Logger log = LoggerFactory.getLogger(SnapBiController.class);

    // BUG-BE-138 FIX: Use service instead of repository
    private final PartnerService partnerService;
    private final SnapBiSignatureService signatureService;
    private final SnapBiTokenService tokenService;
    private final SnapBiPaymentService paymentService;
    private final ObjectMapper objectMapper;

    /**
     * BUG-BE-134: Validates that the X-TIMESTAMP header is within ±5 minute window.
     * Prevents replay attacks by rejecting stale or future timestamps.
     */
    private boolean isTimestampValid(String timestamp) {
        try {
            OffsetDateTime requestTime = OffsetDateTime.parse(timestamp);
            OffsetDateTime now = OffsetDateTime.now();
            Duration diff = Duration.between(requestTime, now).abs();
            return diff.toMinutes() <= 5;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /**
     * Returns the request URI path (without context path) as the endpoint the
     * caller signed. SNAP-BI signatures bind the endpoint path, so the value
     * validated must be the actual path hit — both the standard {@code /v1.0/...}
     * taxonomy and the legacy {@code /v1/partner/...} aliases work (SNAP-PATH-001).
     */
    private String signedEndpoint(HttpServletRequest servletRequest) {
        String uri = servletRequest.getRequestURI();
        String context = servletRequest.getContextPath();
        return (context != null && !context.isEmpty() && uri.startsWith(context))
                ? uri.substring(context.length())
                : uri;
    }

    @PostMapping(value = {"/v1/partner/auth/token", "/v1.0/access-token/b2b"})
    @Operation(
        summary = "Get access token",
        description = "Obtains an OAuth2 access token for SNAP BI API authentication."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Access token generated successfully",
            content = @Content(schema = @Schema(implementation = TokenResponse.class)),
            headers = {
                @Header(name = "X-TIMESTAMP", description = "Response timestamp", schema = @Schema(type = "string")),
                @Header(name = "X-SIGNATURE", description = "Response signature", schema = @Schema(type = "string"))
            }
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request format", content = @Content(schema = @Schema(implementation = SnapErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content(schema = @Schema(implementation = SnapErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = SnapErrorResponse.class)))
    })
    public ResponseEntity<?> getAccessToken(
        @Parameter(description = "PartnerEntity client key", required = true) @RequestHeader("X-CLIENT-KEY") String clientKey,
        @Parameter(description = "Request timestamp", required = true) @RequestHeader("X-TIMESTAMP") String timestamp,
        @Parameter(description = "HMAC signature", required = true) @RequestHeader("X-SIGNATURE") String signature,
        @Valid @RequestBody String rawBody,
        HttpServletRequest servletRequest) {

        // BUG-BE-134: Validate timestamp window to prevent replay attacks
        if (!isTimestampValid(timestamp)) {
            return errorResponse(HttpStatus.BAD_REQUEST, "4002508", "Invalid or expired timestamp");
        }

        // BUG-BE-138: Use service layer instead of repository
        PartnerEntity partner = partnerService.findByClientId(clientKey).orElse(null);
        if (partner == null) {
            return errorResponse(HttpStatus.UNAUTHORIZED, "4012502", "Invalid Client Key");
        }

        if (!partner.isActive()) {
             return errorResponse(HttpStatus.UNAUTHORIZED, "4012503", "PartnerEntity is inactive");
        }

        try {
            // BUG-BE-139 FIX: Use raw request body for signature validation
            // instead of re-serialized JSON (which may change field ordering/whitespace)
            boolean signatureValid = signatureService.validateSignatureWithClientKey(
                partner.getClientSecret(),
                "POST",
                signedEndpoint(servletRequest),
                timestamp,
                rawBody,
                signature
            );

            if (!signatureValid) {
                return errorResponse(HttpStatus.UNAUTHORIZED, "4012504", "Invalid Signature");
            }
        } catch (Exception e) {
            log.error("Signature validation failed for token request", e);
            return errorResponse(HttpStatus.BAD_REQUEST, "4002501", "Invalid Request Body");
        }

        // Parse raw body to extract grant_type if needed
        TokenRequest request;
        try {
            request = objectMapper.readValue(rawBody, TokenRequest.class);
        } catch (Exception e) {
            log.error("Failed to parse token request body", e);
            return errorResponse(HttpStatus.BAD_REQUEST, "4002501", "Invalid Request Body");
        }

        String accessToken = tokenService.generateAccessToken(
            partner.getClientId(),
            partner.getId().toString(),
            partner.getName()
        );

        return ResponseEntity.ok(new TokenResponse(accessToken, "Bearer", "900"));
    }

    @PostMapping(value = {"/v1/partner/payments", "/v1.0/transfer-va/payment"})
    @Idempotent(required = true)
    @Operation(
        summary = "Create payment",
        description = "Initiates a new payment transaction through the BI-FAST network."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Payment created/processed successfully",
            content = @Content(schema = @Schema(implementation = PaymentResponse.class))
        ),
        @ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content(schema = @Schema(implementation = SnapErrorResponse.class)))
    })
    public ResponseEntity<?> createPayment(
        @RequestHeader("Authorization") String authorization,
        @RequestHeader("X-EXTERNAL-ID") String externalId,
        @RequestHeader("X-TIMESTAMP") String timestamp,
        @RequestHeader("X-SIGNATURE") String signature,
        @RequestBody String rawBody,
        HttpServletRequest servletRequest) {

        // BUG-BE-134: Validate timestamp window to prevent replay attacks
        if (!isTimestampValid(timestamp)) {
            return errorResponse(HttpStatus.BAD_REQUEST, "4002508", "Invalid or expired timestamp");
        }

        if (authorization == null || !authorization.startsWith("Bearer ")) {
             return errorResponse(HttpStatus.UNAUTHORIZED, "4012505", "Missing or Invalid Authorization Header");
        }

        String token = authorization.substring(7);
        String clientId = tokenService.getClientIdFromToken(token);

        if (clientId == null) {
            return errorResponse(HttpStatus.UNAUTHORIZED, "4012506", "Invalid or Expired Token");
        }

        // BUG-BE-138: Use service layer
        PartnerEntity partner = partnerService.findByClientId(clientId).orElse(null);
        if (partner == null || !partner.isActive()) {
            return errorResponse(HttpStatus.UNAUTHORIZED, "4012507", "PartnerEntity not found or inactive");
        }

        try {
            // BUG-BE-139 FIX: Use raw body for signature validation
            boolean signatureValid = signatureService.validateSignature(
                partner.getClientSecret(),
                "POST",
                signedEndpoint(servletRequest),
                token,
                rawBody,
                timestamp,
                signature
            );

            if (!signatureValid) {
                return errorResponse(HttpStatus.UNAUTHORIZED, "4012504", "Invalid Signature");
            }
        } catch (Exception e) {
            log.error("Signature validation failed for payment request", e);
            return errorResponse(HttpStatus.BAD_REQUEST, "4002501", "Invalid Request Body");
        }

        // Parse raw body to PaymentRequest
        PaymentRequest request;
        try {
            request = objectMapper.readValue(rawBody, PaymentRequest.class);
        } catch (Exception e) {
            log.error("Failed to parse payment request body", e);
            return errorResponse(HttpStatus.BAD_REQUEST, "4002501", "Invalid Request Body");
        }

        return ResponseEntity.ok(paymentService.createPayment(partner.getId().toString(), request));
    }

    @GetMapping(value = {"/v1/partner/payments/{id}", "/v1.0/transfer-va/payment/{id}"})
    @Operation(summary = "Get payment status")
    public ResponseEntity<?> getPaymentStatus(
        @RequestHeader("Authorization") String authorization,
        @RequestHeader("X-TIMESTAMP") String timestamp,
        @RequestHeader("X-SIGNATURE") String signature,
        @PathVariable("id") String referenceNo,
        HttpServletRequest servletRequest) {

        // BUG-BE-134: Validate timestamp window
        if (!isTimestampValid(timestamp)) {
            return errorResponse(HttpStatus.BAD_REQUEST, "4002508", "Invalid or expired timestamp");
        }

        if (authorization == null || !authorization.startsWith("Bearer ")) {
             return errorResponse(HttpStatus.UNAUTHORIZED, "4012505", "Missing or Invalid Authorization Header");
        }

        String token = authorization.substring(7);
        String clientId = tokenService.getClientIdFromToken(token);

        if (clientId == null) {
             return errorResponse(HttpStatus.UNAUTHORIZED, "4012506", "Invalid or Expired Token");
        }

        // BUG-BE-138: Use service layer
        PartnerEntity partner = partnerService.findByClientId(clientId).orElse(null);
        if (partner == null || !partner.isActive()) {
            return errorResponse(HttpStatus.UNAUTHORIZED, "4012507", "PartnerEntity not found or inactive");
        }

        try {
            String requestBody = "";
            boolean signatureValid = signatureService.validateSignature(
                partner.getClientSecret(),
                "GET",
                signedEndpoint(servletRequest),
                token,
                requestBody,
                timestamp,
                signature
            );

            if (!signatureValid) {
                return errorResponse(HttpStatus.UNAUTHORIZED, "4012504", "Invalid Signature");
            }
        } catch (Exception e) {
            log.error("Signature validation failed for payment status request", e);
            return errorResponse(HttpStatus.BAD_REQUEST, "4002501", "Invalid Request");
        }

        return ResponseEntity.ok(paymentService.getPaymentStatus(partner.getId().toString(), referenceNo));
    }

    @PostMapping("/v1/partner/payments/{id}/refund")
    @Idempotent(required = true)
    @Operation(summary = "Create refund")
    public ResponseEntity<?> createRefund(
        @RequestHeader("Authorization") String authorization,
        @RequestHeader("X-TIMESTAMP") String timestamp,
        @RequestHeader("X-SIGNATURE") String signature,
        @PathVariable("id") String referenceNo,
        @RequestBody String rawBody,
        HttpServletRequest servletRequest) {

        // BUG-BE-134: Validate timestamp window
        if (!isTimestampValid(timestamp)) {
            return errorResponse(HttpStatus.BAD_REQUEST, "4002508", "Invalid or expired timestamp");
        }

        if (authorization == null || !authorization.startsWith("Bearer ")) {
             return errorResponse(HttpStatus.UNAUTHORIZED, "4012505", "Missing or Invalid Authorization Header");
        }

        String token = authorization.substring(7);
        String clientId = tokenService.getClientIdFromToken(token);

        if (clientId == null) {
            return errorResponse(HttpStatus.UNAUTHORIZED, "4012506", "Invalid or Expired Token");
        }

        // BUG-BE-138: Use service layer
        PartnerEntity partner = partnerService.findByClientId(clientId).orElse(null);
        if (partner == null || !partner.isActive()) {
            return errorResponse(HttpStatus.UNAUTHORIZED, "4012507", "PartnerEntity not found or inactive");
        }

        try {
            // BUG-BE-139 FIX: Use raw body for signature validation
            boolean signatureValid = signatureService.validateSignature(
                partner.getClientSecret(),
                "POST",
                signedEndpoint(servletRequest),
                token,
                rawBody,
                timestamp,
                signature
            );

            if (!signatureValid) {
                return errorResponse(HttpStatus.UNAUTHORIZED, "4012504", "Invalid Signature");
            }
        } catch (Exception e) {
            log.error("Signature validation failed for refund request", e);
            return errorResponse(HttpStatus.BAD_REQUEST, "4002501", "Invalid Request Body");
        }

        // Parse raw body to RefundRequest
        RefundRequest request;
        try {
            request = objectMapper.readValue(rawBody, RefundRequest.class);
        } catch (Exception e) {
            log.error("Failed to parse refund request body", e);
            return errorResponse(HttpStatus.BAD_REQUEST, "4002501", "Invalid Request Body");
        }

        return ResponseEntity.ok(paymentService.createRefund(partner.getId().toString(), referenceNo, request));
    }

    /**
     * SNAP-BI v1.0 refund: {@code POST /v1.0/transfer-va/refund}. The original
     * payment reference is carried in the body ({@code originalReferenceNo}),
     * unlike the legacy path-scoped {@code /v1/partner/payments/{id}/refund}.
     */
    @PostMapping("/v1.0/transfer-va/refund")
    @Idempotent(required = true)
    @Operation(summary = "Create refund (SNAP-BI v1.0)")
    public ResponseEntity<?> createRefundV10(
        @RequestHeader("Authorization") String authorization,
        @RequestHeader("X-TIMESTAMP") String timestamp,
        @RequestHeader("X-SIGNATURE") String signature,
        @RequestBody String rawBody,
        HttpServletRequest servletRequest) {

        if (!isTimestampValid(timestamp)) {
            return errorResponse(HttpStatus.BAD_REQUEST, "4002508", "Invalid or expired timestamp");
        }
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return errorResponse(HttpStatus.UNAUTHORIZED, "4012505", "Missing or Invalid Authorization Header");
        }

        String token = authorization.substring(7);
        String clientId = tokenService.getClientIdFromToken(token);
        if (clientId == null) {
            return errorResponse(HttpStatus.UNAUTHORIZED, "4012506", "Invalid or Expired Token");
        }

        PartnerEntity partner = partnerService.findByClientId(clientId).orElse(null);
        if (partner == null || !partner.isActive()) {
            return errorResponse(HttpStatus.UNAUTHORIZED, "4012507", "PartnerEntity not found or inactive");
        }

        try {
            boolean signatureValid = signatureService.validateSignature(
                partner.getClientSecret(),
                "POST",
                signedEndpoint(servletRequest),
                token,
                rawBody,
                timestamp,
                signature
            );
            if (!signatureValid) {
                return errorResponse(HttpStatus.UNAUTHORIZED, "4012504", "Invalid Signature");
            }
        } catch (Exception e) {
            log.error("Signature validation failed for refund request", e);
            return errorResponse(HttpStatus.BAD_REQUEST, "4002501", "Invalid Request Body");
        }

        RefundRequestV10 request;
        try {
            request = objectMapper.readValue(rawBody, RefundRequestV10.class);
        } catch (Exception e) {
            log.error("Failed to parse refund request body", e);
            return errorResponse(HttpStatus.BAD_REQUEST, "4002501", "Invalid Request Body");
        }
        if (request.originalReferenceNo == null || request.originalReferenceNo.isBlank()) {
            return errorResponse(HttpStatus.BAD_REQUEST, "4002501", "originalReferenceNo is required");
        }

        return ResponseEntity.ok(paymentService.createRefund(
                partner.getId().toString(), request.originalReferenceNo, request.toLegacy()));
    }

    private ResponseEntity<SnapErrorResponse> errorResponse(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(new SnapErrorResponse(code, message));
    }

    // BUG-BE-146: SnapErrorResponse extracted to id.payu.partner.dto.snap.SnapErrorResponse
}
