package id.payu.wallet.domain.port.in;

import id.payu.wallet.domain.model.SavingsGoal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Input port for savings goal use cases.
 */
public interface SavingsGoalUseCase {

    List<SavingsGoal> getSavingsGoals(UUID walletId, String authenticatedAccountId);

    SavingsGoal createSavingsGoal(UUID walletId, String authenticatedAccountId, String name, String description,
                                  BigDecimal targetAmount, LocalDate deadline, String icon, String color);

    SavingsGoal updateSavingsGoal(UUID walletId, UUID goalId, String authenticatedAccountId, String name, String description,
                                  BigDecimal targetAmount, LocalDate deadline, String icon, String color);

    void deleteSavingsGoal(UUID walletId, UUID goalId, String authenticatedAccountId);

    SavingsGoal pauseSavingsGoal(UUID walletId, UUID goalId, String authenticatedAccountId);

    SavingsGoal resumeSavingsGoal(UUID walletId, UUID goalId, String authenticatedAccountId);
}
