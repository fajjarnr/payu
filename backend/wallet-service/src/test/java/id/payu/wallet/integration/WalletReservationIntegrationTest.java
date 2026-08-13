package id.payu.wallet.integration;

import id.payu.cache.service.CacheService;
import id.payu.outbox.repository.OutboxRepository;
import id.payu.wallet.config.TestcontainersConfig;
import id.payu.wallet.domain.model.LedgerEntry;
import id.payu.wallet.domain.model.Wallet;
import id.payu.wallet.domain.port.in.WalletUseCase;
import id.payu.wallet.domain.port.out.WalletPersistencePort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * QAMVP-002: reserve → commit money journey against real PostgreSQL
 * (Testcontainers) with real transactional-outbox event publisher.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {"payu.grpc.server.port=0", "spring.jpa.hibernate.ddl-auto=none"})
@Import(TestcontainersConfig.class)
@DisplayName("QAMVP-002 — wallet reserve/commit journey (real PostgreSQL + outbox)")
class WalletReservationIntegrationTest {

    @Autowired
    private WalletUseCase walletUseCase;

    @Autowired
    private WalletPersistencePort walletPersistencePort;

    @Autowired
    private OutboxRepository outboxRepository;

    @MockitoBean
    private CacheService cacheService;

    private Wallet seedWallet(String accountId) {
        Wallet wallet = Wallet.builder()
                .accountId(accountId)
                .currency("IDR")
                .balance(new BigDecimal("1000000.0000"))
                .reservedBalance(BigDecimal.ZERO)
                .status(id.payu.wallet.domain.model.WalletStatus.ACTIVE)
                .version(0L)
                .createdAt(LocalDateTime.now())
                .build();
        return walletPersistencePort.save(wallet);
    }

    @Test
    @DisplayName("reserve debits available balance and writes ledger + outbox")
    void reserveThenCommitJourney() {
        String accountId = "acct-res-" + UUID.randomUUID().toString().substring(0, 8);
        seedWallet(accountId);
        String referenceId = "REF-" + UUID.randomUUID();

        String reservationId = walletUseCase.reserveBalance(accountId, new BigDecimal("200000.0000"), referenceId);
        assertThat(reservationId).isNotBlank();

        Wallet afterReserve = walletPersistencePort.findByAccountId(accountId).orElseThrow();
        assertThat(afterReserve.getAvailableBalance()).isEqualByComparingTo(new BigDecimal("800000.0000"));
        assertThat(afterReserve.getReservedBalance()).isEqualByComparingTo(new BigDecimal("200000.0000"));

        List<LedgerEntry> entries = walletPersistencePort.findByAccountIdOrderByCreatedAtDesc(accountId);
        assertThat(entries).anyMatch(e -> "RESERVATION".equals(e.getReferenceType()));

        assertThat(outboxRepository.findByAggregateId(accountId))
                .as("reserve must publish an outbox event")
                .isNotEmpty();

        walletUseCase.commitReservation(reservationId);

        Wallet afterCommit = walletPersistencePort.findByAccountId(accountId).orElseThrow();
        assertThat(afterCommit.getBalance()).isEqualByComparingTo(new BigDecimal("800000.0000"));
        assertThat(afterCommit.getAvailableBalance()).isEqualByComparingTo(new BigDecimal("800000.0000"));
        assertThat(afterCommit.getReservedBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("reserve with same reference idempotently replays the same reservation")
    void reserveIdempotentReplay() {
        String accountId = "acct-replay-" + UUID.randomUUID().toString().substring(0, 8);
        seedWallet(accountId);
        String referenceId = "REF-" + UUID.randomUUID();

        String first = walletUseCase.reserveBalance(accountId, new BigDecimal("100000.0000"), referenceId);
        String second = walletUseCase.reserveBalance(accountId, new BigDecimal("100000.0000"), referenceId);

        assertThat(second).isEqualTo(first);
        Wallet wallet = walletPersistencePort.findByAccountId(accountId).orElseThrow();
        assertThat(wallet.getAvailableBalance()).isEqualByComparingTo(new BigDecimal("900000.0000"));
    }
}
