package id.payu.promotion.adapter.persistence.repository;

import id.payu.promotion.domain.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, UUID> {

    Optional<Promotion> findByCode(String code);

    @Query("SELECT p FROM Promotion p WHERE p.status = :status AND p.startDate <= :now AND p.endDate >= :now")
    List<Promotion> findActivePromotions(@Param("status") Promotion.Status status, @Param("now") LocalDateTime now);

    List<Promotion> findByPromotionType(Promotion.PromotionType promotionType);

    List<Promotion> findByStatus(Promotion.Status status);

    /**
     * BUG-BE-063 Fix: Atomic increment of redemption count with max check.
     * Uses a single UPDATE with WHERE clause to prevent race conditions.
     * Returns 1 if successfully incremented, 0 if quota already reached.
     */
    @Modifying
    @Query("UPDATE Promotion p SET p.redemptionCount = p.redemptionCount + 1 " +
           "WHERE p.id = :id AND (p.maxRedemptions IS NULL OR p.redemptionCount < p.maxRedemptions)")
    int atomicIncrementRedemptionCount(@Param("id") UUID id);
}
