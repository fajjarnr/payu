package id.payu.investment.application.service;

import id.payu.investment.domain.model.Deposit;
import id.payu.investment.domain.model.Gold;
import id.payu.investment.domain.model.InvestmentAccount;
import id.payu.investment.domain.model.InvestmentTransaction;
import id.payu.investment.domain.model.MutualFund;
import id.payu.investment.domain.model.InvestmentOperation;
import id.payu.investment.domain.model.InvestmentOperationStatus;
import id.payu.investment.domain.model.InvestmentOperationType;
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
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import id.payu.investment.domain.model.AccountStatus;
import id.payu.investment.domain.model.DepositStatus;
import id.payu.investment.domain.model.FundStatus;
import id.payu.investment.domain.model.InvestmentType;
import id.payu.investment.domain.model.TransactionStatus;
import id.payu.investment.domain.model.TransactionType;

@Service
@RequiredArgsConstructor
@Slf4j
// TODO BUG-ARCH-004: Migrate LocalDateTime fields to OffsetDateTime or Instant for timezone safety
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

    public CompletableFuture<List<InvestmentAccount>> getAccountsByUserId(String userId) {
        return CompletableFuture.completedFuture(
                List.of(investmentPersistencePort.findAccountByUserId(userId)
                        .orElseThrow(() -> new IllegalArgumentException("Investment account not found")))
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
                .status(AccountStatus.ACTIVE)
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
    // BUG-LOGIC-006 FIX: Removed @Async — incompatible with @Transactional (proxy boundary issue)
    @CircuitBreaker(name = "walletService", fallbackMethod = "buyDepositFallback")
    @TimeLimiter(name = "walletService")
    public CompletableFuture<Deposit> buyDeposit(String accountId, String userId, BigDecimal amount, int tenure) {
        return buyDepositInternal(accountId, userId, amount, tenure, null);
    }

    @Transactional
    @CircuitBreaker(name = "walletService", fallbackMethod = "buyDepositWithKeyFallback")
    @TimeLimiter(name = "walletService")
    public CompletableFuture<Deposit> buyDeposit(String accountId, String userId, BigDecimal amount,
                                                  int tenure, String idempotencyKey) {
        return buyDepositInternal(accountId, userId, amount, tenure, idempotencyKey);
    }

    private CompletableFuture<Deposit> buyDepositInternal(String accountId, String userId, BigDecimal amount,
                                                           int tenure, String idempotencyKey) {
        log.info("Processing deposit purchase for user: {}, amount: {}", userId, amount);

        InvestmentAccount account = investmentPersistencePort.findAccountById(UUID.fromString(accountId))
                .orElseThrow(() -> new IllegalArgumentException("Investment account not found"));

        BigDecimal interestRate = calculateDepositInterestRate(tenure);
        BigDecimal maturityAmount = calculateMaturityAmount(amount, interestRate, tenure);

        InvestmentOperation operation = idempotencyKey == null ? null : prepareOperation(
                idempotencyKey, accountId, userId, InvestmentOperationType.DEPOSIT_PURCHASE,
                null, tenure, amount, BigDecimal.ZERO);
        if (operation != null && operation.getStatus() == InvestmentOperationStatus.COMPLETED) {
            return CompletableFuture.completedFuture(investmentPersistencePort.findDepositById(operation.getTargetId())
                    .orElseThrow(() -> new IllegalStateException("Completed deposit operation has no deposit")));
        }
        ensureOperationCanContinue(operation);

        if (operation == null && !walletServicePort.hasSufficientBalance(userId, amount)) {
            throw new IllegalArgumentException("Insufficient wallet balance");
        }

        boolean walletDebitConfirmed = false;
        try {
            debitWallet(userId, amount, operation);
            walletDebitConfirmed = true;

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
                    .status(DepositStatus.ACTIVE)
                    .currency("IDR")
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            Deposit savedDeposit = investmentPersistencePort.saveDeposit(deposit);
            investmentPersistencePort.updateAccountBalance(account.getId(), amount);

            InvestmentTransaction transaction = InvestmentTransaction.builder()
                    .id(UUID.randomUUID())
                    .accountId(accountId)
                    .type(TransactionType.BUY)
                    .investmentType(InvestmentType.DEPOSIT)
                    .investmentId(savedDeposit.getId().toString())
                    .amount(amount)
                    .price(BigDecimal.ZERO)
                    .units(BigDecimal.ONE)
                    .fee(BigDecimal.ZERO)
                    .currency("IDR")
                    .status(TransactionStatus.COMPLETED)
                    .referenceNumber(operation == null
                            ? "DEP-" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase()
                            : "DEP-" + operation.getId())
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            investmentPersistencePort.saveTransaction(transaction);

            if (operation != null) {
                operation.complete(savedDeposit.getId());
                investmentPersistencePort.saveInvestmentOperation(operation);
            }

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
        } catch (Exception e) {
            return failedPurchase("Deposit", userId, amount, operation, walletDebitConfirmed, e);
        }
    }

    @Override
    @Transactional
    // BUG-LOGIC-006 FIX: Removed @Async — incompatible with @Transactional (proxy boundary issue)
    @CircuitBreaker(name = "walletService", fallbackMethod = "buyMutualFundFallback")
    @TimeLimiter(name = "walletService")
    public CompletableFuture<InvestmentTransaction> buyMutualFund(String accountId, String userId, 
            String fundCode, BigDecimal amount) {
        return buyMutualFundInternal(accountId, userId, fundCode, amount, null);
    }

    @Transactional
    @CircuitBreaker(name = "walletService", fallbackMethod = "buyMutualFundWithKeyFallback")
    @TimeLimiter(name = "walletService")
    public CompletableFuture<InvestmentTransaction> buyMutualFund(String accountId, String userId,
            String fundCode, BigDecimal amount, String idempotencyKey) {
        return buyMutualFundInternal(accountId, userId, fundCode, amount, idempotencyKey);
    }

    private CompletableFuture<InvestmentTransaction> buyMutualFundInternal(String accountId, String userId,
            String fundCode, BigDecimal amount, String idempotencyKey) {
        log.info("Processing mutual fund purchase for user: {}, fund: {}, amount: {}", userId, fundCode, amount);

        InvestmentAccount account = investmentPersistencePort.findAccountById(UUID.fromString(accountId))
                .orElseThrow(() -> new IllegalArgumentException("Investment account not found"));

        MutualFund fund = investmentPersistencePort.getLatestFundPrice(fundCode);
        if (fund == null || fund.getStatus() != FundStatus.ACTIVE) {
            throw new IllegalArgumentException("Mutual fund not available");
        }

        if (amount.compareTo(fund.getMinimumInvestment()) < 0) {
            throw new IllegalArgumentException("Amount below minimum investment");
        }

        InvestmentOperation operation = idempotencyKey == null ? null : prepareOperation(
                idempotencyKey, accountId, userId, InvestmentOperationType.MUTUAL_FUND_PURCHASE,
                fundCode, null, amount, fund.getNavPerUnit());
        if (operation != null && operation.getStatus() == InvestmentOperationStatus.COMPLETED) {
            return CompletableFuture.completedFuture(investmentPersistencePort.findTransactionById(operation.getTargetId())
                    .orElseThrow(() -> new IllegalStateException("Completed fund operation has no transaction")));
        }
        ensureOperationCanContinue(operation);

        if (operation == null && !walletServicePort.hasSufficientBalance(userId, amount)) {
            throw new IllegalArgumentException("Insufficient wallet balance");
        }

        boolean walletDebitConfirmed = false;
        try {
            debitWallet(userId, amount, operation);
            walletDebitConfirmed = true;

            BigDecimal price = operation != null && operation.getPrice() != null
                    ? operation.getPrice() : fund.getNavPerUnit();
            BigDecimal units = amount.divide(price, 4, RoundingMode.DOWN);
            BigDecimal fee = amount.multiply(fund.getManagementFee());

            LocalDateTime now = LocalDateTime.now();
            InvestmentTransaction transaction = InvestmentTransaction.builder()
                    .id(UUID.randomUUID())
                    .accountId(accountId)
                    .type(TransactionType.BUY)
                    .investmentType(InvestmentType.MUTUAL_FUND)
                    .investmentId(fundCode)
                    .amount(amount)
                    .price(price)
                    .units(units)
                    .fee(fee)
                    .currency("IDR")
                    .status(TransactionStatus.COMPLETED)
                    .referenceNumber(operation == null
                            ? "MF-" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase()
                            : "MF-" + operation.getId())
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            InvestmentTransaction savedTransaction = investmentPersistencePort.saveTransaction(transaction);
            investmentPersistencePort.updateAccountBalance(account.getId(), amount);

            if (operation != null) {
                operation.complete(savedTransaction.getId());
                investmentPersistencePort.saveInvestmentOperation(operation);
            }

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
        } catch (Exception e) {
            return failedPurchase("Mutual fund", userId, amount, operation, walletDebitConfirmed, e);
        }
    }

    @Override
    @Transactional
    // BUG-LOGIC-006 FIX: Removed @Async — incompatible with @Transactional (proxy boundary issue)
    @CircuitBreaker(name = "walletService", fallbackMethod = "buyGoldFallback")
    @TimeLimiter(name = "walletService")
    public CompletableFuture<Gold> buyGold(String userId, BigDecimal amount) {
        return buyGoldInternal(userId, amount, null);
    }

    @Transactional
    @CircuitBreaker(name = "walletService", fallbackMethod = "buyGoldWithKeyFallback")
    @TimeLimiter(name = "walletService")
    public CompletableFuture<Gold> buyGold(String userId, BigDecimal amount, String idempotencyKey) {
        return buyGoldInternal(userId, amount, idempotencyKey);
    }

    private CompletableFuture<Gold> buyGoldInternal(String userId, BigDecimal amount, String idempotencyKey) {
        log.info("Processing gold purchase for user: {}, amount: {}", userId, amount);

        InvestmentOperation existingOperation = idempotencyKey == null ? null
                : investmentPersistencePort.findInvestmentOperationByIdempotencyKey(idempotencyKey).orElse(null);
        BigDecimal currentPrice = existingOperation != null && existingOperation.getPrice() != null
                ? existingOperation.getPrice() : investmentPersistencePort.getLatestGoldPrice();
        if (currentPrice == null) {
            throw new IllegalArgumentException("Gold price not available");
        }

        InvestmentOperation operation = idempotencyKey == null ? null : prepareOperation(
                idempotencyKey, userId, userId, InvestmentOperationType.GOLD_PURCHASE,
                "XAU", null, amount, currentPrice);
        if (operation != null && operation.getStatus() == InvestmentOperationStatus.COMPLETED) {
            return CompletableFuture.completedFuture(investmentPersistencePort.findGoldById(operation.getTargetId())
                    .orElseThrow(() -> new IllegalStateException("Completed gold operation has no holding")));
        }
        ensureOperationCanContinue(operation);

        if (operation == null && !walletServicePort.hasSufficientBalance(userId, amount)) {
            throw new IllegalArgumentException("Insufficient wallet balance");
        }

        boolean walletDebitConfirmed = false;
        try {
            debitWallet(userId, amount, operation);
            walletDebitConfirmed = true;

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
                        .divide(newAmount, 2, RoundingMode.HALF_EVEN);

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

            if (operation != null) {
                operation.complete(savedGold.getId());
                investmentPersistencePort.saveInvestmentOperation(operation);
            }

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
        } catch (Exception e) {
            return failedPurchase("Gold", userId, amount, operation, walletDebitConfirmed, e);
        }
    }

    @Override
    @Transactional
    // BUG-LOGIC-006 FIX: Removed @Async — incompatible with @Transactional (proxy boundary issue)
    @CircuitBreaker(name = "walletService", fallbackMethod = "sellInvestmentFallback")
    public CompletableFuture<InvestmentTransaction> sellInvestment(String accountId, UUID transactionId, BigDecimal amount) {
        log.info("Processing investment sell for account: {}, transaction: {}, amount: {}", accountId, transactionId, amount);

        InvestmentTransaction existingTransaction = investmentPersistencePort.findTransactionById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        // BUG-BE-173 FIX: Verify the transaction belongs to the requesting account
        if (!existingTransaction.getAccountId().equals(accountId)) {
            throw new IllegalArgumentException("Transaction does not belong to account: " + accountId);
        }

        // INVEST-001: deterministic sell id + replay guard — a retried sell must
        // return the original result, never credit the wallet twice.
        UUID sellTransactionId = UUID.nameUUIDFromBytes(
                ("SELL:" + transactionId).getBytes(StandardCharsets.UTF_8));
        return investmentPersistencePort.findTransactionById(sellTransactionId)
                .map(CompletableFuture::completedFuture)
                .orElseGet(() -> executeSell(existingTransaction, accountId, transactionId, amount, sellTransactionId));
    }

    private CompletableFuture<InvestmentTransaction> executeSell(InvestmentTransaction existingTransaction, String accountId,
                                                                 UUID transactionId, BigDecimal amount, UUID sellTransactionId) {
        if (existingTransaction.getStatus() != TransactionStatus.COMPLETED) {
            throw new IllegalArgumentException("Cannot sell investment with status: " + existingTransaction.getStatus());
        }

        if (existingTransaction.getType() != TransactionType.BUY) {
            throw new IllegalArgumentException("Can only sell purchased investments");
        }

        BigDecimal currentPrice;
        if (existingTransaction.getInvestmentType() == InvestmentType.MUTUAL_FUND) {
            MutualFund fund = investmentPersistencePort.getLatestFundPrice(existingTransaction.getInvestmentId());
            currentPrice = fund.getNavPerUnit();
        } else if (existingTransaction.getInvestmentType() == InvestmentType.GOLD) {
            currentPrice = investmentPersistencePort.getLatestGoldPrice();
        } else {
            throw new IllegalArgumentException("Cannot sell deposit before maturity");
        }

        // BUG-BE-174 FIX: Validate that the price used for selling is reasonably fresh.
        // Prices are snapshot into the transaction, but we should guard against stale prices
        // that could result in incorrect sell amounts (e.g., if price feed is down).
        if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Current price is unavailable or invalid — cannot proceed with sell");
        }

        BigDecimal unitsToSell = amount.divide(currentPrice, 4, RoundingMode.DOWN);
        if (unitsToSell.compareTo(existingTransaction.getUnits()) > 0) {
            throw new IllegalArgumentException("Insufficient units to sell");
        }

        BigDecimal sellAmount = unitsToSell.multiply(currentPrice).setScale(4, RoundingMode.HALF_EVEN);
        BigDecimal fee = sellAmount.multiply(BigDecimal.valueOf(0.005)).setScale(4, RoundingMode.HALF_EVEN);
        BigDecimal netAmount = sellAmount.subtract(fee).setScale(4, RoundingMode.HALF_EVEN);

        walletServicePort.creditBalance(investmentPersistencePort.findAccountById(UUID.fromString(accountId))
                .orElseThrow().getUserId(), netAmount, "SELL:" + transactionId);

        LocalDateTime now = LocalDateTime.now();
        InvestmentTransaction sellTransaction = InvestmentTransaction.builder()
                .id(sellTransactionId)
                .accountId(accountId)
                .type(TransactionType.SELL)
                .investmentType(existingTransaction.getInvestmentType())
                .investmentId(existingTransaction.getInvestmentId())
                .amount(sellAmount)
                .price(currentPrice)
                .units(unitsToSell)
                .fee(fee)
                .currency("IDR")
                .status(TransactionStatus.COMPLETED)
                .referenceNumber("SELL-" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase())
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
        BigDecimal rate = annualRate.multiply(BigDecimal.valueOf(months)).divide(BigDecimal.valueOf(12), 4, RoundingMode.HALF_EVEN);
        return principal.multiply(BigDecimal.ONE.add(rate));
    }

    private InvestmentOperation prepareOperation(String idempotencyKey, String accountId, String userId,
                                                  InvestmentOperationType type, String productCode, Integer tenure,
                                                  BigDecimal amount, BigDecimal price) {
        return investmentPersistencePort.findInvestmentOperationByIdempotencyKey(idempotencyKey)
                .map(existing -> {
                    if (!existing.matches(accountId, userId, type, productCode, tenure, amount)) {
                        throw new IllegalArgumentException("Idempotency key was already used for a different investment operation");
                    }
                    return existing;
                })
                .orElseGet(() -> investmentPersistencePort.createInvestmentOperation(
                        InvestmentOperation.requested(idempotencyKey, accountId, userId, type,
                                productCode, tenure, amount, price, "IDR")));
    }

    private void ensureOperationCanContinue(InvestmentOperation operation) {
        if (operation != null && (operation.getStatus() == InvestmentOperationStatus.COMPENSATION_PENDING
                || operation.getStatus() == InvestmentOperationStatus.COMPENSATED)) {
            throw new IllegalStateException("Investment operation is pending or has completed compensation");
        }
    }

    private void debitWallet(String userId, BigDecimal amount, InvestmentOperation operation) {
        if (operation == null) {
            walletServicePort.deductBalance(userId, amount);
            return;
        }
        if (operation.getStatus() == InvestmentOperationStatus.DEBIT_REQUESTED) {
            walletServicePort.deductBalance(userId, amount, operation.getDebitReference());
            operation.markDebited();
            investmentPersistencePort.saveInvestmentOperation(operation);
        }
    }

    private <T> CompletableFuture<T> failedPurchase(String kind, String userId, BigDecimal amount,
                                                     InvestmentOperation operation, boolean walletDebitConfirmed,
                                                     Exception error) {
        if (operation == null) {
            try {
                walletServicePort.creditBalance(userId, amount);
            } catch (Exception refundError) {
                log.error("CRITICAL: {} purchase failed and wallet refund failed for user {}",
                        kind, userId, refundError);
            }
            throw new RuntimeException(kind + " purchase failed, wallet refunded: " + error.getMessage(), error);
        }

        try {
            if (walletDebitConfirmed) {
                investmentPersistencePort.markInvestmentOperationCompensationPending(
                        operation.getId(), error.getMessage());
            } else {
                investmentPersistencePort.markInvestmentOperationRetry(operation.getId(), error.getMessage());
            }
        } catch (Exception stateError) {
            log.error("CRITICAL: could not persist investment operation recovery state for {}",
                    operation.getId(), stateError);
        }
        throw new RuntimeException(kind + " purchase failed; recovery scheduled", error);
    }

    public CompletableFuture<InvestmentAccount> createAccountFallback(String userId, Throwable t) {
        log.error("Wallet service unavailable during account creation. Error: {}", t.getMessage());
        // BUG-ARCH-007 FIX: Return failed future instead of throwing from fallback
        return CompletableFuture.failedFuture(new RuntimeException("Service temporarily unavailable. Please try again later."));
    }

    public CompletableFuture<Deposit> buyDepositFallback(String accountId, String userId, BigDecimal amount, 
            int tenure, Throwable t) {
        log.error("Wallet service unavailable during deposit purchase. Error: {}", t.getMessage());
        // BUG-ARCH-007 FIX: Return failed future instead of throwing from fallback
        return CompletableFuture.failedFuture(new RuntimeException("Service temporarily unavailable. Please try again later."));
    }

    public CompletableFuture<InvestmentTransaction> buyMutualFundFallback(String accountId, String userId, 
            String fundCode, BigDecimal amount, Throwable t) {
        log.error("Wallet service unavailable during mutual fund purchase. Error: {}", t.getMessage());
        // BUG-ARCH-007 FIX: Return failed future instead of throwing from fallback
        return CompletableFuture.failedFuture(new RuntimeException("Service temporarily unavailable. Please try again later."));
    }

    public CompletableFuture<Gold> buyGoldFallback(String userId, BigDecimal amount, Throwable t) {
        log.error("Wallet service unavailable during gold purchase. Error: {}", t.getMessage());
        // BUG-ARCH-007 FIX: Return failed future instead of throwing from fallback
        return CompletableFuture.failedFuture(new RuntimeException("Service temporarily unavailable. Please try again later."));
    }

    public CompletableFuture<Deposit> buyDepositWithKeyFallback(String accountId, String userId,
            BigDecimal amount, int tenure, String idempotencyKey, Throwable t) {
        log.error("Wallet service unavailable during idempotent deposit purchase. Error: {}", t.getMessage());
        return CompletableFuture.failedFuture(new RuntimeException("Service temporarily unavailable. Please try again later."));
    }

    public CompletableFuture<InvestmentTransaction> buyMutualFundWithKeyFallback(String accountId, String userId,
            String fundCode, BigDecimal amount, String idempotencyKey, Throwable t) {
        log.error("Wallet service unavailable during idempotent mutual fund purchase. Error: {}", t.getMessage());
        return CompletableFuture.failedFuture(new RuntimeException("Service temporarily unavailable. Please try again later."));
    }

    public CompletableFuture<Gold> buyGoldWithKeyFallback(String userId, BigDecimal amount,
            String idempotencyKey, Throwable t) {
        log.error("Wallet service unavailable during idempotent gold purchase. Error: {}", t.getMessage());
        return CompletableFuture.failedFuture(new RuntimeException("Service temporarily unavailable. Please try again later."));
    }

    public CompletableFuture<InvestmentTransaction> sellInvestmentFallback(String accountId, UUID transactionId, 
            BigDecimal amount, Throwable t) {
        log.error("Wallet service unavailable during investment sell. Error: {}", t.getMessage());
        // BUG-ARCH-007 FIX: Return failed future instead of throwing from fallback
        return CompletableFuture.failedFuture(new RuntimeException("Service temporarily unavailable. Please try again later."));
    }
}
