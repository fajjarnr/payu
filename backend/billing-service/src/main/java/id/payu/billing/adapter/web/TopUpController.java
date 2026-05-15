package id.payu.billing.adapter.web;

import id.payu.api.common.response.ApiResponse;
import id.payu.billing.adapter.persistence.entity.BillPaymentEntity;
import id.payu.billing.dto.TopUpRequest;
import id.payu.billing.dto.TopUpResponse;
import id.payu.billing.application.service.PaymentService;
import id.payu.commons.idempotency.Idempotent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import id.payu.billing.exception.TopUpNotFoundException;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;
import id.payu.security.annotation.AuditOperation;

/**
 * REST Controller for e-wallet top-ups.
 */
@RestController
@RequestMapping("/api/v1/topup")
@RequiredArgsConstructor
@Tag(name = "E-Wallet Top-Up", description = "E-wallet top-up APIs for GoPay, OVO, DANA, LinkAja")
@SecurityRequirement(name = "bearerAuth")
public class TopUpController {

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
     * BUG-SECURITY-002 FIX: Validate that the authenticated user owns the top-up.
     */
    private void validateOwnership(BillPaymentEntity payment) {
        String userId = extractUserId();
        if (userId != null && payment.getAccountId() != null
                && !payment.getAccountId().equals(userId)) {
            throw new TopUpNotFoundException("Top-up not found");
        }
    }

    @PostMapping
    @Audited(
            operation = id.payu.security.annotation.AuditOperation.TRANSFER,
            entityType = "TopUp",
            maskData = true,
            level = AuditLevel.INFO
    )
    @Idempotent(required = true)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create e-wallet top-up", description = "Process an e-wallet top-up for supported providers")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Top-up created successfully",
            content = @Content(schema = @Schema(implementation = TopUpResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request - validation error or invalid provider")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - valid JWT token required")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    public ApiResponse<TopUpResponse> createTopUp(
            @Parameter(description = "Top-up request details", required = true)
            @Valid @RequestBody TopUpRequest request) {
        // BUG-SECURITY-014 FIX: Prevent balance theft by ensuring authenticated user matches accountId
        String authenticatedUserId = extractUserId();
        if (authenticatedUserId != null && !authenticatedUserId.equals(request.accountId())) {
            throw new TopUpNotFoundException("Unauthorized top-up attempt: account ownership mismatch");
        }

        BillPaymentEntity payment = paymentService.createTopUp(request);
        return ApiResponse.success(TopUpResponse.from(payment));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get top-up by ID", description = "Retrieve top-up details using transaction ID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Top-up found",
            content = @Content(schema = @Schema(implementation = TopUpResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Top-up not found")
    public ApiResponse<TopUpResponse> getTopUp(
            @Parameter(description = "Top-up ID", required = true)
            @PathVariable UUID id) {
        return paymentService.getPayment(id)
                .map(payment -> {
                    // BUG-SECURITY-002 FIX: Validate authenticated user owns this top-up
                    validateOwnership(payment);
                    return ApiResponse.success(TopUpResponse.from(payment));
                })
                .orElseThrow(() -> new TopUpNotFoundException("Top-up not found"));
    }

    @GetMapping("/reference/{referenceNumber}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get top-up by reference number", description = "Retrieve top-up details using the reference number")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Top-up found",
            content = @Content(schema = @Schema(implementation = TopUpResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Top-up not found")
    public ApiResponse<TopUpResponse> getTopUpByReference(
            @Parameter(description = "Reference number", required = true)
            @PathVariable String referenceNumber) {
        return paymentService.getPaymentByReference(referenceNumber)
                .map(payment -> {
                    // BUG-SECURITY-002 FIX: Validate authenticated user owns this top-up
                    validateOwnership(payment);
                    return ApiResponse.success(TopUpResponse.from(payment));
                })
                .orElseThrow(() -> new TopUpNotFoundException("Top-up not found"));
    }

    @GetMapping("/providers")
    @Operation(summary = "List e-wallet providers", description = "Retrieve list of supported e-wallet providers")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Providers retrieved successfully")
    public ApiResponse<List<ProviderInfo>> getProviders() {
        List<ProviderInfo> providers = List.of(
                new ProviderInfo("GOPAY", "GoPay"),
                new ProviderInfo("OVO", "OVO"),
                new ProviderInfo("DANA", "DANA"),
                new ProviderInfo("LINKAJA", "LinkAja")
        );
        return ApiResponse.success(providers);
    }

    /**
     * Provider info record.
     */
    public record ProviderInfo(String code, String name) {}
}
