package id.payu.wallet.integration;

import id.payu.cache.service.CacheService;
import id.payu.wallet.domain.model.EntryType;
import id.payu.wallet.domain.model.JournalEntry;
import id.payu.wallet.domain.port.in.WalletClearingUseCase;
import id.payu.wallet.domain.port.out.JournalPersistencePort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.junit.jupiter.api.DisplayName;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ADR-0029 / B1.2 (ARCH-GLOBAL-003): interbank clearing suspense journaling
 * against real PostgreSQL — every clearing lifecycle stage must persist a
 * balanced double-entry journal (sum(debit) == sum(credit) on persisted rows)
 * and replays with the same referenceId must be idempotent (no new rows).
 */
@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.flyway.enabled=true",
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
    "payu.grpc.server.enabled=false"
})
@DisplayName("ADR-0029 — clearing suspense ledger double-entry + idempotency")
class ClearingLedgerDoubleEntryInvariantTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("wallet_clearing_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.primary.hikari.jdbc-url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.primary.hikari.username", POSTGRES::getUsername);
        registry.add("spring.datasource.primary.hikari.password", POSTGRES::getPassword);
        registry.add("spring.datasource.primary.hikari.driver-class-name", () -> "org.postgresql.Driver");
    }
    @Autowired
    private WalletClearingUseCase clearingUseCase;

    @Autowired
    private JournalPersistencePort journalPersistencePort;

    @MockitoBean
    private CacheService cacheService;

    private BigDecimal total(JournalEntry j, EntryType type) {
        return j.getEntries().stream()
                .filter(e -> e.getEntryType() == type)
                .map(e -> e.getAmount().setScale(4, RoundingMode.HALF_EVEN))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<JournalEntry> persistedJournals(String referenceType, String referenceId) {
        return journalPersistencePort.findJournalsByReference(referenceType, referenceId);
    }

    private void assertBalancedOnPersistedRows(List<JournalEntry> journals) {
        assertThat(journals).isNotEmpty();
        for (JournalEntry j : journals) {
            assertThat(total(j, EntryType.DEBIT))
                    .as("journal %s must balance on persisted rows", j.getJournalNumber())
                    .isEqualByComparingTo(total(j, EntryType.CREDIT));
        }
    }

    @Test
    @DisplayName("outbound hold persists balanced journal: debit CASA(amount+fee) == credit suspense + fee revenue")
    void holdPersistsBalancedDoubleEntry() {
        String referenceId = "PACS008-" + java.util.UUID.randomUUID();
        BigDecimal amount = new BigDecimal("1000000.0000");
        BigDecimal fee = new BigDecimal("2500.0000");

        String journalNumber = clearingUseCase.reserveAndHoldClearing(
                "user-acc-1", "BI_FAST", amount, fee, referenceId, "outbound BI-FAST");

        assertThat(journalNumber).isNotBlank();
        List<JournalEntry> journals = persistedJournals("CLEARING_HOLD", referenceId);
        assertThat(journals).hasSize(1);
        assertBalancedOnPersistedRows(journals);

        JournalEntry j = journals.get(0);
        // suspense leg credited on the 1500-series BI-FAST clearing account
        assertThat(j.getEntries()).anyMatch(e ->
                e.getCoaCode().equals("1510") && e.getEntryType() == EntryType.CREDIT
                        && e.getAmount().compareTo(amount) == 0);
        // user CASA side debited principal + fee
        assertThat(j.getEntries()).anyMatch(e ->
                e.getCoaCode().equals("1100") && e.getEntryType() == EntryType.DEBIT
                        && e.getAmount().compareTo(amount.add(fee)) == 0);
    }

    @Test
    @DisplayName("settlement clears suspense to nostro and stays balanced")
    void settlePersistsBalancedDoubleEntry() {
        String referenceId = "PACS002-" + java.util.UUID.randomUUID();
        BigDecimal amount = new BigDecimal("500000.0000");

        clearingUseCase.reserveAndHoldClearing(
                "user-acc-2", "BI_FAST", amount, BigDecimal.ZERO, referenceId, "hold");
        clearingUseCase.settleClearing("BI_FAST", "NOSTRO_BI_FAST", amount, referenceId);

        List<JournalEntry> settles = persistedJournals("CLEARING_SETTLE", referenceId);
        assertThat(settles).hasSize(1);
        assertBalancedOnPersistedRows(settles);
        assertThat(settles.get(0).getEntries()).anyMatch(e ->
                e.getCoaCode().equals("1550") && e.getEntryType() == EntryType.CREDIT);

        // full lifecycle: suspense account 1510 nets back to zero
        BigDecimal suspenseNet = persistedJournals("CLEARING_HOLD", referenceId).stream()
                .flatMap(j -> j.getEntries().stream())
                .filter(e -> e.getCoaCode().equals("1510"))
                .map(e -> e.getEntryType() == EntryType.DEBIT
                        ? e.getAmount() : e.getAmount().negate())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(persistedJournals("CLEARING_SETTLE", referenceId).stream()
                        .flatMap(j -> j.getEntries().stream())
                        .filter(e -> e.getCoaCode().equals("1510"))
                        .map(e -> e.getEntryType() == EntryType.DEBIT
                                ? e.getAmount() : e.getAmount().negate())
                        .reduce(BigDecimal.ZERO, BigDecimal::add));
        assertThat(suspenseNet.setScale(4, RoundingMode.HALF_EVEN))
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("rejected transfer reversal compensates hold and stays balanced")
    void reversePersistsBalancedDoubleEntry() {
        String referenceId = "PACS004-" + java.util.UUID.randomUUID();
        BigDecimal amount = new BigDecimal("750000.0000");
        BigDecimal fee = new BigDecimal("2500.0000");

        clearingUseCase.reserveAndHoldClearing(
                "user-acc-3", "BI_FAST", amount, fee, referenceId, "hold");
        clearingUseCase.reverseClearing("BI_FAST", "user-acc-3",
                amount, fee, referenceId, "pacs.002 RJCT");

        List<JournalEntry> reversals = persistedJournals("CLEARING_REVERSE", referenceId);
        assertThat(reversals).hasSize(1);
        assertBalancedOnPersistedRows(reversals);
    }

    @Test
    @DisplayName("duplicate callback replay with same referenceId inserts nothing new")
    void replayWithSameReferenceIdIsIdempotent() {
        String referenceId = "REPLAY-" + java.util.UUID.randomUUID();
        BigDecimal amount = new BigDecimal("200000.0000");

        String first = clearingUseCase.reserveAndHoldClearing(
                "user-acc-4", "BI_FAST", amount, BigDecimal.ZERO, referenceId, "first");
        String replay = clearingUseCase.reserveAndHoldClearing(
                "user-acc-4", "BI_FAST", amount, BigDecimal.ZERO, referenceId, "replay");

        assertThat(replay).isEqualTo(first);
        assertThat(persistedJournals("CLEARING_HOLD", referenceId)).hasSize(1);

        // settle replay must be idempotent too
        clearingUseCase.settleClearing("BI_FAST", "NOSTRO_BI_FAST", amount, referenceId);
        clearingUseCase.settleClearing("BI_FAST", "NOSTRO_BI_FAST", amount, referenceId);
        assertThat(persistedJournals("CLEARING_SETTLE", referenceId)).hasSize(1);

        // reverse replay must be idempotent too
        clearingUseCase.reverseClearing("BI_FAST", "user-acc-4",
                amount, BigDecimal.ZERO, referenceId, "rjct");
        clearingUseCase.reverseClearing("BI_FAST", "user-acc-4",
                amount, BigDecimal.ZERO, referenceId, "rjct");
        assertThat(persistedJournals("CLEARING_REVERSE", referenceId)).hasSize(1);
    }
}
