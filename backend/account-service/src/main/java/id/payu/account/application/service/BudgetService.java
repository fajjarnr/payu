package id.payu.account.application.service;

import id.payu.account.domain.model.Budget;
import id.payu.account.domain.model.BudgetPeriod;
import id.payu.account.domain.model.BudgetStatus;
import id.payu.account.domain.port.out.BudgetRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Application service for budget management.
 * Provides CRUD operations and budget checking functionality.
 */
@Service
public class BudgetService {

    private static final Logger log = LoggerFactory.getLogger(BudgetService.class);

    private final BudgetRepositoryPort budgetRepository;

    public BudgetService(BudgetRepositoryPort budgetRepository) {
        this.budgetRepository = budgetRepository;
    }

    /**
     * Create a new budget for a user.
     *
     * @param userId the user ID
     * @param category the spending category
     * @param limitAmount the budget limit
     * @param period the budget period
     * @return the created budget
     */
    @Transactional
    public Budget createBudget(UUID userId, String category, BigDecimal limitAmount, BudgetPeriod period) {
        log.info("Creating budget for user={}, category={}, limit={}, period={}",
                userId, category, limitAmount, period);

        if (budgetRepository.existsByUserIdAndCategory(userId, category)) {
            throw new IllegalArgumentException(
                    "Budget already exists for user and category: " + category);
        }

        Budget budget = Budget.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .category(category)
                .limitAmount(limitAmount)
                .period(period)
                .currentSpent(BigDecimal.ZERO)
                .resetDate(calculateInitialResetDate(period))
                .active(true)
                .build();

        Budget saved = budgetRepository.save(budget);
        log.info("Created budget with id={}", saved.getId());
        return saved;
    }

    /**
     * Get all budgets for a user.
     *
     * @param userId the user ID
     * @return list of budgets
     */
    @Transactional(readOnly = true)
    public List<Budget> getUserBudgets(UUID userId) {
        return budgetRepository.findByUserId(userId);
    }

    /**
     * Get active budgets for a user.
     *
     * @param userId the user ID
     * @return list of active budgets
     */
    @Transactional(readOnly = true)
    public List<Budget> getActiveBudgets(UUID userId) {
        return budgetRepository.findActiveByUserId(userId);
    }

    /**
     * Get a budget by ID.
     *
     * @param budgetId the budget ID
     * @return optional budget
     */
    @Transactional(readOnly = true)
    public Optional<Budget> getBudget(UUID budgetId) {
        return budgetRepository.findById(budgetId);
    }

