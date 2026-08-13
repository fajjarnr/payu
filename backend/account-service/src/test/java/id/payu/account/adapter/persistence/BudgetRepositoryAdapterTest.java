package id.payu.account.adapter.persistence;

import id.payu.account.adapter.persistence.entity.BudgetEntity;
import id.payu.account.domain.model.BudgetPeriod;
import id.payu.account.adapter.persistence.repository.BudgetJpaRepository;
import id.payu.account.domain.model.Budget;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ACCOUNT-006: BudgetRepositoryAdapter coverage.
 */
@DisplayName("BudgetRepositoryAdapter")
class BudgetRepositoryAdapterTest {

    private final BudgetJpaRepository repo = mock(BudgetJpaRepository.class);
    private final BudgetRepositoryAdapter adapter = new BudgetRepositoryAdapter(repo);

    private BudgetEntity entity() {
        BudgetEntity e = new BudgetEntity();
        e.setId(UUID.randomUUID());
        e.setUserId(UUID.randomUUID());
        e.setCategory("FOOD");
        e.setLimitAmount(new BigDecimal("1000.0000"));
        e.setPeriod(BudgetPeriod.MONTHLY);
        e.setCurrentSpent(BigDecimal.ZERO);
        e.setResetDate(LocalDate.now().plusMonths(1));
        e.setActive(true);
        return e;
    }

    @Test
    void savePersistsAndReturnsDomain() {
        Budget b = Budget.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .category("FOOD")
                .limitAmount(new BigDecimal("1000.0000"))
                .period(id.payu.account.domain.model.BudgetPeriod.MONTHLY)
                .currentSpent(BigDecimal.ZERO)
                .active(true)
                .build();
        when(repo.save(any(BudgetEntity.class))).thenAnswer(i -> i.getArgument(0));

        Budget saved = adapter.save(b);

        assertThat(saved.getCategory()).isEqualTo("FOOD");
    }

    @Test
    void findByIdMapsEntity() {
        BudgetEntity e = entity();
        when(repo.findById(e.getId())).thenReturn(Optional.of(e));

        Optional<Budget> found = adapter.findById(e.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo(e.getUserId());
        assertThat(found.get().getLimitAmount()).isEqualByComparingTo(new BigDecimal("1000.0000"));
    }

    @Test
    void findQueries() {
        UUID userId = UUID.randomUUID();
        when(repo.findByUserId(userId)).thenReturn(List.of(entity()));
        when(repo.findByUserIdAndCategory(userId, "FOOD")).thenReturn(List.of(entity()));
        when(repo.findByUserIdAndActive(userId, true)).thenReturn(List.of(entity()));
        when(repo.findBudgetsNeedingReset(LocalDate.now())).thenReturn(List.of(entity()));

        assertThat(adapter.findByUserId(userId)).hasSize(1);
        assertThat(adapter.findByUserIdAndCategory(userId, "FOOD")).hasSize(1);
        assertThat(adapter.findActiveByUserId(userId)).hasSize(1);
        assertThat(adapter.findBudgetsNeedingReset()).hasSize(1);
    }

    @Test
    void deleteAndExists() {
        UUID id = UUID.randomUUID();
        adapter.deleteById(id);
        verify(repo).deleteById(id);

        UUID userId = UUID.randomUUID();
        when(repo.existsByUserIdAndCategory(userId, "FOOD")).thenReturn(true);
        assertThat(adapter.existsByUserIdAndCategory(userId, "FOOD")).isTrue();
    }
}
