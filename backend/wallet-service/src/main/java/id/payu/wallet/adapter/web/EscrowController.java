package id.payu.wallet.adapter.web;

import id.payu.api.common.response.ApiResponse;
import id.payu.commons.idempotency.Idempotent;
import id.payu.wallet.domain.model.EscrowTransaction;
import id.payu.wallet.domain.port.in.EscrowUseCase;
import id.payu.wallet.dto.CreateEscrowRequest;
import id.payu.wallet.dto.EscrowResponse;
import id.payu.wallet.dto.RefundEscrowRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import id.payu.security.annotation.Audited;
import id.payu.security.annotation.Audited.AuditLevel;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST Controller for escrow / payment holding operations.
 * Supports marketplace flows for TokoBapak, Nobar, and other partners.
 */
@RestController
@RequestMapping("/api/v1/escrow")
@Tag(name = "Escrow", description = "Escrow / Payment Holding APIs for marketplace transactions")
@SecurityRequirement(name = "bearerAuth")
public class EscrowController extends BaseController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EscrowController.class);

    private final EscrowUseCase escrowUseCase;

    public EscrowController(EscrowUseCase escrowUseCase) {
        this.escrowUseCase = escrowUseCase;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PARTNER', 'SYSTEM')")
    @Idempotent(required = true)
    @Audited(level = AuditLevel.INFO)
    @Operation(summary = "Create and hold escrow",
            description = "Creates an escrow transaction and immediately holds buyer funds")
    public ResponseEntity<ApiResponse<EscrowResponse>> createEscrow(
            @Valid @RequestBody CreateEscrowRequest request) {
        log.info("Creating escrow for buyer={}, seller={}", maskId(request.getBuyerAccountId()),
                maskId(request.getSellerAccountId()));

        EscrowTransaction escrow = escrowUseCase.createAndHoldEscrow(
                request.getBuyerAccountId(),
                request.getSellerAccountId(),
                request.getPartnerId(),
                request.getAmount(),
                request.getFeeAmount(),
                request.getCurrency(),
                request.getExternalReferenceId(),
                request.getDescription(),
                request.getExpiresInHours());

        EscrowResponse response = EscrowResponse.from(escrow);
        return created(response, "/api/v1/escrow/" + escrow.getId());
    }

    @PostMapping("/{escrowId}/release")
    @PreAuthorize("hasAnyRole('ADMIN', 'PARTNER', 'SYSTEM')")
    @Idempotent(required = true)
    @Audited(level = AuditLevel.INFO)
    @Operation(summary = "Release escrow to merchant",
            description = "Releases held funds to merchant — conditions met (goods received, etc.)")
    public ResponseEntity<ApiResponse<EscrowResponse>> releaseEscrow(
            @PathVariable UUID escrowId) {
        log.info("Releasing escrow: id={}", escrowId);

        EscrowTransaction escrow = escrowUseCase.releaseEscrow(escrowId);
        return ok(EscrowResponse.from(escrow));
    }

    @PostMapping("/{escrowId}/settle")
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM')")
    @Idempotent(required = true)
    @Audited(level = AuditLevel.INFO)
    @Operation(summary = "Settle escrow — credit merchant wallet",
            description = "Credits the merchant wallet with net escrow amount (amount - fee)")
    public ResponseEntity<ApiResponse<EscrowResponse>> settleEscrow(
            @PathVariable UUID escrowId) {
        log.info("Settling escrow: id={}", escrowId);

        EscrowTransaction escrow = escrowUseCase.settleEscrow(escrowId);
        return ok(EscrowResponse.from(escrow));
    }

    @PostMapping("/{escrowId}/refund")
    @PreAuthorize("hasAnyRole('ADMIN', 'PARTNER', 'SYSTEM')")
    @Idempotent(required = true)
    @Audited(level = AuditLevel.INFO)
    @Operation(summary = "Refund escrow to buyer",
            description = "Refunds held funds back to buyer wallet")
    public ResponseEntity<ApiResponse<EscrowResponse>> refundEscrow(
            @PathVariable UUID escrowId,
            @Valid @RequestBody RefundEscrowRequest request) {
        log.info("Refunding escrow: id={}", escrowId);

        EscrowTransaction escrow = escrowUseCase.refundEscrow(escrowId, request.getReason());
        return ok(EscrowResponse.from(escrow));
    }

    @GetMapping("/{escrowId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PARTNER', 'SYSTEM')")
    @Operation(summary = "Get escrow by ID")
    public ResponseEntity<ApiResponse<EscrowResponse>> getEscrow(
            @PathVariable UUID escrowId) {
        EscrowTransaction escrow = escrowUseCase.getEscrow(escrowId);
        return ok(EscrowResponse.from(escrow));
    }

    @GetMapping("/buyer/{buyerAccountId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PARTNER', 'SYSTEM')")
    @Operation(summary = "Get escrows by buyer account")
    public ResponseEntity<ApiResponse<List<EscrowResponse>>> getEscrowsByBuyer(
            @PathVariable String buyerAccountId) {
        List<EscrowResponse> escrows = escrowUseCase.getEscrowsByBuyer(buyerAccountId)
                .stream()
                .map(EscrowResponse::from)
                .collect(Collectors.toList());
        return okList(escrows);
    }

    @GetMapping("/seller/{sellerAccountId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PARTNER', 'SYSTEM')")
    @Operation(summary = "Get escrows by seller account")
    public ResponseEntity<ApiResponse<List<EscrowResponse>>> getEscrowsBySeller(
            @PathVariable String sellerAccountId) {
        List<EscrowResponse> escrows = escrowUseCase.getEscrowsBySeller(sellerAccountId)
                .stream()
                .map(EscrowResponse::from)
                .collect(Collectors.toList());
        return okList(escrows);
    }

    @GetMapping("/partner/{partnerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PARTNER', 'SYSTEM')")
    @Operation(summary = "Get escrows by partner")
    public ResponseEntity<ApiResponse<List<EscrowResponse>>> getEscrowsByPartner(
            @PathVariable String partnerId) {
        List<EscrowResponse> escrows = escrowUseCase.getEscrowsByPartner(partnerId)
                .stream()
                .map(EscrowResponse::from)
                .collect(Collectors.toList());
        return okList(escrows);
    }

    private String maskId(String id) {
        if (id == null || id.length() <= 4) return "****";
        return id.substring(0, 4) + "****";
    }
}