    /**
     * Update a budget.
     *
     * @param budgetId the budget ID
     * @param newLimit the new limit amount (optional)
     * @param newPeriod the new period (optional)
     * @param active the active status (optional)
     * @return the updated budget
     */
    @Transactional
    public Budget updateBudget(UUID budgetId, BigDecimal newLimit,
                               BudgetPeriod newPeriod, Boolean active) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new IllegalArgumentException("Budget not found: " + budgetId));

        if (newLimit != null) {
            budget.updateLimit(newLimit);
        }
        if (newPeriod != null) {
            budget.setPeriod(newPeriod);
            budget.setResetDate(calculateInitialResetDate(newPeriod));
        }
        if (active != null) {
            if (active) {
                budget.resume();
            } else {
                budget.pause();
            }
        }

        return budgetRepository.save(budget);
    }

    /**
     * Delete a budget.
     *
     * @param budgetId the budget ID
     */
    @Transactional
    public void deleteBudget(UUID budgetId) {
        budgetRepository.deleteById(budgetId);
        log.info("Deleted budget with id={}", budgetId);
    }

    /**
     * Check if a transaction is allowed within budget constraints.
     *
     * @param userId the user ID
     * @param category the spending category
     * @param amount the transaction amount
     * @return BudgetCheckResult indicating if transaction is allowed
     */
    @Transactional(readOnly = true)
    public BudgetCheckResult checkBudget(UUID userId, String category, BigDecimal amount) {
        List<Budget> budgets = budgetRepository.findByUserIdAndCategory(userId, category);

        if (budgets.isEmpty()) {
            // No budget defined for this category - allow transaction
            return new BudgetCheckResult(BudgetCheckStatus.ALLOWED, null, null);
        }

        for (Budget budget : budgets) {
            budget.resetIfNeeded();
            BudgetStatus status = budget.getStatus();

            if (status == BudgetStatus.PAUSED) {
                continue; // Skip paused budgets
            }

            if (status == BudgetStatus.EXCEEDED) {
                return new BudgetCheckResult(BudgetCheckStatus.BLOCKED, budget,
                        "Budget exceeded for category: " + category);
            }

            if (!budget.canSpend(amount)) {
                return new BudgetCheckResult(BudgetCheckStatus.BLOCKED, budget,
                        "Transaction would exceed budget for category: " + category);
            }

            if (status == BudgetStatus.NEAR_LIMIT) {
                return new BudgetCheckResult(BudgetCheckStatus.WARNING, budget,
                        "Budget near limit for category: " + category);
            }
        }

        return new BudgetCheckResult(BudgetCheckStatus.ALLOWED, null, null);
    }

    /**
     * Record a transaction against the budget.
     *
     * @param userId the user ID
     * @param category the spending category
     * @param amount the transaction amount
     */
    @Transactional
    public void recordTransaction(UUID userId, String category, BigDecimal amount) {
        List<Budget> budgets = budgetRepository.findByUserIdAndCategory(userId, category);

        for (Budget budget : budgets) {
            if (budget.isActive()) {
                budget.resetIfNeeded();
                budget.recordSpending(amount);
                budgetRepository.save(budget);
                log.debug("Recorded spending of {} against budget {}", amount, budget.getId());
            }
        }
    }

    /**
     * Get budget status for all user budgets.
     *
     * @param userId the user ID
     * @return list of budget status information
     */
    @Transactional(readOnly = true)
    public List<BudgetStatusInfo> getAllBudgetStatus(UUID userId) {
        List<Budget> budgets = budgetRepository.findByUserId(userId);

        return budgets.stream()
                .map(budget -> {
                    budget.resetIfNeeded();
                    return new BudgetStatusInfo(
                            budget.getId(),
                            budget.getCategory(),
                            budget.getStatus(),
                            budget.getLimitAmount(),
                            budget.getCurrentSpent(),
                            budget.getSpentPercentage(),
                            budget.getRemainingAmount(),
                            budget.isActive()
                    );
                })
                .toList();
    }

    /**
     * Scheduled job to reset budgets when their period elapses.
     * Runs daily at midnight.
     */
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void resetBudgets() {
        log.info("Running scheduled budget reset job");

        List<Budget> budgetsNeedingReset = budgetRepository.findBudgetsNeedingReset();
        log.info("Found {} budgets needing reset", budgetsNeedingReset.size());

        for (Budget budget : budgetsNeedingReset) {
            try {
                budget.resetIfNeeded();
                budgetRepository.save(budget);
                log.debug("Reset budget {}", budget.getId());
            } catch (Exception e) {
                log.error("Failed to reset budget {}", budget.getId(), e);
            }
        }
    }

    private LocalDate calculateInitialResetDate(BudgetPeriod period) {
        LocalDate today = LocalDate.now();
        return switch (period) {
            case DAILY -> today.plusDays(1);
            case WEEKLY -> today.plusWeeks(1);
            case MONTHLY -> today.plusMonths(1);
        };
    }

    // Result records
    public record BudgetCheckResult(BudgetCheckStatus status, Budget budget, String message) {}
    public record BudgetStatusInfo(UUID budgetId, String category, BudgetStatus status,
                                   BigDecimal limitAmount, BigDecimal currentSpent,
                                   BigDecimal spentPercentage, BigDecimal remainingAmount,
                                   boolean active) {}
}
