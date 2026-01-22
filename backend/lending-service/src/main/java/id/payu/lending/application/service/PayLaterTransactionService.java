package id.payu.lending.application.service;

import id.payu.lending.domain.model.PayLater;
import id.payu.lending.domain.model.PayLaterTransaction;
import id.payu.lending.domain.port.in.PayLaterTransactionUseCase;
import id.payu.lending.domain.port.out.PayLaterPersistencePort;
import id.payu.lending.domain.port.out.PayLaterTransactionPersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PayLaterTransactionService implements PayLaterTransactionUseCase {

    private final PayLaterPersistencePort payLaterPersistencePort;
    private final PayLaterTransactionPersistencePort transactionPersistencePort;

    @Override
    @Transactional
    public PayLaterTransaction recordPurchase(UUID userId, String merchantName, BigDecimal amount, String description) {
        log.info("Recording PayLater purchase for user: {} at merchant: {}", userId, merchantName);

        PayLater payLater = payLaterPersistencePort.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("PayLater account not found for user: " + userId));

        if (payLater.getStatus() != PayLater.PayLaterStatus.ACTIVE) {
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
        transaction.setExternalId(generateExternalId());
        transaction.setPayLaterAccountId(payLater.getId());
        transaction.setType(PayLaterTransaction.TransactionType.PURCHASE);
        transaction.setAmount(amount);
        transaction.setMerchantName(merchantName);
        transaction.setDescription(description);
        transaction.setStatus(PayLaterTransaction.TransactionStatus.COMPLETED);
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setUpdatedAt(LocalDateTime.now());

        PayLaterTransaction savedTransaction = transactionPersistencePort.save(transaction);

        log.info("Recorded PayLater purchase transaction: {} for user: {}", savedTransaction.getId(), userId);
        return savedTransaction;
    }

    @Override
    @Transactional
    public PayLaterTransaction recordPayment(UUID userId, BigDecimal amount) {
        log.info("Recording PayLater payment for user: {} with amount: {}", userId, amount);

        PayLater payLater = payLaterPersistencePort.findByUserId(userId)
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
        transaction.setExternalId(generateExternalId());
        transaction.setPayLaterAccountId(payLater.getId());
        transaction.setType(PayLaterTransaction.TransactionType.PAYMENT);
        transaction.setAmount(amount);
        transaction.setDescription("PayLater payment");
        transaction.setStatus(PayLaterTransaction.TransactionStatus.COMPLETED);
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setUpdatedAt(LocalDateTime.now());

        PayLaterTransaction savedTransaction = transactionPersistencePort.save(transaction);

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
}
