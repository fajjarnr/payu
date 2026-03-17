package id.payu.transaction.adapter.web;

import id.payu.commons.idempotency.Idempotent;
import id.payu.transaction.application.service.AuthorizationService;
import id.payu.transaction.domain.model.Disbursement;
import id.payu.transaction.domain.model.Money;
import id.payu.transaction.domain.port.in.DisbursementUseCase;
import id.payu.transaction.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for disbursement (payout) operations.
 *
 * <p>Provides endpoints for:
 * <ul>
 *   <li>Creating disbursements</li>
 *   <li>Getting disbursement status</li>
 *   <li>Listing account disbursements</li>
 *   <li>BI-FAST callbacks</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/disbursements")
@RequiredArgsConstructor
@Tag(name = "Disbursements", description = "Disbursement/Payout API")
@SecurityRequirement(name = "bearerAuth")
public class DisbursementController {

    private final DisbursementUseCase disbursementUseCase;
    private final AuthorizationService authorizationService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Idempotent(required = true)
    @Operation(summary = "Create a new disbursement", description = "Creates a payout to an external bank account")
    public ResponseEntity<DisbursementResponse> createDisbursement(
            @Valid @RequestBody CreateDisbursementRequest request,
            @RequestHeader(value = "X-Account-Id", required = false) UUID accountId,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey) {

        // Use header account ID or extract from authentication, then verify ownership
        UUID sourceAccountId = accountId != null ? accountId : getCurrentAccountId();
        String userId = extractUserId();
        authorizationService.verifyAccountOwnership(sourceAccountId, userId);

        Disbursement disbursement = disbursementUseCase.createDisbursement(
                sourceAccountId,
                Money.of(request.getAmount(), request.getCurrency()),
                request.getBankCode(),
                request.getAccountNumber(),
                request.getAccountName(),
                request.getDescription(),
                idempotencyKey != null ? idempotencyKey : request.getIdempotencyKey()
        );

        // Process immediately (or could queue for async processing)
        disbursementUseCase.processDisbursement(disbursement.getId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(DisbursementResponse.fromEntity(disbursement));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get disbursement by ID", description = "Retrieves disbursement details and status")
    public ResponseEntity<DisbursementResponse> getDisbursement(
            @PathVariable @Parameter(description = "Disbursement ID") UUID id) {

        Disbursement disbursement = disbursementUseCase.getDisbursement(id)
                .orElseThrow(() -> new IllegalArgumentException("Disbursement not found: " + id));

        return ResponseEntity.ok(DisbursementResponse.fromEntity(disbursement));
    }

    @GetMapping("/by-idempotency-key/{key}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Find disbursement by idempotency key", description = "Retrieves disbursement using idempotency key")
    public ResponseEntity<DisbursementResponse> getDisbursementByIdempotencyKey(
            @PathVariable @Parameter(description = "Idempotency key") String key) {

        Disbursement disbursement = disbursementUseCase.findByIdempotencyKey(key)
                .orElseThrow(() -> new IllegalArgumentException("Disbursement not found for key: " + key));

        return ResponseEntity.ok(DisbursementResponse.fromEntity(disbursement));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List disbursements", description = "Lists disbursements for the authenticated account")
    public ResponseEntity<List<DisbursementResponse>> listDisbursements(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestHeader(value = "X-Account-Id", required = false) UUID accountId) {

        UUID sourceAccountId = accountId != null ? accountId : getCurrentAccountId();
        String userId = extractUserId();
        authorizationService.verifyAccountOwnership(sourceAccountId, userId);

        List<Disbursement> disbursements = disbursementUseCase.listDisbursementsByAccount(
                sourceAccountId, limit, offset);

        List<DisbursementResponse> responses = disbursements.stream()
                .map(DisbursementResponse::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @PostMapping("/callback")
    @Operation(summary = "BI-FAST callback", description = "Callback endpoint for BI-FAST transfer status updates")
    public ResponseEntity<DisbursementResponse> handleCallback(
            @Valid @RequestBody DisbursementCallbackRequest request) {

        Disbursement disbursement;
        if ("COMPLETED".equalsIgnoreCase(request.getStatus())) {
            disbursement = disbursementUseCase.completeDisbursement(
                    request.getDisbursementId(),
                    request.getBankReference()
            );
        } else {
            disbursement = disbursementUseCase.failDisbursement(
                    request.getDisbursementId(),
                    request.getFailureReason() != null ? request.getFailureReason() : "Transfer failed"
            );
        }

        return ResponseEntity.ok(DisbursementResponse.fromEntity(disbursement));
    }

    @PostMapping("/{id}/process")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Process disbursement (Admin)", description = "Manually trigger disbursement processing")
    public ResponseEntity<DisbursementResponse> processDisbursement(
            @PathVariable UUID id) {

        Disbursement disbursement = disbursementUseCase.processDisbursement(id);
        return ResponseEntity.ok(DisbursementResponse.fromEntity(disbursement));
    }

    private UUID getCurrentAccountId() {
        // Extract account ID from JWT claims
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            String accountId = jwt.getClaim("account_id");
            if (accountId != null) {
                return UUID.fromString(accountId);
            }
        }
        throw new IllegalStateException("No valid JWT authentication found");
    }

    // BUG-AUTH-013: Standardized to use 'account_id' claim with 'sub' fallback
    private String extractUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt)) {
            throw new IllegalStateException("No valid JWT authentication found");
        }
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String accountId = jwt.getClaimAsString("account_id");
        return accountId != null ? accountId : jwt.getSubject();
    }
}
