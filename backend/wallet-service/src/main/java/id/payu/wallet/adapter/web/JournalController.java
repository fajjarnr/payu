package id.payu.wallet.adapter.web;

import id.payu.api.common.response.ApiResponse;
import id.payu.commons.idempotency.Idempotent;
import id.payu.wallet.domain.model.JournalEntry;
import id.payu.wallet.domain.model.LedgerEntry;
import id.payu.wallet.domain.port.in.JournalUseCase;
import id.payu.wallet.interfaces.dto.CreateJournalRequest;
import id.payu.wallet.interfaces.dto.CreateJournalRequest.JournalLedgerEntryRequest;
import id.payu.wallet.interfaces.dto.TrialBalanceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import id.payu.wallet.domain.model.EntryType;

/**
 * REST Controller for double-entry journal operations (IMP-001).
 * Includes trial balance endpoint for accounting verification.
 */
@RestController
@RequestMapping("/api/v1/wallets")
@Tag(name = "Journal & Ledger", description = "Double-entry journal and trial balance APIs")
@SecurityRequirement(name = "bearerAuth")
public class JournalController extends BaseController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(JournalController.class);

    private final JournalUseCase journalUseCase;

    public JournalController(JournalUseCase journalUseCase) {
        this.journalUseCase = journalUseCase;
    }

    @PostMapping("/journals")
    @Idempotent(required = true)
    @PreAuthorize("hasRole('ADMIN') or hasRole('BACKOFFICE')")
    @Operation(summary = "Create journal entry", description = "Create a balanced double-entry journal with paired debit+credit entries")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Journal created and posted")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Unbalanced journal or validation error")
    public ResponseEntity<ApiResponse<JournalEntry>> createJournal(
            @Valid @RequestBody CreateJournalRequest request) {
        log.info("Creating journal: desc={}, entries={}", request.getDescription(), request.getEntries().size());

        List<LedgerEntry> entries = new ArrayList<>();
        for (JournalLedgerEntryRequest entryReq : request.getEntries()) {
            entries.add(LedgerEntry.builder()
                    .id(UUID.randomUUID())
                    .accountId(entryReq.getAccountId())
                    .coaCode(entryReq.getCoaCode())
                    .entryType(EntryType.valueOf(entryReq.getEntryType()))
                    .amount(entryReq.getAmount())
                    .currency(entryReq.getCurrency() != null ? entryReq.getCurrency() : "IDR")
                    .balanceAfter(BigDecimal.ZERO)
                    .createdAt(LocalDateTime.now())
                    .build());
        }

        JournalEntry journal = journalUseCase.createAndPostJournal(
                request.getDescription(),
                request.getReferenceType(),
                request.getReferenceId(),
                entries,
                "SYSTEM"
        );

        return ok(journal);
    }

    @GetMapping("/journals/{journalId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get journal entry", description = "Retrieve a journal entry by ID with all ledger entries")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Journal retrieved")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Journal not found")
    public ResponseEntity<ApiResponse<JournalEntry>> getJournal(
            @Parameter(description = "Journal ID") @PathVariable UUID journalId) {
        log.info("Getting journal: {}", journalId);
        JournalEntry journal = journalUseCase.getJournal(journalId);
        return ok(journal);
    }

    @GetMapping("/trial-balance")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BACKOFFICE')")
    @Operation(summary = "Get trial balance", description = "Generate trial balance report verifying sum(debit) == sum(credit)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Trial balance generated")
    public ResponseEntity<ApiResponse<TrialBalanceResponse>> getTrialBalance(
            @Parameter(description = "Period start date (optional)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Period end date (optional)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        log.info("Generating trial balance: from={}, to={}", from, to);

        TrialBalanceResponse response;
        if (from != null && to != null) {
            response = journalUseCase.getTrialBalance(from, to);
        } else {
            response = journalUseCase.getTrialBalance();
        }

        return ok(response);
    }
}
