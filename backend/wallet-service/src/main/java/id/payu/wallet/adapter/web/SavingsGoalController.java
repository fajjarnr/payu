package id.payu.wallet.adapter.web;

import id.payu.api.common.response.ApiResponse;
import id.payu.commons.idempotency.Idempotent;
import id.payu.wallet.domain.model.SavingsGoal;
import id.payu.wallet.domain.port.in.SavingsGoalUseCase;
import id.payu.wallet.interfaces.dto.SavingsGoalRequest;
import id.payu.wallet.interfaces.dto.SavingsGoalResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/wallets/{walletId}/savings-goals")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Savings Goals", description = "Savings goal management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class SavingsGoalController {

    private final SavingsGoalUseCase savingsGoalUseCase;

    @GetMapping
    @Operation(summary = "Get all savings goals for wallet")
    @PreAuthorize("hasAuthority('read:wallet')")
    public ResponseEntity<ApiResponse<List<SavingsGoalResponse>>> getSavingsGoals(
            @Parameter(description = "Wallet ID", required = true)
            @PathVariable UUID walletId,
            @AuthenticationPrincipal Jwt jwt) {
        log.info("Getting savings goals for wallet: {}", walletId);

        List<SavingsGoal> goals = savingsGoalUseCase.getSavingsGoals(walletId, jwt.getSubject());
        List<SavingsGoalResponse> responses = goals.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @PostMapping
    @Idempotent(required = true)
    @Operation(summary = "Create a new savings goal")
    @PreAuthorize("hasAuthority('write:wallet')")
    public ResponseEntity<ApiResponse<SavingsGoalResponse>> createSavingsGoal(
            @Parameter(description = "Wallet ID", required = true)
            @PathVariable UUID walletId,
            @Valid @RequestBody SavingsGoalRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        log.info("Creating savings goal for wallet: {}", walletId);

        SavingsGoal saved = savingsGoalUseCase.createSavingsGoal(
                walletId,
                jwt.getSubject(),
                request.getName(),
                request.getDescription(),
                request.getTargetAmount(),
                request.getDeadline(),
                request.getIcon(),
                request.getColor()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(toResponse(saved)));
    }

    @PutMapping("/{goalId}")
    @Idempotent(required = true)
    @Operation(summary = "Update savings goal")
    @PreAuthorize("hasAuthority('write:wallet')")
    public ResponseEntity<ApiResponse<SavingsGoalResponse>> updateSavingsGoal(
            @Parameter(description = "Wallet ID", required = true)
            @PathVariable UUID walletId,
            @Parameter(description = "Goal ID", required = true)
            @PathVariable UUID goalId,
            @Valid @RequestBody SavingsGoalRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        log.info("Updating savings goal: {} for wallet: {}", goalId, walletId);

        SavingsGoal saved = savingsGoalUseCase.updateSavingsGoal(
                walletId,
                goalId,
                jwt.getSubject(),
                request.getName(),
                request.getDescription(),
                request.getTargetAmount(),
                request.getDeadline(),
                request.getIcon(),
                request.getColor()
        );

        return ResponseEntity.ok(ApiResponse.success(toResponse(saved)));
    }

    @DeleteMapping("/{goalId}")
    @Operation(summary = "Delete (cancel) a savings goal")
    @PreAuthorize("hasAuthority('write:wallet')")
    public ResponseEntity<ApiResponse<Void>> deleteSavingsGoal(
            @Parameter(description = "Wallet ID", required = true)
            @PathVariable UUID walletId,
            @Parameter(description = "Goal ID", required = true)
            @PathVariable UUID goalId,
            @AuthenticationPrincipal Jwt jwt) {
        log.info("Deleting savings goal: {} for wallet: {}", goalId, walletId);

        savingsGoalUseCase.deleteSavingsGoal(walletId, goalId, jwt.getSubject());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{goalId}/pause")
    @Idempotent(required = true)
    @Operation(summary = "Pause a savings goal")
    @PreAuthorize("hasAuthority('write:wallet')")
    public ResponseEntity<ApiResponse<SavingsGoalResponse>> pauseSavingsGoal(
            @Parameter(description = "Wallet ID", required = true)
            @PathVariable UUID walletId,
            @Parameter(description = "Goal ID", required = true)
            @PathVariable UUID goalId,
            @AuthenticationPrincipal Jwt jwt) {
        log.info("Pausing savings goal: {} for wallet: {}", goalId, walletId);

        SavingsGoal goal = savingsGoalUseCase.pauseSavingsGoal(walletId, goalId, jwt.getSubject());
        return ResponseEntity.ok(ApiResponse.success(toResponse(goal)));
    }

    @PostMapping("/{goalId}/resume")
    @Idempotent(required = true)
    @Operation(summary = "Resume a paused savings goal")
    @PreAuthorize("hasAuthority('write:wallet')")
    public ResponseEntity<ApiResponse<SavingsGoalResponse>> resumeSavingsGoal(
            @Parameter(description = "Wallet ID", required = true)
            @PathVariable UUID walletId,
            @Parameter(description = "Goal ID", required = true)
            @PathVariable UUID goalId,
            @AuthenticationPrincipal Jwt jwt) {
        log.info("Resuming savings goal: {} for wallet: {}", goalId, walletId);

        SavingsGoal goal = savingsGoalUseCase.resumeSavingsGoal(walletId, goalId, jwt.getSubject());
        return ResponseEntity.ok(ApiResponse.success(toResponse(goal)));
    }

    private SavingsGoalResponse toResponse(SavingsGoal domain) {
        if (domain == null) {
            return null;
        }

        BigDecimal progressPercentage = BigDecimal.ZERO;
        if (domain.getTargetAmount() != null && domain.getTargetAmount().compareTo(BigDecimal.ZERO) > 0) {
            progressPercentage = domain.getCurrentAmount()
                    .multiply(BigDecimal.valueOf(100))
                    .divide(domain.getTargetAmount(), 2, java.math.RoundingMode.HALF_EVEN);
        }

        return SavingsGoalResponse.builder()
                .id(domain.getId())
                .pocketId(domain.getPocketId())
                .name(domain.getName())
                .description(domain.getDescription())
                .targetAmount(domain.getTargetAmount())
                .currentAmount(domain.getCurrentAmount())
                .progressPercentage(progressPercentage)
                .currency(domain.getCurrency())
                .deadline(domain.getDeadline())
                .status(domain.getStatus() != null ? domain.getStatus().name() : null)
                .icon(domain.getIcon())
                .color(domain.getColor())
                .createdAt(domain.getCreatedAt())
                .completedAt(domain.getCompletedAt())
                .build();
    }
}
