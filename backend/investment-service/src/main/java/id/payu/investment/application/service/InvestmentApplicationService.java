package id.payu.investment.application.service;

import id.payu.investment.domain.model.Deposit;
import id.payu.investment.domain.model.Gold;
import id.payu.investment.domain.model.InvestmentAccount;
import id.payu.investment.domain.model.InvestmentTransaction;
import id.payu.investment.domain.model.MutualFund;
import id.payu.investment.domain.port.in.*;
import id.payu.investment.domain.port.out.InvestmentEventPublisherPort;
import id.payu.investment.domain.port.out.InvestmentPersistencePort;
import id.payu.investment.domain.port.out.WalletServicePort;
import id.payu.investment.dto.InvestmentEvent;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvestmentApplicationService implements
        CreateInvestmentAccountUseCase,
        BuyDepositUseCase,
        BuyMutualFundUseCase,
        BuyGoldUseCase,
        SellInvestmentUseCase {

    private final InvestmentPersistencePort investmentPersistencePort;

    public CompletableFuture<InvestmentAccount> getAccountByUserId(String userId) {
        return CompletableFuture.completedFuture(
                investmentPersistencePort.findAccountByUserId(userId)
                        .orElseThrow(() -> new IllegalArgumentException("Investment account not found"))
        );
    }

    public CompletableFuture<Gold> getGoldByUserId(String userId) {
        return CompletableFuture.completedFuture(
                investmentPersistencePort.findGoldByUserId(userId)
                        .orElseThrow(() -> new IllegalArgumentException("Gold holdings not found"))
        );
    }
    private final WalletServicePort walletServicePort;
    private final InvestmentEventPublisherPort investmentEventPublisherPort;

    @Override
    @Transactional
    @Async
    @CircuitBreaker(name = "walletService", fallbackMethod = "createAccountFallback")
    @Retry(name = "walletService")
    @TimeLimiter(name = "walletService")
    public CompletableFuture<InvestmentAccount> createAccount(String userId) {
        log.info("Creating investment account for user: {}", userId);

        if (investmentPersistencePort.existsAccountByUserId(userId)) {
            throw new IllegalArgumentException("Investment account already exists for user");
        }

        InvestmentAccount account = InvestmentAccount.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .totalBalance(BigDecimal.ZERO)
                .availableBalance(BigDecimal.ZERO)
                .lockedBalance(BigDecimal.ZERO)
                .status(InvestmentAccount.AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        InvestmentAccount savedAccount = investmentPersistencePort.saveAccount(account);

        log.info("Investment account created successfully: {}", savedAccount.getId());

        investmentEventPublisherPort.publishInvestmentCreated(new InvestmentEvent(
                savedAccount.getId(),
                userId,
                "ACCOUNT_CREATED",
                null,
                BigDecimal.ZERO,
                "COMPLETED",
                LocalDateTime.now()));

        return CompletableFuture.completedFuture(savedAccount);
    }

    @Override
    @Transactional
    @Async
    @CircuitBreaker(name = "walletService", fallbackMethod = "buyDepositFallback")
    @Retry(name = "walletService")
    @TimeLimiter(name = "walletService")
    public CompletableFuture<Deposit> buyDeposit(String accountId, String userId, BigDecimal amount, int tenure) {
        log.info("Processing deposit purchase for user: {}, amount: {}", userId, amount);

        InvestmentAccount account = investmentPersistencePort.findAccountById(UUID.fromString(accountId))
                .orElseThrow(() -> new IllegalArgumentException("Investment account not found"));

        BigDecimal interestRate = calculateDepositInterestRate(tenure);
        BigDecimal maturityAmount = calculateMaturityAmount(amount, interestRate, tenure);

        if (!walletServicePort.hasSufficientBalance(userId, amount)) {
            throw new IllegalArgumentException("Insufficient wallet balance");
        }

        walletServicePort.deductBalance(userId, amount);

        LocalDateTime now = LocalDateTime.now();
        Deposit deposit = Deposit.builder()
                .id(UUID.randomUUID())
                .accountId(accountId)
                .amount(amount)
                .tenure(tenure)
                .interestRate(interestRate)
                .maturityAmount(maturityAmount)
                .startDate(now)
                .maturityDate(now.plusMonths(tenure))
                .status(Deposit.DepositStatus.ACTIVE)
                .currency("IDR")
                .createdAt(now)
                .updatedAt(now)
                .build();

        Deposit savedDeposit = investmentPersistencePort.saveDeposit(deposit);
        investmentPersistencePort.updateAccountBalance(account.getId(), amount);

        InvestmentTransaction transaction = InvestmentTransaction.builder()
                .id(UUID.randomUUID())
                .accountId(accountId)
                .type(InvestmentTransaction.TransactionType.BUY)
                .investmentType(InvestmentTransaction.InvestmentType.DEPOSIT)
                .investmentId(savedDeposit.getId().toString())
                .amount(amount)
                .price(BigDecimal.ZERO)
                .units(BigDecimal.ONE)
                .fee(BigDecimal.ZERO)
                .currency("IDR")
                .status(InvestmentTransaction.TransactionStatus.COMPLETED)
                .referenceNumber("DEP-" + System.currentTimeMillis())
                .createdAt(now)
                .updatedAt(now)
                .build();

        investmentPersistencePort.saveTransaction(transaction);

        log.info("Deposit purchased successfully: {}", savedDeposit.getId());

        investmentEventPublisherPort.publishInvestmentCompleted(new InvestmentEvent(
                savedDeposit.getId(),
                userId,
                "DEPOSIT_PURCHASED",
                "DEPOSIT",
                amount,
                "COMPLETED",
                LocalDateTime.now()));

        return CompletableFuture.completedFuture(savedDeposit);
    }

    @Override
    @Transactional
    @Async
    @CircuitBreaker(name = "walletService", fallbackMethod = "buyMutualFundFallback")
    @Retry(name = "walletService")
    @TimeLimiter(name = "walletService")
    public CompletableFuture<InvestmentTransaction> buyMutualFund(String accountId, String userId, 
            String fundCode, BigDecimal amount) {
        log.info("Processing mutual fund purchase for user: {}, fund: {}, amount: {}", userId, fundCode, amount);

        InvestmentAccount account = investmentPersistencePort.findAccountById(UUID.fromString(accountId))
                .orElseThrow(() -> new IllegalArgumentException("Investment account not found"));

        MutualFund fund = investmentPersistencePort.getLatestFundPrice(fundCode);
        if (fund == null || fund.getStatus() != MutualFund.FundStatus.ACTIVE) {
            throw new IllegalArgumentException("Mutual fund not available");
        }

        if (amount.compareTo(fund.getMinimumInvestment()) < 0) {
            throw new IllegalArgumentException("Amount below minimum investment");
        }

        if (!walletServicePort.hasSufficientBalance(userId, amount)) {
            throw new IllegalArgumentException("Insufficient wallet balance");
        }

        walletServicePort.deductBalance(userId, amount);

        BigDecimal units = amount.divide(fund.getNavPerUnit(), 4, RoundingMode.DOWN);
        BigDecimal fee = amount.multiply(fund.getRedemptionFee());

        LocalDateTime now = LocalDateTime.now();
        InvestmentTransaction transaction = InvestmentTransaction.builder()
                .id(UUID.randomUUID())
                .accountId(accountId)
                .type(InvestmentTransaction.TransactionType.BUY)
                .investmentType(InvestmentTransaction.InvestmentType.MUTUAL_FUND)
                .investmentId(fundCode)
                .amount(amount)
                .price(fund.getNavPerUnit())
                .units(units)
                .fee(fee)
                .currency("IDR")
                .status(InvestmentTransaction.TransactionStatus.COMPLETED)
                .referenceNumber("MF-" + System.currentTimeMillis())
                .createdAt(now)
                .updatedAt(now)
                .build();

        InvestmentTransaction savedTransaction = investmentPersistencePort.saveTransaction(transaction);
        investmentPersistencePort.updateAccountBalance(account.getId(), amount);

        log.info("Mutual fund purchased successfully: {}", savedTransaction.getId());

        investmentEventPublisherPort.publishInvestmentCompleted(new InvestmentEvent(
                savedTransaction.getId(),
                userId,
                "MUTUAL_FUND_PURCHASED",
                "MUTUAL_FUND",
                amount,
                "COMPLETED",
                LocalDateTime.now()));

        return CompletableFuture.completedFuture(savedTransaction);
    }

    @Override
    @Transactional
    @Async
    @CircuitBreaker(name = "walletService", fallbackMethod = "buyGoldFallback")
    @Retry(name = "walletService")
    @TimeLimiter(name = "walletService")
    public CompletableFuture<Gold> buyGold(String userId, BigDecimal amount) {
        log.info("Processing gold purchase for user: {}, amount: {}", userId, amount);

        BigDecimal currentPrice = investmentPersistencePort.getLatestGoldPrice();
        if (currentPrice == null) {
            throw new IllegalArgumentException("Gold price not available");
        }

        if (!walletServicePort.hasSufficientBalance(userId, amount)) {
            throw new IllegalArgumentException("Insufficient wallet balance");
        }

        walletServicePort.deductBalance(userId, amount);

        Gold gold = investmentPersistencePort.findGoldByUserId(userId).orElse(null);

        if (gold == null) {
            gold = Gold.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .amount(amount.divide(currentPrice, 4, RoundingMode.DOWN))
                    .averageBuyPrice(currentPrice)
                    .currentPrice(currentPrice)
                    .currentValue(amount)
                    .unrealizedProfitLoss(BigDecimal.ZERO)
                    .lastPriceUpdate(LocalDateTime.now())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
        } else {
            BigDecimal newAmount = gold.getAmount().add(amount.divide(currentPrice, 4, RoundingMode.DOWN));
            BigDecimal newAveragePrice = gold.getAverageBuyPrice()
                    .multiply(gold.getAmount())
                    .add(currentPrice.multiply(amount.divide(currentPrice, 4, RoundingMode.DOWN)))
                    .divide(newAmount, 2, RoundingMode.HALF_UP);

            gold.setAmount(newAmount);
            gold.setAverageBuyPrice(newAveragePrice);
            gold.setCurrentPrice(currentPrice);
            gold.setCurrentValue(newAmount.multiply(currentPrice));
            gold.setUnrealizedProfitLoss(gold.getCurrentValue().subtract(
                    gold.getAmount().multiply(gold.getAverageBuyPrice())));
            gold.setLastPriceUpdate(LocalDateTime.now());
            gold.setUpdatedAt(LocalDateTime.now());
        }

        Gold savedGold = investmentPersistencePort.saveGold(gold);

        log.info("Gold purchased successfully: {}", savedGold.getId());

        investmentEventPublisherPort.publishInvestmentCompleted(new InvestmentEvent(
                savedGold.getId(),
                userId,
                "GOLD_PURCHASED",
                "GOLD",
                amount,
                "COMPLETED",
                LocalDateTime.now()));

        return CompletableFuture.completedFuture(savedGold);
    }

    @Override
    @Transactional
    @Async
    @CircuitBreaker(name = "walletService", fallbackMethod = "sellInvestmentFallback")
    @Retry(name = "walletService")
    @TimeLimiter(name = "walletService")
    public CompletableFuture<InvestmentTransaction> sellInvestment(String accountId, UUID transactionId, BigDecimal amount) {
        log.info("Processing investment sell for account: {}, transaction: {}, amount: {}", accountId, transactionId, amount);

        InvestmentTransaction existingTransaction = investmentPersistencePort.findTransactionById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        if (existingTransaction.getStatus() != InvestmentTransaction.TransactionStatus.COMPLETED) {
            throw new IllegalArgumentException("Cannot sell investment with status: " + existingTransaction.getStatus());
        }

        if (existingTransaction.getType() != InvestmentTransaction.TransactionType.BUY) {
            throw new IllegalArgumentException("Can only sell purchased investments");
        }

        BigDecimal currentPrice;
        if (existingTransaction.getInvestmentType() == InvestmentTransaction.InvestmentType.MUTUAL_FUND) {
            MutualFund fund = investmentPersistencePort.getLatestFundPrice(existingTransaction.getInvestmentId());
            currentPrice = fund.getNavPerUnit();
        } else if (existingTransaction.getInvestmentType() == InvestmentTransaction.InvestmentType.GOLD) {
            currentPrice = investmentPersistencePort.getLatestGoldPrice();
        } else {
            throw new IllegalArgumentException("Cannot sell deposit before maturity");
        }

        BigDecimal unitsToSell = amount.divide(currentPrice, 4, RoundingMode.DOWN);
        if (unitsToSell.compareTo(existingTransaction.getUnits()) > 0) {
            throw new IllegalArgumentException("Insufficient units to sell");
        }

        BigDecimal sellAmount = unitsToSell.multiply(currentPrice);
        BigDecimal fee = sellAmount.multiply(BigDecimal.valueOf(0.005));
        BigDecimal netAmount = sellAmount.subtract(fee);

        walletServicePort.creditBalance(investmentPersistencePort.findAccountById(UUID.fromString(accountId))
                .orElseThrow().getUserId(), netAmount);

        LocalDateTime now = LocalDateTime.now();
        InvestmentTransaction sellTransaction = InvestmentTransaction.builder()
                .id(UUID.randomUUID())
                .accountId(accountId)
                .type(InvestmentTransaction.TransactionType.SELL)
                .investmentType(existingTransaction.getInvestmentType())
                .investmentId(existingTransaction.getInvestmentId())
                .amount(sellAmount)
                .price(currentPrice)
                .units(unitsToSell)
                .fee(fee)
                .currency("IDR")
                .status(InvestmentTransaction.TransactionStatus.COMPLETED)
                .referenceNumber("SELL-" + System.currentTimeMillis())
                .createdAt(now)
                .updatedAt(now)
                .build();

        InvestmentTransaction savedTransaction = investmentPersistencePort.saveTransaction(sellTransaction);

        investmentPersistencePort.updateAccountBalance(UUID.fromString(accountId), netAmount.negate());

        log.info("Investment sold successfully: {}", savedTransaction.getId());

        investmentEventPublisherPort.publishInvestmentCompleted(new InvestmentEvent(
                savedTransaction.getId(),
                investmentPersistencePort.findAccountById(UUID.fromString(accountId)).orElseThrow().getUserId(),
                "INVESTMENT_SOLD",
                existingTransaction.getInvestmentType().name(),
                netAmount,
                "COMPLETED",
                LocalDateTime.now()));

        return CompletableFuture.completedFuture(savedTransaction);
    }

    private BigDecimal calculateDepositInterestRate(int tenure) {
        return switch (tenure) {
            case 1 -> BigDecimal.valueOf(0.045);
            case 3 -> BigDecimal.valueOf(0.050);
            case 6 -> BigDecimal.valueOf(0.055);
            case 12 -> BigDecimal.valueOf(0.060);
            default -> BigDecimal.valueOf(0.045);
        };
    }

    private BigDecimal calculateMaturityAmount(BigDecimal principal, BigDecimal annualRate, int months) {
        BigDecimal rate = annualRate.multiply(BigDecimal.valueOf(months)).divide(BigDecimal.valueOf(12), 4, RoundingMode.HALF_UP);
        return principal.multiply(BigDecimal.ONE.add(rate));
    }

    public CompletableFuture<InvestmentAccount> createAccountFallback(String userId, Throwable t) {
        log.error("Wallet service unavailable during account creation. Error: {}", t.getMessage());
        throw new RuntimeException("Service temporarily unavailable. Please try again later.");
    }

    public CompletableFuture<Deposit> buyDepositFallback(String accountId, String userId, BigDecimal amount, 
            int tenure, Throwable t) {
        log.error("Wallet service unavailable during deposit purchase. Error: {}", t.getMessage());
        throw new RuntimeException("Service temporarily unavailable. Please try again later.");
    }

    public CompletableFuture<InvestmentTransaction> buyMutualFundFallback(String accountId, String userId, 
            String fundCode, BigDecimal amount, Throwable t) {
        log.error("Wallet service unavailable during mutual fund purchase. Error: {}", t.getMessage());
        throw new RuntimeException("Service temporarily unavailable. Please try again later.");
    }

    public CompletableFuture<Gold> buyGoldFallback(String userId, BigDecimal amount, Throwable t) {
        log.error("Wallet service unavailable during gold purchase. Error: {}", t.getMessage());
        throw new RuntimeException("Service temporarily unavailable. Please try again later.");
    }

    public CompletableFuture<InvestmentTransaction> sellInvestmentFallback(String accountId, UUID transactionId, 
            BigDecimal amount, Throwable t) {
        log.error("Wallet service unavailable during investment sell. Error: {}", t.getMessage());
        throw new RuntimeException("Service temporarily unavailable. Please try again later.");
    }
}
