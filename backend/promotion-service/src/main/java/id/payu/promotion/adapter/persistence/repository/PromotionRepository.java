package id.payu.promotion.adapter.persistence.repository;

import id.payu.promotion.adapter.persistence.entity.PromotionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import id.payu.promotion.domain.PromotionStatus;
import id.payu.promotion.domain.PromotionType;

@Repository
public interface PromotionRepository extends JpaRepository<PromotionEntity, UUID> {

    Optional<PromotionEntity> findByCode(String code);

    @Query("SELECT p FROM PromotionEntity p WHERE p.status = :status AND p.startDate <= :now AND p.endDate >= :now")
    List<PromotionEntity> findActivePromotions(@Param("status") PromotionStatus status, @Param("now") LocalDateTime now);

    List<PromotionEntity> findByPromotionType(PromotionType promotionType);

    List<PromotionEntity> findByStatus(PromotionStatus status);

    /**
     * BUG-BE-063 Fix: Atomic increment of redemption count with max check.
     * Uses a single UPDATE with WHERE clause to prevent race conditions.
     * Returns 1 if successfully incremented, 0 if quota already reached.
     */
    @Modifying
    @Query("UPDATE PromotionEntity p SET p.redemptionCount = p.redemptionCount + 1 " +
           "WHERE p.id = :id AND (p.maxRedemptions IS NULL OR p.redemptionCount < p.maxRedemptions)")
    int atomicIncrementRedemptionCount(@Param("id") UUID id);
}
