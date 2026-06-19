package id.payu.account.adapter.persistence.repository;

import id.payu.account.adapter.persistence.entity.BudgetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for BudgetEntity.
 */
@Repository
public interface BudgetJpaRepository extends JpaRepository<BudgetEntity, UUID> {

    List<BudgetEntity> findByUserId(UUID userId);

    List<BudgetEntity> findByUserIdAndCategory(UUID userId, String category);

    List<BudgetEntity> findByUserIdAndActive(UUID userId, Boolean active);

    Optional<BudgetEntity> findByUserIdAndCategoryAndActive(UUID userId, String category, Boolean active);

    boolean existsByUserIdAndCategory(UUID userId, String category);

    @Query("SELECT b FROM BudgetEntity b WHERE b.active = true AND b.resetDate <= :today")
    List<BudgetEntity> findBudgetsNeedingReset(@Param("today") LocalDate today);

    long countByUserIdAndActive(UUID userId, Boolean active);
}
