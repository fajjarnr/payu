package id.payu.wallet.adapter.persistence.repository;

import id.payu.wallet.adapter.persistence.entity.RevenueSplitEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface RevenueSplitJpaRepository extends JpaRepository<RevenueSplitEntity, UUID> {

    List<RevenueSplitEntity> findByPartnerId(String partnerId);

    List<RevenueSplitEntity> findByPartnerIdAndActive(String partnerId, boolean active);

    @Query("SELECT r FROM RevenueSplitEntity r WHERE r.partnerId = :partnerId " +
           "AND r.active = true AND (r.effectiveFrom IS NULL OR r.effectiveFrom <= :now) " +
           "AND (r.effectiveUntil IS NULL OR r.effectiveUntil >= :now)")
    List<RevenueSplitEntity> findActiveAndEffectiveByPartnerId(
            @Param("partnerId") String partnerId, @Param("now") LocalDateTime now);
}
