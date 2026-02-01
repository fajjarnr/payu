package id.payu.billing.controller;

import id.payu.api.common.response.ApiResponse;
import id.payu.billing.domain.BillPayment;
import id.payu.billing.dto.CreatePaymentRequest;
import id.payu.billing.dto.PaymentResponse;
import id.payu.billing.service.PaymentService;
import id.payu.commons.idempotency.Idempotent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

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

    @PostMapping
    @Idempotent(required = true)
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
        BillPayment payment = paymentService.createPayment(request);
        return ApiResponse.success(PaymentResponse.from(payment), HttpStatus.CREATED);
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
                .map(payment -> ApiResponse.success(PaymentResponse.from(payment)))
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
                .map(payment -> ApiResponse.success(PaymentResponse.from(payment)))
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found"));
    }
}
