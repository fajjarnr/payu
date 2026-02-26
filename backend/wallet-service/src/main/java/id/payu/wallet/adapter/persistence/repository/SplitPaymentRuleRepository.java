package id.payu.wallet.adapter.persistence.repository;

import id.payu.wallet.adapter.persistence.entity.SplitPaymentRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SplitPaymentRuleRepository extends JpaRepository<SplitPaymentRuleEntity, UUID> {

    List<SplitPaymentRuleEntity> findByPartnerIdAndActiveTrue(String partnerId);

    List<SplitPaymentRuleEntity> findByPartnerIdOrderByCreatedAtDesc(String partnerId);
}
