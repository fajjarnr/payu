package id.payu.partner.adapter.web;

import id.payu.commons.idempotency.Idempotent;
import id.payu.partner.application.service.PaymentLinkService;
import id.payu.partner.dto.CreatePaymentLinkRequest;
import id.payu.partner.dto.PaymentLinkResponse;
import id.payu.security.annotation.Audited;
import id.payu.security.annotation.Audited.AuditLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing payment links / invoices.
 * Partners generate shareable payment URLs.
 */
@RestController
@RequestMapping("/partners/{partnerId}/payment-links")
@Tag(name = "Payment Links", description = "Payment link / invoice management")
@PreAuthorize("hasRole('ADMIN')")
public class PaymentLinkController extends BaseController {

    private final PaymentLinkService paymentLinkService;

    public PaymentLinkController(PaymentLinkService paymentLinkService) {
        this.paymentLinkService = paymentLinkService;
    }

    @PostMapping
    @Operation(summary = "Create a payment link",
               description = "Generate a shareable payment URL with amount, description, and expiry")
    @SecurityRequirement(name = OpenApiConstants.SECURITY_SCHEME_BEARER)
    @Audited(operation = Audited.Operation.CREATE, entityType = "PaymentLink", level = AuditLevel.INFO)
    @Idempotent(required = true)
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Payment link created"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Partner not found")
    })
    public ResponseEntity<ApiResponse<PaymentLinkResponse>> create(
            @PathVariable Long partnerId,
            @Valid @RequestBody CreatePaymentLinkRequest request) {
        PaymentLinkResponse response = paymentLinkService.createPaymentLink(partnerId, request);
        return created(response);
    }

    @GetMapping
    @Operation(summary = "List payment links for a partner")
    @SecurityRequirement(name = OpenApiConstants.SECURITY_SCHEME_BEARER)
    public ResponseEntity<ApiResponse<Page<PaymentLinkResponse>>> list(
            @PathVariable Long partnerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<PaymentLinkResponse> links = paymentLinkService.listByPartner(
                partnerId, PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ok(links);
    }

    @GetMapping("/{linkId}")
    @Operation(summary = "Get a payment link by ID")
    @SecurityRequirement(name = OpenApiConstants.SECURITY_SCHEME_BEARER)
    public ResponseEntity<ApiResponse<PaymentLinkResponse>> getById(
            @PathVariable Long partnerId,
            @PathVariable Long linkId) {
        PaymentLinkResponse response = paymentLinkService.getByIdForPartner(partnerId, linkId);
        return ok(response);
    }

    @DeleteMapping("/{linkId}")
    @Operation(summary = "Cancel a payment link")
    @SecurityRequirement(name = OpenApiConstants.SECURITY_SCHEME_BEARER)
    @Audited(operation = Audited.Operation.DELETE, entityType = "PaymentLink", level = AuditLevel.INFO)
    public ResponseEntity<Void> cancel(
            @PathVariable Long partnerId,
            @PathVariable Long linkId) {
        paymentLinkService.cancelPaymentLink(partnerId, linkId);
        return noContent();
    }
}
