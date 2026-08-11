package id.payu.account.application.service;

import id.payu.account.domain.model.Budget;
import id.payu.account.domain.model.BudgetPeriod;
import id.payu.account.domain.port.out.BudgetRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * ACCOUNT-002: budget get/update/delete must be scoped to the owning account;
 * a foreign account must not be able to read or mutate another user's budget.
 */
@DisplayName("BudgetService ownership")
class BudgetOwnershipTest {

    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID FOREIGN = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID BUDGET_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");

    private BudgetRepositoryPort budgetRepository;
    private BudgetService budgetService;
    private Budget ownedBudget;

    @BeforeEach
    void setUp() {
        budgetRepository = mock(BudgetRepositoryPort.class);
        budgetService = new BudgetService(budgetRepository);
        ownedBudget = Budget.builder()
                .id(BUDGET_ID)
                .userId(OWNER)
                .category("FOOD")
                .limitAmount(new BigDecimal("1000000"))
                .period(BudgetPeriod.MONTHLY)
                .build();
        given(budgetRepository.findById(BUDGET_ID)).willReturn(Optional.of(ownedBudget));
        given(budgetRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("getBudget hides another user's budget (empty result, no existence oracle)")
    void getBudgetHiddenForForeignAccount() {
        assertThat(budgetService.getBudget(FOREIGN, BUDGET_ID)).isEmpty();
        assertThat(budgetService.getBudget(OWNER, BUDGET_ID)).contains(ownedBudget);
    }

    @Test
    @DisplayName("updateBudget rejects a foreign account with the same error as a missing budget")
    void updateBudgetRejectedForForeignAccount() {
        assertThatThrownBy(() -> budgetService.updateBudget(FOREIGN, BUDGET_ID, new BigDecimal("500000"), null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Budget not found");
        verify(budgetRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteBudget rejects a foreign account without deleting")
    void deleteBudgetRejectedForForeignAccount() {
        assertThatThrownBy(() -> budgetService.deleteBudget(FOREIGN, BUDGET_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Budget not found");
        verify(budgetRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("owner can update and delete their own budget")
    void ownerCanMutateOwnBudget() {
        Budget updated = budgetService.updateBudget(OWNER, BUDGET_ID, new BigDecimal("500000"), null, null);
        assertThat(updated.getLimitAmount()).isEqualByComparingTo(new BigDecimal("500000"));
        verify(budgetRepository).save(ownedBudget);

        budgetService.deleteBudget(OWNER, BUDGET_ID);
        verify(budgetRepository).deleteById(BUDGET_ID);
    }
}
