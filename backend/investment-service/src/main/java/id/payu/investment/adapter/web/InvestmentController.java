package id.payu.investment.adapter.web;

import id.payu.api.common.response.ApiResponse;
import id.payu.commons.idempotency.Idempotent;
import id.payu.investment.application.service.InvestmentApplicationService;
import id.payu.investment.domain.model.Deposit;
import id.payu.investment.domain.model.Gold;
import id.payu.investment.domain.model.InvestmentAccount;
import id.payu.investment.domain.model.InvestmentTransaction;
import id.payu.investment.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import id.payu.security.annotation.Audited;
import id.payu.security.annotation.Audited.AuditLevel;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * REST Controller for investment operations.
 * Driving adapter in Hexagonal Architecture.
 */
@RestController
@RequestMapping("/api/v1/investments")
@RequiredArgsConstructor
@Tag(name = "Investment", description = "Investment management APIs for deposits, mutual funds, and gold")
public class InvestmentController extends BaseController {

    private final InvestmentApplicationService investmentApplicationService;

    @PostMapping("/accounts")
    @Operation(summary = "Create investment account", description = "Creates a new investment account for a user")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Account created successfully",
            content = @Content(schema = @Schema(implementation = InvestmentAccount.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request")
    public CompletableFuture<ResponseEntity<ApiResponse<InvestmentAccount>>> createAccount(
            @Valid @RequestBody CreateInvestmentAccountRequest request) {
        return investmentApplicationService.createAccount(request.userId())
                .thenApply(this::ok);
    }

    @PostMapping("/deposits")
    @Audited(
            operation = id.payu.security.annotation.Audited.Operation.OTHER,
            entityType = "Deposit",
            maskData = true,
            level = AuditLevel.INFO
    )
    @Idempotent(required = true)
    @Operation(summary = "Buy deposit", description = "Purchases a time deposit product")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Deposit purchased successfully",
            content = @Content(schema = @Schema(implementation = Deposit.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request or insufficient balance")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Account not found")
    public CompletableFuture<ResponseEntity<ApiResponse<Deposit>>> buyDeposit(
            @Parameter(description = "Account ID", required = true) @RequestParam String accountId,
            @Parameter(description = "User ID", required = true) @RequestParam String userId,
            @Parameter(description = "Amount to invest", required = true) @RequestParam BigDecimal amount,
            @Parameter(description = "Tenure in months", required = true) @RequestParam Integer tenure) {
        return investmentApplicationService.buyDeposit(accountId, userId, amount, tenure)
                .thenApply(this::ok);
    }

    @PostMapping("/mutual-funds")
    @Audited(
            operation = id.payu.security.annotation.Audited.Operation.OTHER,
            entityType = "InvestmentTransaction",
            maskData = true,
            level = AuditLevel.INFO
    )
    @Idempotent(required = true)
    @Operation(summary = "Buy mutual fund", description = "Purchases a mutual fund product")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Mutual fund purchased successfully",
            content = @Content(schema = @Schema(implementation = InvestmentTransaction.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request or insufficient balance")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Account or fund not found")
    public CompletableFuture<ResponseEntity<ApiResponse<InvestmentTransaction>>> buyMutualFund(
            @Parameter(description = "Account ID", required = true) @RequestParam String accountId,
            @Parameter(description = "User ID", required = true) @RequestParam String userId,
            @Parameter(description = "Fund code", required = true) @RequestParam String fundCode,
            @Parameter(description = "Amount to invest", required = true) @RequestParam BigDecimal amount) {
        return investmentApplicationService.buyMutualFund(accountId, userId, fundCode, amount)
                .thenApply(this::ok);
    }

    @PostMapping("/gold")
    @Audited(
            operation = id.payu.security.annotation.Audited.Operation.OTHER,
            entityType = "Gold",
            maskData = true,
            level = AuditLevel.INFO
    )
    @Idempotent(required = true)
    @Operation(summary = "Buy gold", description = "Purchases gold investment")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Gold purchased successfully",
            content = @Content(schema = @Schema(implementation = Gold.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request or insufficient balance")
    public CompletableFuture<ResponseEntity<ApiResponse<Gold>>> buyGold(
            @Parameter(description = "User ID", required = true) @RequestParam String userId,
            @Parameter(description = "Amount to invest", required = true) @RequestParam BigDecimal amount) {
        return investmentApplicationService.buyGold(userId, amount)
                .thenApply(this::ok);
    }

    @PostMapping("/sell")
    @Audited(
            operation = id.payu.security.annotation.Audited.Operation.OTHER,
            entityType = "InvestmentTransaction",
            maskData = true,
            level = AuditLevel.INFO
    )
    @Idempotent(required = true)
    @Operation(summary = "Sell investment", description = "Sells an existing investment")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Investment sold successfully",
            content = @Content(schema = @Schema(implementation = InvestmentTransaction.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Investment not found")
    public CompletableFuture<ResponseEntity<ApiResponse<InvestmentTransaction>>> sellInvestment(
            @Parameter(description = "Account ID", required = true) @RequestParam String accountId,
            @Parameter(description = "Transaction ID", required = true) @RequestParam UUID transactionId,
            @Parameter(description = "Amount to sell", required = true) @RequestParam BigDecimal amount) {
        return investmentApplicationService.sellInvestment(accountId, transactionId, amount)
                .thenApply(this::ok);
    }

    @GetMapping("/accounts/{userId}")
    @Operation(summary = "Get investment account", description = "Retrieves investment account for a user")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Account found",
            content = @Content(schema = @Schema(implementation = InvestmentAccount.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Account not found")
    public CompletableFuture<ResponseEntity<ApiResponse<InvestmentAccount>>> getAccount(
            @Parameter(description = "User ID", required = true) @PathVariable String userId) {
        return investmentApplicationService.getAccountByUserId(userId)
                .thenApply(this::ok);
    }

    @GetMapping("/gold/{userId}")
    @Operation(summary = "Get gold investment", description = "Retrieves gold investment for a user")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Gold investment found",
            content = @Content(schema = @Schema(implementation = Gold.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Gold investment not found")
    public CompletableFuture<ResponseEntity<ApiResponse<Gold>>> getGold(
            @Parameter(description = "User ID", required = true) @PathVariable String userId) {
        return investmentApplicationService.getGoldByUserId(userId)
                .thenApply(this::ok);
    }
}
