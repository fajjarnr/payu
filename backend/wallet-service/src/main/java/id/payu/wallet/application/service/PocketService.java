package id.payu.wallet.application.service;

import id.payu.cache.service.CacheService;
import id.payu.wallet.domain.model.EntryType;
import id.payu.wallet.domain.model.FxRateInfo;
import id.payu.wallet.domain.model.LedgerEntry;
import id.payu.wallet.domain.model.Pocket;
import id.payu.wallet.domain.model.PocketStatus;
import id.payu.wallet.domain.model.TransactionType;
import id.payu.wallet.domain.model.Wallet;
import id.payu.wallet.domain.model.WalletTransaction;
import id.payu.wallet.domain.port.in.JournalUseCase;
import id.payu.wallet.domain.port.in.PocketUseCase;
import id.payu.wallet.domain.port.out.FxRateProviderPort;
import id.payu.wallet.domain.port.out.PocketPersistencePort;
import id.payu.wallet.domain.port.out.WalletEventPublisherPort;
import id.payu.wallet.domain.port.out.WalletPersistencePort;
import id.payu.wallet.application.exception.WalletNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.ArrayList;

@Service
public class PocketService implements PocketUseCase {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PocketService.class);

    private static final String COA_USER_WALLETS = "1100";
    private static final String POCKET_LEDGER_ACCOUNT_PREFIX = "POCKET:";
    private static final String CREATED_BY = "wallet-service";

    private final PocketPersistencePort pocketPersistencePort;
    private final FxRateProviderPort fxRateProviderPort;
    private final WalletPersistencePort walletPersistencePort;
    private final JournalUseCase journalUseCase;
    private final WalletEventPublisherPort walletEventPublisher;
    private final CacheService cacheService;

    public PocketService(PocketPersistencePort pocketPersistencePort,
                         FxRateProviderPort fxRateProviderPort,
                         WalletPersistencePort walletPersistencePort,
                         JournalUseCase journalUseCase,
                         WalletEventPublisherPort walletEventPublisher,
                         CacheService cacheService) {
        this.pocketPersistencePort = pocketPersistencePort;
        this.fxRateProviderPort = fxRateProviderPort;
        this.walletPersistencePort = walletPersistencePort;
        this.journalUseCase = journalUseCase;
        this.walletEventPublisher = walletEventPublisher;
        this.cacheService = cacheService;
    }

    @Override
    @Transactional
    public Pocket createPocket(String accountId, String name, String description, String currency) {
        log.info("Creating pocket {} for account {} with currency {}", name, accountId, currency);

        Pocket pocket = Pocket.builder()
                .accountId(accountId)
                .name(name)
                .description(description)
                .currency(currency)
                .balance(BigDecimal.ZERO)
                .status(PocketStatus.ACTIVE)
                .build();

        Pocket saved = pocketPersistencePort.save(pocket);
        log.info("Pocket created successfully: {}", saved.getId());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<Pocket> getPocketById(UUID pocketId) {
        log.debug("Getting pocket by ID: {}", pocketId);
        return pocketPersistencePort.findById(pocketId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pocket> getPocketsByAccountId(String accountId) {
        log.debug("Getting pockets for account: {}", accountId);
        return pocketPersistencePort.findByAccountId(accountId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pocket> getPocketsByAccountIdAndCurrency(String accountId, String currency) {
        log.debug("Getting pockets for account {} with currency {}", accountId, currency);
        return pocketPersistencePort.findByAccountIdAndCurrency(accountId, currency);
    }

    /**
     * WALLET-LEDGER-001: credit pocket = debit main wallet of the same account +
     * credit pocket, posted as one balanced double-entry journal. Idempotent via
     * referenceId (deterministic transactionId, same pattern as WalletService.repayLoan).
     */
    @Override
    @Transactional
    public void creditPocket(UUID pocketId, BigDecimal amount, String referenceId) {
        validateAmount(amount);
        if (referenceId == null || referenceId.isBlank()) {
            throw new IllegalArgumentException("Reference ID is required");
        }

        UUID transactionId = pocketTransactionId("POCKET_CREDIT", referenceId);
        if (isReplay(transactionId, amount)) {
            return;
        }

        log.info("Crediting {} to pocket {} with reference {}", amount, pocketId, referenceId);

        Pocket pocket = requireActivePocket(pocketId);
        Wallet wallet = requireWalletForPocket(pocket);
        // Re-check after the pessimistic lock so a concurrent replay of the same
        // referenceId cannot double-execute (WalletService.credit pattern).
        if (isReplay(transactionId, amount)) {
            return;
        }
        if (!wallet.hasSufficientBalance(amount)) {
            throw new InsufficientBalanceException(pocket.getAccountId(), amount, wallet.getAvailableBalance());
        }

        LocalDateTime now = LocalDateTime.now();
        pocket.credit(amount);
        pocketPersistencePort.save(pocket);
        wallet.debit(amount);
        walletPersistencePort.save(wallet);

        walletPersistencePort.saveTransaction(WalletTransaction.builder()
                .id(transactionId)
                .walletId(wallet.getId())
                .referenceId(referenceId)
                .type(TransactionType.DEBIT)
                .amount(amount)
                .balanceAfter(wallet.getBalance())
                .description("Pocket credit: " + pocketId)
                .createdAt(now)
                .build());

        // Double-entry journal: DR User Wallet (1100) / CR Pocket (1100)
        List<LedgerEntry> entries = new ArrayList<>();
        entries.add(LedgerEntry.builder()
                .transactionId(transactionId)
                .accountId(pocket.getAccountId())
                .coaCode(COA_USER_WALLETS)
                .entryType(EntryType.DEBIT)
                .amount(amount)
                .currency(pocket.getCurrency())
                .balanceAfter(wallet.getBalance())
                .referenceType("POCKET_CREDIT")
                .referenceId(referenceId)
                .createdAt(now)
                .build());
        entries.add(LedgerEntry.builder()
                .transactionId(transactionId)
                .accountId(POCKET_LEDGER_ACCOUNT_PREFIX + pocketId)
                .coaCode(COA_USER_WALLETS)
                .entryType(EntryType.CREDIT)
                .amount(amount)
                .currency(pocket.getCurrency())
                .balanceAfter(pocket.getBalance())
                .referenceType("POCKET_CREDIT")
                .referenceId(referenceId)
                .createdAt(now)
                .build());

        journalUseCase.createAndPostJournal(
                "Pocket credit: " + pocketId,
                "POCKET_CREDIT",
                referenceId,
                entries,
                CREATED_BY);

        invalidateWalletCaches(wallet);
        walletEventPublisher.publishBalanceChanged(pocket.getAccountId(), wallet.getBalance(),
                wallet.getAvailableBalance(), amount, "DEBIT");

        log.info("Credited {} to pocket {}, transaction: {}", amount, pocketId, transactionId);
    }

    /**
     * WALLET-LEDGER-001: debit pocket = credit main wallet of the same account,
     * posted as one balanced double-entry journal. Idempotent via referenceId.
     */
    @Override
    @Transactional
    public void debitPocket(UUID pocketId, BigDecimal amount, String referenceId) {
        validateAmount(amount);
        if (referenceId == null || referenceId.isBlank()) {
            throw new IllegalArgumentException("Reference ID is required");
        }

        UUID transactionId = pocketTransactionId("POCKET_DEBIT", referenceId);
        if (isReplay(transactionId, amount)) {
            return;
        }

        log.info("Debiting {} from pocket {} with reference {}", amount, pocketId, referenceId);

        Pocket pocket = requireActivePocket(pocketId);
        Wallet wallet = requireWalletForPocket(pocket);
        if (isReplay(transactionId, amount)) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        // Domain guard: throws IllegalStateException when the pocket balance is
        // insufficient — rolls the whole transaction back.
        pocket.debit(amount);
        pocketPersistencePort.save(pocket);
        wallet.credit(amount);
        walletPersistencePort.save(wallet);

        walletPersistencePort.saveTransaction(WalletTransaction.builder()
                .id(transactionId)
                .walletId(wallet.getId())
                .referenceId(referenceId)
                .type(TransactionType.CREDIT)
                .amount(amount)
                .balanceAfter(wallet.getBalance())
                .description("Pocket debit: " + pocketId)
                .createdAt(now)
                .build());

        // Double-entry journal: DR Pocket (1100) / CR User Wallet (1100)
        List<LedgerEntry> entries = new ArrayList<>();
        entries.add(LedgerEntry.builder()
                .transactionId(transactionId)
                .accountId(POCKET_LEDGER_ACCOUNT_PREFIX + pocketId)
                .coaCode(COA_USER_WALLETS)
                .entryType(EntryType.DEBIT)
                .amount(amount)
                .currency(pocket.getCurrency())
                .balanceAfter(pocket.getBalance())
                .referenceType("POCKET_DEBIT")
                .referenceId(referenceId)
                .createdAt(now)
                .build());
        entries.add(LedgerEntry.builder()
                .transactionId(transactionId)
                .accountId(pocket.getAccountId())
                .coaCode(COA_USER_WALLETS)
                .entryType(EntryType.CREDIT)
                .amount(amount)
                .currency(pocket.getCurrency())
                .balanceAfter(wallet.getBalance())
                .referenceType("POCKET_DEBIT")
                .referenceId(referenceId)
                .createdAt(now)
                .build());

        journalUseCase.createAndPostJournal(
                "Pocket debit: " + pocketId,
                "POCKET_DEBIT",
                referenceId,
                entries,
                CREATED_BY);

        invalidateWalletCaches(wallet);
        walletEventPublisher.publishBalanceChanged(pocket.getAccountId(), wallet.getBalance(),
                wallet.getAvailableBalance(), amount, "CREDIT");

        log.info("Debited {} from pocket {}, transaction: {}", amount, pocketId, transactionId);
    }

    @Override
    @Transactional
    public void freezePocket(UUID pocketId) {
        log.info("Freezing pocket: {}", pocketId);

        Pocket pocket = pocketPersistencePort.findById(pocketId)
                .orElseThrow(() -> new PocketNotFoundException(pocketId.toString()));

        pocket.freeze();
        pocketPersistencePort.save(pocket);

        log.info("Pocket frozen successfully: {}", pocketId);
    }

    @Override
    @Transactional
    public void unfreezePocket(UUID pocketId) {
        log.info("Unfreezing pocket: {}", pocketId);

        Pocket pocket = pocketPersistencePort.findById(pocketId)
                .orElseThrow(() -> new PocketNotFoundException(pocketId.toString()));

        pocket.unfreeze();
        pocketPersistencePort.save(pocket);

        log.info("Pocket unfrozen successfully: {}", pocketId);
    }

    @Override
    @Transactional
    public void closePocket(UUID pocketId) {
        log.info("Closing pocket: {}", pocketId);

        Pocket pocket = pocketPersistencePort.findById(pocketId)
                .orElseThrow(() -> new PocketNotFoundException(pocketId.toString()));

        pocket.close();
        pocketPersistencePort.save(pocket);

        log.info("Pocket closed successfully: {}", pocketId);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalBalanceInCurrency(String accountId, String targetCurrency) {
        log.debug("Calculating total balance for account {} in currency {}", accountId, targetCurrency);

        List<Pocket> pockets = pocketPersistencePort.findByAccountId(accountId);
        BigDecimal total = BigDecimal.ZERO;

        for (Pocket pocket : pockets) {
            BigDecimal pocketBalance = pocket.getBalance();

            if (pocket.getCurrency().equals(targetCurrency)) {
                total = total.add(pocketBalance);
            } else {
                // BUG-BE-109: Use proper typed access instead of reflection hack
                Optional<FxRateInfo> rateOptional = fxRateProviderPort.getCurrentRate(
                        pocket.getCurrency(), targetCurrency);
                if (rateOptional.isEmpty()) {
                    throw new FxRateNotFoundException(
                                "No FX rate available for " + pocket.getCurrency() + " to " + targetCurrency);
                }
                FxRateInfo fxRate = rateOptional.get();
                BigDecimal convertedAmount = pocketBalance.multiply(fxRate.rate());
                total = total.add(convertedAmount);
            }
        }

        return total;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pocket> getAllActivePockets() {
        log.debug("Getting all active pockets");
        return pocketPersistencePort.findAllActive();
    }

    private static void validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0 || amount.scale() > 4) {
            throw new IllegalArgumentException("Amount must be positive with at most 4 decimals");
        }
    }

    /**
     * Deterministic transactionId per referenceId (same derivation as
     * WalletService.transfer/repayLoan) so a replay of the same referenceId maps
     * to the already-posted journal instead of a new one.
     */
    private static UUID pocketTransactionId(String referenceType, String referenceId) {
        return UUID.nameUUIDFromBytes(
                (referenceType + ":" + referenceId).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /**
     * Idempotency: an existing entry for the deterministic transactionId means the
     * pocket movement was already posted. A replay with a different amount is a
     * referenceId conflict.
     */
    private boolean isReplay(UUID transactionId, BigDecimal amount) {
        List<LedgerEntry> existing = walletPersistencePort.findByTransactionId(transactionId);
        if (existing.isEmpty()) {
            return false;
        }
        boolean sameCommand = existing.stream().allMatch(entry ->
                entry.getAmount() != null && entry.getAmount().compareTo(amount) == 0);
        if (!sameCommand) {
            throw new IllegalArgumentException("Reference ID was already used for a different amount");
        }
        return true;
    }

    /**
     * Loads the pocket and rejects non-ACTIVE statuses (CLOSED/FROZEN) so no
     * money can move through a pocket that should not accept it.
     */
    private Pocket requireActivePocket(UUID pocketId) {
        Pocket pocket = pocketPersistencePort.findById(pocketId)
                .orElseThrow(() -> new PocketNotFoundException(pocketId.toString()));
        if (pocket.getStatus() != PocketStatus.ACTIVE) {
            throw new IllegalStateException("Pocket is not active: " + pocket.getStatus());
        }
        return pocket;
    }

    /**
     * Loads the account's main wallet under a pessimistic lock. The wallet row
     * lock serializes every pocket movement for the account; the currency must
     * match the pocket so the move cannot create or destroy value across currencies.
     */
    private Wallet requireWalletForPocket(Pocket pocket) {
        Wallet wallet = walletPersistencePort.findByAccountIdForUpdate(pocket.getAccountId())
                .orElseThrow(() -> new WalletNotFoundException(pocket.getAccountId()));
        if (!wallet.getCurrency().equals(pocket.getCurrency())) {
            throw new IllegalArgumentException("Pocket currency does not match wallet currency");
        }
        return wallet;
    }

    private void invalidateWalletCaches(Wallet wallet) {
        cacheService.invalidate("balance:account:" + wallet.getAccountId());
        cacheService.invalidate("balance:available:account:" + wallet.getAccountId());
        cacheService.invalidate("wallet:account:" + wallet.getAccountId());
        cacheService.invalidate("wallet:id:" + wallet.getId());
    }
}
