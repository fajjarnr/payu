package id.payu.account.adapter.persistence;

import id.payu.account.domain.model.Budget;
import id.payu.account.domain.port.out.BudgetRepositoryPort;
import id.payu.account.adapter.persistence.entity.BudgetEntity;
import id.payu.account.adapter.persistence.repository.BudgetJpaRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Adapter implementation of BudgetRepositoryPort using JPA.
 */
@Component
public class BudgetRepositoryAdapter implements BudgetRepositoryPort {

    private final BudgetJpaRepository budgetJpaRepository;

    public BudgetRepositoryAdapter(BudgetJpaRepository budgetJpaRepository) {
        this.budgetJpaRepository = budgetJpaRepository;
    }

    @Override
    public Budget save(Budget budget) {
        BudgetEntity entity = toEntity(budget);
        BudgetEntity saved = budgetJpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Budget> findById(UUID id) {
        return budgetJpaRepository.findById(id)
                .map(this::toDomain);
    }

    @Override
    public List<Budget> findByUserId(UUID userId) {
        return budgetJpaRepository.findByUserId(userId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Budget> findByUserIdAndCategory(UUID userId, String category) {
        return budgetJpaRepository.findByUserIdAndCategory(userId, category).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Budget> findActiveByUserId(UUID userId) {
        return budgetJpaRepository.findByUserIdAndActive(userId, true).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(UUID id) {
        budgetJpaRepository.deleteById(id);
    }

    @Override
    public boolean existsByUserIdAndCategory(UUID userId, String category) {
        return budgetJpaRepository.existsByUserIdAndCategory(userId, category);
    }

    @Override
    public List<Budget> findBudgetsNeedingReset() {
        return budgetJpaRepository.findBudgetsNeedingReset(LocalDate.now()).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    private BudgetEntity toEntity(Budget budget) {
        BudgetEntity entity = new BudgetEntity();
        if (budget.getId() != null) {
            entity.setId(budget.getId());
        }
        entity.setUserId(budget.getUserId());
        entity.setCategory(budget.getCategory());
        entity.setLimitAmount(budget.getLimitAmount());
        entity.setPeriod(budget.getPeriod());
        entity.setCurrentSpent(budget.getCurrentSpent());
        entity.setResetDate(budget.getResetDate());
        entity.setActive(budget.isActive());
        entity.setWarningThreshold(budget.getWarningThreshold());
        entity.setCreatedAt(budget.getCreatedAt());
        entity.setUpdatedAt(budget.getUpdatedAt());
        entity.setVersion(budget.getVersion());
        return entity;
    }

    private Budget toDomain(BudgetEntity entity) {
        return Budget.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .category(entity.getCategory())
                .limitAmount(entity.getLimitAmount())
                .period(entity.getPeriod())
                .currentSpent(entity.getCurrentSpent())
                .resetDate(entity.getResetDate())
                .active(entity.getActive() != null ? entity.getActive() : true)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .version(entity.getVersion())
                .build();
    }
}
