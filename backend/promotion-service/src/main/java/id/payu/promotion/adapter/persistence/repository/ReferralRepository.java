package id.payu.promotion.adapter.persistence.repository;

import id.payu.promotion.adapter.persistence.entity.ReferralEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT r FROM ReferralEntity r WHERE r.referralCode = :referralCode")
    Optional<ReferralEntity> findByReferralCodeForUpdate(@org.springframework.data.repository.query.Param("referralCode") String referralCode);
}
