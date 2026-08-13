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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * QAMVP-002: wallet transfer money journey against real PostgreSQL
 * (Testcontainers) — debit sender + credit recipient atomically, idempotent
 * replay, and outbox events.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {"payu.grpc.server.port=0", "spring.jpa.hibernate.ddl-auto=none"})
@Import(TestcontainersConfig.class)
@DisplayName("QAMVP-002 — wallet transfer journey (real PostgreSQL + outbox)")
class WalletTransferIntegrationTest {

    @Autowired
    private WalletUseCase walletUseCase;

    @Autowired
    private WalletPersistencePort walletPersistencePort;

    @Autowired
    private OutboxRepository outboxRepository;

    @MockitoBean
    private CacheService cacheService;

    private String seedWallet(String accountId) {
        Wallet wallet = Wallet.builder()
                .accountId(accountId)
                .currency("IDR")
                .balance(new BigDecimal("1000000.0000"))
                .reservedBalance(BigDecimal.ZERO)
                .status(id.payu.wallet.domain.model.WalletStatus.ACTIVE)
                .version(0L)
                .createdAt(LocalDateTime.now())
                .build();
        walletPersistencePort.save(wallet);
        return accountId;
    }

    @Test
    @DisplayName("transfer debits sender, credits recipient, writes ledger + outbox")
    void transferJourney() {
        String sender = seedWallet("sender-" + UUID.randomUUID().toString().substring(0, 8));
        String recipient = seedWallet("recip-" + UUID.randomUUID().toString().substring(0, 8));
        String referenceId = "TFR-" + UUID.randomUUID();

        String transactionId = walletUseCase.transfer(
                sender, recipient, new BigDecimal("200000.0000"), "IDR", referenceId, "test transfer");

        assertThat(transactionId).isNotBlank();

        Wallet senderWallet = walletPersistencePort.findByAccountId(sender).orElseThrow();
        Wallet recipientWallet = walletPersistencePort.findByAccountId(recipient).orElseThrow();
        assertThat(senderWallet.getBalance()).isEqualByComparingTo(new BigDecimal("800000.0000"));
        assertThat(recipientWallet.getBalance()).isEqualByComparingTo(new BigDecimal("1200000.0000"));

        assertThat(outboxRepository.findByAggregateId(sender))
                .as("transfer must publish outbox events")
                .isNotEmpty();
    }

    @Test
    @DisplayName("transfer replay with same reference is idempotent (no double debit)")
    void transferIdempotentReplay() {
        String sender = seedWallet("sender-replay-" + UUID.randomUUID().toString().substring(0, 8));
        String recipient = seedWallet("recip-replay-" + UUID.randomUUID().toString().substring(0, 8));
        String referenceId = "TFR-" + UUID.randomUUID();

        String first = walletUseCase.transfer(
                sender, recipient, new BigDecimal("100000.0000"), "IDR", referenceId, "t");
        String second = walletUseCase.transfer(
                sender, recipient, new BigDecimal("100000.0000"), "IDR", referenceId, "t");

        assertThat(second).isEqualTo(first);
        Wallet senderWallet = walletPersistencePort.findByAccountId(sender).orElseThrow();
        assertThat(senderWallet.getBalance()).isEqualByComparingTo(new BigDecimal("900000.0000"));
    }

    @Test
    @DisplayName("transfer to self or invalid args is rejected")
    void transferValidation() {
        String accountId = seedWallet("self-" + UUID.randomUUID().toString().substring(0, 8));

        assertThatThrownBy(() -> walletUseCase.transfer(
                accountId, accountId, new BigDecimal("100.0000"), "IDR", "ref-self", "t"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> walletUseCase.transfer(
                accountId, "other-" + UUID.randomUUID(), new BigDecimal("-5.0000"), "IDR", "ref-neg", "t"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> walletUseCase.transfer(
                accountId, "other-" + UUID.randomUUID(), new BigDecimal("100.0000"), null, "ref-null", "t"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
