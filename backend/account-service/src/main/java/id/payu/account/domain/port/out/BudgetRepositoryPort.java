package id.payu.account.domain.port.out;

import id.payu.account.domain.model.Budget;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port for Budget persistence operations.
 */
public interface BudgetRepositoryPort {
    Budget save(Budget budget);
    Optional<Budget> findById(UUID id);
    List<Budget> findByUserId(UUID userId);
    List<Budget> findByUserIdAndCategory(UUID userId, String category);
    List<Budget> findActiveByUserId(UUID userId);
    void deleteById(UUID id);
    boolean existsByUserIdAndCategory(UUID userId, String category);
    List<Budget> findBudgetsNeedingReset();
}
