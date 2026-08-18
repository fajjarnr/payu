package id.payu.partner.adapter.web;

import id.payu.commons.idempotency.Idempotent;
import id.payu.partner.application.service.PaymentLinkService;
import id.payu.partner.interfaces.dto.PaymentLinkResponse;
import id.payu.security.annotation.Audited;
import id.payu.security.annotation.AuditLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import id.payu.security.annotation.AuditOperation;

/**
 * Public REST endpoint for payers to view payment link details.
 * No authentication required — payers access via shareable URL.
 */
@RestController
@RequestMapping({"/v1/pay", "/pay"})
@Tag(name = "Payment Links (Public)", description = "Public payment link endpoints for payers")
public class PublicPaymentLinkController extends BaseController {

    private final PaymentLinkService paymentLinkService;

    public PublicPaymentLinkController(PaymentLinkService paymentLinkService) {
        this.paymentLinkService = paymentLinkService;
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Get payment link details by slug",
               description = "Public endpoint for payer to view payment details and select a payment method")
    public ResponseEntity<ApiResponse<PaymentLinkResponse>> getBySlug(@PathVariable String slug) {
        PaymentLinkResponse response = paymentLinkService.getBySlug(slug);
        return ok(response);
    }

    @PostMapping("/{slug}/confirm")
    @Operation(summary = "Confirm payment on a payment link",
               description = "Called after payment is processed to mark the link as paid. Validates payment method and reference.")
    @Audited(operation = AuditOperation.UPDATE, entityType = "PaymentLinkEntity", level = AuditLevel.INFO)
    @Idempotent(required = true)
    public ResponseEntity<ApiResponse<PaymentLinkResponse>> confirmPayment(
            @PathVariable String slug,
            @RequestParam String paymentMethod,
            @RequestParam String paymentReference) {
        // PAY-LINK-001: Validate inputs — reject blank or unknown values
        if (paymentMethod == null || paymentMethod.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("PAY_001", "paymentMethod is required"));
        }
        if (paymentReference == null || paymentReference.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("PAY_002", "paymentReference is required"));
        }
        if (!java.util.Set.of("BANK_TRANSFER", "VA", "QRIS", "EWALLET", "CREDIT_CARD", "DEBIT_CARD")
                .contains(paymentMethod.toUpperCase())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("PAY_003", "Unknown paymentMethod: " + paymentMethod));
        }
        PaymentLinkResponse response = paymentLinkService.confirmPayment(slug, paymentMethod, paymentReference);
        return ok(response);
    }
}
