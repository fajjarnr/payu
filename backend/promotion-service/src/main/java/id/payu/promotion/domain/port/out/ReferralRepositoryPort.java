package id.payu.promotion.domain.port.out;

import id.payu.promotion.domain.model.Referral;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReferralRepositoryPort {
    Referral save(Referral referral);
    Optional<Referral> findById(UUID id);
    Optional<Referral> findByReferralCode(String code);
    List<Referral> findByReferrerAccountId(String accountId);
}
