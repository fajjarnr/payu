package id.payu.dispute.adapter.web;

import id.payu.commons.idempotency.Idempotent;
import id.payu.dispute.domain.model.Dispute;
import id.payu.dispute.domain.model.DisputeResolutionType;
import id.payu.dispute.domain.port.in.DisputeUseCase;
import id.payu.dispute.interfaces.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST Controller for Dispute operations.
 *
 * <p>Provides endpoints for creating and managing disputes.</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/disputes")
@RequiredArgsConstructor
@Tag(name = "Disputes", description = "Dispute management endpoints")
public class DisputeController {

    private final DisputeUseCase disputeUseCase;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'BACKOFFICE', 'DISPUTE_AGENT')")
    @Operation(summary = "List disputes", description = "Lists all disputes in the system")
    @ApiResponse(responseCode = "200", description = "List of disputes",
            content = @Content(schema = @Schema(implementation = DisputeListResponse.class)))
    public ResponseEntity<DisputeListResponse> listDisputes() {
        log.debug("Listing all disputes");
        List<Dispute> disputes = disputeUseCase.getAllDisputes();
        return ResponseEntity.ok(toListResponse(disputes));
    }

    @PostMapping
    @Idempotent(required = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'BACKOFFICE', 'DISPUTE_AGENT', 'USER')")
    @Operation(summary = "Open a dispute", description = "Creates a new dispute for a transaction")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Dispute created successfully",
                    content = @Content(schema = @Schema(implementation = DisputeResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Transaction not found")
    })
    public ResponseEntity<DisputeResponse> openDispute(
            @Valid @RequestBody OpenDisputeRequest request,
            @CurrentSecurityContext(expression = "authentication") Authentication authentication) {
        UUID customerId = isOperational(authentication)
                ? request.getCustomerId()
                : authenticatedCustomerId(authentication);
        log.info("Opening dispute for transaction: {} by customer: {}",
                request.getTransactionId(), customerId);
        Dispute dispute = disputeUseCase.openDispute(
                request.getTransactionId(),
                customerId,
                request.getMerchantId(),
                request.getDisputedAmount(),
                request.getCurrency(),
                request.getReason()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(dispute));
    }

    @PostMapping("/{disputeId}/investigate")
    @PreAuthorize("hasAnyRole('ADMIN', 'BACKOFFICE', 'DISPUTE_AGENT')")
    @Operation(summary = "Start investigation", description = "Transitions dispute from OPEN to INVESTIGATING")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Investigation started",
                    content = @Content(schema = @Schema(implementation = DisputeResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid state transition"),
            @ApiResponse(responseCode = "404", description = "Dispute not found")
    })
    public ResponseEntity<DisputeResponse> startInvestigation(
            @Parameter(description = "Dispute ID") @PathVariable UUID disputeId,
            @Valid @RequestBody StartInvestigationRequest request) {
        log.info("Starting investigation for dispute: {} with ID: {}", disputeId, request.getInvestigationId());
        Dispute dispute = disputeUseCase.startInvestigation(disputeId, request.getInvestigationId());
        return ResponseEntity.ok(toResponse(dispute));
    }

    @PostMapping("/{disputeId}/resolve")
    @Idempotent(required = true, headerName = "X-Idempotency-Key")
    @PreAuthorize("hasAnyRole('ADMIN', 'BACKOFFICE', 'DISPUTE_AGENT')")
    @Operation(summary = "Resolve a dispute", description = "Transitions dispute from INVESTIGATING to RESOLVED")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dispute resolved",
                    content = @Content(schema = @Schema(implementation = DisputeResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid state transition"),
            @ApiResponse(responseCode = "404", description = "Dispute not found")
    })
    public ResponseEntity<DisputeResponse> resolveDispute(
            @Parameter(description = "Dispute ID") @PathVariable UUID disputeId,
            @Valid @RequestBody ResolveDisputeRequest request) {
        log.info("Resolving dispute: {} with type: {}", disputeId, request.getResolutionType());
        DisputeResolutionType resolutionType = DisputeResolutionType.valueOf(request.getResolutionType());
        Dispute dispute = disputeUseCase.resolveDispute(disputeId, resolutionType, request.getResolution());
        return ResponseEntity.ok(toResponse(dispute));
    }

    @PostMapping("/{disputeId}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'BACKOFFICE', 'DISPUTE_AGENT')")
    @Operation(summary = "Reject a dispute", description = "Transitions dispute to REJECTED")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dispute rejected",
                    content = @Content(schema = @Schema(implementation = DisputeResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid state transition"),
            @ApiResponse(responseCode = "404", description = "Dispute not found")
    })
    public ResponseEntity<DisputeResponse> rejectDispute(
            @Parameter(description = "Dispute ID") @PathVariable UUID disputeId,
            @Valid @RequestBody RejectDisputeRequest request) {
        log.info("Rejecting dispute: {} with reason: {}", disputeId, request.getRejectionReason());
        Dispute dispute = disputeUseCase.rejectDispute(disputeId, request.getRejectionReason());
        return ResponseEntity.ok(toResponse(dispute));
    }

    @PostMapping("/{disputeId}/escalate")
    @PreAuthorize("hasAnyRole('ADMIN', 'BACKOFFICE', 'DISPUTE_AGENT')")
    @Operation(summary = "Escalate a dispute", description = "Transitions dispute from INVESTIGATING to ESCALATED")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dispute escalated",
                    content = @Content(schema = @Schema(implementation = DisputeResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid state transition"),
            @ApiResponse(responseCode = "404", description = "Dispute not found")
    })
    public ResponseEntity<DisputeResponse> escalateDispute(
            @Parameter(description = "Dispute ID") @PathVariable UUID disputeId,
            @Valid @RequestBody EscalateDisputeRequest request) {
        log.info("Escalating dispute: {} with reason: {}", disputeId, request.getEscalationReason());
        Dispute dispute = disputeUseCase.escalateDispute(disputeId, request.getEscalationReason());
        return ResponseEntity.ok(toResponse(dispute));
    }

    @PostMapping("/{disputeId}/evidence")
    @PreAuthorize("hasAnyRole('ADMIN', 'BACKOFFICE', 'DISPUTE_AGENT', 'USER')")
    @Operation(summary = "Add evidence", description = "Adds evidence to a dispute")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Evidence added",
                    content = @Content(schema = @Schema(implementation = DisputeResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request or dispute in terminal state"),
            @ApiResponse(responseCode = "404", description = "Dispute not found")
    })
    public ResponseEntity<DisputeResponse> addEvidence(
            @Parameter(description = "Dispute ID") @PathVariable UUID disputeId,
            @Valid @RequestBody AddEvidenceRequest request,
            @CurrentSecurityContext(expression = "authentication") Authentication authentication) {
        if (!isOperational(authentication)
                && disputeUseCase.getDisputeForCustomer(disputeId, authenticatedCustomerId(authentication)).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        log.info("Adding evidence to dispute: {} - file: {}", disputeId, request.getFileName());
        Dispute dispute = disputeUseCase.addEvidence(
                disputeId,
                request.getFileName(),
                request.getFileUrl(),
                request.getUploadedBy()
        );
        return ResponseEntity.ok(toResponse(dispute));
    }

    @GetMapping("/{disputeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BACKOFFICE', 'DISPUTE_AGENT', 'USER')")
    @Operation(summary = "Get dispute by ID", description = "Retrieves a dispute by its ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dispute found",
                    content = @Content(schema = @Schema(implementation = DisputeResponse.class))),
            @ApiResponse(responseCode = "404", description = "Dispute not found")
    })
    public ResponseEntity<DisputeResponse> getDispute(
            @Parameter(description = "Dispute ID") @PathVariable UUID disputeId,
            @CurrentSecurityContext(expression = "authentication") Authentication authentication) {
        log.debug("Getting dispute: {}", disputeId);
        Optional<Dispute> dispute = isOperational(authentication)
                ? disputeUseCase.getDispute(disputeId)
                : disputeUseCase.getDisputeForCustomer(disputeId, authenticatedCustomerId(authentication));
        return dispute
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/transaction/{transactionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BACKOFFICE', 'DISPUTE_AGENT', 'USER')")
    @Operation(summary = "Get disputes by transaction", description = "Retrieves all disputes for a transaction")
    @ApiResponse(responseCode = "200", description = "List of disputes",
            content = @Content(schema = @Schema(implementation = DisputeListResponse.class)))
    public ResponseEntity<DisputeListResponse> getDisputesByTransaction(
            @Parameter(description = "Transaction ID") @PathVariable UUID transactionId,
            @CurrentSecurityContext(expression = "authentication") Authentication authentication) {
        log.debug("Getting disputes for transaction: {}", transactionId);
        List<Dispute> disputes = isOperational(authentication)
                ? disputeUseCase.getDisputesByTransaction(transactionId)
                : disputeUseCase.getDisputesByTransactionForCustomer(
                        transactionId, authenticatedCustomerId(authentication));
        return ResponseEntity.ok(toListResponse(disputes));
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BACKOFFICE', 'DISPUTE_AGENT', 'USER')")
    @Operation(summary = "Get disputes by customer", description = "Retrieves all disputes for a customer")
    @ApiResponse(responseCode = "200", description = "List of disputes",
            content = @Content(schema = @Schema(implementation = DisputeListResponse.class)))
    public ResponseEntity<DisputeListResponse> getDisputesByCustomer(
            @Parameter(description = "Customer ID") @PathVariable UUID customerId,
            @CurrentSecurityContext(expression = "authentication") Authentication authentication) {
        if (!isOperational(authentication)) {
            UUID authenticatedCustomerId = authenticatedCustomerId(authentication);
            if (!authenticatedCustomerId.equals(customerId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            customerId = authenticatedCustomerId;
        }
        log.debug("Getting disputes for customer: {}", customerId);
        List<Dispute> disputes = disputeUseCase.getDisputesByCustomer(customerId);
        return ResponseEntity.ok(toListResponse(disputes));
    }

    @GetMapping("/merchant/{merchantId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BACKOFFICE', 'DISPUTE_AGENT')")
    @Operation(summary = "Get disputes by merchant", description = "Retrieves all disputes for a merchant")
    @ApiResponse(responseCode = "200", description = "List of disputes",
            content = @Content(schema = @Schema(implementation = DisputeListResponse.class)))
    public ResponseEntity<DisputeListResponse> getDisputesByMerchant(
            @Parameter(description = "Merchant ID") @PathVariable UUID merchantId) {
        log.debug("Getting disputes for merchant: {}", merchantId);
        List<Dispute> disputes = disputeUseCase.getDisputesByMerchant(merchantId);
        return ResponseEntity.ok(toListResponse(disputes));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BACKOFFICE', 'DISPUTE_AGENT')")
    @Operation(summary = "Get disputes by status", description = "Retrieves disputes filtered by status")
    @ApiResponse(responseCode = "200", description = "List of disputes",
            content = @Content(schema = @Schema(implementation = DisputeListResponse.class)))
    public ResponseEntity<DisputeListResponse> getDisputesByStatus(
            @Parameter(description = "Dispute status") @PathVariable String status) {
        log.debug("Getting disputes by status: {}", status);
        List<Dispute> disputes = disputeUseCase.getDisputesByStatus(status);
        return ResponseEntity.ok(toListResponse(disputes));
    }

    private DisputeResponse toResponse(Dispute dispute) {
        List<DisputeEvidenceResponse> evidenceResponses = dispute.getEvidenceList().stream()
                .map(e -> DisputeEvidenceResponse.builder()
                        .id(e.getId())
                        .fileName(e.getFileName())
                        .fileUrl(e.getFileUrl())
                        .uploadedBy(e.getUploadedBy())
                        .uploadedAt(e.getUploadedAt())
                        .build())
                .collect(Collectors.toList());

        return DisputeResponse.builder()
                .id(dispute.getId())
                .transactionId(dispute.getTransactionId())
                .customerId(dispute.getCustomerId())
                .merchantId(dispute.getMerchantId())
                .disputedAmount(dispute.getDisputedAmount())
                .currency(dispute.getCurrency())
                .reason(dispute.getReason())
                .status(dispute.getStatus().name())
                .investigationId(dispute.getInvestigationId())
                .resolutionType(dispute.getResolutionType() != null ? dispute.getResolutionType().name() : null)
                .resolution(dispute.getResolution())
                .rejectionReason(dispute.getRejectionReason())
                .escalationReason(dispute.getEscalationReason())
                .openedAt(dispute.getOpenedAt())
                .investigationStartedAt(dispute.getInvestigationStartedAt())
                .resolvedAt(dispute.getResolvedAt())
                .rejectedAt(dispute.getRejectedAt())
                .escalatedAt(dispute.getEscalatedAt())
                .evidenceList(evidenceResponses)
                .build();
    }

    private DisputeListResponse toListResponse(List<Dispute> disputes) {
        List<DisputeResponse> responses = disputes.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return DisputeListResponse.builder()
                .disputes(responses)
                .total(responses.size())
                .build();
    }

    private boolean isOperational(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .anyMatch(authority -> authority.equals("admin")
                        || authority.equals("ROLE_ADMIN")
                        || authority.equals("backoffice")
                        || authority.equals("ROLE_BACKOFFICE")
                        || authority.equals("dispute_agent")
                        || authority.equals("ROLE_DISPUTE_AGENT"));
    }

    private UUID authenticatedCustomerId(Authentication authentication) {
        String principalId = authentication.getName();
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            principalId = jwt.getClaimAsString("account_id");
            if (principalId == null || principalId.isBlank()) {
                principalId = jwt.getSubject();
            }
        }

        try {
            return UUID.fromString(principalId);
        } catch (RuntimeException ex) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Authenticated customer identity is invalid", ex);
        }
    }
}
