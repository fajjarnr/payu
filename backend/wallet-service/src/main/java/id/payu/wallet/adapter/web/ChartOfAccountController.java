package id.payu.wallet.adapter.web;

import id.payu.api.common.response.ApiResponse;
import id.payu.wallet.domain.model.ChartOfAccount;
import id.payu.wallet.domain.port.in.ChartOfAccountUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Chart of Accounts (IMP-002).
 * Backend/backoffice only — no frontend impact.
 */
@RestController
@RequestMapping("/api/v1/wallets/chart-of-accounts")
@Tag(name = "Chart of Accounts", description = "GL account classification APIs")
@SecurityRequirement(name = "bearerAuth")
public class ChartOfAccountController extends BaseController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ChartOfAccountController.class);

    private final ChartOfAccountUseCase chartOfAccountUseCase;

    public ChartOfAccountController(ChartOfAccountUseCase chartOfAccountUseCase) {
        this.chartOfAccountUseCase = chartOfAccountUseCase;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('BACKOFFICE')")
    @Operation(summary = "Get all chart of accounts", description = "Retrieve all active chart of accounts")
    public ResponseEntity<ApiResponse<List<ChartOfAccount>>> getAllAccounts() {
        log.info("Getting all chart of accounts");
        List<ChartOfAccount> accounts = chartOfAccountUseCase.getAllActiveAccounts();
        return ok(accounts);
    }

    @GetMapping("/{code}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BACKOFFICE')")
    @Operation(summary = "Get account by code", description = "Retrieve a chart of account by its code")
    public ResponseEntity<ApiResponse<ChartOfAccount>> getByCode(
            @Parameter(description = "Account code (e.g., 1100)") @PathVariable String code) {
        log.info("Getting chart of account by code: {}", code);
        ChartOfAccount account = chartOfAccountUseCase.getByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + code));
        return ok(account);
    }

    @GetMapping("/type/{type}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BACKOFFICE')")
    @Operation(summary = "Get accounts by type", description = "Retrieve chart of accounts by type (ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE)")
    public ResponseEntity<ApiResponse<List<ChartOfAccount>>> getByType(
            @Parameter(description = "Account type") @PathVariable String type) {
        log.info("Getting chart of accounts by type: {}", type);
        ChartOfAccount.AccountType accountType = ChartOfAccount.AccountType.valueOf(type.toUpperCase());
        List<ChartOfAccount> accounts = chartOfAccountUseCase.getByType(accountType);
        return ok(accounts);
    }

    @GetMapping("/children/{parentId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BACKOFFICE')")
    @Operation(summary = "Get child accounts", description = "Retrieve child accounts of a parent account")
    public ResponseEntity<ApiResponse<List<ChartOfAccount>>> getChildren(
            @Parameter(description = "Parent account ID") @PathVariable UUID parentId) {
        log.info("Getting children of account: {}", parentId);
        List<ChartOfAccount> children = chartOfAccountUseCase.getChildren(parentId);
        return ok(children);
    }
}
