package id.payu.transaction.adapter.web;

import id.payu.commons.idempotency.Idempotent;
import id.payu.transaction.application.service.AsyncDisbursementProcessorService;
import id.payu.transaction.application.service.AuthorizationService;
import id.payu.transaction.adapter.persistence.entity.DisbursementEntity;
import id.payu.transaction.domain.model.Money;
import id.payu.transaction.domain.port.in.DisbursementUseCase;
import id.payu.transaction.interfaces.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@RestController
@RequestMapping("/api/v1/disbursements")
@RequiredArgsConstructor
@Tag(name = "Disbursements", description = "DisbursementEntity/Payout API")
@SecurityRequirement(name = "bearerAuth")
public class DisbursementController {

    private final DisbursementUseCase disbursementUseCase;
    private final AsyncDisbursementProcessorService asyncDisbursementProcessor;
    private final AuthorizationService authorizationService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Idempotent(required = true)
    @Operation(summary = "Create a new disbursement", description = "Creates a payout to an external bank account")
    public ResponseEntity<DisbursementResponse> createDisbursement(
            @Valid @RequestBody CreateDisbursementRequest request,
            @RequestHeader(value = "X-Account-Id", required = false) UUID accountId,
            @RequestHeader(value = "X-Idempotency-Key", required = true) String idempotencyKey) {

        // Use header account ID or extract from authentication, then verify ownership
        UUID sourceAccountId = accountId != null ? accountId : getCurrentAccountId();
        String userId = extractUserId();
        authorizationService.verifyAccountOwnership(sourceAccountId, userId);

        DisbursementEntity disbursement = disbursementUseCase.createDisbursement(
                sourceAccountId,
                Money.of(request.getAmount(), request.getCurrency()),
                request.getBankCode(),
                request.getAccountNumber(),
                request.getAccountName(),
                request.getDescription(),
                idempotencyKey != null ? idempotencyKey : request.getIdempotencyKey()
        );

        // Dispatch BI-FAST processing to async thread (returns 201 immediately).
        // Synchronous call would hold the request hostage and risk optimistic-lock races
        // when the BI-FAST callback updates the row mid-flight. AsyncDisbursementProcessorService
        // logs errors; failures are retried by the outbox async worker.
        asyncDisbursementProcessor.processDisbursementAsync(disbursement.getId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(DisbursementResponse.fromEntity(disbursement));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get disbursement by ID", description = "Retrieves disbursement details and status")
    public ResponseEntity<DisbursementResponse> getDisbursement(
            @PathVariable @Parameter(description = "DisbursementEntity ID") UUID id) {

        DisbursementEntity disbursement = disbursementUseCase.getDisbursement(id)
                .orElseThrow(() -> new IllegalArgumentException("DisbursementEntity not found: " + id));

        // SEC-DISB-001: Verify caller owns the source account
        authorizationService.verifyAccountOwnership(disbursement.getSourceAccountId(), extractUserId());

        return ResponseEntity.ok(DisbursementResponse.fromEntity(disbursement));
    }

    @GetMapping("/by-idempotency-key/{key}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Find disbursement by idempotency key", description = "Retrieves disbursement using idempotency key")
    public ResponseEntity<DisbursementResponse> getDisbursementByIdempotencyKey(
            @PathVariable @Parameter(description = "Idempotency key") String key) {

        DisbursementEntity disbursement = disbursementUseCase.findByIdempotencyKey(key)
                .orElseThrow(() -> new IllegalArgumentException("DisbursementEntity not found for key: " + key));

        // SEC-DISB-001: Verify caller owns the source account
        authorizationService.verifyAccountOwnership(disbursement.getSourceAccountId(), extractUserId());

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

        List<DisbursementEntity> disbursements = disbursementUseCase.listDisbursementsByAccount(
                sourceAccountId, limit, offset);

        List<DisbursementResponse> responses = disbursements.stream()
                .map(DisbursementResponse::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @PostMapping("/callback")
    @Idempotent(required = true)
    @Operation(summary = "BI-FAST callback", description = "Callback endpoint for BI-FAST transfer status updates")
    public ResponseEntity<DisbursementResponse> handleCallback(
            @Valid @RequestBody DisbursementCallbackRequest request) {

        DisbursementEntity disbursement;
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

        DisbursementEntity disbursement = disbursementUseCase.processDisbursement(id);
        return ResponseEntity.ok(DisbursementResponse.fromEntity(disbursement));
    }

    // BUG-TXN-ACCOUNT-001: Standardized to use 'account_id' claim with 'sub'
    // fallback (matches extractUserId() sibling helper, BUG-AUTH-013).
    // Customer1 JWT has sub=7a51ced3-... but no account_id claim → was throwing
    // 409 on POST /api/v1/disbursements. Now falls back to sub claim.
    private UUID getCurrentAccountId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt)) {
            throw new IllegalStateException("No valid JWT authentication found");
        }
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String accountId = jwt.getClaimAsString("account_id");
        if (accountId != null) {
            return UUID.fromString(accountId);
        }
        // Fallback to sub claim (matches extractUserId behavior)
        String sub = jwt.getSubject();
        if (sub != null) {
            return UUID.fromString(sub);
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
