package id.payu.wallet.adapter.web;

import id.payu.api.common.response.ApiResponse;
import id.payu.commons.idempotency.Idempotent;
import id.payu.wallet.adapter.persistence.entity.SavingsGoalEntity;
import id.payu.wallet.adapter.persistence.repository.PocketJpaRepository;
import id.payu.wallet.adapter.persistence.repository.SavingsGoalJpaRepository;
import id.payu.wallet.dto.SavingsGoalRequest;
import id.payu.wallet.dto.SavingsGoalResponse;
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

import java.util.Objects;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import id.payu.wallet.adapter.persistence.entity.SavingsGoalStatus;

@RestController
@RequestMapping("/api/v1/wallets/{walletId}/savings-goals")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Savings Goals", description = "Savings goal management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class SavingsGoalController {

    private final SavingsGoalJpaRepository savingsGoalRepository;
    private final PocketJpaRepository pocketRepository;

    @GetMapping
    @Operation(summary = "Get all savings goals for wallet")
    @PreAuthorize("hasAuthority('read:wallet')")
    public ResponseEntity<ApiResponse<List<SavingsGoalResponse>>> getSavingsGoals(
            @Parameter(description = "Wallet ID", required = true)
            @PathVariable UUID walletId,
            @AuthenticationPrincipal Jwt jwt) {
        log.info("Getting savings goals for wallet: {}", walletId);

        // Ownership check: verify pocket belongs to authenticated user
        var pocket = pocketRepository.findById(walletId).orElse(null);
        if (pocket == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("WAL_001", "Wallet not found"));
        }
        String authenticatedAccountId = jwt.getSubject();
        if (!Objects.equals(pocket.getAccountId(), authenticatedAccountId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("SAV_403", "Not authorized to access savings goals for this wallet"));
        }

        List<SavingsGoalEntity> goals = savingsGoalRepository.findByPocketIdAndStatusNot(
                walletId, SavingsGoalStatus.CANCELLED);

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

        // Verify pocket exists
        var pocket = pocketRepository.findById(walletId).orElse(null);
        if (pocket == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("WAL_001", "Wallet not found"));
        }

        // Ownership check
        String authenticatedAccountId = jwt.getSubject();
        if (!Objects.equals(pocket.getAccountId(), authenticatedAccountId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("SAV_403", "Not authorized to create savings goals for this wallet"));
        }

        SavingsGoalEntity goal = new SavingsGoalEntity();
        goal.setPocketId(walletId);
        goal.setUserId(UUID.fromString(pocket.getAccountId())); // Using accountId as userId for simplicity
        goal.setName(request.getName());
        goal.setDescription(request.getDescription());
        goal.setTargetAmount(request.getTargetAmount());
        goal.setCurrentAmount(BigDecimal.ZERO);
        goal.setCurrency(pocket.getCurrency());
        goal.setDeadline(request.getDeadline());
        goal.setStatus(SavingsGoalStatus.ACTIVE);
        goal.setIcon(request.getIcon());
        goal.setColor(request.getColor());

        SavingsGoalEntity saved = savingsGoalRepository.save(goal);
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

        // Ownership check
        var pocket = pocketRepository.findById(walletId).orElse(null);
        if (pocket == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("WAL_001", "Wallet not found"));
        }
        String authenticatedAccountId = jwt.getSubject();
        if (!Objects.equals(pocket.getAccountId(), authenticatedAccountId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("SAV_403", "Not authorized to update savings goals for this wallet"));
        }

        SavingsGoalEntity goal = savingsGoalRepository.findById(goalId).orElse(null);
        if (goal == null || !Objects.equals(goal.getPocketId(), walletId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("SAV_001", "Savings goal not found"));
        }

        goal.setName(request.getName());
        goal.setDescription(request.getDescription());
        goal.setTargetAmount(request.getTargetAmount());
        goal.setDeadline(request.getDeadline());
        goal.setIcon(request.getIcon());
        goal.setColor(request.getColor());
        goal.setUpdatedAt(LocalDateTime.now());

        SavingsGoalEntity saved = savingsGoalRepository.save(goal);
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

        // Ownership check
        var pocket = pocketRepository.findById(walletId).orElse(null);
        if (pocket == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("WAL_001", "Wallet not found"));
        }
        String authenticatedAccountId = jwt.getSubject();
        if (!Objects.equals(pocket.getAccountId(), authenticatedAccountId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("SAV_403", "Not authorized to delete savings goals for this wallet"));
        }

        SavingsGoalEntity goal = savingsGoalRepository.findById(goalId).orElse(null);
        if (goal == null || !Objects.equals(goal.getPocketId(), walletId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("SAV_001", "Savings goal not found"));
        }

        goal.setStatus(SavingsGoalStatus.CANCELLED);
        goal.setUpdatedAt(LocalDateTime.now());
        savingsGoalRepository.save(goal);

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{goalId}/pause")
    @Idempotent(required = false)
    @Operation(summary = "Pause a savings goal")
    @PreAuthorize("hasAuthority('write:wallet')")
    public ResponseEntity<ApiResponse<SavingsGoalResponse>> pauseSavingsGoal(
            @Parameter(description = "Wallet ID", required = true)
            @PathVariable UUID walletId,
            @Parameter(description = "Goal ID", required = true)
            @PathVariable UUID goalId,
            @AuthenticationPrincipal Jwt jwt) {
        log.info("Pausing savings goal: {} for wallet: {}", goalId, walletId);

        // Ownership check
        var pocket = pocketRepository.findById(walletId).orElse(null);
        if (pocket == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("WAL_001", "Wallet not found"));
        }
        String authenticatedAccountId = jwt.getSubject();
        if (!Objects.equals(pocket.getAccountId(), authenticatedAccountId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("SAV_403", "Not authorized to pause savings goals for this wallet"));
        }

        SavingsGoalEntity goal = savingsGoalRepository.findById(goalId).orElse(null);
        if (goal == null || !Objects.equals(goal.getPocketId(), walletId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("SAV_001", "Savings goal not found"));
        }

        if (goal.getStatus() == SavingsGoalStatus.ACTIVE) {
            goal.setStatus(SavingsGoalStatus.PAUSED);
            goal.setUpdatedAt(LocalDateTime.now());
            savingsGoalRepository.save(goal);
        }

        return ResponseEntity.ok(ApiResponse.success(toResponse(goal)));
    }

    @PostMapping("/{goalId}/resume")
    @Idempotent(required = false)
    @Operation(summary = "Resume a paused savings goal")
    @PreAuthorize("hasAuthority('write:wallet')")
    public ResponseEntity<ApiResponse<SavingsGoalResponse>> resumeSavingsGoal(
            @Parameter(description = "Wallet ID", required = true)
            @PathVariable UUID walletId,
            @Parameter(description = "Goal ID", required = true)
            @PathVariable UUID goalId,
            @AuthenticationPrincipal Jwt jwt) {
        log.info("Resuming savings goal: {} for wallet: {}", goalId, walletId);

        // Ownership check
        var pocket = pocketRepository.findById(walletId).orElse(null);
        if (pocket == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("WAL_001", "Wallet not found"));
        }
        String authenticatedAccountId = jwt.getSubject();
        if (!Objects.equals(pocket.getAccountId(), authenticatedAccountId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("SAV_403", "Not authorized to resume savings goals for this wallet"));
        }

        SavingsGoalEntity goal = savingsGoalRepository.findById(goalId).orElse(null);
        if (goal == null || !Objects.equals(goal.getPocketId(), walletId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("SAV_001", "Savings goal not found"));
        }

        if (goal.getStatus() == SavingsGoalStatus.PAUSED) {
            goal.setStatus(SavingsGoalStatus.ACTIVE);
            goal.setUpdatedAt(LocalDateTime.now());
            savingsGoalRepository.save(goal);
        }

        return ResponseEntity.ok(ApiResponse.success(toResponse(goal)));
    }

    private SavingsGoalResponse toResponse(SavingsGoalEntity entity) {
        if (entity == null) {
            return null;
        }

        BigDecimal progressPercentage = BigDecimal.ZERO;
        if (entity.getTargetAmount() != null && entity.getTargetAmount().compareTo(BigDecimal.ZERO) > 0) {
            progressPercentage = entity.getCurrentAmount()
                    .multiply(BigDecimal.valueOf(100))
                    .divide(entity.getTargetAmount(), 2, java.math.RoundingMode.HALF_EVEN);
        }

        return SavingsGoalResponse.builder()
                .id(entity.getId())
                .pocketId(entity.getPocketId())
                .name(entity.getName())
                .description(entity.getDescription())
                .targetAmount(entity.getTargetAmount())
                .currentAmount(entity.getCurrentAmount())
                .progressPercentage(progressPercentage)
                .currency(entity.getCurrency())
                .deadline(entity.getDeadline())
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .icon(entity.getIcon())
                .color(entity.getColor())
                .createdAt(entity.getCreatedAt())
                .completedAt(entity.getCompletedAt())
                .build();
    }
}
