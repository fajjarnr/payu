package id.payu.account.domain.port.out;

import id.payu.account.dto.UserCreatedEvent;

public interface UserEventPublisherPort {
    void publishUserCreated(UserCreatedEvent event);
    void publishUserUpdated(UserCreatedEvent event);
    void publishKycCompleted(UserCreatedEvent event);
}
