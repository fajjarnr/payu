package id.payu.partner.adapter.web;

import id.payu.commons.idempotency.Idempotent;
import id.payu.partner.application.service.MerchantService;
import id.payu.partner.dto.CreateMerchantRequest;
import id.payu.partner.dto.CreateQrPaymentRequest;
import id.payu.partner.dto.MerchantResponse;
import id.payu.partner.dto.QrPaymentResponse;
import id.payu.security.annotation.Audited;
import id.payu.security.annotation.Audited.AuditLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for merchant management and dynamic QR generation.
 */
@RestController
@RequestMapping("/merchants")
@Tag(name = "Merchants", description = "Merchant onboarding and dynamic QR payment operations")
@PreAuthorize("hasRole('ADMIN')")
public class MerchantController extends BaseController {

    private final MerchantService merchantService;

    public MerchantController(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    @PostMapping("/partners/{partnerId}")
    @Operation(summary = "Onboard a new merchant under a partner")
    @SecurityRequirement(name = OpenApiConstants.SECURITY_SCHEME_BEARER)
    @Audited(operation = Audited.Operation.CREATE, entityType = "Merchant", level = AuditLevel.INFO)
    @Idempotent(required = true)
    public ResponseEntity<ApiResponse<MerchantResponse>> createMerchant(
            @PathVariable Long partnerId,
            @Valid @RequestBody CreateMerchantRequest request) {
        MerchantResponse response = merchantService.createMerchant(partnerId, request);
        return created(response);
    }

    @GetMapping("/{merchantId}")
    @Operation(summary = "Get merchant details")
    @SecurityRequirement(name = OpenApiConstants.SECURITY_SCHEME_BEARER)
    public ResponseEntity<ApiResponse<MerchantResponse>> getMerchant(@PathVariable Long merchantId) {
        return ok(merchantService.getMerchant(merchantId));
    }

    @GetMapping("/partners/{partnerId}")
    @Operation(summary = "List merchants for a partner")
    @SecurityRequirement(name = OpenApiConstants.SECURITY_SCHEME_BEARER)
    public ResponseEntity<ApiResponse<Page<MerchantResponse>>> listByPartner(
            @PathVariable Long partnerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<MerchantResponse> merchants = merchantService.listByPartner(
                partnerId, PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ok(merchants);
    }

    @PostMapping("/{merchantId}/activate")
    @Operation(summary = "Activate a pending merchant")
    @SecurityRequirement(name = OpenApiConstants.SECURITY_SCHEME_BEARER)
    @Audited(operation = Audited.Operation.UPDATE, entityType = "Merchant", level = AuditLevel.INFO)
    public ResponseEntity<ApiResponse<MerchantResponse>> activate(@PathVariable Long merchantId) {
        return ok(merchantService.activateMerchant(merchantId));
    }

    @PostMapping("/{merchantId}/qr")
    @Operation(summary = "Generate a dynamic QR code for payment")
    @SecurityRequirement(name = OpenApiConstants.SECURITY_SCHEME_BEARER)
    @Audited(operation = Audited.Operation.CREATE, entityType = "MerchantQrPayment", level = AuditLevel.INFO)
    @Idempotent(required = true)
    public ResponseEntity<ApiResponse<QrPaymentResponse>> generateQr(
            @PathVariable Long merchantId,
            @Valid @RequestBody CreateQrPaymentRequest request) {
        QrPaymentResponse response = merchantService.generateDynamicQr(merchantId, request);
        return created(response);
    }

    @GetMapping("/qr/{referenceId}")
    @Operation(summary = "Get QR payment status by reference ID")
    @SecurityRequirement(name = OpenApiConstants.SECURITY_SCHEME_BEARER)
    public ResponseEntity<ApiResponse<QrPaymentResponse>> getQrPayment(@PathVariable String referenceId) {
        return ok(merchantService.getQrPayment(referenceId));
    }

    @PostMapping("/qr/{referenceId}/pay")
    @Operation(summary = "Confirm QR payment (simulates payer scan & pay)")
    @SecurityRequirement(name = OpenApiConstants.SECURITY_SCHEME_BEARER)
    @PreAuthorize("isAuthenticated()") // Payer-facing: any authenticated user, not just ADMIN
    @Audited(operation = Audited.Operation.UPDATE, entityType = "MerchantQrPayment", level = AuditLevel.INFO)
    @Idempotent(required = true)
    public ResponseEntity<ApiResponse<QrPaymentResponse>> confirmQrPayment(
            @PathVariable String referenceId,
            @RequestParam String payerAccountId) {
        // BUG-BE-185 FIX: Verify the payerAccountId belongs to the authenticated user.
        // Extract authenticated user's account ID from JWT and compare.
        Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String authenticatedAccountId = jwt.getClaimAsString("account_id");
        if (authenticatedAccountId == null) {
            authenticatedAccountId = jwt.getSubject();
        }
        if (!authenticatedAccountId.equals(payerAccountId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Cannot initiate QR payment for another user's account");
        }
        return ok(merchantService.confirmQrPayment(referenceId, payerAccountId));
    }
}
