package id.payu.wallet.application.service;

import id.payu.wallet.domain.model.EntryType;
import id.payu.wallet.domain.model.JournalEntry;
import id.payu.wallet.domain.model.JournalStatus;
import id.payu.wallet.domain.model.LedgerEntry;
import id.payu.wallet.domain.port.in.WalletClearingUseCase;
import id.payu.wallet.domain.port.out.JournalPersistencePort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

/**
 * ADR-0029 ISO20022 interbank clearing & suspense ledgering.
 * Every lifecycle stage persists exactly one immutable balanced double-entry
 * journal into journal_entries/ledger_entries via {@link JournalPersistencePort};
 * DB triggers enforce immutability (V112) and per-journal balance (V118),
 * V119 enforces one journal per (stage, referenceId).
 *
 * COA decision: keeps the 1500-series codes seeded by V115 (1500 suspense,
 * 1510-1540 rail suspense, 1550 NOSTRO_BI_FAST) plus existing 1100 user CASA
 * and 4100 fee revenue from V8 — the ADR's well-known-UUID scheme
 * (SystemAccountConstants) is aspirational and NOT adopted now; migrating the
 * COA to liability UUIDs would rewrite seeded accounts for no behavioural gain.
 */
@Service
public class WalletClearingService implements WalletClearingUseCase {

    static final String REF_TYPE_HOLD = "CLEARING_HOLD";
    static final String REF_TYPE_SETTLE = "CLEARING_SETTLE";
    static final String REF_TYPE_REVERSE = "CLEARING_REVERSE";

    private static final String COA_USER_CASA = "1100";
    private static final String COA_REVENUE_TRANSFER_FEE = "4100";

    private final JournalPersistencePort journalPersistencePort;

    public WalletClearingService(JournalPersistencePort journalPersistencePort) {
        this.journalPersistencePort = journalPersistencePort;
    }

    @Override
    @Transactional
    public String reserveAndHoldClearing(String accountId, String clearingAccountSlug,
                                         BigDecimal amount, BigDecimal fee,
                                         String referenceId, String description) {
        Optional<String> replay = postedJournalNumber(REF_TYPE_HOLD, referenceId);
        if (replay.isPresent()) {
            return replay.get();
        }

        BigDecimal amt = money(amount);
        BigDecimal f = money(fee);
        JournalEntry j = newJournal(REF_TYPE_HOLD, referenceId, description);
        leg(j, accountId, COA_USER_CASA, EntryType.DEBIT, amt.add(f));
        leg(j, accountId, clearingCode(clearingAccountSlug), EntryType.CREDIT, amt);
        if (f.signum() > 0) {
            leg(j, accountId, COA_REVENUE_TRANSFER_FEE, EntryType.CREDIT, f);
        }
        return post(j);
    }

    @Override
    @Transactional
    public void settleClearing(String clearingAccountSlug, String settlementAccountSlug,
                               BigDecimal amount, String referenceId) {
        if (postedJournalNumber(REF_TYPE_SETTLE, referenceId).isPresent()) {
            return;
        }

        JournalEntry j = newJournal(REF_TYPE_SETTLE, referenceId, "Clearing settle");
        leg(j, settlementAccountSlug, clearingCode(clearingAccountSlug), EntryType.DEBIT, money(amount));
        leg(j, settlementAccountSlug, nostroCode(settlementAccountSlug), EntryType.CREDIT, money(amount));
        post(j);
    }

    @Override
    @Transactional
    public void reverseClearing(String clearingAccountSlug, String accountId,
                                BigDecimal amount, BigDecimal fee,
                                String referenceId, String reason) {
        if (postedJournalNumber(REF_TYPE_REVERSE, referenceId).isPresent()) {
            return;
        }

        BigDecimal amt = money(amount);
        BigDecimal f = money(fee);
        JournalEntry j = newJournal(REF_TYPE_REVERSE, referenceId, "Clearing reverse: " + reason);
        leg(j, accountId, clearingCode(clearingAccountSlug), EntryType.DEBIT, amt);
        if (f.signum() > 0) {
            leg(j, accountId, COA_REVENUE_TRANSFER_FEE, EntryType.DEBIT, f);
        }
        leg(j, accountId, COA_USER_CASA, EntryType.CREDIT, amt.add(f));
        post(j);
    }

    // ---- helpers ----

    private Optional<String> postedJournalNumber(String referenceType, String referenceId) {
        return journalPersistencePort.findJournalsByReference(referenceType, referenceId).stream()
                .findFirst()
                .map(JournalEntry::getJournalNumber);
    }

    private JournalEntry newJournal(String referenceType, String referenceId, String description) {
        // no explicit id: Hibernate generates it (assigned id + null @Version breaks save)
        return JournalEntry.builder()
                .journalNumber(journalPersistencePort.generateJournalNumber())
                .description(description)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .status(JournalStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .createdBy("clearing-service")
                .entries(new ArrayList<>())
                .build();
    }

    private void leg(JournalEntry j, String accountId, String coaCode,
                     EntryType type, BigDecimal amount) {
        j.addEntry(LedgerEntry.builder()
                .transactionId(UUID.randomUUID())
                .accountId(accountId)
                .coaCode(coaCode)
                .entryType(type)
                .amount(amount)
                .currency("IDR")
                .balanceAfter(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_EVEN))
                .createdAt(LocalDateTime.now())
                .build());
    }

    private String post(JournalEntry j) {
        j.post(); // fail-fast UnbalancedJournal-style guard before any insert
        return journalPersistencePort.saveJournal(j).getJournalNumber();
    }

    /**
     * Rail slug -> 1500-series suspense code seeded by V115.
     * ponytail: string slugs until transaction-service wires the typed port; switch to an enum when it does
     */
    private static String clearingCode(String slug) {
        return switch (slug) {
            case "BI_FAST", "SYSTEM_BI_FAST_CLEARING" -> "1510";
            case "SKN", "SYSTEM_SKN_CLEARING" -> "1520";
            case "RTGS", "SYSTEM_RTGS_CLEARING" -> "1530";
            case "QRIS", "SYSTEM_QRIS_CLEARING" -> "1540";
            default -> "1500";
        };
    }

    private static String nostroCode(String slug) {
        if ("NOSTRO_BI_FAST".equals(slug)) {
            return "1550"; // only BI-FAST nostro seeded by V115 so far
        }
        throw new IllegalArgumentException("Unknown settlement account slug: " + slug
                + "; only NOSTRO_BI_FAST (1550) is seeded");
    }

    private static BigDecimal money(BigDecimal v) {
        return v.setScale(4, RoundingMode.HALF_EVEN);
    }
}
