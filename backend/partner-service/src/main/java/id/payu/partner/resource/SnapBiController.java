package id.payu.partner.resource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.partner.domain.Partner;
import id.payu.partner.dto.snap.*;
import id.payu.partner.repository.PartnerRepository;
import id.payu.partner.service.SnapBiPaymentService;
import id.payu.partner.service.SnapBiSignatureService;
import id.payu.partner.service.SnapBiTokenService;
import id.payu.partner.web.OpenApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for SNAP BI (BI-FAST) payment integration.
 * Implements the SNAP BI API specification for payment processing.
 */
@RestController
@RequestMapping("/v1/partner")
@Tag(name = OpenApiConstants.TAG_SNAP_BI, description = "SNAP BI payment integration operations")
@RequiredArgsConstructor
public class SnapBiController {

    private final PartnerRepository partnerRepository;
    private final SnapBiSignatureService signatureService;
    private final SnapBiTokenService tokenService;
    private final SnapBiPaymentService paymentService;
    private final ObjectMapper objectMapper;

    @PostMapping("/auth/token")
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
        @Parameter(description = "Partner client key", required = true) @RequestHeader("X-CLIENT-KEY") String clientKey,
        @Parameter(description = "Request timestamp", required = true) @RequestHeader("X-TIMESTAMP") String timestamp,
        @Parameter(description = "HMAC signature", required = true) @RequestHeader("X-SIGNATURE") String signature,
        @Valid @RequestBody TokenRequest request) {

        Partner partner = partnerRepository.findByClientId(clientKey).orElse(null);
        if (partner == null) {
            return errorResponse(HttpStatus.UNAUTHORIZED, "4012502", "Invalid Client Key");
        }

        if (!partner.isActive()) {
             return errorResponse(HttpStatus.UNAUTHORIZED, "4012503", "Partner is inactive");
        }

        try {
            String requestBody = objectMapper.writeValueAsString(request);
            boolean signatureValid = signatureService.validateSignatureWithClientKey(
                partner.getClientSecret(),
                "POST",
                "/v1/partner/auth/token",
                timestamp,
                requestBody,
                signature
            );

            if (!signatureValid) {
                return errorResponse(HttpStatus.UNAUTHORIZED, "4012504", "Invalid Signature");
            }
        } catch (Exception e) {
            return errorResponse(HttpStatus.BAD_REQUEST, "4002501", "Invalid Request Body");
        }

        String accessToken = tokenService.generateAccessToken(
            partner.getClientId(),
            partner.getId().toString(),
            partner.getName()
        );

        return ResponseEntity.ok(new TokenResponse(accessToken, "Bearer", "900"));
    }

    @PostMapping("/payments")
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
        @Valid @RequestBody PaymentRequest request) {

        if (authorization == null || !authorization.startsWith("Bearer ")) {
             return errorResponse(HttpStatus.UNAUTHORIZED, "4012505", "Missing or Invalid Authorization Header");
        }

        String token = authorization.substring(7);
        String clientId = tokenService.getClientIdFromToken(token);

        if (clientId == null) {
            return errorResponse(HttpStatus.UNAUTHORIZED, "4012506", "Invalid or Expired Token");
        }

        Partner partner = partnerRepository.findByClientId(clientId).orElse(null);
        if (partner == null || !partner.isActive()) {
            return errorResponse(HttpStatus.UNAUTHORIZED, "4012507", "Partner not found or inactive");
        }

        try {
            String requestBody = objectMapper.writeValueAsString(request);
            boolean signatureValid = signatureService.validateSignature(
                partner.getClientSecret(),
                "POST",
                "/v1/partner/payments",
                token,
                requestBody,
                timestamp,
                signature
            );

            if (!signatureValid) {
                return errorResponse(HttpStatus.UNAUTHORIZED, "4012504", "Invalid Signature");
            }
        } catch (Exception e) {
            return errorResponse(HttpStatus.BAD_REQUEST, "4002501", "Invalid Request Body");
        }

        return ResponseEntity.ok(paymentService.createPayment(partner.getId().toString(), request));
    }

    @GetMapping("/payments/{id}")
    @Operation(summary = "Get payment status")
    public ResponseEntity<?> getPaymentStatus(
        @RequestHeader("Authorization") String authorization,
        @RequestHeader("X-TIMESTAMP") String timestamp,
        @RequestHeader("X-SIGNATURE") String signature,
        @PathVariable("id") String referenceNo) {

        if (authorization == null || !authorization.startsWith("Bearer ")) {
             return errorResponse(HttpStatus.UNAUTHORIZED, "4012505", "Missing or Invalid Authorization Header");
        }

        String token = authorization.substring(7);
        String clientId = tokenService.getClientIdFromToken(token);

        if (clientId == null) {
             return errorResponse(HttpStatus.UNAUTHORIZED, "4012506", "Invalid or Expired Token");
        }

        Partner partner = partnerRepository.findByClientId(clientId).orElse(null);
        if (partner == null || !partner.isActive()) {
            return errorResponse(HttpStatus.UNAUTHORIZED, "4012507", "Partner not found or inactive");
        }

        try {
            String requestBody = "";
            boolean signatureValid = signatureService.validateSignature(
                partner.getClientSecret(),
                "GET",
                "/v1/partner/payments/" + referenceNo,
                token,
                requestBody,
                timestamp,
                signature
            );

            if (!signatureValid) {
                return errorResponse(HttpStatus.UNAUTHORIZED, "4012504", "Invalid Signature");
            }
        } catch (Exception e) {
            return errorResponse(HttpStatus.BAD_REQUEST, "4002501", "Invalid Request");
        }

        return ResponseEntity.ok(paymentService.getPaymentStatus(partner.getId().toString(), referenceNo));
    }

    @PostMapping("/payments/{id}/refund")
    @Operation(summary = "Create refund")
    public ResponseEntity<?> createRefund(
        @RequestHeader("Authorization") String authorization,
        @RequestHeader("X-TIMESTAMP") String timestamp,
        @RequestHeader("X-SIGNATURE") String signature,
        @PathVariable("id") String referenceNo,
        @Valid @RequestBody RefundRequest request) {

        if (authorization == null || !authorization.startsWith("Bearer ")) {
             return errorResponse(HttpStatus.UNAUTHORIZED, "4012505", "Missing or Invalid Authorization Header");
        }

        String token = authorization.substring(7);
        String clientId = tokenService.getClientIdFromToken(token);

        if (clientId == null) {
            return errorResponse(HttpStatus.UNAUTHORIZED, "4012506", "Invalid or Expired Token");
        }

        Partner partner = partnerRepository.findByClientId(clientId).orElse(null);
        if (partner == null || !partner.isActive()) {
            return errorResponse(HttpStatus.UNAUTHORIZED, "4012507", "Partner not found or inactive");
        }

        try {
            String requestBody = objectMapper.writeValueAsString(request);
            boolean signatureValid = signatureService.validateSignature(
                partner.getClientSecret(),
                "POST",
                "/v1/partner/payments/" + referenceNo + "/refund",
                token,
                requestBody,
                timestamp,
                signature
            );

            if (!signatureValid) {
                return errorResponse(HttpStatus.UNAUTHORIZED, "4012504", "Invalid Signature");
            }
        } catch (Exception e) {
            return errorResponse(HttpStatus.BAD_REQUEST, "4002501", "Invalid Request Body");
        }

        return ResponseEntity.ok(paymentService.createRefund(partner.getId().toString(), referenceNo, request));
    }

    private ResponseEntity<SnapErrorResponse> errorResponse(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(new SnapErrorResponse(code, message));
    }

    @Schema(name = "SnapErrorResponse")
    @lombok.Data
    @lombok.AllArgsConstructor
    private static class SnapErrorResponse {
        public String responseCode;
        public String responseMessage;
    }
}
