package id.payu.transaction.adapter.web;

import id.payu.transaction.domain.model.BatchDisbursement;
import id.payu.transaction.domain.model.Disbursement;
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
@Tag(name = "Batch Disbursements", description = "Batch/Bulk Disbursement API")
@SecurityRequirement(name = "bearerAuth")
public class BatchDisbursementController {

    private final BatchDisbursementUseCase batchUseCase;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create a new batch", description = "Creates a batch disbursement for bulk payouts")
    public ResponseEntity<BatchResponse> createBatch(
            @Valid @RequestBody CreateBatchRequest request,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey) {

        BatchDisbursement batch = batchUseCase.createBatch(
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

        Disbursement item = batchUseCase.addBatchItem(
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

        BatchDisbursement batch = batchUseCase.getBatch(id)
                .orElseThrow(() -> new IllegalArgumentException("Batch not found: " + id));

        return ResponseEntity.ok(BatchResponse.fromEntity(batch));
    }

    @GetMapping("/{id}/items")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get batch items", description = "Retrieves all disbursement items in a batch")
    public ResponseEntity<List<DisbursementResponse>> getBatchItems(
            @PathVariable @Parameter(description = "Batch ID") UUID id) {

        List<Disbursement> items = batchUseCase.getBatchItems(id);

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

        BatchDisbursement batch = batchUseCase.processBatch(id);
        return ResponseEntity.ok(BatchResponse.fromEntity(batch));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Complete batch (Admin)", description = "Manually complete a processing batch")
    public ResponseEntity<BatchResponse> completeBatch(
            @PathVariable @Parameter(description = "Batch ID") UUID id) {

        BatchDisbursement batch = batchUseCase.completeBatch(id);
        return ResponseEntity.ok(BatchResponse.fromEntity(batch));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List batches", description = "Lists batch disbursements for an account")
    public ResponseEntity<List<BatchResponse>> listBatches(
            @RequestParam UUID sourceAccountId,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset) {

        List<BatchDisbursement> batches = batchUseCase.listBatchesByAccount(
                sourceAccountId, limit, offset);

        List<BatchResponse> responses = batches.stream()
                .map(BatchResponse::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }
}
