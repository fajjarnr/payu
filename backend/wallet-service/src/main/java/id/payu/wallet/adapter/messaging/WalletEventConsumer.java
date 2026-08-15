package id.payu.wallet.adapter.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.wallet.domain.port.in.WalletUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WalletEventConsumer {

    private final WalletUseCase walletUseCase;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "payu.account.user-created.v1",
            groupId = "wallet-service-group",
            // outbox publishes JSON strings; the global JacksonJsonDeserializer
            // cannot read them without type headers (ARCH-TOPIC-003)
            properties = "value.deserializer=org.apache.kafka.common.serialization.StringDeserializer")
    public void consumeUserCreatedEvent(ConsumerRecord<String, String> record) {
        try {
            JsonNode root = objectMapper.readTree(record.value());
            JsonNode data = root.has("data") ? root.get("data") : root;
            Map<String, Object> payload = objectMapper.convertValue(data, Map.class);

            log.info("Received user.created event: {}", payload);
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
            throw new IllegalStateException("Invalid user.created event", e);
        }
    }
}
