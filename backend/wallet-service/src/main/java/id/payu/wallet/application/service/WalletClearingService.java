package id.payu.wallet.application.service;

import id.payu.wallet.domain.model.EntryType;
import id.payu.wallet.domain.model.JournalEntry;
import id.payu.wallet.domain.model.LedgerEntry;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * ADR-0029 ISO20022 clearing & ADR-0038 saga: stub clearing use-case.
 * Real impl: reserve -> hold in SYSTEM_*_CLEARING, settle -> NOSTRO, reverse -> back.
 * ponytail: in-memory journal only, no DB persist yet — add LedgerRepositoryPort + @Transactional when wiring transaction-service
 */
@Service
public class WalletClearingService {

    public JournalEntry reserveAndHoldClearing(String referenceId, BigDecimal amount, String channel) {
        String clearingCode = switch (channel) {
            case "BI_FAST" -> "1510";
            case "SKN" -> "1520";
            case "RTGS" -> "1530";
            case "QRIS" -> "1540";
            default -> "1500";
        };
        BigDecimal amt = amount.setScale(4, RoundingMode.HALF_EVEN);
        JournalEntry j = new JournalEntry();
        j.setId(UUID.randomUUID());
        j.setJournalNumber("CLR-" + UUID.randomUUID().toString().substring(0, 8));
        j.setDescription("Clearing hold " + channel);
        j.setReferenceType("CLEARING_HOLD");
        j.setReferenceId(referenceId);
        j.setCreatedAt(LocalDateTime.now());

        LedgerEntry debit = new LedgerEntry();
        debit.setId(UUID.randomUUID());
        debit.setJournalEntryId(j.getId());
        debit.setAccountId(clearingCode);
        debit.setAmount(amt);
        debit.setEntryType(EntryType.DEBIT);
        debit.setCoaCode(clearingCode);

        LedgerEntry credit = new LedgerEntry();
        credit.setId(UUID.randomUUID());
        credit.setJournalEntryId(j.getId());
        credit.setAccountId("USER_WALLET");
        credit.setAmount(amt);
        credit.setEntryType(EntryType.CREDIT);
        credit.setCoaCode("1100");

        j.setEntries(List.of(debit, credit));
        if (!j.isBalanced()) throw new IllegalStateException("Clearing hold not balanced");
        return j;
    }

    public JournalEntry settleClearing(String referenceId, BigDecimal amount) {
        BigDecimal amt = amount.setScale(4, RoundingMode.HALF_EVEN);
        JournalEntry j = new JournalEntry();
        j.setId(UUID.randomUUID());
        j.setJournalNumber("STL-" + UUID.randomUUID().toString().substring(0, 8));
        j.setDescription("Clearing settle");
        j.setReferenceType("CLEARING_SETTLE");
        j.setReferenceId(referenceId);
        j.setCreatedAt(LocalDateTime.now());
        LedgerEntry debit = new LedgerEntry();
        debit.setId(UUID.randomUUID());
        debit.setJournalEntryId(j.getId());
        debit.setAccountId("1550");
        debit.setAmount(amt);
        debit.setEntryType(EntryType.DEBIT);
        debit.setCoaCode("1550");
        LedgerEntry credit = new LedgerEntry();
        credit.setId(UUID.randomUUID());
        credit.setJournalEntryId(j.getId());
        credit.setAccountId("1510");
        credit.setAmount(amt);
        credit.setEntryType(EntryType.CREDIT);
        credit.setCoaCode("1510");
        j.setEntries(List.of(debit, credit));
        if (!j.isBalanced()) throw new IllegalStateException("Settle not balanced");
        return j;
    }

    public JournalEntry reverseClearing(String referenceId, BigDecimal amount, String reason) {
        BigDecimal amt = amount.setScale(4, RoundingMode.HALF_EVEN);
        JournalEntry j = new JournalEntry();
        j.setId(UUID.randomUUID());
        j.setJournalNumber("REV-" + UUID.randomUUID().toString().substring(0, 8));
        j.setDescription("Clearing reverse: " + reason);
        j.setReferenceType("CLEARING_REVERSE");
        j.setReferenceId(referenceId);
        j.setCreatedAt(LocalDateTime.now());
        LedgerEntry debit = new LedgerEntry();
        debit.setId(UUID.randomUUID());
        debit.setJournalEntryId(j.getId());
        debit.setAccountId("USER_WALLET");
        debit.setAmount(amt);
        debit.setEntryType(EntryType.DEBIT);
        debit.setCoaCode("1100");
        LedgerEntry credit = new LedgerEntry();
        credit.setId(UUID.randomUUID());
        credit.setJournalEntryId(j.getId());
        credit.setAccountId("1510");
        credit.setAmount(amt);
        credit.setEntryType(EntryType.CREDIT);
        credit.setCoaCode("1510");
        j.setEntries(List.of(debit, credit));
        if (!j.isBalanced()) throw new IllegalStateException("Reverse not balanced");
        return j;
    }
}
