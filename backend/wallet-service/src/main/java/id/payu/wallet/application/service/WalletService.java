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

        return cacheService.get(
                cacheKey,
                Wallet.class,
                () -> {
                    Optional<Wallet> wallet = walletPersistencePort.findByAccountId(accountId);
                    wallet.ifPresent(w -> cacheService.put(cacheKey, w, Duration.ofMinutes(10)));
                    return wallet;
                }
        );
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

        if (walletPersistencePort.findByAccountId(accountId).isPresent()) {
            log.warn("Wallet already exists for account: {}", accountId);
            return walletPersistencePort.findByAccountId(accountId).get();
        }

        Wallet wallet = Wallet.builder()
                .id(UUID.randomUUID())
                .accountId(accountId)
                .balance(BigDecimal.ZERO)
                .reservedBalance(BigDecimal.ZERO)
                .currency("IDR")
                .status(Wallet.WalletStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        walletPersistencePort.save(wallet);
        walletEventPublisher.publishWalletCreated(accountId, wallet.getId().toString());
        
        log.info("Wallet created successfully: {}", wallet.getId());
        return wallet;
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
        log.info("Reserving {} for account {} with reference {}", amount, accountId, referenceId);

        Wallet wallet = getWalletByAccountId(accountId)
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

        LedgerEntry debitEntry = LedgerEntry.builder()
                .id(UUID.randomUUID())
                .transactionId(UUID.fromString(reservationId))
                .accountId(UUID.fromString(accountId)) // accountId is String in Wallet but often UUID in Ledger
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
        UUID accountId = debitEntry.getAccountId(); // LedgerEntry uses UUID for accountId

        Wallet wallet = getWalletByAccountId(accountId.toString())
                .orElseThrow(() -> new WalletNotFoundException(accountId.toString()));
        wallet.commitReservation(reservedAmount);
        walletPersistencePort.save(wallet);

        // Invalidate balance cache
        cacheService.invalidate("balance:account:" + accountId.toString());
        cacheService.invalidate("balance:available:account:" + accountId.toString());
        cacheService.invalidate("wallet:account:" + accountId.toString());

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

        walletEventPublisher.publishReservationCommitted(accountId.toString(), reservationId, reservedAmount);
        walletEventPublisher.publishBalanceChanged(accountId.toString(), wallet.getBalance(), wallet.getAvailableBalance());
        
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
        UUID accountId = releaseEntry.getAccountId();

        Wallet wallet = getWalletByAccountId(accountId.toString())
                .orElseThrow(() -> new WalletNotFoundException(accountId.toString()));
        wallet.releaseReservation(reservedAmount);
        walletPersistencePort.save(wallet);

        // Invalidate balance cache
        cacheService.invalidate("balance:account:" + accountId.toString());
        cacheService.invalidate("balance:available:account:" + accountId.toString());
        cacheService.invalidate("wallet:account:" + accountId.toString());

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

        walletEventPublisher.publishReservationReleased(accountId.toString(), reservationId, reservedAmount);
        walletEventPublisher.publishBalanceChanged(accountId.toString(), wallet.getBalance(), wallet.getAvailableBalance());
        
        log.info("Released reservation {} for account {}, amount: {}", reservationId, reservedAmount);
    }

    @Override
    @Transactional
    public void credit(String accountId, BigDecimal amount, String referenceId, String description) {
        log.info("Crediting {} to account {} with reference {}", amount, accountId, referenceId);

        Wallet wallet = getWalletByAccountId(accountId)
                .orElseThrow(() -> new WalletNotFoundException(accountId));
        
        // Update wallet balance
        BigDecimal oldBalance = wallet.getBalance();
        wallet.credit(amount);
        walletPersistencePort.save(wallet);

        // Invalidate balance cache
        cacheService.invalidate("balance:account:" + accountId);
        cacheService.invalidate("balance:available:account:" + accountId);
        cacheService.invalidate("wallet:account:" + accountId);

        // Create Ledger Entry
        LedgerEntry creditEntry = LedgerEntry.builder()
                .id(UUID.randomUUID())
                .transactionId(UUID.fromString(UUID.randomUUID().toString())) // simplified
                .accountId(UUID.fromString(accountId))
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
                .id(UUID.randomUUID())
                .walletId(wallet.getId())
                .referenceId(referenceId)
                .type(WalletTransaction.TransactionType.CREDIT)
                .amount(amount)
                .balanceAfter(wallet.getBalance()) // Use balance, not available balance? Depends on logic
                .description(description)
                .createdAt(LocalDateTime.now())
                .build();
        walletPersistencePort.saveTransaction(walletTransaction);

        walletEventPublisher.publishBalanceChanged(accountId, wallet.getBalance(), wallet.getAvailableBalance());
        
        log.info("Credited {} to account {}, amount: {}", accountId, amount);
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
    public List<LedgerEntry> getLedgerEntriesByAccountId(UUID accountId) {
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
