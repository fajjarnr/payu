package id.payu.account.adapter.web;

import id.payu.account.application.service.BudgetService;
import id.payu.account.application.service.BudgetService.BudgetCheckResult;
import id.payu.account.application.service.BudgetService.BudgetStatusInfo;
import id.payu.account.domain.model.Budget;
import id.payu.account.domain.model.BudgetPeriod;
import id.payu.api.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for budget management.
 */
@RestController
@RequestMapping("/api/v1/accounts/{accountId}/budgets")
@Tag(name = "Budgets", description = "Budget management endpoints for spending control")
@SecurityRequirement(name = "bearerAuth")
public class BudgetController {

    private static final Logger log = LoggerFactory.getLogger(BudgetController.class);

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    /**
     * Create a new budget.
     */
    @PostMapping
    @PreAuthorize("hasRole('USER') and @accountSecurityService.isAccountOwner(#accountId, authentication)")
    @Operation(summary = "Create budget", description = "Creates a new spending budget for a specific category")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Budget created successfully",
            content = @Content(schema = @Schema(implementation = Budget.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - not account owner")
    })
    public ResponseEntity<ApiResponse<Budget>> createBudget(
            @Parameter(description = "Account ID", required = true) @PathVariable UUID accountId,
            @Valid @RequestBody CreateBudgetRequest request) {
        log.info("Creating budget for account={}", accountId);

        Budget budget = budgetService.createBudget(
                accountId,
                request.category(),
                request.limitAmount(),
                request.period()
        );

        return ResponseEntity.ok(ApiResponse.success("Budget created successfully", budget));
    }

    /**
     * Get all budgets for an account.
     */
    @GetMapping
    @PreAuthorize("hasRole('USER') and @accountSecurityService.isAccountOwner(#accountId, authentication)")
    @Operation(summary = "Get all budgets", description = "Returns all budgets for the specified account")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Budgets retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - not account owner")
    })
    public ResponseEntity<ApiResponse<List<Budget>>> getBudgets(
            @Parameter(description = "Account ID", required = true) @PathVariable UUID accountId) {
        log.debug("Getting budgets for account={}", accountId);

        List<Budget> budgets = budgetService.getUserBudgets(accountId);
        return ResponseEntity.ok(ApiResponse.success(budgets));
    }

    /**
     * Get a specific budget.
     */
    @GetMapping("/{budgetId}")
    @PreAuthorize("hasRole('USER') and @accountSecurityService.isAccountOwner(#accountId, authentication)")
    @Operation(summary = "Get budget by ID", description = "Returns a specific budget by its ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Budget found",
            content = @Content(schema = @Schema(implementation = Budget.class))),
        @ApiResponse(responseCode = "404", description = "Budget not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - not account owner")
    })
    public ResponseEntity<ApiResponse<Budget>> getBudget(
            @Parameter(description = "Account ID", required = true) @PathVariable UUID accountId,
            @Parameter(description = "Budget ID", required = true) @PathVariable UUID budgetId) {
        log.debug("Getting budget {} for account={}", budgetId, accountId);

        return budgetService.getBudget(budgetId)
                .map(budget -> ResponseEntity.ok(ApiResponse.success(budget)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update a budget.
     */
    @PutMapping("/{budgetId}")
    @PreAuthorize("hasRole('USER') and @accountSecurityService.isAccountOwner(#accountId, authentication)")
    @Operation(summary = "Update budget", description = "Updates an existing budget")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Budget updated successfully",
            content = @Content(schema = @Schema(implementation = Budget.class))),
        @ApiResponse(responseCode = "404", description = "Budget not found"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - not account owner")
    })
    public ResponseEntity<ApiResponse<Budget>> updateBudget(
            @Parameter(description = "Account ID", required = true) @PathVariable UUID accountId,
            @Parameter(description = "Budget ID", required = true) @PathVariable UUID budgetId,
            @Valid @RequestBody UpdateBudgetRequest request) {
        log.info("Updating budget {} for account={}", budgetId, accountId);

        Budget budget = budgetService.updateBudget(
                budgetId,
                request.limitAmount(),
                request.period(),
                request.active()
        );

        return ResponseEntity.ok(ApiResponse.success("Budget updated successfully", budget));
    }

    /**
     * Delete a budget.
     */
    @DeleteMapping("/{budgetId}")
    @PreAuthorize("hasRole('USER') and @accountSecurityService.isAccountOwner(#accountId, authentication)")
    @Operation(summary = "Delete budget", description = "Deletes a budget by its ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Budget deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Budget not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - not account owner")
    })
    public ResponseEntity<ApiResponse<Void>> deleteBudget(
            @Parameter(description = "Account ID", required = true) @PathVariable UUID accountId,
            @Parameter(description = "Budget ID", required = true) @PathVariable UUID budgetId) {
        log.info("Deleting budget {} for account={}", budgetId, accountId);

        budgetService.deleteBudget(budgetId);
        return ResponseEntity.ok(ApiResponse.success("Budget deleted successfully", null));
    }

    /**
     * Get budget status for all budgets.
     */
    @GetMapping("/status")
    @PreAuthorize("hasRole('USER') and @accountSecurityService.isAccountOwner(#accountId, authentication)")
    @Operation(summary = "Get budget status", description = "Returns the current status of all budgets for the account")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Budget status retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - not account owner")
    })
    public ResponseEntity<ApiResponse<List<BudgetStatusInfo>>> getBudgetStatus(
            @Parameter(description = "Account ID", required = true) @PathVariable UUID accountId) {
        log.debug("Getting budget status for account={}", accountId);

        List<BudgetStatusInfo> status = budgetService.getAllBudgetStatus(accountId);
        return ResponseEntity.ok(ApiResponse.success(status));
    }

    /**
     * Check if a transaction is allowed within budget.
     */
    @PostMapping("/check")
    @PreAuthorize("hasRole('USER') and @accountSecurityService.isAccountOwner(#accountId, authentication)")
    public ResponseEntity<ApiResponse<BudgetCheckResponse>> checkBudget(
            @PathVariable UUID accountId,
            @Valid @RequestBody CheckBudgetRequest request) {
        log.debug("Checking budget for account={}, category={}, amount={}",
                accountId, request.category(), request.amount());

        BudgetCheckResult result = budgetService.checkBudget(
                accountId,
                request.category(),
                request.amount()
        );

        BudgetCheckResponse response = new BudgetCheckResponse(
                result.status().name(),
                result.message(),
                result.budget() != null ? result.budget().getId() : null,
                result.budget() != null ? result.budget().getRemainingAmount() : null
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // Request/Response DTOs
    public record CreateBudgetRequest(
            @NotNull String category,
            @NotNull @Positive BigDecimal limitAmount,
            @NotNull BudgetPeriod period) {}

    public record UpdateBudgetRequest(
            BigDecimal limitAmount,
            BudgetPeriod period,
            Boolean active) {}

    public record CheckBudgetRequest(
            @NotNull String category,
            @NotNull @Positive BigDecimal amount) {}

    public record BudgetCheckResponse(
            String status,
            String message,
            UUID budgetId,
            BigDecimal remainingAmount) {}

    // Simple ApiResponse wrapper
    public record ApiResponse<T>(boolean success, String message, T data) {
        public static <T> ApiResponse<T> success(T data) {
            return new ApiResponse<>(true, "Success", data);
        }
        public static <T> ApiResponse<T> success(String message, T data) {
            return new ApiResponse<>(true, message, data);
        }
    }
}
