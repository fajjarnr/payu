package id.payu.wallet.adapter.web;

import id.payu.api.common.response.ApiResponse;
import id.payu.wallet.domain.port.in.GeneralLedgerUseCase;
import id.payu.wallet.interfaces.dto.BalanceSheetResponse;
import id.payu.wallet.interfaces.dto.DailySettlementResponse;
import id.payu.wallet.interfaces.dto.IncomeStatementResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * REST Controller for General Ledger reporting (IMP-012).
 * Balance sheet, income statement, and daily settlement report.
 * Backoffice/reporting only — no frontend impact.
 */
@RestController
@RequestMapping("/api/v1/wallets/gl")
@Tag(name = "General Ledger", description = "Financial reporting APIs (Balance Sheet, Income Statement, Settlement)")
@SecurityRequirement(name = "bearerAuth")
public class GeneralLedgerController extends BaseController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GeneralLedgerController.class);

    private final GeneralLedgerUseCase generalLedgerUseCase;

    public GeneralLedgerController(GeneralLedgerUseCase generalLedgerUseCase) {
        this.generalLedgerUseCase = generalLedgerUseCase;
    }

    @GetMapping("/balance-sheet")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BACKOFFICE')")
    @Operation(summary = "Get balance sheet", description = "Generate balance sheet (neraca) as of a specific date")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Balance sheet generated")
    public ResponseEntity<ApiResponse<BalanceSheetResponse>> getBalanceSheet(
            @Parameter(description = "As-of date (default: today)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {
        LocalDate effectiveDate = asOfDate != null ? asOfDate : LocalDate.now();
        log.info("Generating balance sheet as of {}", effectiveDate);
        BalanceSheetResponse response = generalLedgerUseCase.getBalanceSheet(effectiveDate);
        return ok(response);
    }

    @GetMapping("/income-statement")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BACKOFFICE')")
    @Operation(summary = "Get income statement", description = "Generate income statement (laba-rugi) for a date range")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Income statement generated")
    public ResponseEntity<ApiResponse<IncomeStatementResponse>> getIncomeStatement(
            @Parameter(description = "Period start date", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Period end date", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        log.info("Generating income statement: {} to {}", from, to);
        IncomeStatementResponse response = generalLedgerUseCase.getIncomeStatement(from, to);
        return ok(response);
    }

    @GetMapping("/daily-settlement")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BACKOFFICE')")
    @Operation(summary = "Get daily settlement report", description = "Generate daily settlement report for partner reconciliation")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Settlement report generated")
    public ResponseEntity<ApiResponse<DailySettlementResponse>> getDailySettlement(
            @Parameter(description = "Settlement date (default: today)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate effectiveDate = date != null ? date : LocalDate.now();
        log.info("Generating daily settlement report for {}", effectiveDate);
        DailySettlementResponse response = generalLedgerUseCase.getDailySettlementReport(effectiveDate);
        return ok(response);
    }
}
