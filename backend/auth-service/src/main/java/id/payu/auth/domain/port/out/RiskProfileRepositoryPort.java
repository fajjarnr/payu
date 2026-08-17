package id.payu.auth.domain.port.out;

import id.payu.auth.domain.model.UserRiskProfile;

import java.util.Optional;

/**
 * Outbound port for UserRiskProfile persistence.
 */
public interface RiskProfileRepositoryPort {

    Optional<UserRiskProfile> findByUsername(String username);

    UserRiskProfile save(UserRiskProfile profile);
}
