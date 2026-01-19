package id.payu.wallet.application.service;

import id.payu.wallet.domain.model.Wallet;
import id.payu.wallet.domain.model.WalletTransaction;
import id.payu.wallet.domain.port.in.WalletUseCase;
import id.payu.wallet.domain.port.out.WalletEventPublisherPort;
import id.payu.wallet.domain.port.out.WalletPersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of WalletUseCase - the application service layer.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService implements WalletUseCase {

    private final WalletPersistencePort walletPersistencePort;
    private final WalletEventPublisherPort walletEventPublisher;

    // In-memory reservation tracking (in production, use Redis)
    private final Map<String, ReservationInfo> reservations = new ConcurrentHashMap<>();

    @Override
    @Transactional(readOnly = true)
    public Optional<Wallet> getWalletByAccountId(String accountId) {
        log.debug("Getting wallet for account: {}", accountId);
        return walletPersistencePort.findByAccountId(accountId);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getBalance(String accountId) {
        return getWalletByAccountId(accountId)
                .map(Wallet::getBalance)
                .orElseThrow(() -> new WalletNotFoundException(accountId));
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getAvailableBalance(String accountId) {
        return getWalletByAccountId(accountId)
                .map(Wallet::getAvailableBalance)
                .orElseThrow(() -> new WalletNotFoundException(accountId));
    }

    @Override
    @Transactional
    public String reserveBalance(String accountId, BigDecimal amount, String referenceId) {
        log.info("Reserving {} for account {} with reference {}", amount, accountId, referenceId);

        Wallet wallet = walletPersistencePort.findByAccountId(accountId)
                .orElseThrow(() -> new WalletNotFoundException(accountId));

        if (!wallet.hasSufficientBalance(amount)) {
            throw new InsufficientBalanceException(accountId, amount, wallet.getAvailableBalance());
        }

        wallet.reserve(amount);
        walletPersistencePort.save(wallet);

        String reservationId = UUID.randomUUID().toString();
        reservations.put(reservationId, new ReservationInfo(accountId, amount, referenceId));

        walletEventPublisher.publishBalanceReserved(accountId, reservationId, amount);

        log.info("Reserved {} for account {}, reservation ID: {}", amount, accountId, reservationId);
        return reservationId;
    }

    @Override
    @Transactional
    public void commitReservation(String reservationId) {
        ReservationInfo info = reservations.remove(reservationId);
        if (info == null) {
            throw new ReservationNotFoundException(reservationId);
        }

        log.info("Committing reservation {} for account {}", reservationId, info.accountId());

        Wallet wallet = walletPersistencePort.findByAccountId(info.accountId())
                .orElseThrow(() -> new WalletNotFoundException(info.accountId()));

        wallet.commitReservation(info.amount());
        walletPersistencePort.save(wallet);

        // Create ledger entry
        WalletTransaction transaction = WalletTransaction.builder()
                .id(UUID.randomUUID())
                .walletId(wallet.getId())
                .referenceId(info.referenceId())
                .type(WalletTransaction.TransactionType.DEBIT)
                .amount(info.amount())
                .balanceAfter(wallet.getBalance())
                .description("Transfer out - " + info.referenceId())
                .createdAt(LocalDateTime.now())
                .build();
        walletPersistencePort.saveTransaction(transaction);

        walletEventPublisher.publishReservationCommitted(info.accountId(), reservationId, info.amount());
        walletEventPublisher.publishBalanceChanged(info.accountId(), wallet.getBalance(), wallet.getAvailableBalance());

        log.info("Committed reservation {} for account {}", reservationId, info.accountId());
    }

    @Override
    @Transactional
    public void releaseReservation(String reservationId) {
        ReservationInfo info = reservations.remove(reservationId);
        if (info == null) {
            throw new ReservationNotFoundException(reservationId);
        }

        log.info("Releasing reservation {} for account {}", reservationId, info.accountId());

        Wallet wallet = walletPersistencePort.findByAccountId(info.accountId())
                .orElseThrow(() -> new WalletNotFoundException(info.accountId()));

        wallet.releaseReservation(info.amount());
        walletPersistencePort.save(wallet);

        walletEventPublisher.publishReservationReleased(info.accountId(), reservationId, info.amount());
        walletEventPublisher.publishBalanceChanged(info.accountId(), wallet.getBalance(), wallet.getAvailableBalance());

        log.info("Released reservation {} for account {}", reservationId, info.accountId());
    }

    @Override
    @Transactional
    public void credit(String accountId, BigDecimal amount, String referenceId, String description) {
        log.info("Crediting {} to account {} with reference {}", amount, accountId, referenceId);

        Wallet wallet = walletPersistencePort.findByAccountId(accountId)
                .orElseThrow(() -> new WalletNotFoundException(accountId));

        wallet.credit(amount);
        walletPersistencePort.save(wallet);

        // Create ledger entry
        WalletTransaction transaction = WalletTransaction.builder()
                .id(UUID.randomUUID())
                .walletId(wallet.getId())
                .referenceId(referenceId)
                .type(WalletTransaction.TransactionType.CREDIT)
                .amount(amount)
                .balanceAfter(wallet.getBalance())
                .description(description)
                .createdAt(LocalDateTime.now())
                .build();
        walletPersistencePort.saveTransaction(transaction);

        walletEventPublisher.publishBalanceChanged(accountId, wallet.getBalance(), wallet.getAvailableBalance());

        log.info("Credited {} to account {}", amount, accountId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WalletTransaction> getTransactionHistory(String accountId, int page, int size) {
        Wallet wallet = walletPersistencePort.findByAccountId(accountId)
                .orElseThrow(() -> new WalletNotFoundException(accountId));
        return walletPersistencePort.findTransactionsByWalletId(wallet.getId(), page, size);
    }

    // Record for tracking reservations
    private record ReservationInfo(String accountId, BigDecimal amount, String referenceId) {}

    // Custom exceptions
    public static class WalletNotFoundException extends RuntimeException {
        public WalletNotFoundException(String accountId) {
            super("Wallet not found for account: " + accountId);
        }
    }

    public static class InsufficientBalanceException extends RuntimeException {
        public InsufficientBalanceException(String accountId, BigDecimal requested, BigDecimal available) {
            super(String.format("Insufficient balance for account %s. Requested: %s, Available: %s",
                    accountId, requested, available));
        }
    }

    public static class ReservationNotFoundException extends RuntimeException {
        public ReservationNotFoundException(String reservationId) {
            super("Reservation not found: " + reservationId);
        }
    }
}
