package id.payu.investment.adapter.web;

import id.payu.investment.application.service.InvestmentApplicationService;
import id.payu.investment.domain.model.Deposit;
import id.payu.investment.domain.model.Gold;
import id.payu.investment.domain.model.InvestmentAccount;
import id.payu.investment.domain.model.InvestmentTransaction;
import id.payu.investment.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/investments")
@RequiredArgsConstructor
public class InvestmentController {

    private final InvestmentApplicationService investmentApplicationService;

    @PostMapping("/accounts")
    public CompletableFuture<ResponseEntity<InvestmentAccount>> createAccount(
            @Valid @RequestBody CreateInvestmentAccountRequest request) {
        return investmentApplicationService.createAccount(request.userId())
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/deposits")
    public CompletableFuture<ResponseEntity<Deposit>> buyDeposit(
            @RequestParam String accountId,
            @RequestParam String userId,
            @RequestParam BigDecimal amount,
            @RequestParam Integer tenure) {
        return investmentApplicationService.buyDeposit(accountId, userId, amount, tenure)
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/mutual-funds")
    public CompletableFuture<ResponseEntity<InvestmentTransaction>> buyMutualFund(
            @RequestParam String accountId,
            @RequestParam String userId,
            @RequestParam String fundCode,
            @RequestParam BigDecimal amount) {
        return investmentApplicationService.buyMutualFund(accountId, userId, fundCode, amount)
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/gold")
    public CompletableFuture<ResponseEntity<Gold>> buyGold(
            @RequestParam String userId,
            @RequestParam BigDecimal amount) {
        return investmentApplicationService.buyGold(userId, amount)
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/sell")
    public CompletableFuture<ResponseEntity<InvestmentTransaction>> sellInvestment(
            @RequestParam String accountId,
            @RequestParam UUID transactionId,
            @RequestParam BigDecimal amount) {
        return investmentApplicationService.sellInvestment(accountId, transactionId, amount)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/accounts/{userId}")
    public CompletableFuture<ResponseEntity<InvestmentAccount>> getAccount(@PathVariable String userId) {
        return investmentApplicationService.getAccountByUserId(userId)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/gold/{userId}")
    public CompletableFuture<ResponseEntity<Gold>> getGold(@PathVariable String userId) {
        return investmentApplicationService.getGoldByUserId(userId)
                .thenApply(ResponseEntity::ok);
    }
}
