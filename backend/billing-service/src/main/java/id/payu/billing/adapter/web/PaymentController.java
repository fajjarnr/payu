package id.payu.billing.adapter.web;

import id.payu.api.common.response.ApiResponse;
import id.payu.billing.domain.model.BillPayment;
import id.payu.billing.domain.port.in.PaymentQueryUseCase;
import id.payu.billing.interfaces.dto.CreatePaymentRequest;
import id.payu.billing.interfaces.dto.PaymentResponse;
import id.payu.billing.application.service.PaymentService;
import id.payu.commons.idempotency.Idempotent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import id.payu.billing.exception.PaymentNotFoundException;
import id.payu.security.annotation.Audited;
import id.payu.security.annotation.AuditLevel;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import id.payu.security.annotation.AuditOperation;

/**
 * REST Controller for bill payments.
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Bill Payments", description = "Bill payment APIs for utilities and recurring bills")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Extract authenticated user ID from JWT token.
     */
    private String extractUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            // BUG-AUTH-013: Use 'account_id' claim with 'sub' fallback
            return jwt.getClaimAsString("account_id") != null ? jwt.getClaimAsString("account_id") : jwt.getSubject();
        }
        return null;
    }

    /**
     * BUG-BE-159 FIX: Validate that the authenticated user owns the payment.
     * Prevents unauthorized access to other users' payment details.
     */
    private void validateOwnership(BillPayment payment) {
        String userId = extractUserId();
        if (userId != null && payment.getAccountId() != null
                && !Objects.equals(payment.getAccountId(), userId)) {
            throw new PaymentNotFoundException("Payment not found");
        }
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get payment history", description = "Retrieve paginated bill payment history for the authenticated user's account")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payment history retrieved")
    public ApiResponse<Map<String, Object>> getPaymentHistory(
            @Parameter(description = "Page index (zero-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        String userId = extractUserId();
        if (userId == null) {
            throw new PaymentNotFoundException("Payment not found");
        }
        PaymentQueryUseCase.PaymentPage history = paymentService.getPaymentHistory(userId, page, size);
        return ApiResponse.success(Map.of(
                "content", history.content().stream().map(PaymentResponse::from).toList(),
                "totalElements", history.totalElements()));
    }

    @PostMapping
    @Audited(
            operation = id.payu.security.annotation.AuditOperation.TRANSFER,
            entityType = "BillPaymentEntity",
            maskData = true,
            level = AuditLevel.INFO
    )
    @Idempotent(required = true, headerName = "X-Idempotency-Key")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create bill payment", description = "Process a bill payment for utilities like PLN, PDAM, BPJS, etc.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Payment created successfully",
            content = @Content(schema = @Schema(implementation = PaymentResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request - validation error or insufficient balance")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - valid JWT token required")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    public ApiResponse<PaymentResponse> createPayment(
            @Parameter(description = "Payment request details", required = true)
            @Valid @RequestBody CreatePaymentRequest request) {
        // BUG-SECURITY-013 FIX: Prevent balance theft by ensuring authenticated user matches accountId
        String authenticatedUserId = extractUserId();
        if (authenticatedUserId != null && !authenticatedUserId.equals(request.accountId())) {
            throw new PaymentNotFoundException("Unauthorized payment attempt: account ownership mismatch");
        }

        BillPayment payment = paymentService.createPayment(request, extractIdempotencyKey());
        return ApiResponse.success(PaymentResponse.from(payment));
    }

    private String extractIdempotencyKey() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            String key = attributes.getRequest().getHeader("X-Idempotency-Key");
            if (key == null) {
                key = attributes.getRequest().getHeader("Idempotency-Key");
            }
            if (key != null && !key.isBlank()) {
                return key;
            }
        }
        // BE-BILL-002: never silently mint a key — the caller violated the
        // idempotency contract and must be told (400), not hidden.
        throw new IllegalArgumentException("X-Idempotency-Key header is required");
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get payment by ID", description = "Retrieve payment details using payment ID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payment found",
            content = @Content(schema = @Schema(implementation = PaymentResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Payment not found")
    public ApiResponse<PaymentResponse> getPayment(
            @Parameter(description = "Payment ID", required = true)
            @PathVariable UUID id) {
        return paymentService.getPayment(id)
                .map(payment -> {
                    // BUG-BE-159 FIX: Validate authenticated user owns this payment
                    validateOwnership(payment);
                    return ApiResponse.success(PaymentResponse.from(payment));
                })
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found"));
    }

    @GetMapping("/reference/{referenceNumber}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get payment by reference number", description = "Retrieve payment details using the reference number")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payment found",
            content = @Content(schema = @Schema(implementation = PaymentResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Payment not found")
    public ApiResponse<PaymentResponse> getPaymentByReference(
            @Parameter(description = "Reference number", required = true)
            @PathVariable String referenceNumber) {
        return paymentService.getPaymentByReference(referenceNumber)
                .map(payment -> {
                    // BUG-BE-159 FIX: Validate authenticated user owns this payment
                    validateOwnership(payment);
                    return ApiResponse.success(PaymentResponse.from(payment));
                })
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found"));
    }
}
