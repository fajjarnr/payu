package id.payu.promotion.adapter.persistence.repository;

import id.payu.promotion.domain.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
