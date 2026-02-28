package id.payu.dispute.adapter.web;

import id.payu.dispute.domain.model.Refund;
import id.payu.dispute.domain.port.in.RefundUseCase;
import id.payu.dispute.dto.*;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST Controller for Refund operations.
 *
 * <p>Provides endpoints for creating and managing refunds.</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/refunds")
@RequiredArgsConstructor
@Tag(name = "Refunds", description = "Refund management endpoints")
public class RefundController {

    private final RefundUseCase refundUseCase;

    @PostMapping("/full")
    @Operation(summary = "Create a full refund", description = "Creates a full refund for a transaction")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Refund created successfully",
                    content = @Content(schema = @Schema(implementation = RefundResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Transaction not found")
    })
    public ResponseEntity<RefundResponse> createFullRefund(
            @Valid @RequestBody CreateFullRefundRequest request) {
        log.info("Creating full refund for transaction: {}", request.getTransactionId());
        Refund refund = refundUseCase.createFullRefund(
                request.getTransactionId(),
                request.getReason()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(refund));
    }

    @PostMapping("/partial")
    @Operation(summary = "Create a partial refund", description = "Creates a partial refund for a transaction")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Refund created successfully",
                    content = @Content(schema = @Schema(implementation = RefundResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Transaction not found")
    })
    public ResponseEntity<RefundResponse> createPartialRefund(
            @Valid @RequestBody CreatePartialRefundRequest request) {
        log.info("Creating partial refund for transaction: {} with amount: {} {}",
                request.getTransactionId(), request.getAmount(), request.getCurrency());
        Refund refund = refundUseCase.createPartialRefund(
                request.getTransactionId(),
                request.getAmount(),
                request.getCurrency(),
                request.getReason()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(refund));
    }

    @PostMapping("/{refundId}/process")
    @Operation(summary = "Process a refund", description = "Transitions refund from PENDING to PROCESSING")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Refund processed successfully",
                    content = @Content(schema = @Schema(implementation = RefundResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid state transition"),
            @ApiResponse(responseCode = "404", description = "Refund not found")
    })
    public ResponseEntity<RefundResponse> processRefund(
            @Parameter(description = "Refund ID") @PathVariable UUID refundId) {
        log.info("Processing refund: {}", refundId);
        Refund refund = refundUseCase.processRefund(refundId);
        return ResponseEntity.ok(toResponse(refund));
    }

    @PostMapping("/{refundId}/complete")
    @Operation(summary = "Complete a refund", description = "Transitions refund from PROCESSING to COMPLETED")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Refund completed successfully",
                    content = @Content(schema = @Schema(implementation = RefundResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid state transition"),
            @ApiResponse(responseCode = "404", description = "Refund not found")
    })
    public ResponseEntity<RefundResponse> completeRefund(
            @Parameter(description = "Refund ID") @PathVariable UUID refundId) {
        log.info("Completing refund: {}", refundId);
        Refund refund = refundUseCase.completeRefund(refundId);
        return ResponseEntity.ok(toResponse(refund));
    }

    @PostMapping("/{refundId}/fail")
    @Operation(summary = "Fail a refund", description = "Transitions refund from PROCESSING to FAILED")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Refund marked as failed",
                    content = @Content(schema = @Schema(implementation = RefundResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid state transition"),
            @ApiResponse(responseCode = "404", description = "Refund not found")
    })
    public ResponseEntity<RefundResponse> failRefund(
            @Parameter(description = "Refund ID") @PathVariable UUID refundId,
            @Valid @RequestBody FailRefundRequest request) {
        log.info("Failing refund: {} with reason: {}", refundId, request.getFailureReason());
        Refund refund = refundUseCase.failRefund(refundId, request.getFailureReason());
        return ResponseEntity.ok(toResponse(refund));
    }

    @PostMapping("/{refundId}/cancel")
    @Operation(summary = "Cancel a refund", description = "Transitions refund from PENDING to CANCELLED")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Refund cancelled successfully",
                    content = @Content(schema = @Schema(implementation = RefundResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid state transition"),
            @ApiResponse(responseCode = "404", description = "Refund not found")
    })
    public ResponseEntity<RefundResponse> cancelRefund(
            @Parameter(description = "Refund ID") @PathVariable UUID refundId,
            @Valid @RequestBody CancelRefundRequest request) {
        log.info("Cancelling refund: {} with reason: {}", refundId, request.getCancellationReason());
        Refund refund = refundUseCase.cancelRefund(refundId, request.getCancellationReason());
        return ResponseEntity.ok(toResponse(refund));
    }

    @GetMapping("/{refundId}")
    @Operation(summary = "Get refund by ID", description = "Retrieves a refund by its ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Refund found",
                    content = @Content(schema = @Schema(implementation = RefundResponse.class))),
            @ApiResponse(responseCode = "404", description = "Refund not found")
    })
    public ResponseEntity<RefundResponse> getRefund(
            @Parameter(description = "Refund ID") @PathVariable UUID refundId) {
        log.debug("Getting refund: {}", refundId);
        return refundUseCase.getRefund(refundId)
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/transaction/{transactionId}")
    @Operation(summary = "Get refunds by transaction", description = "Retrieves all refunds for a transaction")
    @ApiResponse(responseCode = "200", description = "List of refunds",
            content = @Content(schema = @Schema(implementation = RefundListResponse.class)))
    public ResponseEntity<RefundListResponse> getRefundsByTransaction(
            @Parameter(description = "Transaction ID") @PathVariable UUID transactionId) {
        log.debug("Getting refunds for transaction: {}", transactionId);
        List<Refund> refunds = refundUseCase.getRefundsByTransaction(transactionId);
        return ResponseEntity.ok(toListResponse(refunds));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get refunds by status", description = "Retrieves refunds filtered by status")
    @ApiResponse(responseCode = "200", description = "List of refunds",
            content = @Content(schema = @Schema(implementation = RefundListResponse.class)))
    public ResponseEntity<RefundListResponse> getRefundsByStatus(
            @Parameter(description = "Refund status") @PathVariable String status) {
        log.debug("Getting refunds by status: {}", status);
        List<Refund> refunds = refundUseCase.getRefundsByStatus(status);
        return ResponseEntity.ok(toListResponse(refunds));
    }

    private RefundResponse toResponse(Refund refund) {
        return RefundResponse.builder()
                .id(refund.getId())
                .transactionId(refund.getTransactionId())
                .amount(refund.getAmount())
                .currency(refund.getCurrency())
                .reason(refund.getReason())
                .status(refund.getStatus().name())
                .failureReason(refund.getFailureReason())
                .createdAt(refund.getCreatedAt())
                .processedAt(refund.getProcessedAt())
                .completedAt(refund.getCompletedAt())
                .failedAt(refund.getFailedAt())
                .cancelledAt(refund.getCancelledAt())
                .build();
    }

    private RefundListResponse toListResponse(List<Refund> refunds) {
        List<RefundResponse> responses = refunds.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return RefundListResponse.builder()
                .refunds(responses)
                .total(responses.size())
                .build();
    }
}
