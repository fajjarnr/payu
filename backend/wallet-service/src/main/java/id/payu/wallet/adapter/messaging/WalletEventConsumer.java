package id.payu.wallet.adapter.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "user.created", groupId = "wallet-service-group")
    public void consumeUserCreatedEvent(String message) {
        log.info("Received user.created event: {}", message);
        try {
            // We expect JSON payload. Since account-service sends an object, it's
            // serialized to JSON by Spring Kafka.
            // We can parse it to a Map or a specific DTO.
            Map<String, Object> payload = objectMapper.readValue(message, Map.class);
            String accountId = (String) payload.get("userId"); // DTO has userId, but it's UUID.

            // Note: account-service sends user ID as UUID, but wallet service uses String
            // for accountId.
            // Let's verify what account-service sends.
            // It sends UserCreatedEvent(UUID userId, ...).
            // JSON will have "userId": "uuid-string"

            if (accountId == null) {
                // Try "id" if "userId" is missing? AccountDto usually has "id" or "userId".
                // Looking at UserCreatedEvent: record UserCreatedEvent(UUID userId, ...)
                // So field name is "userId".
                log.warn("userId not found in payload");
                return;
            }

            walletUseCase.createWallet(accountId);
            log.info("Processed user.created event for accountId: {}", accountId);

        } catch (Exception e) {
            log.error("Failed to process user.created event", e);
        }
    }
}
