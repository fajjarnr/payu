package id.payu.wallet.adapter.web;

import id.payu.api.common.response.ApiResponse;
import id.payu.commons.idempotency.Idempotent;
import id.payu.wallet.domain.model.CalculatedSplit;
import id.payu.wallet.domain.model.RevenueSplit;
import id.payu.wallet.domain.model.SettlementBatch;
import id.payu.wallet.domain.port.in.SettlementUseCase;
import id.payu.wallet.interfaces.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import id.payu.wallet.domain.model.SplitType;

/**
 * REST Controller for Settlement operations (GAP-003, GAP-013).
 * Provides APIs for daily settlement, reconciliation, and revenue sharing.
 */
@RestController
@RequestMapping("/api/v1/settlements")
@Tag(name = "Settlement", description = "Settlement batch processing, reconciliation, and revenue sharing APIs")
@SecurityRequirement(name = "bearerAuth")
public class SettlementController extends BaseController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SettlementController.class);

    private final SettlementUseCase settlementUseCase;

    public SettlementController(SettlementUseCase settlementUseCase) {
        this.settlementUseCase = settlementUseCase;
    }

    @PostMapping("/batches")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BACKOFFICE')")
    @Idempotent(required = true, headerName = "X-Idempotency-Key")
    @Operation(summary = "Create settlement batch", description = "Create a new settlement batch for a partner")
    public ResponseEntity<ApiResponse<SettlementBatchResponse>> createSettlementBatch(
            @RequestBody CreateSettlementBatchRequest request) {
        log.info("Creating settlement batch for partner {} on date {}", request.getPartnerId(), request.getSettlementDate());

        SettlementBatch batch = settlementUseCase.createSettlementBatch(
                request.getPartnerId(), request.getSettlementDate(), request.getCurrency());

        SettlementBatchResponse response = toSettlementBatchResponse(batch);
        String location = "/api/v1/settlements/batches/" + batch.getId();
        return created(response, location);
    }

    @GetMapping("/batches/{batchId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BACKOFFICE')")
    @Operation(summary = "Get settlement batch", description = "Get settlement batch by ID")
    public ResponseEntity<ApiResponse<SettlementBatchResponse>> getSettlementBatch(
            @PathVariable UUID batchId) {
        SettlementBatch batch = settlementUseCase.getSettlementBatch(batchId);
        return ok(toSettlementBatchResponse(batch));
    }

    @GetMapping("/batches")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BACKOFFICE')")
    @Operation(summary = "List settlement batches", description = "Get settlement batches for a partner within date range")
    public ResponseEntity<ApiResponse<List<SettlementBatchResponse>>> listSettlementBatches(
            @RequestParam String partnerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        List<SettlementBatch> batches = settlementUseCase.getSettlementBatchesByPartner(partnerId, from, to);
        List<SettlementBatchResponse> responses = batches.stream()
                .map(this::toSettlementBatchResponse)
                .collect(Collectors.toList());
        return ok(responses);
    }

    @PostMapping("/batches/{batchId}/process")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BACKOFFICE')")
    @Idempotent(required = true, headerName = "X-Idempotency-Key")
    @Operation(summary = "Start processing settlement", description = "Start processing a settlement batch")
    public ResponseEntity<ApiResponse<SettlementBatchResponse>> startProcessing(
            @PathVariable UUID batchId,
            @RequestParam String processedBy) {
        SettlementBatch batch = settlementUseCase.startProcessing(batchId, processedBy);
        return ok(toSettlementBatchResponse(batch));
    }

    @PostMapping("/batches/{batchId}/complete")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BACKOFFICE')")
    @Idempotent(required = true, headerName = "X-Idempotency-Key")
    @Operation(summary = "Complete settlement", description = "Mark settlement batch as completed")
    public ResponseEntity<ApiResponse<SettlementBatchResponse>> completeSettlement(
            @PathVariable UUID batchId) {
        SettlementBatch batch = settlementUseCase.completeSettlement(batchId);
        return ok(toSettlementBatchResponse(batch));
    }

    @PostMapping("/batches/{batchId}/fail")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BACKOFFICE')")
    @Idempotent(required = true, headerName = "X-Idempotency-Key")
    @Operation(summary = "Fail settlement", description = "Mark settlement batch as failed")
    public ResponseEntity<ApiResponse<SettlementBatchResponse>> failSettlement(
            @PathVariable UUID batchId,
            @RequestParam String reason) {
        SettlementBatch batch = settlementUseCase.failSettlement(batchId, reason);
        return ok(toSettlementBatchResponse(batch));
    }

    @PostMapping("/batches/{batchId}/override")
    @PreAuthorize("hasRole('ADMIN')")
    @Idempotent(required = true, headerName = "X-Idempotency-Key")
    @Operation(summary = "Manual override settlement", description = "Manually override a failed settlement")
    public ResponseEntity<ApiResponse<SettlementBatchResponse>> manualOverride(
            @PathVariable UUID batchId,
            @RequestBody ManualOverrideRequest request) {
        SettlementBatch batch = settlementUseCase.manualOverride(batchId, request.getReason(), request.getOverriddenBy());
        return ok(toSettlementBatchResponse(batch));
    }

    @GetMapping("/batches/{batchId}/report")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BACKOFFICE')")
    @Operation(summary = "Get reconciliation report", description = "Generate reconciliation report for a settlement batch")
    public ResponseEntity<ApiResponse<String>> getReconciliationReport(
            @PathVariable UUID batchId) {
        String report = settlementUseCase.generateReconciliationReport(batchId);
        return ok(report);
    }

    @PostMapping("/batches/{batchId}/discrepancies/detect")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BACKOFFICE')")
    @Idempotent(required = true, headerName = "X-Idempotency-Key")
    @Operation(summary = "Detect discrepancies", description = "Run discrepancy detection on settlement batch")
    public ResponseEntity<ApiResponse<SettlementBatchResponse>> detectDiscrepancies(
            @PathVariable UUID batchId) {
        SettlementBatch batch = settlementUseCase.detectDiscrepancies(batchId);
        return ok(toSettlementBatchResponse(batch));
    }

    // Revenue Split endpoints

    @PostMapping("/revenue-splits")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BACKOFFICE')")
    @Idempotent(required = true, headerName = "X-Idempotency-Key")
    @Operation(summary = "Create revenue split", description = "Create a new revenue split configuration")
    public ResponseEntity<ApiResponse<RevenueSplitResponse>> createRevenueSplit(
            @RequestBody CreateRevenueSplitRequest request) {
        log.info("Creating revenue split '{}' for partner {}", request.getName(), request.getPartnerId());

        RevenueSplit split = settlementUseCase.createRevenueSplit(
                request.getPartnerId(), request.getName(), request.getDescription(),
                SplitType.valueOf(request.getSplitType()), request.getCreatedBy());

        RevenueSplitResponse response = toRevenueSplitResponse(split);
        String location = "/api/v1/settlements/revenue-splits/" + split.getId();
        return created(response, location);
    }

    @GetMapping("/revenue-splits/{splitId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BACKOFFICE')")
    @Operation(summary = "Get revenue split", description = "Get revenue split by ID")
    public ResponseEntity<ApiResponse<RevenueSplitResponse>> getRevenueSplit(
            @PathVariable UUID splitId) {
        RevenueSplit split = settlementUseCase.getRevenueSplit(splitId);
        return ok(toRevenueSplitResponse(split));
    }

    @GetMapping("/revenue-splits")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BACKOFFICE')")
    @Operation(summary = "List revenue splits", description = "Get revenue splits for a partner")
    public ResponseEntity<ApiResponse<List<RevenueSplitResponse>>> listRevenueSplits(
            @RequestParam String partnerId) {
        List<RevenueSplit> splits = settlementUseCase.getRevenueSplitsByPartner(partnerId);
        List<RevenueSplitResponse> responses = splits.stream()
                .map(this::toRevenueSplitResponse)
                .collect(Collectors.toList());
        return ok(responses);
    }

    @PostMapping("/revenue-splits/{splitId}/stakeholders")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BACKOFFICE')")
    @Idempotent(required = true, headerName = "X-Idempotency-Key")
    @Operation(summary = "Add stakeholder", description = "Add a stakeholder to a revenue split")
    public ResponseEntity<ApiResponse<RevenueSplitResponse>> addStakeholder(
            @PathVariable UUID splitId,
            @RequestBody AddStakeholderRequest request) {
        RevenueSplit split = settlementUseCase.addStakeholder(
                splitId, request.getAccountId(), request.getName(),
                request.getPercentage(), request.getFixedAmount(), request.getPriority());
        return ok(toRevenueSplitResponse(split));
    }

    @PostMapping("/revenue-splits/{splitId}/deactivate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BACKOFFICE')")
    @Idempotent(required = true, headerName = "X-Idempotency-Key")
    @Operation(summary = "Deactivate revenue split", description = "Deactivate a revenue split configuration")
    public ResponseEntity<ApiResponse<RevenueSplitResponse>> deactivateRevenueSplit(
            @PathVariable UUID splitId) {
        RevenueSplit split = settlementUseCase.deactivateRevenueSplit(splitId);
        return ok(toRevenueSplitResponse(split));
    }

    @PostMapping("/revenue-splits/{splitId}/calculate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BACKOFFICE')")
    @Idempotent(required = true, headerName = "X-Idempotency-Key")
    @Operation(summary = "Calculate splits", description = "Calculate revenue splits for a given amount")
    public ResponseEntity<ApiResponse<List<CalculatedSplitResponse>>> calculateSplits(
            @PathVariable UUID splitId,
            @RequestParam BigDecimal amount) {
        List<CalculatedSplit> splits = settlementUseCase.calculateRevenueSplits(splitId, amount);
        List<CalculatedSplitResponse> responses = splits.stream()
                .map(this::toCalculatedSplitResponse)
                .collect(Collectors.toList());
        return ok(responses);
    }

    @GetMapping("/royalty-statement")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BACKOFFICE')")
    @Operation(summary = "Generate royalty statement", description = "Generate monthly royalty statement for a stakeholder")
    public ResponseEntity<ApiResponse<String>> generateRoyaltyStatement(
            @RequestParam String partnerId,
            @RequestParam String accountId,
            @RequestParam int year,
            @RequestParam int month) {
        String statement = settlementUseCase.generateRoyaltyStatement(partnerId, accountId, year, month);
        return ok(statement);
    }

    // DTO mappers

    private SettlementBatchResponse toSettlementBatchResponse(SettlementBatch batch) {
        SettlementBatchResponse response = new SettlementBatchResponse();
        response.setId(batch.getId());
        response.setPartnerId(batch.getPartnerId());
        response.setSettlementDate(batch.getSettlementDate());
        response.setCurrency(batch.getCurrency());
        response.setTotalAmount(batch.getTotalAmount());
        response.setFeeAmount(batch.getFeeAmount());
        response.setNetAmount(batch.getNetAmount());
        response.setStatus(batch.getStatus().name());
        response.setEntryCount(batch.getEntries() != null ? batch.getEntries().size() : 0);
        response.setHasDiscrepancies(batch.hasDiscrepancies());
        response.setFailureReason(batch.getFailureReason());
        response.setProcessedBy(batch.getProcessedBy());
        response.setProcessedAt(batch.getProcessedAt());
        response.setCreatedAt(batch.getCreatedAt());
        return response;
    }

    private RevenueSplitResponse toRevenueSplitResponse(RevenueSplit split) {
        RevenueSplitResponse response = new RevenueSplitResponse();
        response.setId(split.getId());
        response.setPartnerId(split.getPartnerId());
        response.setName(split.getName());
        response.setDescription(split.getDescription());
        response.setSplitType(split.getSplitType().name());
        response.setActive(split.isActive());
        response.setEffectiveFrom(split.getEffectiveFrom());
        response.setEffectiveUntil(split.getEffectiveUntil());
        response.setCreatedBy(split.getCreatedBy());
        response.setCreatedAt(split.getCreatedAt());

        if (split.getStakeholders() != null) {
            response.setStakeholders(split.getStakeholders().stream()
                    .map(s -> {
                        StakeholderResponse sr = new StakeholderResponse();
                        sr.setId(s.getId());
                        sr.setAccountId(s.getAccountId());
                        sr.setName(s.getName());
                        sr.setPercentage(s.getPercentage());
                        sr.setFixedAmount(s.getFixedAmount());
                        sr.setPriority(s.getPriority());
                        return sr;
                    })
                    .collect(Collectors.toList()));
        }

        return response;
    }

    private CalculatedSplitResponse toCalculatedSplitResponse(CalculatedSplit split) {
        CalculatedSplitResponse response = new CalculatedSplitResponse();
        response.setStakeholderId(split.getStakeholderId());
        response.setAccountId(split.getAccountId());
        response.setName(split.getName());
        response.setAmount(split.getAmount());
        return response;
    }
}
