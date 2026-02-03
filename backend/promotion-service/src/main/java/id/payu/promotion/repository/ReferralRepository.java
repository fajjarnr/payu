package id.payu.promotion.repository;

import id.payu.promotion.domain.Referral;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReferralRepository extends JpaRepository<Referral, UUID> {

    Optional<Referral> findByReferrerAccountIdAndStatus(String referrerAccountId, Referral.Status status);

    List<Referral> findByReferrerAccountId(String referrerAccountId);

    List<Referral> findByRefereeAccountId(String refereeAccountId);

    Optional<Referral> findByReferralCode(String referralCode);
}
