package id.payu.promotion.adapter.persistence.repository;

import id.payu.promotion.adapter.persistence.entity.ReferralEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import id.payu.promotion.domain.ReferralStatus;

@Repository
public interface ReferralRepository extends JpaRepository<ReferralEntity, UUID> {

    Optional<ReferralEntity> findByReferrerAccountIdAndStatus(String referrerAccountId, ReferralStatus status);

    List<ReferralEntity> findByReferrerAccountId(String referrerAccountId);

    List<ReferralEntity> findByRefereeAccountId(String refereeAccountId);

    Optional<ReferralEntity> findByReferralCode(String referralCode);
}
