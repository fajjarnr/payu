package id.payu.billing.adapter.messaging;

import id.payu.billing.domain.port.in.SubscriptionUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import java.util.UUID;

/**
 * JMS Listener for processing scheduled billing commands from ActiveMQ Artemis.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionScheduledChargeListener {

    private final SubscriptionUseCase subscriptionService;

    @JmsListener(destination = "payu.billing.scheduled")
    public void onScheduledBilling(String subscriptionIdStr) {
        log.info("Received scheduled billing command for subscription: {}", subscriptionIdStr);
        try {
            UUID subscriptionId = UUID.fromString(subscriptionIdStr);
            subscriptionService.processScheduledCharge(subscriptionId);
        } catch (Exception e) {
            log.error("Failed to process scheduled billing for subscription ID: {}", subscriptionIdStr, e);
            throw new RuntimeException("Scheduled billing execution failed, rollback to DLQ", e);
        }
    }
}
