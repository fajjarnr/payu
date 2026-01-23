package id.payu.wallet.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LedgerEntryTest {

    @Test
    @DisplayName("Should create ledger entry with builder")
    void shouldCreateLedgerEntryWithBuilder() {
        UUID id = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();

        LedgerEntry entry = LedgerEntry.builder()
                .id(id)
                .transactionId(transactionId)
                .accountId(accountId)
                .entryType(LedgerEntry.EntryType.DEBIT)
                .amount(new BigDecimal("5000000"))
                .currency("IDR")
                .balanceAfter(new BigDecimal("5000000"))
                .referenceType("RESERVATION")
                .referenceId("REF-001")
                .createdAt(LocalDateTime.now())
                .build();

        assertThat(entry).isNotNull();
        assertThat(entry.getId()).isEqualTo(id);
        assertThat(entry.getTransactionId()).isEqualTo(transactionId);
        assertThat(entry.getEntryType()).isEqualTo(LedgerEntry.EntryType.DEBIT);
    }

    @Test
    @DisplayName("Should create debit ledger entry")
    void shouldCreateDebitLedgerEntry() {
        LedgerEntry entry = LedgerEntry.builder()
                .entryType(LedgerEntry.EntryType.DEBIT)
                .build();

        assertThat(entry.getEntryType()).isEqualTo(LedgerEntry.EntryType.DEBIT);
    }

    @Test
    @DisplayName("Should create credit ledger entry")
    void shouldCreateCreditLedgerEntry() {
        LedgerEntry entry = LedgerEntry.builder()
                .entryType(LedgerEntry.EntryType.CREDIT)
                .build();

        assertThat(entry.getEntryType()).isEqualTo(LedgerEntry.EntryType.CREDIT);
    }
}
