package id.payu.account.application.service;

import id.payu.account.domain.model.Budget;
import id.payu.account.domain.model.BudgetPeriod;
import id.payu.account.domain.model.BudgetStatus;
import id.payu.account.domain.port.out.BudgetRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ACCOUNT-006: BudgetService coverage.
 */
@DisplayName("BudgetService")
class BudgetServiceTest {

    private final BudgetRepositoryPort repo = mock(BudgetRepositoryPort.class);
    private final BudgetService service = new BudgetService(repo);

    private Budget budget(UUID owner, BigDecimal limit) {
        return Budget.builder()
                .id(UUID.randomUUID())
                .userId(owner)
                .category("FOOD")
                .limitAmount(limit)
                .period(BudgetPeriod.MONTHLY)
                .currentSpent(BigDecimal.ZERO)
                .active(true)
                .build();
    }

    @Test
    void createBudgetSavesAndRejectsDuplicate() {
        UUID userId = UUID.randomUUID();
        when(repo.existsByUserIdAndCategory(userId, "FOOD")).thenReturn(false);
        when(repo.save(org.mockito.ArgumentMatchers.any())).thenAnswer(i -> i.getArgument(0));

        Budget created = service.createBudget(userId, "FOOD", new BigDecimal("1000.0000"), BudgetPeriod.MONTHLY);

        assertThat(created.getUserId()).isEqualTo(userId);
        assertThat(created.getCurrentSpent()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(created.getResetDate()).isEqualTo(LocalDate.now().plusMonths(1));

        when(repo.existsByUserIdAndCategory(userId, "FOOD")).thenReturn(true);
        assertThatThrownBy(() -> service.createBudget(userId, "FOOD", new BigDecimal("1000.0000"), BudgetPeriod.MONTHLY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createBudgetComputesResetDatePerPeriod() {
        UUID userId = UUID.randomUUID();
        when(repo.existsByUserIdAndCategory(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(false);
        when(repo.save(org.mockito.ArgumentMatchers.any())).thenAnswer(i -> i.getArgument(0));

        Budget weekly = service.createBudget(userId, "WK", new BigDecimal("100"), BudgetPeriod.WEEKLY);
        assertThat(weekly.getResetDate()).isEqualTo(LocalDate.now().plusWeeks(1));

        Budget daily = service.createBudget(userId, "DY", new BigDecimal("100"), BudgetPeriod.DAILY);
        assertThat(daily.getResetDate()).isEqualTo(LocalDate.now().plusDays(1));
    }

    @Test
    void queriesDelegate() {
        UUID userId = UUID.randomUUID();
        List<Budget> all = List.of(budget(userId, BigDecimal.TEN));
        when(repo.findByUserId(userId)).thenReturn(all);
        when(repo.findActiveByUserId(userId)).thenReturn(all);

        assertThat(service.getUserBudgets(userId)).hasSize(1);
        assertThat(service.getActiveBudgets(userId)).hasSize(1);
    }

    @Test
    void getBudgetFiltersOwnership() {
        UUID owner = UUID.randomUUID();
        Budget b = budget(owner, BigDecimal.TEN);
        when(repo.findById(b.getId())).thenReturn(Optional.of(b));

        assertThat(service.getBudget(owner, b.getId())).contains(b);
        assertThat(service.getBudget(UUID.randomUUID(), b.getId())).isEmpty();
    }

    @Test
    void updateBudgetAppliesChanges() {
        UUID owner = UUID.randomUUID();
        Budget b = budget(owner, new BigDecimal("100.0000"));
        when(repo.findById(b.getId())).thenReturn(Optional.of(b));
        when(repo.save(b)).thenReturn(b);

        Budget updated = service.updateBudget(owner, b.getId(), new BigDecimal("200.0000"),
                BudgetPeriod.WEEKLY, false);

        assertThat(updated.getLimitAmount()).isEqualByComparingTo(new BigDecimal("200.0000"));
        assertThat(updated.getPeriod()).isEqualTo(BudgetPeriod.WEEKLY);
        assertThat(updated.getStatus()).isEqualTo(BudgetStatus.PAUSED);
    }

    @Test
    void updateBudgetRejectsNonOwner() {
        Budget b = budget(UUID.randomUUID(), BigDecimal.TEN);
        when(repo.findById(b.getId())).thenReturn(Optional.of(b));

        assertThatThrownBy(() -> service.updateBudget(UUID.randomUUID(), b.getId(), null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deleteBudgetRequiresOwnership() {
        Budget b = budget(UUID.randomUUID(), BigDecimal.TEN);
        when(repo.findById(b.getId())).thenReturn(Optional.of(b));

        assertThatThrownBy(() -> service.deleteBudget(UUID.randomUUID(), b.getId()))
                .isInstanceOf(IllegalArgumentException.class);

        when(repo.findById(b.getId())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.deleteBudget(UUID.randomUUID(), b.getId()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void checkBudgetStatuses() {
        UUID userId = UUID.randomUUID();

        when(repo.findByUserIdAndCategory(userId, "NONE")).thenReturn(List.of());
        assertThat(service.checkBudget(userId, "NONE", BigDecimal.TEN).status())
                .isEqualTo(BudgetCheckStatus.ALLOWED);

        Budget exceeded = budget(userId, new BigDecimal("100.0000"));
        exceeded.recordSpending(new BigDecimal("150.0000"));
        when(repo.findByUserIdAndCategory(userId, "EXC")).thenReturn(List.of(exceeded));
        assertThat(service.checkBudget(userId, "EXC", BigDecimal.ONE).status())
                .isEqualTo(BudgetCheckStatus.BLOCKED);

        Budget overSpend = budget(userId, new BigDecimal("100.0000"));
        overSpend.recordSpending(new BigDecimal("95.0000"));
        when(repo.findByUserIdAndCategory(userId, "OVER")).thenReturn(List.of(overSpend));
        assertThat(service.checkBudget(userId, "OVER", new BigDecimal("10.0000")).status())
                .isEqualTo(BudgetCheckStatus.BLOCKED);

        Budget near = budget(userId, new BigDecimal("100.0000"));
        near.recordSpending(new BigDecimal("80.0000"));
        when(repo.findByUserIdAndCategory(userId, "NEAR")).thenReturn(List.of(near));
        assertThat(service.checkBudget(userId, "NEAR", BigDecimal.ONE).status())
                .isEqualTo(BudgetCheckStatus.WARNING);

        Budget ok = budget(userId, new BigDecimal("100.0000"));
        when(repo.findByUserIdAndCategory(userId, "OK")).thenReturn(List.of(ok));
        assertThat(service.checkBudget(userId, "OK", BigDecimal.ONE).status())
                .isEqualTo(BudgetCheckStatus.ALLOWED);

        Budget paused = budget(userId, new BigDecimal("100.0000"));
        paused.pause();
        when(repo.findByUserIdAndCategory(userId, "PAUSED")).thenReturn(List.of(paused));
        assertThat(service.checkBudget(userId, "PAUSED", BigDecimal.TEN).status())
                .isEqualTo(BudgetCheckStatus.ALLOWED);
    }

    @Test
    void recordTransactionRecordsSpending() {
        UUID userId = UUID.randomUUID();
        Budget b = budget(userId, new BigDecimal("100.0000"));
        when(repo.findByUserIdAndCategory(userId, "FOOD")).thenReturn(List.of(b));

        service.recordTransaction(userId, "FOOD", new BigDecimal("25.0000"));

        assertThat(b.getCurrentSpent()).isEqualByComparingTo(new BigDecimal("25.0000"));
        verify(repo).save(b);
    }

    @Test
    void getAllBudgetStatusMapsRecords() {
        UUID userId = UUID.randomUUID();
        Budget b = budget(userId, new BigDecimal("100.0000"));
        when(repo.findByUserId(userId)).thenReturn(List.of(b));

        List<BudgetService.BudgetStatusInfo> infos = service.getAllBudgetStatus(userId);

        assertThat(infos).hasSize(1);
        assertThat(infos.get(0).category()).isEqualTo("FOOD");
        assertThat(infos.get(0).status()).isEqualTo(BudgetStatus.ACTIVE);
        assertThat(infos.get(0).limitAmount()).isEqualByComparingTo(new BigDecimal("100.0000"));
    }

    @Test
    void resetBudgetsResetsEligible() {
        Budget b = budget(UUID.randomUUID(), BigDecimal.TEN);
        b.setResetDate(LocalDate.now().minusDays(1));
        b.recordSpending(new BigDecimal("5.0000"));
        when(repo.findBudgetsNeedingReset()).thenReturn(List.of(b));

        service.resetBudgets();

        assertThat(b.getCurrentSpent()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(repo).save(b);
    }
}
