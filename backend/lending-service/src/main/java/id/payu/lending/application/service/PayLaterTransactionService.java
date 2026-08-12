package id.payu.lending.application.service;

import id.payu.lending.domain.model.PayLater;
import id.payu.lending.domain.model.PayLaterTransaction;
import id.payu.lending.domain.port.in.PayLaterTransactionUseCase;
import id.payu.lending.domain.port.out.PayLaterPersistencePort;
import id.payu.lending.domain.port.out.PayLaterTransactionPersistencePort;
import id.payu.lending.domain.port.out.WalletPaymentPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import id.payu.lending.domain.model.PayLaterStatus;
import id.payu.lending.domain.model.TransactionStatus;
import id.payu.lending.domain.model.TransactionType;

@Service
public class PayLaterTransactionService implements PayLaterTransactionUseCase {

    private static final Logger log = LoggerFactory.getLogger(PayLaterTransactionService.class);

    private final PayLaterPersistencePort payLaterPersistencePort;
    private final PayLaterTransactionPersistencePort transactionPersistencePort;
    private final WalletPaymentPort walletPaymentPort;

    public PayLaterTransactionService(PayLaterPersistencePort payLaterPersistencePort, 
                                      PayLaterTransactionPersistencePort transactionPersistencePort,
                                      WalletPaymentPort walletPaymentPort) {
        this.payLaterPersistencePort = payLaterPersistencePort;
        this.transactionPersistencePort = transactionPersistencePort;
        this.walletPaymentPort = walletPaymentPort;
    }

    @Override
    @Transactional
    public PayLaterTransaction recordPurchase(UUID userId, String merchantName, BigDecimal amount, String description) {
        return recordPurchase(userId, merchantName, amount, description, null);
    }

    @Override
    @Transactional
    public PayLaterTransaction recordPurchase(UUID userId, String merchantName, BigDecimal amount,
                                              String description, String externalId) {
        validateAmount(amount);
        log.info("Recording PayLater purchase for user: {} at merchant: {}", userId, merchantName);

        // PAYLATER-001: replay protection — the caller's idempotency key is the
        // unique external_id, so a replay returns the original record instead of
        // double-charging. The unique constraint on external_id is the backstop.
        if (externalId != null && !externalId.isBlank()) {
            java.util.Optional<PayLaterTransaction> existing = transactionPersistencePort.findByExternalId(externalId);
            if (existing.isPresent()) {
                log.info("Replay detected for PayLater purchase, returning existing: {}", existing.get().getId());
                return existing.get();
            }
        }

        // PAYLATER-001: pessimistic write lock serializes concurrent purchases
        // so read-modify-write of usedCredit cannot lose updates.
        PayLater payLater = payLaterPersistencePort.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new IllegalArgumentException("PayLater account not found for user: " + userId));

        if (payLater.getStatus() != PayLaterStatus.ACTIVE) {
            throw new IllegalStateException("PayLater account is not active");
        }

        BigDecimal newUsedCredit = payLater.getUsedCredit().add(amount);

        if (newUsedCredit.compareTo(payLater.getCreditLimit()) > 0) {
            throw new IllegalStateException("Insufficient PayLater credit limit");
        }

        payLater.setUsedCredit(newUsedCredit);
        payLater.setAvailableCredit(payLater.getCreditLimit().subtract(newUsedCredit));
        payLater.setUpdatedAt(LocalDateTime.now());

        payLaterPersistencePort.save(payLater);

        PayLaterTransaction transaction = new PayLaterTransaction();
        transaction.setExternalId(externalId != null && !externalId.isBlank() ? externalId : generateExternalId());
        transaction.setPayLaterAccountId(payLater.getId());
        transaction.setType(TransactionType.PURCHASE);
        transaction.setAmount(amount);
        transaction.setMerchantName(merchantName);
        transaction.setDescription(description);
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setUpdatedAt(LocalDateTime.now());

        PayLaterTransaction savedTransaction = transactionPersistencePort.save(transaction);

        // PAYLATER-001: money movement — disbursement of the purchase amount into
        // the user's wallet. Failure rolls the whole transaction back so the
        // paylater record and usedCredit never exist without the wallet credit.
        walletPaymentPort.creditAccount(
                payLater.getUserId().toString(),
                amount,
                "IDR",
                savedTransaction.getExternalId(),
                "PayLater purchase disbursement");

        log.info("Recorded PayLater purchase transaction: {} for user: {}", savedTransaction.getId(), userId);
        return savedTransaction;
    }

    @Override
    @Transactional
    public PayLaterTransaction recordPayment(UUID userId, BigDecimal amount) {
        return recordPayment(userId, amount, null);
    }

    @Override
    @Transactional
    public PayLaterTransaction recordPayment(UUID userId, BigDecimal amount, String externalId) {
        validateAmount(amount);
        log.info("Recording PayLater payment for user: {} with amount: {}", userId, amount);

        if (externalId != null && !externalId.isBlank()) {
            java.util.Optional<PayLaterTransaction> existing = transactionPersistencePort.findByExternalId(externalId);
            if (existing.isPresent()) {
                log.info("Replay detected for PayLater payment, returning existing: {}", existing.get().getId());
                return existing.get();
            }
        }

        PayLater payLater = payLaterPersistencePort.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new IllegalArgumentException("PayLater account not found for user: " + userId));

        BigDecimal newUsedCredit = payLater.getUsedCredit().subtract(amount);

        if (newUsedCredit.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Payment amount exceeds used credit");
        }

        payLater.setUsedCredit(newUsedCredit);
        payLater.setAvailableCredit(payLater.getCreditLimit().subtract(newUsedCredit));
        payLater.setUpdatedAt(LocalDateTime.now());

        payLaterPersistencePort.save(payLater);

        PayLaterTransaction transaction = new PayLaterTransaction();
        transaction.setExternalId(externalId != null && !externalId.isBlank() ? externalId : generateExternalId());
        transaction.setPayLaterAccountId(payLater.getId());
        transaction.setType(TransactionType.PAYMENT);
        transaction.setAmount(amount);
        transaction.setDescription("PayLater payment");
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setUpdatedAt(LocalDateTime.now());

        PayLaterTransaction savedTransaction = transactionPersistencePort.save(transaction);

        // PAYLATER-001: money movement — repayment debits the wallet. Failure
        // rolls back the paylater record and usedCredit.
        walletPaymentPort.collectRepayment(
                payLater.getId(), userId, amount, "IDR",
                savedTransaction.getExternalId(), "PayLater payment");

        log.info("Recorded PayLater payment transaction: {} for user: {}", savedTransaction.getId(), userId);
        return savedTransaction;
    }

    @Override
    public List<PayLaterTransaction> getTransactionHistory(UUID userId) {
        log.info("Fetching transaction history for user: {}", userId);

        PayLater payLater = payLaterPersistencePort.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("PayLater account not found for user: " + userId));

        return transactionPersistencePort.findByPayLaterAccountIdOrderByTransactionDateDesc(payLater.getId());
    }

    private String generateExternalId() {
        return "PYLT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0 || amount.scale() > 4) {
            throw new IllegalArgumentException("PayLater amount must be positive with at most 4 decimal places");
        }
    }
}
