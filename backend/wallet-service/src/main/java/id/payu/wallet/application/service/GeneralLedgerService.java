package id.payu.wallet.application.service;

import id.payu.wallet.domain.model.ChartOfAccount;
import id.payu.wallet.domain.model.JournalEntry;
import id.payu.wallet.domain.model.LedgerEntry;
import id.payu.wallet.domain.port.in.GeneralLedgerUseCase;
import id.payu.wallet.domain.port.out.JournalPersistencePort;
import id.payu.wallet.interfaces.dto.BalanceSheetResponse;
import id.payu.wallet.interfaces.dto.BalanceSheetResponse.BalanceSheetEntry;
import id.payu.wallet.interfaces.dto.DailySettlementResponse;
import id.payu.wallet.interfaces.dto.DailySettlementResponse.SettlementEntry;
import id.payu.wallet.interfaces.dto.IncomeStatementResponse;
import id.payu.wallet.interfaces.dto.IncomeStatementResponse.IncomeStatementEntry;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import id.payu.wallet.domain.model.AccountType;
import id.payu.wallet.domain.model.EntryType;
import id.payu.wallet.domain.model.NormalBalance;

/**
 * General Ledger engine for financial reporting (IMP-012).
 * Provides balance sheet, income statement, and daily settlement reports.
 */
