package id.payu.wallet.domain.port.in;

import id.payu.wallet.dto.BalanceSheetResponse;
import id.payu.wallet.dto.DailySettlementResponse;
import id.payu.wallet.dto.IncomeStatementResponse;

import java.time.LocalDate;

/**
 * Input port for General Ledger (GL) engine use cases — IMP-012.
 * Provides financial reporting for settlement reconciliation.
 */
public interface GeneralLedgerUseCase {

    /**
     * Generate balance sheet (neraca) as of a specific date.
     */
    BalanceSheetResponse getBalanceSheet(LocalDate asOfDate);

    /**
     * Generate income statement (laba-rugi) for a date range.
     */
    IncomeStatementResponse getIncomeStatement(LocalDate from, LocalDate to);

    /**
     * Generate daily settlement report for partner reconciliation (e.g., TokoBapak).
     */
    DailySettlementResponse getDailySettlementReport(LocalDate date);
}
