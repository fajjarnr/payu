package id.payu.wallet.adapter.messaging;

import id.payu.events.cloudevents.CloudEventBuilder;
import id.payu.events.cloudevents.CloudEventEnvelope;
import id.payu.outbox.service.OutboxService;
import id.payu.wallet.domain.port.out.WalletEventPublisherPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Outbox-backed adapter for publishing wallet events using CloudEvents 1.0 envelopes.
 * <p>
 * Events are written to the outbox_events table within the same DB transaction
 * as the wallet operation, guaranteeing at-least-once delivery to Kafka.
 * <p>
 * All events conform to CloudEvents 1.0 spec via the events-starter CloudEventEnvelope.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WalletEventPublisherAdapter implements WalletEventPublisherPort {

    private final OutboxService outboxService;

    private static final String AGGREGATE_TYPE = "Wallet";
    private static final String SERVICE_NAME = "wallet-service";

    @Override
    public void publishBalanceChanged(String accountId, BigDecimal newBalance, BigDecimal availableBalance) {
        Map<String, Object> payload = Map.of(
                "accountId", accountId,
                "newBalance", newBalance,
                "availableBalance", availableBalance,
                "timestamp", LocalDateTime.now().toString());

        CloudEventEnvelope<Map<String, Object>> envelope = CloudEventBuilder
                .<Map<String, Object>>forService(SERVICE_NAME)
                .type("id.payu.wallet.balance-changed")
                .subject(accountId)
                .data(payload)
                .build();

        outboxService.createEvent(AGGREGATE_TYPE, accountId, "BalanceChanged",
                envelopeToMap(envelope), null, "wallet.balance.changed");
        log.debug("Created CloudEvent outbox event for balance-changed: accountId={}", accountId);
    }

    @Override
    public void publishBalanceReserved(String accountId, String reservationId, BigDecimal amount) {
        Map<String, Object> payload = Map.of(
                "accountId", accountId,
                "reservationId", reservationId,
                "amount", amount,
                "timestamp", LocalDateTime.now().toString());

        CloudEventEnvelope<Map<String, Object>> envelope = CloudEventBuilder
                .<Map<String, Object>>forService(SERVICE_NAME)
                .type("id.payu.wallet.balance-reserved")
                .subject(accountId)
                .data(payload)
                .build();

        outboxService.createEvent(AGGREGATE_TYPE, accountId, "BalanceReserved",
                envelopeToMap(envelope), null, "wallet.balance.reserved");
        log.debug("Created CloudEvent outbox event for balance-reserved: accountId={}", accountId);
    }

    @Override
    public void publishReservationCommitted(String accountId, String reservationId, BigDecimal amount) {
        Map<String, Object> payload = Map.of(
                "accountId", accountId,
                "reservationId", reservationId,
                "amount", amount,
                "timestamp", LocalDateTime.now().toString());

        CloudEventEnvelope<Map<String, Object>> envelope = CloudEventBuilder
                .<Map<String, Object>>forService(SERVICE_NAME)
                .type("id.payu.wallet.reservation-committed")
                .subject(accountId)
                .data(payload)
                .build();

        outboxService.createEvent(AGGREGATE_TYPE, accountId, "ReservationCommitted",
                envelopeToMap(envelope), null, "wallet.reservation.committed");
        log.debug("Created CloudEvent outbox event for reservation-committed: accountId={}", accountId);
    }

    @Override
    public void publishReservationReleased(String accountId, String reservationId, BigDecimal amount) {
        Map<String, Object> payload = Map.of(
                "accountId", accountId,
                "reservationId", reservationId,
                "amount", amount,
                "timestamp", LocalDateTime.now().toString());

        CloudEventEnvelope<Map<String, Object>> envelope = CloudEventBuilder
                .<Map<String, Object>>forService(SERVICE_NAME)
                .type("id.payu.wallet.reservation-released")
                .subject(accountId)
                .data(payload)
                .build();

        outboxService.createEvent(AGGREGATE_TYPE, accountId, "ReservationReleased",
                envelopeToMap(envelope), null, "wallet.reservation.released");
        log.debug("Created CloudEvent outbox event for reservation-released: accountId={}", accountId);
    }

    @Override
    public void publishWalletCreated(String accountId, String walletId) {
        Map<String, Object> payload = Map.of(
                "accountId", accountId,
                "walletId", walletId,
                "timestamp", LocalDateTime.now().toString());

        CloudEventEnvelope<Map<String, Object>> envelope = CloudEventBuilder
                .<Map<String, Object>>forService(SERVICE_NAME)
                .type("id.payu.wallet.created")
                .subject(accountId)
                .data(payload)
                .build();

        outboxService.createEvent(AGGREGATE_TYPE, accountId, "WalletCreated",
                envelopeToMap(envelope), null, "wallet.created");
        log.debug("Created CloudEvent outbox event for wallet-created: accountId={}", accountId);
    }

    // --- Escrow lifecycle events ---

    private static final String ESCROW_AGGREGATE_TYPE = "Escrow";

    @Override
    public void publishEscrowHeld(UUID escrowId, String buyerAccountId, String sellerAccountId,
                                   String partnerId, BigDecimal amount, String currency,
                                   String externalReferenceId) {
        Map<String, Object> payload = Map.of(
                "escrowId", escrowId.toString(),
                "buyerAccountId", buyerAccountId,
                "sellerAccountId", sellerAccountId,
                "partnerId", partnerId,
                "amount", amount,
                "currency", currency,
                "externalReferenceId", externalReferenceId != null ? externalReferenceId : "",
                "timestamp", LocalDateTime.now().toString());

        CloudEventEnvelope<Map<String, Object>> envelope = CloudEventBuilder
                .<Map<String, Object>>forService(SERVICE_NAME)
                .type("id.payu.escrow.held")
                .subject(escrowId.toString())
                .data(payload)
                .build();

        outboxService.createEvent(ESCROW_AGGREGATE_TYPE, escrowId.toString(), "EscrowHeld",
                envelopeToMap(envelope), null, "escrow.held");
        log.debug("Created outbox event for escrow-held: escrowId={}, partnerId={}", escrowId, partnerId);
    }

    @Override
    public void publishEscrowReleased(UUID escrowId, String partnerId, BigDecimal amount, String currency) {
        Map<String, Object> payload = Map.of(
                "escrowId", escrowId.toString(),
                "partnerId", partnerId,
                "amount", amount,
                "currency", currency,
                "timestamp", LocalDateTime.now().toString());

        CloudEventEnvelope<Map<String, Object>> envelope = CloudEventBuilder
                .<Map<String, Object>>forService(SERVICE_NAME)
                .type("id.payu.escrow.released")
                .subject(escrowId.toString())
                .data(payload)
                .build();

        outboxService.createEvent(ESCROW_AGGREGATE_TYPE, escrowId.toString(), "EscrowReleased",
                envelopeToMap(envelope), null, "escrow.released");
        log.debug("Created outbox event for escrow-released: escrowId={}, partnerId={}", escrowId, partnerId);
    }

    @Override
    public void publishEscrowSettled(UUID escrowId, String sellerAccountId, String partnerId,
                                      BigDecimal netAmount, String currency) {
        Map<String, Object> payload = Map.of(
                "escrowId", escrowId.toString(),
                "sellerAccountId", sellerAccountId,
                "partnerId", partnerId,
                "netAmount", netAmount,
                "currency", currency,
                "timestamp", LocalDateTime.now().toString());

        CloudEventEnvelope<Map<String, Object>> envelope = CloudEventBuilder
                .<Map<String, Object>>forService(SERVICE_NAME)
                .type("id.payu.escrow.settled")
                .subject(escrowId.toString())
                .data(payload)
                .build();

        outboxService.createEvent(ESCROW_AGGREGATE_TYPE, escrowId.toString(), "EscrowSettled",
                envelopeToMap(envelope), null, "escrow.settled");
        log.debug("Created outbox event for escrow-settled: escrowId={}, partnerId={}", escrowId, partnerId);
    }

    @Override
    public void publishEscrowRefunded(UUID escrowId, String buyerAccountId, String partnerId,
                                       BigDecimal amount, String currency, String reason) {
        Map<String, Object> payload = Map.of(
                "escrowId", escrowId.toString(),
                "buyerAccountId", buyerAccountId,
                "partnerId", partnerId,
                "amount", amount,
                "currency", currency,
                "reason", reason != null ? reason : "",
                "timestamp", LocalDateTime.now().toString());

        CloudEventEnvelope<Map<String, Object>> envelope = CloudEventBuilder
                .<Map<String, Object>>forService(SERVICE_NAME)
                .type("id.payu.escrow.refunded")
                .subject(escrowId.toString())
                .data(payload)
                .build();

        outboxService.createEvent(ESCROW_AGGREGATE_TYPE, escrowId.toString(), "EscrowRefunded",
                envelopeToMap(envelope), null, "escrow.refunded");
        log.debug("Created outbox event for escrow-refunded: escrowId={}, partnerId={}", escrowId, partnerId);
    }

    @Override
    public void publishEscrowExpired(UUID escrowId, String partnerId, BigDecimal amount, String currency) {
        Map<String, Object> payload = Map.of(
                "escrowId", escrowId.toString(),
                "partnerId", partnerId,
                "amount", amount,
                "currency", currency,
                "timestamp", LocalDateTime.now().toString());

        CloudEventEnvelope<Map<String, Object>> envelope = CloudEventBuilder
                .<Map<String, Object>>forService(SERVICE_NAME)
                .type("id.payu.escrow.expired")
                .subject(escrowId.toString())
                .data(payload)
                .build();

        outboxService.createEvent(ESCROW_AGGREGATE_TYPE, escrowId.toString(), "EscrowExpired",
                envelopeToMap(envelope), null, "escrow.expired");
        log.debug("Created outbox event for escrow-expired: escrowId={}, partnerId={}", escrowId, partnerId);
    }

    /**
     * Convert a CloudEventEnvelope to a Map suitable for outbox JSON serialization.
     */
    private Map<String, Object> envelopeToMap(CloudEventEnvelope<Map<String, Object>> envelope) {
        Map<String, Object> ceMap = new HashMap<>();
        ceMap.put("specversion", envelope.getSpecVersion());
        ceMap.put("id", envelope.getId().toString());
        ceMap.put("source", envelope.getSource().toString());
        ceMap.put("type", envelope.getType());
        ceMap.put("datacontenttype", envelope.getDataContentType());
        ceMap.put("time", envelope.getTime().toString());
        if (envelope.getSubject() != null) ceMap.put("subject", envelope.getSubject());
        if (envelope.getData() != null) ceMap.put("data", envelope.getData());
        if (envelope.getPayuCorrelationId() != null) ceMap.put("payucorrelationid", envelope.getPayuCorrelationId());
        return ceMap;
    }
}
