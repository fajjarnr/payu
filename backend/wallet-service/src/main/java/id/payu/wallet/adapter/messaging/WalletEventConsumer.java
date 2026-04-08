package id.payu.wallet.adapter.messaging;

import id.payu.wallet.domain.port.in.WalletUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WalletEventConsumer {

    private final WalletUseCase walletUseCase;

    @KafkaListener(
            topics = "user.created",
            groupId = "wallet-service-group",
            properties = {"spring.json.value.default.type=java.util.HashMap"}
    )
    public void consumeUserCreatedEvent(Map<String, Object> payload) {
        log.info("Received user.created event: {}", payload);
        try {
            Object externalId = payload.get("externalId");
            Object userId = payload.get("userId");
            String accountId = externalId != null && !externalId.toString().isBlank()
                    ? externalId.toString()
                    : userId != null ? userId.toString() : null;

            if (accountId == null) {
                log.warn("No account identifier found in user.created payload");
                return;
            }

            walletUseCase.createWallet(accountId);
            log.info("Processed user.created event for accountId: {}", accountId);

        } catch (Exception e) {
            log.error("Failed to process user.created event", e);
        }
    }
}
