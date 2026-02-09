package id.payu.wallet.adapter.messaging;

import id.payu.outbox.service.OutboxService;
import id.payu.wallet.domain.port.out.WalletEventPublisherPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Outbox-backed adapter for publishing wallet events.
 * <p>
 * Events are written to the outbox_events table within the same DB transaction
 * as the wallet operation, guaranteeing at-least-once delivery to Kafka.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WalletEventPublisherAdapter implements WalletEventPublisherPort {

    private final OutboxService outboxService;

    private static final String AGGREGATE_TYPE = "Wallet";

    @Override
    public void publishBalanceChanged(String accountId, BigDecimal newBalance, BigDecimal availableBalance) {
        Map<String, Object> payload = Map.of(
                "accountId", accountId,
                "newBalance", newBalance,
                "availableBalance", availableBalance,
                "timestamp", LocalDateTime.now().toString());
        outboxService.createEvent(AGGREGATE_TYPE, accountId, "BalanceChanged",
                payload, null, "wallet.balance.changed");
        log.debug("Created outbox event for balance-changed: accountId={}", accountId);
    }

    @Override
    public void publishBalanceReserved(String accountId, String reservationId, BigDecimal amount) {
        Map<String, Object> payload = Map.of(
                "accountId", accountId,
                "reservationId", reservationId,
                "amount", amount,
                "timestamp", LocalDateTime.now().toString());
        outboxService.createEvent(AGGREGATE_TYPE, accountId, "BalanceReserved",
                payload, null, "wallet.balance.reserved");
        log.debug("Created outbox event for balance-reserved: accountId={}", accountId);
    }

    @Override
    public void publishReservationCommitted(String accountId, String reservationId, BigDecimal amount) {
        Map<String, Object> payload = Map.of(
                "accountId", accountId,
                "reservationId", reservationId,
                "amount", amount,
                "timestamp", LocalDateTime.now().toString());
        outboxService.createEvent(AGGREGATE_TYPE, accountId, "ReservationCommitted",
                payload, null, "wallet.reservation.committed");
        log.debug("Created outbox event for reservation-committed: accountId={}", accountId);
    }

    @Override
    public void publishReservationReleased(String accountId, String reservationId, BigDecimal amount) {
        Map<String, Object> payload = Map.of(
                "accountId", accountId,
                "reservationId", reservationId,
                "amount", amount,
                "timestamp", LocalDateTime.now().toString());
        outboxService.createEvent(AGGREGATE_TYPE, accountId, "ReservationReleased",
                payload, null, "wallet.reservation.released");
        log.debug("Created outbox event for reservation-released: accountId={}", accountId);
    }

    @Override
    public void publishWalletCreated(String accountId, String walletId) {
        Map<String, Object> payload = Map.of(
                "accountId", accountId,
                "walletId", walletId,
                "timestamp", LocalDateTime.now().toString());
        outboxService.createEvent(AGGREGATE_TYPE, accountId, "WalletCreated",
                payload, null, "wallet.created");
        log.debug("Created outbox event for wallet-created: accountId={}", accountId);
    }
}
