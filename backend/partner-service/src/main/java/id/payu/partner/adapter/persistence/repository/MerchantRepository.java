package id.payu.partner.adapter.persistence.repository;

import id.payu.partner.adapter.persistence.entity.MerchantEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import id.payu.partner.domain.MerchantStatus;

@Repository
public interface MerchantRepository extends JpaRepository<MerchantEntity, Long> {

    Optional<MerchantEntity> findByMerchantCode(String merchantCode);

    Page<MerchantEntity> findByPartnerId(Long partnerId, Pageable pageable);

    boolean existsByMerchantCode(String merchantCode);

    long countByPartnerIdAndStatus(Long partnerId, MerchantStatus status);
}