@Service
public class GeneralLedgerService implements GeneralLedgerUseCase {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GeneralLedgerService.class);

    private final JournalPersistencePort journalPersistencePort;

    public GeneralLedgerService(JournalPersistencePort journalPersistencePort) {
        this.journalPersistencePort = journalPersistencePort;
    }

    @Override
    @Transactional(readOnly = true)
    public BalanceSheetResponse getBalanceSheet(LocalDate asOfDate) {
        log.info("Generating balance sheet as of {}", asOfDate);

        List<ChartOfAccount> allCoa = journalPersistencePort.findAllActiveChartOfAccounts();
        LocalDateTime endOfDay = asOfDate.plusDays(1).atStartOfDay();

        List<BalanceSheetEntry> assets = new ArrayList<>();
        List<BalanceSheetEntry> liabilities = new ArrayList<>();
        List<BalanceSheetEntry> equity = new ArrayList<>();

        BigDecimal totalAssets = BigDecimal.ZERO;
        BigDecimal totalLiabilities = BigDecimal.ZERO;
        BigDecimal totalEquity = BigDecimal.ZERO;

        for (ChartOfAccount coa : allCoa) {
            if (!coa.isBalanceSheetAccount()) {
                continue;
            }

            // Get all entries for this CoA up to the date
            List<LedgerEntry> entries = journalPersistencePort
                    .findLedgerEntriesByCoaCodeAndDateRange(
                            coa.getCode(),
                            LocalDateTime.of(2000, 1, 1, 0, 0),
                            endOfDay);

            BigDecimal balance = computeBalance(entries, coa.getNormalBalance());

            if (balance.compareTo(BigDecimal.ZERO) == 0) {
                continue; // Skip zero-balance accounts
            }

            BalanceSheetEntry bsEntry = new BalanceSheetEntry(
                    coa.getCode(), coa.getName(),
                    coa.getCategory() != null ? coa.getCategory().name() : coa.getAccountType().name(),
                    balance
            );

            switch (coa.getAccountType()) {
                case ASSET:
                    assets.add(bsEntry);
                    totalAssets = totalAssets.add(balance);
                    break;
                case LIABILITY:
                    liabilities.add(bsEntry);
                    totalLiabilities = totalLiabilities.add(balance);
                    break;
                case EQUITY:
                    equity.add(bsEntry);
                    totalEquity = totalEquity.add(balance);
                    break;
                default:
                    break;
            }
        }

        boolean balanced = totalAssets.compareTo(totalLiabilities.add(totalEquity)) == 0;

        return BalanceSheetResponse.builder()
                .asOfDate(asOfDate)
                .assets(assets)
                .liabilities(liabilities)
                .equity(equity)
                .totalAssets(totalAssets)
                .totalLiabilities(totalLiabilities)
                .totalEquity(totalEquity)
                .balanced(balanced)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public IncomeStatementResponse getIncomeStatement(LocalDate from, LocalDate to) {
        log.info("Generating income statement: {} to {}", from, to);

        List<ChartOfAccount> allCoa = journalPersistencePort.findAllActiveChartOfAccounts();
        LocalDateTime fromDateTime = from.atStartOfDay();
        LocalDateTime toDateTime = to.plusDays(1).atStartOfDay();

        List<IncomeStatementEntry> revenues = new ArrayList<>();
        List<IncomeStatementEntry> expenses = new ArrayList<>();

        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;

        for (ChartOfAccount coa : allCoa) {
            if (!coa.isIncomeStatementAccount()) {
                continue;
            }

            List<LedgerEntry> entries = journalPersistencePort
                    .findLedgerEntriesByCoaCodeAndDateRange(coa.getCode(), fromDateTime, toDateTime);

            BigDecimal amount = computeBalance(entries, coa.getNormalBalance());

            if (amount.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            IncomeStatementEntry isEntry = new IncomeStatementEntry(
                    coa.getCode(), coa.getName(),
                    coa.getCategory() != null ? coa.getCategory().name() : coa.getAccountType().name(),
                    amount
            );

            if (coa.getAccountType() == AccountType.REVENUE) {
                revenues.add(isEntry);
                totalRevenue = totalRevenue.add(amount);
            } else if (coa.getAccountType() == AccountType.EXPENSE) {
                expenses.add(isEntry);
                totalExpenses = totalExpenses.add(amount);
            }
        }

        return IncomeStatementResponse.builder()
                .periodFrom(from)
                .periodTo(to)
                .revenues(revenues)
                .expenses(expenses)
                .totalRevenue(totalRevenue)
                .totalExpenses(totalExpenses)
                .netIncome(totalRevenue.subtract(totalExpenses))
                .generatedAt(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public DailySettlementResponse getDailySettlementReport(LocalDate date) {
        log.info("Generating daily settlement report for {}", date);

        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to = date.plusDays(1).atStartOfDay();

        List<JournalEntry> journals = journalPersistencePort.findJournalsByPostedAtBetween(from, to);

        List<SettlementEntry> settlementEntries = new ArrayList<>();
        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;

        for (JournalEntry journal : journals) {
            // Find debit and credit accounts from entries
            String debitAccount = journal.getEntries().stream()
                    .filter(e -> e.getEntryType() == EntryType.DEBIT)
                    .map(e -> e.getCoaCode() != null ? e.getCoaCode() : e.getAccountId())
                    .findFirst()
                    .orElse("N/A");

            String creditAccount = journal.getEntries().stream()
                    .filter(e -> e.getEntryType() == EntryType.CREDIT)
                    .map(e -> e.getCoaCode() != null ? e.getCoaCode() : e.getAccountId())
                    .findFirst()
                    .orElse("N/A");

            BigDecimal journalDebit = journal.getTotalDebit();
            BigDecimal journalCredit = journal.getTotalCredit();

            settlementEntries.add(new SettlementEntry(
                    journal.getJournalNumber(),
                    journal.getReferenceType(),
                    journal.getReferenceId(),
                    debitAccount,
                    creditAccount,
                    journalDebit,
                    journal.getPostedAt()
            ));

            totalDebits = totalDebits.add(journalDebit);
            totalCredits = totalCredits.add(journalCredit);
        }

        return DailySettlementResponse.builder()
                .settlementDate(date)
                .totalTransactions(journals.size())
                .totalDebits(totalDebits)
                .totalCredits(totalCredits)
                .netSettlement(totalDebits.subtract(totalCredits))
                .entries(settlementEntries)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Computes the balance for an account based on its normal balance side.
     * For DEBIT-normal accounts (Assets, Expenses): balance = sum(debit) - sum(credit)
     * For CREDIT-normal accounts (Liabilities, Equity, Revenue): balance = sum(credit) - sum(debit)
     */
    private BigDecimal computeBalance(List<LedgerEntry> entries, NormalBalance normalBalance) {
        BigDecimal totalDebit = entries.stream()
                .filter(e -> e.getEntryType() == EntryType.DEBIT)
                .map(LedgerEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCredit = entries.stream()
                .filter(e -> e.getEntryType() == EntryType.CREDIT)
                .map(LedgerEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (normalBalance == NormalBalance.DEBIT) {
            return totalDebit.subtract(totalCredit);
        } else {
            return totalCredit.subtract(totalDebit);
        }
    }
}
