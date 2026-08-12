package id.payu.transaction.application.scheduler;

import id.payu.transaction.adapter.persistence.entity.TransactionEntity;
import id.payu.transaction.adapter.persistence.entity.VirtualAccountEntity;
import id.payu.transaction.domain.model.TransactionStatus;
import id.payu.transaction.domain.model.VaStatus;
import id.payu.transaction.domain.port.out.TransactionPersistencePort;
import id.payu.transaction.domain.port.out.VirtualAccountPersistencePort;
import id.payu.transaction.domain.port.out.WalletServicePort;
import id.payu.outbox.service.OutboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * IMP-2: expiry must be a conditional transition so a paid VA is never
 * overwritten to EXPIRED by the scheduler racing a bank callback.
 * GRPC-004: reserved balance release must go through the wallet port
 * (gRPC adapter), not a raw RestTemplate call to a non-existent endpoint.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentExpiryScheduler VA expiry (IMP-2)")
class PaymentExpirySchedulerTest {

    @Mock
    private TransactionPersistencePort transactionPersistencePort;
    @Mock
    private VirtualAccountPersistencePort virtualAccountPersistencePort;
    @Mock
    private OutboxService outboxService;
    @Mock
    private WalletServicePort walletServicePort;

    private PaymentExpiryScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new PaymentExpiryScheduler(
                transactionPersistencePort, virtualAccountPersistencePort, outboxService,
                walletServicePort, Clock.fixed(Instant.now(), ZoneOffset.UTC));
    }

    private static VirtualAccountEntity expiredPendingVa() {
        return VirtualAccountEntity.builder()
                .id(UUID.randomUUID())
                .vaNumber("8999000011")
                .bankCode("BCA")
                .status(VaStatus.PENDING)
                .expiresAt(Instant.now().minus(1, ChronoUnit.HOURS))
                .build();
    }

    @Test
    @DisplayName("expires pending VAs and publishes the event once per transition")
    void expiresPendingVasAndPublishesEvent() {
        VirtualAccountEntity va = expiredPendingVa();
        when(virtualAccountPersistencePort.findExpiredPendingVAs(any(Instant.class)))
                .thenReturn(List.of(va));
        when(virtualAccountPersistencePort.markExpiredIfPending(eq(va.getId()))).thenReturn(1);

        scheduler.expireVirtualAccounts();

        verify(outboxService).createEvent(eq("VirtualAccount"), eq(va.getId().toString()),
                eq("VirtualAccountExpired"), anyMap(), isNull(), eq("payu.transaction.payment-expired.v1"));
    }

    @Test
    @DisplayName("never marks a VA expired that a callback already paid (conditional transition)")
    void doesNotOverwritePaidVaWithExpired() {
        VirtualAccountEntity va = expiredPendingVa();
        when(virtualAccountPersistencePort.findExpiredPendingVAs(any(Instant.class)))
                .thenReturn(List.of(va));
        when(virtualAccountPersistencePort.markExpiredIfPending(eq(va.getId()))).thenReturn(0);

        scheduler.expireVirtualAccounts();

        verify(outboxService, never()).createEvent(any(), any(), any(), any(), any(), any());
        verify(virtualAccountPersistencePort, never()).saveAll(any());
    }

    @Test
    @DisplayName("releases reserved balance through the wallet port, not raw HTTP (GRPC-004)")
    void releasesReservedBalanceThroughWalletPort() {
        UUID accountId = UUID.randomUUID();
        UUID reservationId = UUID.randomUUID();
        TransactionEntity tx = TransactionEntity.builder()
                .id(UUID.randomUUID())
                .referenceNumber("REF-EXP-1")
                .senderAccountId(accountId)
                .reservationId(reservationId.toString())
                .amountValue(new BigDecimal("25000.0000"))
                .currencyCode("IDR")
                .status(TransactionStatus.PENDING)
                .expiresAt(Instant.now().minus(1, ChronoUnit.HOURS))
                .build();
        when(transactionPersistencePort.findExpiredPendingTransactions(any(Instant.class)))
                .thenReturn(List.of(tx));

        scheduler.expirePendingTransactions();

        verify(walletServicePort).releaseBalance(eq(accountId), eq(tx.getId().toString()),
                eq(reservationId.toString()), eq(tx.getAmountValue()));
        verify(outboxService).createEvent(eq("Transaction"), eq(tx.getId().toString()),
                eq("PaymentExpired"), anyMap(), isNull(), eq("payu.transaction.payment-expired.v1"));
    }
}
