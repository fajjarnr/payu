package id.payu.partner.adapter.persistence.repository;

import id.payu.partner.domain.ApiKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKeyEntity, Long> {

    Optional<ApiKeyEntity> findByKeyHash(String keyHash);

    List<ApiKeyEntity> findByPartnerId(Long partnerId);

    List<ApiKeyEntity> findByPartnerIdAndStatus(Long partnerId, ApiKeyEntity.KeyStatus status);

    @Query("SELECT k FROM ApiKeyEntity k WHERE k.partner.id = :partnerId " +
           "AND k.status IN ('ACTIVE', 'ROTATED') " +
           "AND k.environment = :env")
    List<ApiKeyEntity> findActiveKeysByPartnerAndEnv(
            @Param("partnerId") Long partnerId,
            @Param("env") ApiKeyEntity.KeyEnvironment env);

    @Query("SELECT k FROM ApiKeyEntity k WHERE k.status = 'ROTATED' " +
           "AND k.gracePeriodEndsAt <= :now")
    List<ApiKeyEntity> findExpiredGracePeriodKeys(@Param("now") LocalDateTime now);

    @Query("SELECT k FROM ApiKeyEntity k WHERE k.status = 'ACTIVE' " +
           "AND k.expiresAt IS NOT NULL AND k.expiresAt <= :now")
    List<ApiKeyEntity> findExpiredKeys(@Param("now") LocalDateTime now);

    long countByPartnerIdAndStatusIn(Long partnerId,
                                     List<ApiKeyEntity.KeyStatus> statuses);

    boolean existsByKeyHash(String keyHash);
}
