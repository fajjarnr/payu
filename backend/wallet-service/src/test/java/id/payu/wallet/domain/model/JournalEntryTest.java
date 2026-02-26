package id.payu.wallet.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JournalEntryTest {

    private LedgerEntry debitEntry(BigDecimal amount, String coaCode) {
        return LedgerEntry.builder()
                .id(UUID.randomUUID())
                .transactionId(UUID.randomUUID())
                .accountId("ACC-001")
                .entryType(LedgerEntry.EntryType.DEBIT)
                .amount(amount)
                .currency("IDR")
                .balanceAfter(amount)
                .coaCode(coaCode)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private LedgerEntry creditEntry(BigDecimal amount, String coaCode) {
        return LedgerEntry.builder()
                .id(UUID.randomUUID())
                .transactionId(UUID.randomUUID())
                .accountId("ACC-002")
                .entryType(LedgerEntry.EntryType.CREDIT)
                .amount(amount)
                .currency("IDR")
                .balanceAfter(amount)
                .coaCode(coaCode)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should create journal entry with builder")
    void shouldCreateJournalEntryWithBuilder() {
        UUID id = UUID.randomUUID();
        JournalEntry journal = JournalEntry.builder()
                .id(id)
                .journalNumber("JRN-20260224-0001")
                .description("Test transfer")
                .referenceType("TRANSFER")
                .referenceId("TRX-001")
                .status(JournalEntry.JournalStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .createdBy("system")
                .build();

        assertThat(journal).isNotNull();
        assertThat(journal.getId()).isEqualTo(id);
        assertThat(journal.getJournalNumber()).isEqualTo("JRN-20260224-0001");
        assertThat(journal.getStatus()).isEqualTo(JournalEntry.JournalStatus.PENDING);
    }

    @Nested
    @DisplayName("Balance validation")
    class BalanceValidation {

        @Test
        @DisplayName("Should return true when debit equals credit")
        void shouldReturnTrueWhenBalanced() {
            BigDecimal amount = new BigDecimal("1000000");
            JournalEntry journal = JournalEntry.builder()
                    .id(UUID.randomUUID())
                    .journalNumber("JRN-001")
                    .status(JournalEntry.JournalStatus.PENDING)
                    .entries(List.of(
                            debitEntry(amount, "1100"),
                            creditEntry(amount, "2100")
                    ))
                    .build();

            assertThat(journal.isBalanced()).isTrue();
        }

        @Test
        @DisplayName("Should return false when debit does not equal credit")
        void shouldReturnFalseWhenUnbalanced() {
            JournalEntry journal = JournalEntry.builder()
                    .id(UUID.randomUUID())
                    .journalNumber("JRN-002")
                    .status(JournalEntry.JournalStatus.PENDING)
                    .entries(List.of(
                            debitEntry(new BigDecimal("1000000"), "1100"),
                            creditEntry(new BigDecimal("500000"), "2100")
                    ))
                    .build();

            assertThat(journal.isBalanced()).isFalse();
        }

        @Test
        @DisplayName("Should handle multiple debit and credit entries")
        void shouldHandleMultipleEntries() {
            JournalEntry journal = JournalEntry.builder()
                    .id(UUID.randomUUID())
                    .journalNumber("JRN-003")
                    .status(JournalEntry.JournalStatus.PENDING)
                    .entries(List.of(
                            debitEntry(new BigDecimal("500000"), "1100"),
                            debitEntry(new BigDecimal("500000"), "1200"),
                            creditEntry(new BigDecimal("1000000"), "2100")
                    ))
                    .build();

            assertThat(journal.isBalanced()).isTrue();
            assertThat(journal.getTotalDebit()).isEqualByComparingTo(new BigDecimal("1000000"));
            assertThat(journal.getTotalCredit()).isEqualByComparingTo(new BigDecimal("1000000"));
        }
    }

    @Nested
    @DisplayName("Matching pairs validation")
    class MatchingPairsValidation {

        @Test
        @DisplayName("Should return true when has both debit and credit")
        void shouldReturnTrueWithPairs() {
            JournalEntry journal = JournalEntry.builder()
                    .id(UUID.randomUUID())
                    .journalNumber("JRN-004")
                    .entries(List.of(
                            debitEntry(new BigDecimal("100"), "1100"),
                            creditEntry(new BigDecimal("100"), "2100")
                    ))
                    .build();

            assertThat(journal.hasMatchingPairs()).isTrue();
        }

        @Test
        @DisplayName("Should return false when only debit entries")
        void shouldReturnFalseWithOnlyDebits() {
            JournalEntry journal = JournalEntry.builder()
                    .id(UUID.randomUUID())
                    .journalNumber("JRN-005")
                    .entries(List.of(
                            debitEntry(new BigDecimal("100"), "1100")
                    ))
                    .build();

            assertThat(journal.hasMatchingPairs()).isFalse();
        }

        @Test
        @DisplayName("Should return false when no entries")
        void shouldReturnFalseWhenEmpty() {
            JournalEntry journal = JournalEntry.builder()
                    .id(UUID.randomUUID())
                    .journalNumber("JRN-006")
                    .build();

            assertThat(journal.hasMatchingPairs()).isFalse();
        }
    }

    @Nested
    @DisplayName("Post operation")
    class PostOperation {

        @Test
        @DisplayName("Should post successfully when balanced with matching pairs")
        void shouldPostWhenValid() {
            BigDecimal amount = new BigDecimal("5000000");
            JournalEntry journal = JournalEntry.builder()
                    .id(UUID.randomUUID())
                    .journalNumber("JRN-007")
                    .status(JournalEntry.JournalStatus.PENDING)
                    .entries(List.of(
                            debitEntry(amount, "1100"),
                            creditEntry(amount, "2100")
                    ))
                    .build();

            journal.post();

            assertThat(journal.getStatus()).isEqualTo(JournalEntry.JournalStatus.POSTED);
            assertThat(journal.getPostedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should throw when posting unbalanced journal")
        void shouldThrowWhenUnbalanced() {
            JournalEntry journal = JournalEntry.builder()
                    .id(UUID.randomUUID())
                    .journalNumber("JRN-008")
                    .status(JournalEntry.JournalStatus.PENDING)
                    .entries(List.of(
                            debitEntry(new BigDecimal("1000"), "1100"),
                            creditEntry(new BigDecimal("500"), "2100")
                    ))
                    .build();

            assertThatThrownBy(journal::post)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("not balanced");
        }

        @Test
        @DisplayName("Should throw when posting without matching pairs")
        void shouldThrowWhenNoPairs() {
            JournalEntry journal = JournalEntry.builder()
                    .id(UUID.randomUUID())
                    .journalNumber("JRN-009")
                    .status(JournalEntry.JournalStatus.PENDING)
                    .entries(List.of(
                            debitEntry(new BigDecimal("1000"), "1100")
                    ))
                    .build();

            assertThatThrownBy(journal::post)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("at least one DEBIT and one CREDIT");
        }
    }

    @Test
    @DisplayName("Should add entry to journal")
    void shouldAddEntry() {
        JournalEntry journal = JournalEntry.builder()
                .id(UUID.randomUUID())
                .journalNumber("JRN-010")
                .build();

        journal.addEntry(debitEntry(new BigDecimal("1000"), "1100"));
        journal.addEntry(creditEntry(new BigDecimal("1000"), "2100"));

        assertThat(journal.getEntries()).hasSize(2);
        assertThat(journal.isBalanced()).isTrue();
        assertThat(journal.hasMatchingPairs()).isTrue();
    }

    @Test
    @DisplayName("Should calculate total debit and credit amounts")
    void shouldCalculateTotals() {
        JournalEntry journal = JournalEntry.builder()
                .id(UUID.randomUUID())
                .journalNumber("JRN-011")
                .entries(List.of(
                        debitEntry(new BigDecimal("300000"), "1100"),
                        debitEntry(new BigDecimal("200000"), "1200"),
                        creditEntry(new BigDecimal("500000"), "2100")
                ))
                .build();

        assertThat(journal.getTotalDebit()).isEqualByComparingTo(new BigDecimal("500000"));
        assertThat(journal.getTotalCredit()).isEqualByComparingTo(new BigDecimal("500000"));
    }
}
