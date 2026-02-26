package id.payu.wallet.application.service;

import id.payu.wallet.domain.model.ChartOfAccount;
import id.payu.wallet.domain.model.JournalEntry;
import id.payu.wallet.domain.model.LedgerEntry;
import id.payu.wallet.domain.port.out.JournalPersistencePort;
import id.payu.wallet.dto.BalanceSheetResponse;
import id.payu.wallet.dto.DailySettlementResponse;
import id.payu.wallet.dto.IncomeStatementResponse;

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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeneralLedgerServiceTest {

    @Mock
    private JournalPersistencePort journalPersistencePort;

    @InjectMocks
    private GeneralLedgerService generalLedgerService;

    private ChartOfAccount assetCoa;
    private ChartOfAccount liabilityCoa;
    private ChartOfAccount revenueCoa;
    private ChartOfAccount expenseCoa;

    @BeforeEach
    void setUp() {
        assetCoa = ChartOfAccount.builder()
                .code("1100")
                .name("User Wallets")
                .accountType(ChartOfAccount.AccountType.ASSET)
                .normalBalance(ChartOfAccount.NormalBalance.DEBIT)
                .active(true)
                .build();

        liabilityCoa = ChartOfAccount.builder()
                .code("2100")
                .name("Escrow Holdings")
                .accountType(ChartOfAccount.AccountType.LIABILITY)
                .normalBalance(ChartOfAccount.NormalBalance.CREDIT)
                .active(true)
                .build();

        revenueCoa = ChartOfAccount.builder()
                .code("4100")
                .name("Transaction Fees")
                .accountType(ChartOfAccount.AccountType.REVENUE)
                .category(ChartOfAccount.AccountCategory.TRANSACTION_FEE)
                .normalBalance(ChartOfAccount.NormalBalance.CREDIT)
                .active(true)
                .build();

        expenseCoa = ChartOfAccount.builder()
                .code("5100")
                .name("Operational Costs")
                .accountType(ChartOfAccount.AccountType.EXPENSE)
                .category(ChartOfAccount.AccountCategory.OPERATIONAL_COST)
                .normalBalance(ChartOfAccount.NormalBalance.DEBIT)
                .active(true)
                .build();
    }

    @Nested
    @DisplayName("Balance Sheet")
    class BalanceSheetTests {

        @Test
        @DisplayName("Should generate balance sheet with assets and liabilities")
        void shouldGenerateBalanceSheet() {
            LocalDate asOfDate = LocalDate.of(2026, 2, 24);

            when(journalPersistencePort.findAllActiveChartOfAccounts())
                    .thenReturn(List.of(assetCoa, liabilityCoa, revenueCoa, expenseCoa));

            // Asset has 5M debit, so balance = 5M
            when(journalPersistencePort.findLedgerEntriesByCoaCodeAndDateRange(
                    eq("1100"), any(), any()))
                    .thenReturn(List.of(
                            LedgerEntry.builder()
                                    .entryType(LedgerEntry.EntryType.DEBIT)
                                    .amount(new BigDecimal("5000000"))
                                    .coaCode("1100")
                                    .build()
                    ));

            // Liability has 5M credit, so balance = 5M
            when(journalPersistencePort.findLedgerEntriesByCoaCodeAndDateRange(
                    eq("2100"), any(), any()))
                    .thenReturn(List.of(
                            LedgerEntry.builder()
                                    .entryType(LedgerEntry.EntryType.CREDIT)
                                    .amount(new BigDecimal("5000000"))
                                    .coaCode("2100")
                                    .build()
                    ));

            BalanceSheetResponse result = generalLedgerService.getBalanceSheet(asOfDate);

            assertThat(result).isNotNull();
            assertThat(result.getAsOfDate()).isEqualTo(asOfDate);
            assertThat(result.getTotalAssets()).isEqualByComparingTo(new BigDecimal("5000000"));
            assertThat(result.getTotalLiabilities()).isEqualByComparingTo(new BigDecimal("5000000"));
            assertThat(result.isBalanced()).isTrue();
            assertThat(result.getAssets()).hasSize(1);
            assertThat(result.getLiabilities()).hasSize(1);
        }

        @Test
        @DisplayName("Should skip zero-balance accounts")
        void shouldSkipZeroBalanceAccounts() {
            LocalDate asOfDate = LocalDate.of(2026, 2, 24);

            when(journalPersistencePort.findAllActiveChartOfAccounts())
                    .thenReturn(List.of(assetCoa, liabilityCoa));

            // Asset: 100 debit - 100 credit = 0
            when(journalPersistencePort.findLedgerEntriesByCoaCodeAndDateRange(
                    eq("1100"), any(), any()))
                    .thenReturn(List.of(
                            LedgerEntry.builder()
                                    .entryType(LedgerEntry.EntryType.DEBIT)
                                    .amount(new BigDecimal("100"))
                                    .build(),
                            LedgerEntry.builder()
                                    .entryType(LedgerEntry.EntryType.CREDIT)
                                    .amount(new BigDecimal("100"))
                                    .build()
                    ));

            when(journalPersistencePort.findLedgerEntriesByCoaCodeAndDateRange(
                    eq("2100"), any(), any()))
                    .thenReturn(List.of());

            BalanceSheetResponse result = generalLedgerService.getBalanceSheet(asOfDate);

            assertThat(result.getAssets()).isEmpty();
            assertThat(result.getLiabilities()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Income Statement")
    class IncomeStatementTests {

        @Test
        @DisplayName("Should generate income statement with revenue and expenses")
        void shouldGenerateIncomeStatement() {
            LocalDate from = LocalDate.of(2026, 2, 1);
            LocalDate to = LocalDate.of(2026, 2, 28);

            when(journalPersistencePort.findAllActiveChartOfAccounts())
                    .thenReturn(List.of(assetCoa, liabilityCoa, revenueCoa, expenseCoa));

            // Revenue: 2M credit (CREDIT normal balance)
            when(journalPersistencePort.findLedgerEntriesByCoaCodeAndDateRange(
                    eq("4100"), any(), any()))
                    .thenReturn(List.of(
                            LedgerEntry.builder()
                                    .entryType(LedgerEntry.EntryType.CREDIT)
                                    .amount(new BigDecimal("2000000"))
                                    .build()
                    ));

            // Expense: 800K debit (DEBIT normal balance)
            when(journalPersistencePort.findLedgerEntriesByCoaCodeAndDateRange(
                    eq("5100"), any(), any()))
                    .thenReturn(List.of(
                            LedgerEntry.builder()
                                    .entryType(LedgerEntry.EntryType.DEBIT)
                                    .amount(new BigDecimal("800000"))
                                    .build()
                    ));

            IncomeStatementResponse result = generalLedgerService.getIncomeStatement(from, to);

            assertThat(result).isNotNull();
            assertThat(result.getPeriodFrom()).isEqualTo(from);
            assertThat(result.getPeriodTo()).isEqualTo(to);
            assertThat(result.getTotalRevenue()).isEqualByComparingTo(new BigDecimal("2000000"));
            assertThat(result.getTotalExpenses()).isEqualByComparingTo(new BigDecimal("800000"));
            assertThat(result.getNetIncome()).isEqualByComparingTo(new BigDecimal("1200000"));
            assertThat(result.getRevenues()).hasSize(1);
            assertThat(result.getExpenses()).hasSize(1);
        }

        @Test
        @DisplayName("Should skip balance sheet accounts in income statement")
        void shouldSkipBalanceSheetAccounts() {
            LocalDate from = LocalDate.of(2026, 2, 1);
            LocalDate to = LocalDate.of(2026, 2, 28);

            when(journalPersistencePort.findAllActiveChartOfAccounts())
                    .thenReturn(List.of(assetCoa, liabilityCoa));

            IncomeStatementResponse result = generalLedgerService.getIncomeStatement(from, to);

            assertThat(result.getRevenues()).isEmpty();
            assertThat(result.getExpenses()).isEmpty();
            assertThat(result.getNetIncome()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("Daily Settlement")
    class DailySettlementTests {

        @Test
        @DisplayName("Should generate daily settlement report")
        void shouldGenerateDailySettlement() {
            LocalDate date = LocalDate.of(2026, 2, 24);
            LocalDateTime postedAt = date.atTime(10, 30);

            LedgerEntry debit = LedgerEntry.builder()
                    .entryType(LedgerEntry.EntryType.DEBIT)
                    .amount(new BigDecimal("1000000"))
                    .coaCode("1100")
                    .build();

            LedgerEntry credit = LedgerEntry.builder()
                    .entryType(LedgerEntry.EntryType.CREDIT)
                    .amount(new BigDecimal("1000000"))
                    .coaCode("2100")
                    .build();

            JournalEntry journal = JournalEntry.builder()
                    .id(UUID.randomUUID())
                    .journalNumber("JRN-20260224-0001")
                    .referenceType("TRANSFER")
                    .referenceId("TRX-001")
                    .status(JournalEntry.JournalStatus.POSTED)
                    .postedAt(postedAt)
                    .entries(List.of(debit, credit))
                    .build();

            when(journalPersistencePort.findJournalsByPostedAtBetween(any(), any()))
                    .thenReturn(List.of(journal));

            DailySettlementResponse result = generalLedgerService.getDailySettlementReport(date);

            assertThat(result).isNotNull();
            assertThat(result.getSettlementDate()).isEqualTo(date);
            assertThat(result.getTotalTransactions()).isEqualTo(1);
            assertThat(result.getTotalDebits()).isEqualByComparingTo(new BigDecimal("1000000"));
            assertThat(result.getTotalCredits()).isEqualByComparingTo(new BigDecimal("1000000"));
            assertThat(result.getNetSettlement()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getEntries()).hasSize(1);
        }

        @Test
        @DisplayName("Should return empty report when no journals")
        void shouldReturnEmptyWhenNoJournals() {
            LocalDate date = LocalDate.of(2026, 2, 24);

            when(journalPersistencePort.findJournalsByPostedAtBetween(any(), any()))
                    .thenReturn(List.of());

            DailySettlementResponse result = generalLedgerService.getDailySettlementReport(date);

            assertThat(result.getTotalTransactions()).isEqualTo(0);
            assertThat(result.getEntries()).isEmpty();
            assertThat(result.getTotalDebits()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }
}
