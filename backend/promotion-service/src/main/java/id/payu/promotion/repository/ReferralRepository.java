package id.payu.promotion.repository;

import id.payu.promotion.domain.Referral;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReferralRepository extends JpaRepository<Referral, UUID> {

    Optional<Referral> findByReferrerIdAndStatus(String referrerId, Referral.Status status);

    List<Referral> findByReferrerId(String referrerId);

    List<Referral> findByReferredAccountId(String referredAccountId);

    Optional<Referral> findByReferralCode(String referralCode);
}
