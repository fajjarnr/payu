package id.payu.wallet.application.service;

import id.payu.wallet.domain.model.ChartOfAccount;
import id.payu.wallet.domain.model.JournalEntry;
import id.payu.wallet.domain.model.LedgerEntry;
import id.payu.wallet.domain.port.out.JournalPersistencePort;
import id.payu.wallet.dto.TrialBalanceResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JournalServiceTest {

    @Mock
    private JournalPersistencePort journalPersistencePort;

    @InjectMocks
    private JournalService journalService;

    private LedgerEntry debitEntry;
    private LedgerEntry creditEntry;

    @BeforeEach
    void setUp() {
        debitEntry = LedgerEntry.builder()
                .accountId("ACC-001")
                .entryType(LedgerEntry.EntryType.DEBIT)
                .amount(new BigDecimal("1000000"))
                .currency("IDR")
                .balanceAfter(new BigDecimal("1000000"))
                .coaCode("1100")
                .build();

        creditEntry = LedgerEntry.builder()
                .accountId("ACC-002")
                .entryType(LedgerEntry.EntryType.CREDIT)
                .amount(new BigDecimal("1000000"))
                .currency("IDR")
                .balanceAfter(BigDecimal.ZERO)
                .coaCode("2100")
                .build();
    }

    @Nested
    @DisplayName("createAndPostJournal")
    class CreateAndPostJournal {

        @Test
        @DisplayName("Should create and post a balanced journal")
        void shouldCreateAndPostBalancedJournal() {
            when(journalPersistencePort.generateJournalNumber()).thenReturn("JRN-20260224-0001");
            when(journalPersistencePort.saveJournal(any(JournalEntry.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            JournalEntry result = journalService.createAndPostJournal(
                    "Test transfer", "TRANSFER", "TRX-001",
                    List.of(debitEntry, creditEntry), "system"
            );

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(JournalEntry.JournalStatus.POSTED);
            assertThat(result.getJournalNumber()).isEqualTo("JRN-20260224-0001");
            assertThat(result.getPostedAt()).isNotNull();
            assertThat(result.getEntries()).hasSize(2);

            verify(journalPersistencePort).generateJournalNumber();
            verify(journalPersistencePort).saveJournal(any(JournalEntry.class));
        }

        @Test
        @DisplayName("Should reject null entries")
        void shouldRejectNullEntries() {
            assertThatThrownBy(() -> journalService.createAndPostJournal(
                    "Test", "TRANSFER", "TRX-001", null, "system"
            )).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at least 2 entries");
        }

        @Test
        @DisplayName("Should reject single entry")
        void shouldRejectSingleEntry() {
            assertThatThrownBy(() -> journalService.createAndPostJournal(
                    "Test", "TRANSFER", "TRX-001", List.of(debitEntry), "system"
            )).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at least 2 entries");
        }

        @Test
        @DisplayName("Should reject unbalanced entries")
        void shouldRejectUnbalancedEntries() {
            when(journalPersistencePort.generateJournalNumber()).thenReturn("JRN-001");

            LedgerEntry smallCredit = LedgerEntry.builder()
                    .accountId("ACC-002")
                    .entryType(LedgerEntry.EntryType.CREDIT)
                    .amount(new BigDecimal("500000"))
                    .currency("IDR")
                    .balanceAfter(BigDecimal.ZERO)
                    .coaCode("2100")
                    .build();

            assertThatThrownBy(() -> journalService.createAndPostJournal(
                    "Test", "TRANSFER", "TRX-001",
                    List.of(debitEntry, smallCredit), "system"
            )).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("not balanced");
        }

        @Test
        @DisplayName("Should assign IDs to entries without IDs")
        void shouldAssignIds() {
            when(journalPersistencePort.generateJournalNumber()).thenReturn("JRN-001");
            when(journalPersistencePort.saveJournal(any(JournalEntry.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // Create entries without IDs
            LedgerEntry noIdDebit = LedgerEntry.builder()
                    .accountId("ACC-001")
                    .entryType(LedgerEntry.EntryType.DEBIT)
                    .amount(new BigDecimal("100"))
                    .currency("IDR")
                    .coaCode("1100")
                    .build();

            LedgerEntry noIdCredit = LedgerEntry.builder()
                    .accountId("ACC-002")
                    .entryType(LedgerEntry.EntryType.CREDIT)
                    .amount(new BigDecimal("100"))
                    .currency("IDR")
                    .coaCode("2100")
                    .build();

            JournalEntry result = journalService.createAndPostJournal(
                    "Test", "TRANSFER", "TRX-001",
                    List.of(noIdDebit, noIdCredit), "system"
            );

            assertThat(result.getEntries()).allMatch(e -> e.getId() != null);
            assertThat(result.getEntries()).allMatch(e -> e.getTransactionId() != null);
            assertThat(result.getEntries()).allMatch(e -> e.getCreatedAt() != null);
        }
    }

    @Nested
    @DisplayName("getJournal")
    class GetJournal {

        @Test
        @DisplayName("Should return journal by ID")
        void shouldReturnJournal() {
            UUID journalId = UUID.randomUUID();
            JournalEntry journal = JournalEntry.builder()
                    .id(journalId)
                    .journalNumber("JRN-001")
                    .status(JournalEntry.JournalStatus.POSTED)
                    .build();

            when(journalPersistencePort.findJournalById(journalId)).thenReturn(Optional.of(journal));

            JournalEntry result = journalService.getJournal(journalId);

            assertThat(result.getId()).isEqualTo(journalId);
            verify(journalPersistencePort).findJournalById(journalId);
        }

        @Test
        @DisplayName("Should throw when journal not found")
        void shouldThrowWhenNotFound() {
            UUID journalId = UUID.randomUUID();
            when(journalPersistencePort.findJournalById(journalId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> journalService.getJournal(journalId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Journal not found");
        }
    }

    @Nested
    @DisplayName("getTrialBalance")
    class GetTrialBalance {

        @Test
        @DisplayName("Should generate trial balance grouped by CoA code")
        void shouldGenerateTrialBalance() {
            List<LedgerEntry> entries = List.of(
                    LedgerEntry.builder()
                            .entryType(LedgerEntry.EntryType.DEBIT)
                            .amount(new BigDecimal("5000000"))
                            .coaCode("1100")
                            .createdAt(LocalDateTime.now())
                            .build(),
                    LedgerEntry.builder()
                            .entryType(LedgerEntry.EntryType.CREDIT)
                            .amount(new BigDecimal("5000000"))
                            .coaCode("2100")
                            .createdAt(LocalDateTime.now())
                            .build()
            );

            ChartOfAccount userWallets = ChartOfAccount.builder()
                    .code("1100")
                    .name("User Wallets")
                    .accountType(ChartOfAccount.AccountType.ASSET)
                    .build();

            ChartOfAccount escrowHoldings = ChartOfAccount.builder()
                    .code("2100")
                    .name("Escrow Holdings")
                    .accountType(ChartOfAccount.AccountType.LIABILITY)
                    .build();

            when(journalPersistencePort.findAllLedgerEntries()).thenReturn(entries);
            when(journalPersistencePort.findChartOfAccountByCode("1100"))
                    .thenReturn(Optional.of(userWallets));
            when(journalPersistencePort.findChartOfAccountByCode("2100"))
                    .thenReturn(Optional.of(escrowHoldings));

            TrialBalanceResponse result = journalService.getTrialBalance();

            assertThat(result).isNotNull();
            assertThat(result.isBalanced()).isTrue();
            assertThat(result.getTotalDebits()).isEqualByComparingTo(new BigDecimal("5000000"));
            assertThat(result.getTotalCredits()).isEqualByComparingTo(new BigDecimal("5000000"));
            assertThat(result.getEntries()).hasSize(2);
        }

        @Test
        @DisplayName("Should generate trial balance with date range")
        void shouldGenerateTrialBalanceWithDateRange() {
            LocalDate from = LocalDate.of(2026, 2, 1);
            LocalDate to = LocalDate.of(2026, 2, 28);

            LedgerEntry inRange = LedgerEntry.builder()
                    .entryType(LedgerEntry.EntryType.DEBIT)
                    .amount(new BigDecimal("1000"))
                    .coaCode("1100")
                    .createdAt(LocalDateTime.of(2026, 2, 15, 10, 0))
                    .build();

            LedgerEntry outOfRange = LedgerEntry.builder()
                    .entryType(LedgerEntry.EntryType.CREDIT)
                    .amount(new BigDecimal("1000"))
                    .coaCode("2100")
                    .createdAt(LocalDateTime.of(2026, 1, 15, 10, 0))
                    .build();

            when(journalPersistencePort.findAllLedgerEntries()).thenReturn(List.of(inRange, outOfRange));
            when(journalPersistencePort.findChartOfAccountByCode("1100"))
                    .thenReturn(Optional.of(ChartOfAccount.builder()
                            .code("1100").name("User Wallets")
                            .accountType(ChartOfAccount.AccountType.ASSET).build()));

            TrialBalanceResponse result = journalService.getTrialBalance(from, to);

            assertThat(result).isNotNull();
            assertThat(result.getEntries()).hasSize(1);
            assertThat(result.getPeriodFrom()).isEqualTo(from);
            assertThat(result.getPeriodTo()).isEqualTo(to);
        }
    }
}
