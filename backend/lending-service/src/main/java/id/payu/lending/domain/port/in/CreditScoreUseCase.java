package id.payu.lending.domain.port.in;

import id.payu.lending.domain.model.CreditScore;
import java.util.Optional;
import java.util.UUID;

public interface CreditScoreUseCase {
    CreditScore calculateCreditScore(UUID userId);
    Optional<CreditScore> getCreditScoreByUserId(UUID userId);
}
