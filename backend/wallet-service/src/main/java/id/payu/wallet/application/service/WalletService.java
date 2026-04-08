package id.payu.wallet.application.service;

import id.payu.cache.service.CacheService;
import id.payu.wallet.domain.model.Wallet;
import id.payu.wallet.domain.model.WalletTransaction;
import id.payu.wallet.domain.model.LedgerEntry;
import id.payu.wallet.domain.port.in.WalletUseCase;
import id.payu.wallet.domain.port.out.WalletEventPublisherPort;
import id.payu.wallet.domain.port.out.WalletPersistencePort;
import id.payu.wallet.application.exception.WalletNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class WalletService implements WalletUseCase {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WalletService.class);

    private final WalletPersistencePort walletPersistencePort;
    private final WalletEventPublisherPort walletEventPublisher;
    private final CacheService cacheService;

    public WalletService(
            WalletPersistencePort walletPersistencePort,
            WalletEventPublisherPort walletEventPublisher,
            CacheService cacheService) {
        this.walletPersistencePort = walletPersistencePort;
        this.walletEventPublisher = walletEventPublisher;
        this.cacheService = cacheService;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Wallet> getWalletByAccountId(String accountId) {
        log.debug("Getting wallet for account: {}", accountId);
        String cacheKey = "wallet:account:" + accountId;

        // Try cache first
        Wallet cached = cacheService.get(cacheKey, Wallet.class);
        if (cached != null) {
            return Optional.of(cached);
        }

        // Cache miss - fetch from DB
        Optional<Wallet> wallet = walletPersistencePort.findByAccountId(accountId);
        wallet.ifPresent(w -> cacheService.put(cacheKey, w, Duration.ofMinutes(10)));
        return wallet;
    }

    @Override
    @Transactional(readOnly = true)
    public Wallet getWallet(UUID walletId) {
        log.debug("Getting wallet by ID: {}", walletId);
        String cacheKey = "wallet:id:" + walletId;

        return cacheService.get(
                cacheKey,
                Wallet.class,
                () -> {
                    Wallet wallet = walletPersistencePort.findById(walletId)
                            .orElseThrow(() -> new WalletNotFoundException(walletId.toString()));
                    cacheService.put(cacheKey, wallet, Duration.ofMinutes(10));
                    return wallet;
                }
        );
    }

    @Override
    @Transactional
    public Wallet createWallet(String accountId) {
        log.info("Creating wallet for account: {}", accountId);

        // BUG-BE-013 Fix: Reuse result from first query instead of querying twice
        Optional<Wallet> existing = walletPersistencePort.findByAccountId(accountId);
        if (existing.isPresent()) {
            log.warn("Wallet already exists for account: {}", accountId);
            return existing.get();
        }

        Wallet wallet = Wallet.builder()
                .accountId(accountId)
                .balance(BigDecimal.ZERO)
                .reservedBalance(BigDecimal.ZERO)
                .currency("IDR")
                .status(Wallet.WalletStatus.ACTIVE)
                .build();

        Wallet savedWallet = walletPersistencePort.save(wallet);
        walletEventPublisher.publishWalletCreated(accountId, savedWallet.getId().toString());

        log.info("Wallet created successfully: {}", savedWallet.getId());
        return savedWallet;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getBalance(String accountId) {
        log.debug("Getting balance for account: {}", accountId);
        String cacheKey = "balance:account:" + accountId;

        return cacheService.getWithStaleWhileRevalidate(
                cacheKey,
                BigDecimal.class,
                () -> getWalletByAccountId(accountId)
                        .map(Wallet::getBalance)
                        .orElseThrow(() -> new WalletNotFoundException(accountId)),
                Duration.ofSeconds(15),  // Soft TTL - serve stale data
                Duration.ofSeconds(30)   // Hard TTL - must refresh
        );
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getAvailableBalance(String accountId) {
        log.debug("Getting available balance for account: {}", accountId);
        String cacheKey = "balance:available:account:" + accountId;

        return cacheService.getWithStaleWhileRevalidate(
                cacheKey,
                BigDecimal.class,
                () -> getWalletByAccountId(accountId)
                        .map(Wallet::getAvailableBalance)
                        .orElseThrow(() -> new WalletNotFoundException(accountId)),
                Duration.ofSeconds(15),  // Soft TTL
                Duration.ofSeconds(30)   // Hard TTL
        );
    }

    @Override
    @Transactional
    public String reserveBalance(String accountId, BigDecimal amount, String referenceId) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        log.info("Reserving {} for account {} with reference {}", amount, accountId, referenceId);

        // BUG-BE-164 FIX: Use pessimistic lock for balance-modifying operation
        Wallet wallet = walletPersistencePort.findByAccountIdForUpdate(accountId)
                .orElseThrow(() -> new WalletNotFoundException(accountId));

        if (!wallet.hasSufficientBalance(amount)) {
            throw new InsufficientBalanceException(accountId, amount, wallet.getAvailableBalance());
        }

        String reservationId = UUID.randomUUID().toString();
        wallet.reserve(amount);

        walletPersistencePort.save(wallet);

        // Invalidate balance cache
        cacheService.invalidate("balance:account:" + accountId);
        cacheService.invalidate("balance:available:account:" + accountId);
        cacheService.invalidate("wallet:account:" + accountId);
        cacheService.invalidate("wallet:id:" + wallet.getId());

        LedgerEntry debitEntry = LedgerEntry.builder()
                .id(UUID.randomUUID())
                .transactionId(UUID.fromString(reservationId))
                .accountId(accountId) // accountId is String in both Wallet and Ledger now
                .entryType(LedgerEntry.EntryType.DEBIT)
                .amount(amount)
                .currency(wallet.getCurrency())
                .balanceAfter(wallet.getAvailableBalance())
                .referenceType("RESERVATION")
                .referenceId(referenceId)
                .createdAt(LocalDateTime.now())
                .build();

        walletPersistencePort.saveLedgerEntry(debitEntry);

        walletEventPublisher.publishBalanceReserved(accountId, reservationId, amount);

        log.info("Reserved {} for account {}, reservation ID: {}, amount: {}", accountId, reservationId, amount);
        return reservationId;
    }

    @Override
    @Transactional
    public void commitReservation(String reservationId) {
        log.info("Committing reservation {} for account", reservationId);

        LedgerEntry debitEntry = walletPersistencePort.findByTransactionId(UUID.fromString(reservationId))
                .stream()
                .filter(entry -> "RESERVATION".equals(entry.getReferenceType()))
                .findFirst()
                .orElseThrow(() -> new ReservationNotFoundException(reservationId));

        BigDecimal reservedAmount = debitEntry.getAmount();
        String accountId = debitEntry.getAccountId();

        // BUG-BE-164 FIX: Use pessimistic lock for balance-modifying operation
        Wallet wallet = walletPersistencePort.findByAccountIdForUpdate(accountId)
                .orElseThrow(() -> new WalletNotFoundException(accountId));
        wallet.commitReservation(reservedAmount);
        walletPersistencePort.save(wallet);

        // Invalidate balance cache
        cacheService.invalidate("balance:account:" + accountId);
        cacheService.invalidate("balance:available:account:" + accountId);
        cacheService.invalidate("wallet:account:" + accountId);
        cacheService.invalidate("wallet:id:" + wallet.getId());

        LedgerEntry commitEntry = LedgerEntry.builder()
                .id(UUID.randomUUID())
                .transactionId(UUID.fromString(reservationId))
                .accountId(accountId)
                .entryType(LedgerEntry.EntryType.DEBIT)
                .amount(reservedAmount)
                .currency(wallet.getCurrency())
                .balanceAfter(wallet.getAvailableBalance())
                .referenceType("COMMIT")
                .createdAt(LocalDateTime.now())
                .build();

        walletPersistencePort.saveLedgerEntry(commitEntry);

        walletEventPublisher.publishReservationCommitted(accountId, reservationId, reservedAmount);
        walletEventPublisher.publishBalanceChanged(accountId, wallet.getBalance(), wallet.getAvailableBalance());

        log.info("Committed reservation {} for account {}, amount: {}", reservationId, reservedAmount);
    }

    @Override
    @Transactional
    public void releaseReservation(String reservationId) {
        log.info("Releasing reservation {} for account", reservationId);

        LedgerEntry releaseEntry = walletPersistencePort.findByTransactionId(UUID.fromString(reservationId))
                .stream()
                .filter(entry -> "RESERVATION".equals(entry.getReferenceType()))
                .findFirst()
                .orElseThrow(() -> new ReservationNotFoundException(reservationId));

        BigDecimal reservedAmount = releaseEntry.getAmount();
        String accountId = releaseEntry.getAccountId();

        // BUG-BE-164 FIX: Use pessimistic lock for balance-modifying operation
        Wallet wallet = walletPersistencePort.findByAccountIdForUpdate(accountId)
                .orElseThrow(() -> new WalletNotFoundException(accountId));
        wallet.releaseReservation(reservedAmount);
        walletPersistencePort.save(wallet);

        // Invalidate balance cache
        cacheService.invalidate("balance:account:" + accountId);
        cacheService.invalidate("balance:available:account:" + accountId);
        cacheService.invalidate("wallet:account:" + accountId);
        cacheService.invalidate("wallet:id:" + wallet.getId());

        LedgerEntry creditEntry = LedgerEntry.builder()
                .id(UUID.randomUUID())
                .transactionId(UUID.fromString(reservationId))
                .accountId(accountId)
                .entryType(LedgerEntry.EntryType.CREDIT)
                .amount(reservedAmount)
                .currency(wallet.getCurrency())
                .balanceAfter(wallet.getAvailableBalance())
                .referenceType("RELEASE")
                .createdAt(LocalDateTime.now())
                .build();

        walletPersistencePort.saveLedgerEntry(creditEntry);

        walletEventPublisher.publishReservationReleased(accountId, reservationId, reservedAmount);
        walletEventPublisher.publishBalanceChanged(accountId, wallet.getBalance(), wallet.getAvailableBalance());

        log.info("Released reservation {} for account {}, amount: {}", reservationId, reservedAmount);
    }

    @Override
    @Transactional(readOnly = true)
    public String getAccountIdByReservationId(String reservationId) {
        log.debug("Getting account ID for reservation: {}", reservationId);

        return walletPersistencePort.findByTransactionId(UUID.fromString(reservationId))
                .stream()
                .filter(entry -> "RESERVATION".equals(entry.getReferenceType()))
                .findFirst()
                .map(entry -> entry.getAccountId().toString())
                .orElseThrow(() -> new ReservationNotFoundException(reservationId));
    }

    @Override
    @Transactional
    public String credit(String accountId, BigDecimal amount, String referenceId, String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        log.info("Crediting {} to account {} with reference {}", amount, accountId, referenceId);

        // BUG-BE-164 FIX: Use pessimistic lock for balance-modifying operation
        Wallet wallet = walletPersistencePort.findByAccountIdForUpdate(accountId)
                .orElseThrow(() -> new WalletNotFoundException(accountId));

        // Update wallet balance
        BigDecimal oldBalance = wallet.getBalance();
        wallet.credit(amount);
        walletPersistencePort.save(wallet);

        // Invalidate balance cache
        cacheService.invalidate("balance:account:" + accountId);
        cacheService.invalidate("balance:available:account:" + accountId);
        cacheService.invalidate("wallet:account:" + accountId);
        cacheService.invalidate("wallet:id:" + wallet.getId());

        // Generate transaction ID
        UUID transactionId = UUID.randomUUID();

        // Create Ledger Entry
        LedgerEntry creditEntry = LedgerEntry.builder()
                .id(UUID.randomUUID())
                .transactionId(transactionId)
                .accountId(accountId)
                .entryType(LedgerEntry.EntryType.CREDIT)
                .amount(amount)
                .currency(wallet.getCurrency())
                .balanceAfter(wallet.getAvailableBalance())
                .referenceType("CREDIT")
                .referenceId(referenceId)
                .createdAt(LocalDateTime.now())
                .build();

        walletPersistencePort.saveLedgerEntry(creditEntry);

        // Create Wallet Transaction
        WalletTransaction walletTransaction = WalletTransaction.builder()
                .id(transactionId)
                .walletId(wallet.getId())
                .referenceId(referenceId)
                .type(WalletTransaction.TransactionType.CREDIT)
                .amount(amount)
                .balanceAfter(wallet.getBalance())
                .description(description)
                .createdAt(LocalDateTime.now())
                .build();
        walletPersistencePort.saveTransaction(walletTransaction);

        walletEventPublisher.publishBalanceChanged(accountId, wallet.getBalance(), wallet.getAvailableBalance());

        log.info("Credited {} to account {}, transactionId: {}", amount, accountId, transactionId);
        return transactionId.toString();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WalletTransaction> getTransactionHistory(String accountId, int page, int size) {
        log.debug("Getting transaction history for account: {}, page: {}, size: {}", accountId, page, size);
        Wallet wallet = getWalletByAccountId(accountId)
                .orElseThrow(() -> new WalletNotFoundException(accountId));
        return walletPersistencePort.findTransactionsByWalletId(wallet.getId(), page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LedgerEntry> getLedgerEntriesByAccountId(String accountId) {
        log.debug("Getting ledger entries for account: {}", accountId);
        return walletPersistencePort.findByAccountIdOrderByCreatedAtDesc(accountId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LedgerEntry> getLedgerEntriesByTransactionId(UUID transactionId) {
        log.debug("Getting ledger entries for transaction: {}", transactionId);
        return walletPersistencePort.findByTransactionId(transactionId);
    }
}
