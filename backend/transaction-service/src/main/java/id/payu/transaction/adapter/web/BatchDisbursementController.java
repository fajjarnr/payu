package id.payu.transaction.adapter.web;

import id.payu.commons.idempotency.Idempotent;
import id.payu.transaction.application.service.AuthorizationService;
import id.payu.transaction.adapter.persistence.entity.BatchDisbursementEntity;
import id.payu.transaction.adapter.persistence.entity.DisbursementEntity;
import id.payu.transaction.domain.model.Money;
import id.payu.transaction.domain.port.in.BatchDisbursementUseCase;
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
 * REST controller for batch disbursement (bulk payout) operations.
 *
 * <p>Provides endpoints for:
 * <ul>
 *   <li>Creating batch disbursements</li>
 *   <li>Adding items to batches</li>
 *   <li>Processing batches</li>
 *   <li>Tracking batch progress</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/disbursements/batch")
@RequiredArgsConstructor
@Tag(name = "Batch Disbursements", description = "Batch/Bulk DisbursementEntity API")
@SecurityRequirement(name = "bearerAuth")
public class BatchDisbursementController {

    private final BatchDisbursementUseCase batchUseCase;
    private final AuthorizationService authorizationService;

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

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Idempotent(required = true)
    @Operation(summary = "Create a new batch", description = "Creates a batch disbursement for bulk payouts")
    public ResponseEntity<BatchResponse> createBatch(
            @Valid @RequestBody CreateBatchRequest request,
            @RequestHeader(value = "X-Idempotency-Key", required = true) String idempotencyKey) {

        // Verify the authenticated user owns the source account
        String userId = extractUserId();
        authorizationService.verifyAccountOwnership(request.getSourceAccountId(), userId);

        BatchDisbursementEntity batch = batchUseCase.createBatch(
                request.getSourceAccountId(),
                request.getName(),
                request.getDescription(),
                idempotencyKey != null ? idempotencyKey : request.getIdempotencyKey()
        );

        // Add items if provided
        if (request.getItems() != null) {
            for (BatchItemRequest item : request.getItems()) {
                batchUseCase.addBatchItem(
                        batch.getId(),
                        Money.of(item.getAmount(), item.getCurrency()),
                        item.getBankCode(),
                        item.getAccountNumber(),
                        item.getAccountName(),
                        item.getDescription()
                );
            }
        }

        // Refresh to get updated state
        batch = batchUseCase.getBatch(batch.getId()).orElseThrow();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BatchResponse.fromEntity(batch));
    }

    @PostMapping("/{id}/items")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Add item to batch", description = "Adds a disbursement item to a pending batch")
    public ResponseEntity<DisbursementResponse> addBatchItem(
            @PathVariable @Parameter(description = "Batch ID") UUID id,
            @Valid @RequestBody BatchItemRequest request) {

        DisbursementEntity item = batchUseCase.addBatchItem(
                id,
                Money.of(request.getAmount(), request.getCurrency()),
                request.getBankCode(),
                request.getAccountNumber(),
                request.getAccountName(),
                request.getDescription()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(DisbursementResponse.fromEntity(item));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get batch by ID", description = "Retrieves batch details and status")
    public ResponseEntity<BatchResponse> getBatch(
            @PathVariable @Parameter(description = "Batch ID") UUID id) {

        BatchDisbursementEntity batch = batchUseCase.getBatch(id)
                .orElseThrow(() -> new IllegalArgumentException("Batch not found: " + id));

        return ResponseEntity.ok(BatchResponse.fromEntity(batch));
    }

    @GetMapping("/{id}/items")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get batch items", description = "Retrieves all disbursement items in a batch")
    public ResponseEntity<List<DisbursementResponse>> getBatchItems(
            @PathVariable @Parameter(description = "Batch ID") UUID id) {

        List<DisbursementEntity> items = batchUseCase.getBatchItems(id);

        List<DisbursementResponse> responses = items.stream()
                .map(DisbursementResponse::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}/progress")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get batch progress", description = "Returns batch progress percentage")
    public ResponseEntity<BatchProgressResponse> getBatchProgress(
            @PathVariable @Parameter(description = "Batch ID") UUID id) {

        int progress = batchUseCase.getBatchProgress(id);

        return ResponseEntity.ok(BatchProgressResponse.builder()
                .batchId(id)
                .progressPercentage(progress)
                .build());
    }

    @PostMapping("/{id}/process")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Process batch", description = "Starts processing a pending batch")
    public ResponseEntity<BatchResponse> processBatch(
            @PathVariable @Parameter(description = "Batch ID") UUID id) {

        BatchDisbursementEntity batch = batchUseCase.processBatch(id);
        return ResponseEntity.ok(BatchResponse.fromEntity(batch));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Complete batch (Admin)", description = "Manually complete a processing batch")
    public ResponseEntity<BatchResponse> completeBatch(
            @PathVariable @Parameter(description = "Batch ID") UUID id) {

        BatchDisbursementEntity batch = batchUseCase.completeBatch(id);
        return ResponseEntity.ok(BatchResponse.fromEntity(batch));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List batches", description = "Lists batch disbursements for an account")
    public ResponseEntity<List<BatchResponse>> listBatches(
            @RequestParam UUID sourceAccountId,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset) {

        // Verify the authenticated user owns the source account
        String userId = extractUserId();
        authorizationService.verifyAccountOwnership(sourceAccountId, userId);

        List<BatchDisbursementEntity> batches = batchUseCase.listBatchesByAccount(
                sourceAccountId, limit, offset);

        List<BatchResponse> responses = batches.stream()
                .map(BatchResponse::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }
}
