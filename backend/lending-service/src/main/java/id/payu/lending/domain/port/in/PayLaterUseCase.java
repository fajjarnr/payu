package id.payu.lending.domain.port.in;

import id.payu.lending.domain.model.PayLater;
import id.payu.lending.dto.PayLaterLimitRequest;
import java.util.Optional;
import java.util.UUID;

public interface PayLaterUseCase {
    PayLater activatePayLater(UUID userId, PayLaterLimitRequest request);
    Optional<PayLater> getPayLaterByUserId(UUID userId);
}
