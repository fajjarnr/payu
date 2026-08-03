package id.payu.promotion.adapter.persistence.repository;

import id.payu.promotion.adapter.persistence.entity.PromoUsageEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PromoUsageRepository extends JpaRepository<PromoUsageEntity, UUID> {
    boolean existsByUserIdAndPromoCode(String userId, String promoCode);
    Optional<PromoUsageEntity> findByIdempotencyKey(String idempotencyKey);

    @Modifying
    @Query(value = """
            INSERT INTO promo_usage
                (id, user_id, promo_code, transaction_id, discount_amount, final_amount,
                 idempotency_key, timestamp, usage_type)
            VALUES (CAST(:id AS UUID), :userId, :promoCode, :transactionId, :discountAmount,
                    :finalAmount, :idempotencyKey, :timestamp, :usageType)
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    int insertIgnore(
            @Param("id") String id,
            @Param("userId") String userId,
            @Param("promoCode") String promoCode,
            @Param("transactionId") String transactionId,
            @Param("discountAmount") BigDecimal discountAmount,
            @Param("finalAmount") BigDecimal finalAmount,
            @Param("idempotencyKey") String idempotencyKey,
            @Param("timestamp") Instant timestamp,
            @Param("usageType") String usageType);
}
