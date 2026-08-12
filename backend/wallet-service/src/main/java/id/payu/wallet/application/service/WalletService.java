package id.payu.wallet.application.service;

import id.payu.cache.service.CacheService;
import id.payu.wallet.domain.model.Wallet;
import id.payu.wallet.domain.model.WalletTransaction;
import id.payu.wallet.domain.model.LedgerEntry;
import id.payu.wallet.domain.port.in.JournalUseCase;
import id.payu.wallet.domain.port.in.WalletUseCase;
import id.payu.wallet.domain.port.out.WalletEventPublisherPort;
import id.payu.wallet.domain.port.out.WalletPersistencePort;
import id.payu.wallet.application.exception.WalletNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.Objects;
import id.payu.wallet.domain.model.EntryType;
import id.payu.wallet.domain.model.TransactionType;
import id.payu.wallet.domain.model.WalletStatus;

@Service
public class WalletService implements WalletUseCase {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WalletService.class);

    private final WalletPersistencePort walletPersistencePort;
    private final WalletEventPublisherPort walletEventPublisher;
    private final CacheService cacheService;
    private final JournalUseCase journalUseCase;

    public WalletService(
            WalletPersistencePort walletPersistencePort,
            WalletEventPublisherPort walletEventPublisher,
            CacheService cacheService,
            JournalUseCase journalUseCase) {
        this.walletPersistencePort = walletPersistencePort;
        this.walletEventPublisher = walletEventPublisher;
        this.cacheService = cacheService;
        this.journalUseCase = journalUseCase;
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
                .status(WalletStatus.ACTIVE)
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
        if (amount == null || amount.signum() <= 0 || amount.scale() > 4) {
            throw new IllegalArgumentException("Reserve amount must be positive with at most 4 decimals");
        }
        if (referenceId == null || referenceId.isBlank()) {
            throw new IllegalArgumentException("Reference ID is required");
        }

        Optional<LedgerEntry> existingReservation = walletPersistencePort.findReservationByReference(referenceId);
        if (existingReservation.isPresent()) {
            validateReservationReplay(existingReservation.get(), accountId, amount);
            return existingReservation.get().getTransactionId().toString();
        }

        log.info("Reserving {} for account {} with reference {}", amount, accountId, referenceId);

        // BUG-BE-164 FIX: Use pessimistic lock for balance-modifying operation
        Wallet wallet = walletPersistencePort.findByAccountIdForUpdate(accountId)
                .orElseThrow(() -> new WalletNotFoundException(accountId));

        existingReservation = walletPersistencePort.findReservationByReference(referenceId);
        if (existingReservation.isPresent()) {
            validateReservationReplay(existingReservation.get(), accountId, amount);
            return existingReservation.get().getTransactionId().toString();
        }

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
                .transactionId(UUID.fromString(reservationId))
                .accountId(accountId) // accountId is String in both Wallet and Ledger now
                .entryType(EntryType.DEBIT)
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

        if (walletPersistencePort.findByTransactionId(UUID.fromString(reservationId)).stream()
                .anyMatch(entry -> "COMMIT".equals(entry.getReferenceType()))) {
            return;
        }

        BigDecimal reservedAmount = debitEntry.getAmount();
        String accountId = debitEntry.getAccountId();

        // BUG-BE-164 FIX: Use pessimistic lock for balance-modifying operation
        Wallet wallet = walletPersistencePort.findByAccountIdForUpdate(accountId)
                .orElseThrow(() -> new WalletNotFoundException(accountId));
        if (walletPersistencePort.findByTransactionId(UUID.fromString(reservationId)).stream()
                .anyMatch(entry -> "COMMIT".equals(entry.getReferenceType()))) {
            return;
        }
        wallet.commitReservation(reservedAmount);
        walletPersistencePort.save(wallet);

        // Invalidate balance cache
        cacheService.invalidate("balance:account:" + accountId);
        cacheService.invalidate("balance:available:account:" + accountId);
        cacheService.invalidate("wallet:account:" + accountId);
        cacheService.invalidate("wallet:id:" + wallet.getId());

        LedgerEntry commitEntry = LedgerEntry.builder()
                .transactionId(UUID.fromString(reservationId))
                .accountId(accountId)
                .entryType(EntryType.DEBIT)
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

        if (walletPersistencePort.findByTransactionId(UUID.fromString(reservationId)).stream()
                .anyMatch(entry -> "RELEASE".equals(entry.getReferenceType()))) {
            return;
        }

        BigDecimal reservedAmount = releaseEntry.getAmount();
        String accountId = releaseEntry.getAccountId();

        // BUG-BE-164 FIX: Use pessimistic lock for balance-modifying operation
        Wallet wallet = walletPersistencePort.findByAccountIdForUpdate(accountId)
                .orElseThrow(() -> new WalletNotFoundException(accountId));
        if (walletPersistencePort.findByTransactionId(UUID.fromString(reservationId)).stream()
                .anyMatch(entry -> "RELEASE".equals(entry.getReferenceType()))) {
            return;
        }
        wallet.releaseReservation(reservedAmount);
        walletPersistencePort.save(wallet);

        // Invalidate balance cache
        cacheService.invalidate("balance:account:" + accountId);
        cacheService.invalidate("balance:available:account:" + accountId);
        cacheService.invalidate("wallet:account:" + accountId);
        cacheService.invalidate("wallet:id:" + wallet.getId());

        LedgerEntry creditEntry = LedgerEntry.builder()
                .transactionId(UUID.fromString(reservationId))
                .accountId(accountId)
                .entryType(EntryType.CREDIT)
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
        if (amount == null || amount.signum() <= 0 || amount.scale() > 4) {
            throw new IllegalArgumentException("Credit amount must be positive with at most 4 decimals");
        }
        if (referenceId == null || referenceId.isBlank()) {
            throw new IllegalArgumentException("Reference ID is required");
        }

        Optional<WalletTransaction> existingTransaction = walletPersistencePort.findTransactionByReference(referenceId);
        if (existingTransaction.isPresent()) {
            return validateCreditReplay(existingTransaction.get(), accountId, amount);
        }

        log.info("Crediting {} to account {} with reference {}", amount, accountId, referenceId);

        // BUG-BE-164 FIX: Use pessimistic lock for balance-modifying operation
        Wallet wallet = walletPersistencePort.findByAccountIdForUpdate(accountId)
                .orElseThrow(() -> new WalletNotFoundException(accountId));

        existingTransaction = walletPersistencePort.findTransactionByReference(referenceId);
        if (existingTransaction.isPresent()) {
            return validateCreditReplay(existingTransaction.get(), accountId, amount);
        }

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
                .transactionId(transactionId)
                .accountId(accountId)
                .entryType(EntryType.CREDIT)
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
                .type(TransactionType.CREDIT)
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

    private void validateReservationReplay(LedgerEntry reservation, String accountId, BigDecimal amount) {
        if (!accountId.equals(reservation.getAccountId())
                || reservation.getAmount() == null
                || reservation.getAmount().compareTo(amount) != 0) {
            throw new IllegalArgumentException("Reference ID was already used for a different reservation");
        }
    }

    private String validateCreditReplay(WalletTransaction transaction, String accountId, BigDecimal amount) {
        Wallet wallet = walletPersistencePort.findByAccountId(accountId)
                .orElseThrow(() -> new WalletNotFoundException(accountId));
        if (!wallet.getId().equals(transaction.getWalletId())
                || transaction.getType() != TransactionType.CREDIT
                || transaction.getAmount() == null
                || transaction.getAmount().compareTo(amount) != 0) {
            throw new IllegalArgumentException("Reference ID was already used for a different credit");
        }
        return transaction.getId().toString();
    }

    @Override
    @Transactional
    public String transfer(String senderAccountId, String recipientAccountId, BigDecimal amount,
                           String currency, String referenceId, String description) {
        if (senderAccountId == null || recipientAccountId == null
                || senderAccountId.equals(recipientAccountId)
                || referenceId == null || referenceId.isBlank()
                || currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Transfer requires distinct accounts and a reference");
        }
        String normalizedCurrency = currency.trim().toUpperCase(Locale.ROOT);
        if (amount == null || amount.signum() <= 0 || amount.scale() > 4) {
            throw new IllegalArgumentException("Transfer amount must be positive with at most 4 decimals");
        }

        UUID transactionId = UUID.nameUUIDFromBytes(
                ("WALLET_TRANSFER:" + referenceId).getBytes(StandardCharsets.UTF_8));
        List<LedgerEntry> existingEntries = walletPersistencePort.findByTransactionId(transactionId);
        if (!existingEntries.isEmpty()) {
            boolean sameCommand = existingEntries.stream().allMatch(entry ->
                    "TRANSFER".equals(entry.getReferenceType())
                            && referenceId.equals(entry.getReferenceId())
                            && entry.getAmount() != null
                            && entry.getAmount().compareTo(amount) == 0
                            && normalizedCurrency.equals(entry.getCurrency()));
            if (!sameCommand) {
                throw new IllegalArgumentException("Transfer reference was already used for a different command");
            }
            return transactionId.toString();
        }

        String firstAccount = senderAccountId.compareTo(recipientAccountId) < 0
                ? senderAccountId : recipientAccountId;
        String secondAccount = firstAccount.equals(senderAccountId) ? recipientAccountId : senderAccountId;
        Wallet firstWallet = walletPersistencePort.findByAccountIdForUpdate(firstAccount)
                .orElseThrow(() -> new WalletNotFoundException(firstAccount));
        Wallet secondWallet = walletPersistencePort.findByAccountIdForUpdate(secondAccount)
                .orElseThrow(() -> new WalletNotFoundException(secondAccount));
        Wallet sender = firstWallet.getAccountId().equals(senderAccountId) ? firstWallet : secondWallet;
        Wallet recipient = firstWallet.getAccountId().equals(recipientAccountId) ? firstWallet : secondWallet;

        if (!Objects.equals(normalizedCurrency, sender.getCurrency())
                || !Objects.equals(normalizedCurrency, recipient.getCurrency())) {
            throw new IllegalArgumentException("Transfer currency must match both wallet currencies");
        }

        sender.debit(amount);
        recipient.credit(amount);
        walletPersistencePort.save(sender);
        walletPersistencePort.save(recipient);

        LocalDateTime now = LocalDateTime.now();
        walletPersistencePort.saveLedgerEntry(LedgerEntry.builder()
                .transactionId(transactionId)
                .accountId(sender.getAccountId())
                .entryType(EntryType.DEBIT)
                .amount(amount)
                .currency(normalizedCurrency)
                .balanceAfter(sender.getBalance())
                .referenceType("TRANSFER")
                .referenceId(referenceId)
                .createdAt(now)
                .build());
        walletPersistencePort.saveLedgerEntry(LedgerEntry.builder()
                .transactionId(transactionId)
                .accountId(recipient.getAccountId())
                .entryType(EntryType.CREDIT)
                .amount(amount)
                .currency(normalizedCurrency)
                .balanceAfter(recipient.getBalance())
                .referenceType("TRANSFER")
                .referenceId(referenceId)
                .createdAt(now)
                .build());

        walletEventPublisher.publishBalanceChanged(sender.getAccountId(), sender.getBalance(), sender.getAvailableBalance());
        walletEventPublisher.publishBalanceChanged(recipient.getAccountId(), recipient.getBalance(), recipient.getAvailableBalance());
        invalidateWalletCaches(sender);
        invalidateWalletCaches(recipient);
        return transactionId.toString();
    }

    @Override
    @Transactional
    public String repayLoan(String accountId, String loanId, BigDecimal amount, String currency,
                            String referenceId, String description) {
        if (accountId == null || accountId.isBlank() || loanId == null || loanId.isBlank()
                || referenceId == null || referenceId.isBlank()) {
            throw new IllegalArgumentException("Loan repayment account, loan, and reference are required");
        }
        if (amount == null || amount.signum() <= 0 || amount.scale() > 4) {
            throw new IllegalArgumentException("Loan repayment amount must be positive with at most 4 decimals");
        }

        String normalizedCurrency = currency == null ? "" : currency.trim().toUpperCase(Locale.ROOT);
        UUID transactionId = UUID.nameUUIDFromBytes(
                ("LOAN_REPAYMENT:" + referenceId).getBytes(StandardCharsets.UTF_8));
        List<LedgerEntry> existingEntries = walletPersistencePort.findByTransactionId(transactionId);
        if (!existingEntries.isEmpty()) {
            boolean sameCommand = existingEntries.stream().allMatch(entry ->
                    referenceId.equals(entry.getReferenceId())
                            && entry.getAmount() != null
                            && entry.getAmount().compareTo(amount) == 0
                            && normalizedCurrency.equals(entry.getCurrency()));
            if (!sameCommand) {
                throw new IllegalArgumentException("Repayment reference was already used for a different command");
            }
            return transactionId.toString();
        }

        Wallet wallet = walletPersistencePort.findByAccountIdForUpdate(accountId)
                .orElseThrow(() -> new WalletNotFoundException(accountId));
        if (!normalizedCurrency.equals(wallet.getCurrency())) {
            throw new IllegalArgumentException("Wallet currency does not match repayment currency");
        }

        // ponytail: one wallet lock and one journal transaction; split settlement only if loan products diverge.
        wallet.debit(amount);
        walletPersistencePort.save(wallet);

        walletPersistencePort.saveTransaction(WalletTransaction.builder()
                .id(transactionId)
                .walletId(wallet.getId())
                .referenceId(referenceId)
                .type(TransactionType.DEBIT)
                .amount(amount)
                .balanceAfter(wallet.getBalance())
                .description(description)
                .createdAt(LocalDateTime.now())
                .build());

        List<LedgerEntry> entries = new ArrayList<>();
        entries.add(LedgerEntry.builder()
                .transactionId(transactionId)
                .accountId(accountId)
                .coaCode("1100")
                .entryType(EntryType.DEBIT)
                .amount(amount)
                .currency(normalizedCurrency)
                .balanceAfter(wallet.getAvailableBalance())
                .referenceType("LOAN_REPAYMENT")
                .referenceId(referenceId)
                .createdAt(LocalDateTime.now())
                .build());
        entries.add(LedgerEntry.builder()
                .transactionId(transactionId)
                .accountId("LOAN_RECEIVABLE:" + loanId)
                .coaCode("1500")
                .entryType(EntryType.CREDIT)
                .amount(amount)
                .currency(normalizedCurrency)
                .balanceAfter(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_EVEN))
                .referenceType("LOAN_REPAYMENT")
                .referenceId(referenceId)
                .createdAt(LocalDateTime.now())
                .build());

        journalUseCase.createAndPostJournal(
                "Loan repayment: " + loanId,
                "LOAN_REPAYMENT",
                referenceId,
                entries,
                "lending-service");

        cacheService.invalidate("balance:account:" + accountId);
        cacheService.invalidate("balance:available:account:" + accountId);
        cacheService.invalidate("wallet:account:" + accountId);
        cacheService.invalidate("wallet:id:" + wallet.getId());
        walletEventPublisher.publishBalanceChanged(accountId, wallet.getBalance(), wallet.getAvailableBalance());
        return transactionId.toString();
    }

    @Override
    @Transactional
    public void reverseTransfer(String senderAccountId, String recipientAccountId, BigDecimal amount,
                                String currency, UUID refundId, String description) {
        if (senderAccountId == null || recipientAccountId == null || senderAccountId.equals(recipientAccountId)) {
            throw new IllegalArgumentException("Refund reversal requires distinct sender and recipient wallets");
        }
        if (amount == null || amount.signum() <= 0 || refundId == null) {
            throw new IllegalArgumentException("Refund reversal requires a positive amount and refund ID");
        }

        String firstAccount = senderAccountId.compareTo(recipientAccountId) < 0
                ? senderAccountId : recipientAccountId;
        String secondAccount = firstAccount.equals(senderAccountId) ? recipientAccountId : senderAccountId;
        Wallet firstWallet = walletPersistencePort.findByAccountIdForUpdate(firstAccount)
                .orElseThrow(() -> new WalletNotFoundException(firstAccount));
        Wallet secondWallet = walletPersistencePort.findByAccountIdForUpdate(secondAccount)
                .orElseThrow(() -> new WalletNotFoundException(secondAccount));

        if (walletPersistencePort.findByTransactionId(refundId).stream()
                .anyMatch(entry -> "REFUND_REVERSAL".equals(entry.getReferenceType()))) {
            return;
        }

        Wallet sender = firstWallet.getAccountId().equals(senderAccountId) ? firstWallet : secondWallet;
        Wallet recipient = firstWallet.getAccountId().equals(recipientAccountId) ? firstWallet : secondWallet;
        if (!Objects.equals(currency, sender.getCurrency()) || !Objects.equals(currency, recipient.getCurrency())) {
            throw new IllegalArgumentException("Refund currency must match both wallet currencies");
        }

        recipient.debit(amount);
        sender.credit(amount);
        walletPersistencePort.save(recipient);
        walletPersistencePort.save(sender);

        LocalDateTime now = LocalDateTime.now();
        walletPersistencePort.saveLedgerEntry(LedgerEntry.builder()
                .transactionId(refundId)
                .accountId(recipient.getAccountId())
                .entryType(EntryType.DEBIT)
                .amount(amount)
                .currency(currency)
                .balanceAfter(recipient.getBalance())
                .referenceType("REFUND_REVERSAL")
                .referenceId(refundId.toString())
                .createdAt(now)
                .build());
        walletPersistencePort.saveLedgerEntry(LedgerEntry.builder()
                .transactionId(refundId)
                .accountId(sender.getAccountId())
                .entryType(EntryType.CREDIT)
                .amount(amount)
                .currency(currency)
                .balanceAfter(sender.getBalance())
                .referenceType("REFUND_REVERSAL")
                .referenceId(refundId.toString())
                .createdAt(now)
                .build());

        walletEventPublisher.publishBalanceChanged(recipient.getAccountId(), recipient.getBalance(), recipient.getAvailableBalance());
        walletEventPublisher.publishBalanceChanged(sender.getAccountId(), sender.getBalance(), sender.getAvailableBalance());
        invalidateWalletCaches(recipient);
        invalidateWalletCaches(sender);
        log.info("Reversed transfer for refund {}: {} -> {} amount {}", refundId, recipientAccountId, senderAccountId, amount);
    }

    private void invalidateWalletCaches(Wallet wallet) {
        cacheService.invalidate("balance:account:" + wallet.getAccountId());
        cacheService.invalidate("balance:available:account:" + wallet.getAccountId());
        cacheService.invalidate("wallet:account:" + wallet.getAccountId());
        cacheService.invalidate("wallet:id:" + wallet.getId());
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
