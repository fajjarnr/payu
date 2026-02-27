package id.payu.partner.adapter.persistence.repository;

import id.payu.partner.domain.Merchant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MerchantRepository extends JpaRepository<Merchant, Long> {

    Optional<Merchant> findByMerchantCode(String merchantCode);

    Page<Merchant> findByPartnerId(Long partnerId, Pageable pageable);

    boolean existsByMerchantCode(String merchantCode);

    long countByPartnerIdAndStatus(Long partnerId, Merchant.MerchantStatus status);
}
