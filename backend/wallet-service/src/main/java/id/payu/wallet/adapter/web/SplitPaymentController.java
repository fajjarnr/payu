package id.payu.wallet.adapter.web;

import id.payu.api.common.response.ApiResponse;
import id.payu.commons.idempotency.Idempotent;
import id.payu.wallet.domain.model.SplitPaymentExecution;
import id.payu.wallet.domain.model.SplitPaymentRule;
import id.payu.wallet.domain.model.SplitRecipient;
import id.payu.wallet.domain.port.in.SplitPaymentUseCase;
import id.payu.wallet.dto.CreateSplitPaymentRuleRequest;
import id.payu.wallet.dto.ExecuteSplitPaymentRequest;
import id.payu.wallet.dto.SplitPaymentExecutionResponse;
import id.payu.wallet.dto.SplitPaymentRuleResponse;
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
 * REST Controller for split payment operations.
 * Supports multi-merchant payment splitting for TokoBapak, Nobar, and other marketplace partners.
 */
@RestController
@RequestMapping("/api/v1/split-payments")
@Tag(name = "Split Payments", description = "Split Payment APIs for marketplace multi-merchant disbursement")
@SecurityRequirement(name = "bearerAuth")
public class SplitPaymentController extends BaseController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SplitPaymentController.class);

    private final SplitPaymentUseCase splitPaymentUseCase;

    public SplitPaymentController(SplitPaymentUseCase splitPaymentUseCase) {
        this.splitPaymentUseCase = splitPaymentUseCase;
    }

    // ─── Rule Management ───────────────────────────────────────────

    @PostMapping("/rules")
    @PreAuthorize("hasAnyRole('ADMIN', 'PARTNER', 'SYSTEM')")
    @Idempotent(required = true)
    @Audited(level = AuditLevel.INFO)
    @Operation(summary = "Create split payment rule",
            description = "Creates a reusable split payment rule defining how payments are distributed among recipients")
    public ResponseEntity<ApiResponse<SplitPaymentRuleResponse>> createRule(
            @Valid @RequestBody CreateSplitPaymentRuleRequest request) {
        log.info("Creating split payment rule: partner={}, name={}", maskId(request.getPartnerId()),
                request.getRuleName());

        List<SplitRecipient> recipients = request.getRecipients().stream()
                .map(dto -> SplitRecipient.builder()
                        .recipientAccountId(dto.getRecipientAccountId())
                        .recipientLabel(dto.getRecipientLabel())
                        .type(dto.getType() != null ? SplitRecipient.RecipientType.valueOf(dto.getType()) : null)
                        .percentage(dto.getPercentage())
                        .fixedAmount(dto.getFixedAmount())
                        .priority(dto.getPriority())
                        .build())
                .collect(Collectors.toList());

        SplitPaymentRule rule = splitPaymentUseCase.createRule(
                request.getPartnerId(),
                request.getRuleName(),
                SplitPaymentRule.SplitType.valueOf(request.getSplitType()),
                request.getCurrency() != null ? request.getCurrency() : "IDR",
                recipients);

        SplitPaymentRuleResponse response = SplitPaymentRuleResponse.from(rule);
        return created(response, "/api/v1/split-payments/rules/" + rule.getId());
    }

    @GetMapping("/rules/{ruleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PARTNER', 'SYSTEM')")
    @Operation(summary = "Get split payment rule by ID")
    public ResponseEntity<ApiResponse<SplitPaymentRuleResponse>> getRule(
            @PathVariable UUID ruleId) {
        SplitPaymentRule rule = splitPaymentUseCase.getRule(ruleId);
        return ok(SplitPaymentRuleResponse.from(rule));
    }

    @GetMapping("/rules/partner/{partnerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PARTNER', 'SYSTEM')")
    @Operation(summary = "Get split payment rules by partner")
    public ResponseEntity<ApiResponse<List<SplitPaymentRuleResponse>>> getRulesByPartner(
            @PathVariable String partnerId) {
        List<SplitPaymentRule> rules = splitPaymentUseCase.getRulesByPartner(partnerId);
        List<SplitPaymentRuleResponse> responses = rules.stream()
                .map(SplitPaymentRuleResponse::from)
                .collect(Collectors.toList());
        return ok(responses);
    }

    @DeleteMapping("/rules/{ruleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PARTNER', 'SYSTEM')")
    @Audited(level = AuditLevel.INFO)
    @Operation(summary = "Deactivate split payment rule",
            description = "Soft-deletes a split payment rule — existing executions are not affected")
    public ResponseEntity<Void> deactivateRule(
            @PathVariable UUID ruleId) {
        log.info("Deactivating split payment rule: id={}", ruleId);
        splitPaymentUseCase.deactivateRule(ruleId);
        return noContent();
    }

    // ─── Execution ─────────────────────────────────────────────────

    @PostMapping("/execute")
    @PreAuthorize("hasAnyRole('ADMIN', 'PARTNER', 'SYSTEM')")
    @Idempotent(required = true)
    @Audited(level = AuditLevel.INFO)
    @Operation(summary = "Execute split payment",
            description = "Debits payer and credits all recipients according to the split rule or ad-hoc recipients")
    public ResponseEntity<ApiResponse<SplitPaymentExecutionResponse>> executeSplit(
            @Valid @RequestBody ExecuteSplitPaymentRequest request) {
        log.info("Executing split payment: payer={}, amount={}", maskId(request.getPayerAccountId()),
                request.getTotalAmount());

        SplitPaymentExecution execution;

        if (request.getRuleId() != null && !request.getRuleId().isBlank()) {
            // Rule-based execution
            execution = splitPaymentUseCase.executeSplit(
                    UUID.fromString(request.getRuleId()),
                    request.getPayerAccountId(),
                    request.getTotalAmount(),
                    request.getExternalReferenceId(),
                    request.getDescription(),
                    request.getIdempotencyKey());
        } else {
            // Ad-hoc execution
            List<SplitRecipient> recipients = request.getRecipients().stream()
                    .map(dto -> SplitRecipient.builder()
                            .recipientAccountId(dto.getRecipientAccountId())
                            .recipientLabel(dto.getRecipientLabel())
                            .type(dto.getType() != null ? SplitRecipient.RecipientType.valueOf(dto.getType()) : null)
                            .percentage(dto.getPercentage())
                            .fixedAmount(dto.getFixedAmount())
                            .priority(dto.getPriority())
                            .build())
                    .collect(Collectors.toList());

            execution = splitPaymentUseCase.executeAdHocSplit(
                    request.getPayerAccountId(),
                    request.getPartnerId(),
                    request.getTotalAmount(),
                    request.getCurrency() != null ? request.getCurrency() : "IDR",
                    recipients,
                    request.getExternalReferenceId(),
                    request.getDescription(),
                    request.getIdempotencyKey());
        }

        SplitPaymentExecutionResponse response = SplitPaymentExecutionResponse.from(execution);
        return created(response, "/api/v1/split-payments/executions/" + execution.getId());
    }

    @GetMapping("/executions/{executionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PARTNER', 'SYSTEM')")
    @Operation(summary = "Get execution by ID")
    public ResponseEntity<ApiResponse<SplitPaymentExecutionResponse>> getExecution(
            @PathVariable UUID executionId) {
        SplitPaymentExecution execution = splitPaymentUseCase.getExecution(executionId);
        return ok(SplitPaymentExecutionResponse.from(execution));
    }

    @GetMapping("/executions/payer/{payerAccountId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PARTNER', 'SYSTEM')")
    @Operation(summary = "Get executions by payer account")
    public ResponseEntity<ApiResponse<List<SplitPaymentExecutionResponse>>> getExecutionsByPayer(
            @PathVariable String payerAccountId) {
        List<SplitPaymentExecution> executions = splitPaymentUseCase.getExecutionsByPayer(payerAccountId);
        List<SplitPaymentExecutionResponse> responses = executions.stream()
                .map(SplitPaymentExecutionResponse::from)
                .collect(Collectors.toList());
        return ok(responses);
    }

    @PostMapping("/executions/{executionId}/reverse")
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM')")
    @Idempotent(required = true)
    @Audited(level = AuditLevel.INFO)
    @Operation(summary = "Reverse a split payment",
            description = "Reverses a completed split payment — debits all recipients and credits payer")
    public ResponseEntity<ApiResponse<SplitPaymentExecutionResponse>> reverseExecution(
            @PathVariable UUID executionId,
            @RequestParam(required = false, defaultValue = "Manual reversal") String reason) {
        log.info("Reversing split payment execution: id={}", executionId);
        SplitPaymentExecution execution = splitPaymentUseCase.reverseExecution(executionId, reason);
        return ok(SplitPaymentExecutionResponse.from(execution));
    }

    private String maskId(String id) {
        if (id == null || id.length() < 8) return "***";
        return id.substring(0, 4) + "****" + id.substring(id.length() - 4);
    }
}
