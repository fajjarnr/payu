package id.payu.wallet.domain.port.out;

import id.payu.wallet.domain.model.SavingsGoal;
import id.payu.wallet.domain.model.SavingsGoalStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port for savings goal persistence operations.
 */
public interface SavingsGoalPersistencePort {

    SavingsGoal save(SavingsGoal savingsGoal);

    Optional<SavingsGoal> findById(UUID id);

    List<SavingsGoal> findByPocketIdAndStatusNot(UUID pocketId, SavingsGoalStatus status);

    List<SavingsGoal> findByUserIdAndStatusNot(UUID userId, SavingsGoalStatus status);

    Optional<SavingsGoal> findByIdAndUserId(UUID id, UUID userId);

    long countByPocketIdAndStatusNot(UUID pocketId, SavingsGoalStatus status);
}
