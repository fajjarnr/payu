package id.payu.investment.adapter.web;

import id.payu.api.common.response.ApiResponse;
import id.payu.commons.idempotency.Idempotent;
import id.payu.investment.application.service.InvestmentApplicationService;
import id.payu.investment.application.service.InvestmentSecurityService;
import id.payu.investment.domain.model.Deposit;
import id.payu.investment.domain.model.Gold;
import id.payu.investment.domain.model.InvestmentAccount;
import id.payu.investment.domain.model.InvestmentTransaction;
import id.payu.investment.interfaces.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import id.payu.security.annotation.Audited;
import id.payu.security.annotation.AuditLevel;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import id.payu.security.annotation.AuditOperation;

/**
 * REST Controller for investment operations.
 * Driving adapter in Hexagonal Architecture.
 */
@RestController
@RequestMapping("/api/v1/investments")
@Tag(name = "Investment", description = "Investment management APIs for deposits, mutual funds, and gold")
@SecurityRequirement(name = "bearerAuth")
public class InvestmentController extends BaseController {

    private final InvestmentApplicationService investmentApplicationService;
    private final InvestmentSecurityService investmentSecurityService;

    public InvestmentController(InvestmentApplicationService investmentApplicationService,
                                InvestmentSecurityService investmentSecurityService) {
        this.investmentApplicationService = investmentApplicationService;
        this.investmentSecurityService = investmentSecurityService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Investment service status", description = "Returns investment service health and available endpoints")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getInvestmentStatus() {
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "service", "investment-service",
                "status", "UP",
                "version", "1.0.0"
        )));
    }

    @PostMapping("/accounts")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create investment account", description = "Creates a new investment account for a user")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Account created successfully",
            content = @Content(schema = @Schema(implementation = InvestmentAccount.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request")
    public CompletableFuture<ResponseEntity<ApiResponse<InvestmentAccount>>> createAccount(
            @AuthenticationPrincipal Jwt jwt) {
        // BUG-AUTH-013: Standardized to use 'account_id' claim with 'sub' fallback
        String userId = jwt.getClaimAsString("account_id") != null ? jwt.getClaimAsString("account_id") : jwt.getSubject();
        return investmentApplicationService.createAccount(userId)
                .orTimeout(30, TimeUnit.SECONDS)
                .thenApply(this::ok);
    }

    @PostMapping("/deposits")
    @PreAuthorize("isAuthenticated() and @investmentSecurityService.isAccountOwner(#request.accountId(), authentication.principal.subject)")
    @Audited(
            operation = AuditOperation.OTHER,
            entityType = "Deposit",
            maskData = true,
            level = AuditLevel.INFO
    )
    @Idempotent(required = true, headerName = "X-Idempotency-Key")
    @Operation(summary = "Buy deposit", description = "Purchases a time deposit product")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Deposit purchased successfully",
            content = @Content(schema = @Schema(implementation = Deposit.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request or insufficient balance")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Account not found")
    public CompletableFuture<ResponseEntity<ApiResponse<Deposit>>> buyDeposit(
            @Valid @RequestBody BuyDepositRequest request,
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal Jwt jwt) {
        // BUG-AUTH-013: Standardized to use 'account_id' claim with 'sub' fallback
        String userId = jwt.getClaimAsString("account_id") != null ? jwt.getClaimAsString("account_id") : jwt.getSubject();
        return investmentApplicationService.buyDeposit(request.accountId(), userId, request.amount(), request.tenure(), idempotencyKey)
                .orTimeout(30, TimeUnit.SECONDS)
                .thenApply(this::ok);
    }

    @PostMapping("/mutual-funds")
    @PreAuthorize("isAuthenticated() and @investmentSecurityService.isAccountOwner(#request.accountId(), authentication.principal.subject)")
    @Audited(
            operation = AuditOperation.OTHER,
            entityType = "InvestmentTransaction",
            maskData = true,
            level = AuditLevel.INFO
    )
    @Idempotent(required = true, headerName = "X-Idempotency-Key")
    @Operation(summary = "Buy mutual fund", description = "Purchases a mutual fund product")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Mutual fund purchased successfully",
            content = @Content(schema = @Schema(implementation = InvestmentTransaction.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request or insufficient balance")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Account or fund not found")
    public CompletableFuture<ResponseEntity<ApiResponse<InvestmentTransaction>>> buyMutualFund(
            @Valid @RequestBody BuyMutualFundRequest request,
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal Jwt jwt) {
        // BUG-AUTH-013: Standardized to use 'account_id' claim with 'sub' fallback
        String userId = jwt.getClaimAsString("account_id") != null ? jwt.getClaimAsString("account_id") : jwt.getSubject();
        return investmentApplicationService.buyMutualFund(request.accountId(), userId, request.fundCode(), request.amount(), idempotencyKey)
                .orTimeout(30, TimeUnit.SECONDS)
                .thenApply(this::ok);
    }

    @PostMapping("/gold")
    @PreAuthorize("isAuthenticated()")
    @Audited(
            operation = AuditOperation.OTHER,
            entityType = "Gold",
            maskData = true,
            level = AuditLevel.INFO
    )
    @Idempotent(required = true, headerName = "X-Idempotency-Key")
    @Operation(summary = "Buy gold", description = "Purchases gold investment")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Gold purchased successfully",
            content = @Content(schema = @Schema(implementation = Gold.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request or insufficient balance")
    public CompletableFuture<ResponseEntity<ApiResponse<Gold>>> buyGold(
            @Valid @RequestBody BuyGoldRequest request,
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal Jwt jwt) {
        // BUG-AUTH-013: Standardized to use 'account_id' claim with 'sub' fallback
        String userId = jwt.getClaimAsString("account_id") != null ? jwt.getClaimAsString("account_id") : jwt.getSubject();
        return investmentApplicationService.buyGold(userId, request.amount(), idempotencyKey)
                .orTimeout(30, TimeUnit.SECONDS)
                .thenApply(this::ok);
    }

    @PostMapping("/sell")
    @PreAuthorize("isAuthenticated() and @investmentSecurityService.isAccountOwner(#request.accountId(), authentication.principal.subject)")
    @Audited(
            operation = AuditOperation.OTHER,
            entityType = "InvestmentTransaction",
            maskData = true,
            level = AuditLevel.INFO
    )
    @Idempotent(required = true, headerName = "X-Idempotency-Key")
    @Operation(summary = "Sell investment", description = "Sells an existing investment")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Investment sold successfully",
            content = @Content(schema = @Schema(implementation = InvestmentTransaction.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Investment not found")
    public CompletableFuture<ResponseEntity<ApiResponse<InvestmentTransaction>>> sellInvestment(
            @Valid @RequestBody SellInvestmentRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return investmentApplicationService.sellInvestment(request.accountId(), request.transactionId(), request.amount())
                .orTimeout(30, TimeUnit.SECONDS)
                .thenApply(this::ok);
    }

    @GetMapping("/accounts")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List investment accounts", description = "Lists all investment accounts for the authenticated user")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Accounts found",
            content = @Content(schema = @Schema(implementation = InvestmentAccount.class)))
    public CompletableFuture<ResponseEntity<ApiResponse<List<InvestmentAccount>>>> listAccounts(
            @AuthenticationPrincipal Jwt jwt) {
        // BUG-AUTH-013: Standardized to use 'account_id' claim with 'sub' fallback
        String userId = jwt.getClaimAsString("account_id") != null ? jwt.getClaimAsString("account_id") : jwt.getSubject();
        return investmentApplicationService.getAccountsByUserId(userId)
                .orTimeout(30, TimeUnit.SECONDS)
                .thenApply(list -> ResponseEntity.ok(ApiResponse.success(list)));
    }

    @GetMapping("/accounts/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get investment account", description = "Retrieves investment account for the authenticated user")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Account found",
            content = @Content(schema = @Schema(implementation = InvestmentAccount.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Account not found")
    public CompletableFuture<ResponseEntity<ApiResponse<InvestmentAccount>>> getAccount(
            @AuthenticationPrincipal Jwt jwt) {
        // BUG-AUTH-013: Standardized to use 'account_id' claim with 'sub' fallback
        String userId = jwt.getClaimAsString("account_id") != null ? jwt.getClaimAsString("account_id") : jwt.getSubject();
        return investmentApplicationService.getAccountByUserId(userId)
                .orTimeout(30, TimeUnit.SECONDS)
                .thenApply(this::ok);
    }

    @GetMapping("/gold/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get gold investment", description = "Retrieves gold investment for the authenticated user")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Gold investment found",
            content = @Content(schema = @Schema(implementation = Gold.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Gold investment not found")
    public CompletableFuture<ResponseEntity<ApiResponse<Gold>>> getGold(
            @AuthenticationPrincipal Jwt jwt) {
        // BUG-AUTH-013: Standardized to use 'account_id' claim with 'sub' fallback
        String userId = jwt.getClaimAsString("account_id") != null ? jwt.getClaimAsString("account_id") : jwt.getSubject();
        return investmentApplicationService.getGoldByUserId(userId)
                .orTimeout(30, TimeUnit.SECONDS)
                .thenApply(this::ok);
    }
}
