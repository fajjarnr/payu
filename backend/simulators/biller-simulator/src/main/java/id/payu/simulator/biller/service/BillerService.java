package id.payu.simulator.biller.service;

import id.payu.simulator.biller.config.SimulatorConfig;
import id.payu.simulator.biller.dto.*;
import id.payu.simulator.biller.entity.BillerAccount;
import id.payu.simulator.biller.entity.BillerTransaction;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Random;
import java.util.UUID;
import id.payu.simulator.biller.entity.TransactionStatus;

/**
 * Core service for biller simulation — inquiry, payment, status check.
 * Simulates realistic biller behavior with configurable latency & failure rates.
 */
@ApplicationScoped
public class BillerService {

    private final Random random = new Random();

    @Inject
    SimulatorConfig config;

    /**
     * Inquiry — look up customer's outstanding bill at a biller.
     */
    public InquiryResponse inquiry(InquiryRequest request) {
        Log.infof("Biller inquiry: code=%s, customer=%s", request.billerCode(), request.customerNumber());

        simulateLatency();

        if (shouldSimulateFailure()) {
            Log.warn("Simulating random biller failure for inquiry");
            return InquiryResponse.error("Biller system temporarily unavailable");
        }

        BillerAccount account = BillerAccount.findByBillerAndCustomer(
                request.billerCode(), request.customerNumber());

        if (account == null) {
            return InquiryResponse.notFound(request.billerCode(), request.customerNumber());
        }

        return switch (account.status) {
            case ACTIVE -> InquiryResponse.success(account.billerCode, account.customerNumber,
                    account.customerName, account.outstandingAmount);
            case BLOCKED -> InquiryResponse.blocked(account.billerCode, account.customerNumber);
            case NOT_FOUND -> InquiryResponse.notFound(account.billerCode, account.customerNumber);
        };
    }

    /**
     * Payment — process a bill payment. Validates customer, checks amount, 
     * deducts from outstanding, and returns a biller transaction ID.
     */
    @Transactional
    public PaymentResponse pay(PaymentRequest request) {
        Log.infof("Biller payment: code=%s, customer=%s, amount=%s, ref=%s",
                request.billerCode(), request.customerNumber(),
                request.amount(), request.referenceNumber());

        simulateLatency();

        // Check for duplicate reference (idempotency)
        BillerTransaction existing = BillerTransaction.findByReference(request.referenceNumber());
        if (existing != null) {
            Log.infof("Duplicate reference detected: %s → returning existing txId=%s",
                    request.referenceNumber(), existing.billerTransactionId);
            return PaymentResponse.duplicate(existing.billerTransactionId);
        }

        if (shouldSimulateFailure()) {
            Log.warn("Simulating random biller failure for payment");
            return PaymentResponse.error("Biller system temporarily unavailable");
        }

        // Find biller account
        BillerAccount account = BillerAccount.findByBillerAndCustomer(
                request.billerCode(), request.customerNumber());
        if (account == null) {
            return PaymentResponse.customerNotFound(request.billerCode(), request.customerNumber());
        }

        // For billers with outstanding amounts (PLN, PDAM), validate amount
        if (account.outstandingAmount != null && request.amount().compareTo(account.outstandingAmount) > 0) {
            return PaymentResponse.insufficientBill(request.billerCode(), request.customerNumber());
        }

        // Process payment
        String billerTxId = "BILLER-" + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 16).toUpperCase();

        // Deduct outstanding amount (for utility bills)
        if (account.outstandingAmount != null) {
            account.outstandingAmount = account.outstandingAmount.subtract(request.amount());
            if (account.outstandingAmount.compareTo(BigDecimal.ZERO) < 0) {
                account.outstandingAmount = BigDecimal.ZERO;
            }
            account.persist();
        }

        // Record transaction
        BillerTransaction tx = new BillerTransaction();
        tx.billerCode = request.billerCode();
        tx.customerNumber = request.customerNumber();
        tx.amount = request.amount();
        tx.referenceNumber = request.referenceNumber();
        tx.billerTransactionId = billerTxId;
        tx.status = TransactionStatus.COMPLETED;
        tx.createdAt = Instant.now();
        tx.completedAt = Instant.now();
        tx.persist();

        Log.infof("Biller payment completed: ref=%s, billerTxId=%s",
                request.referenceNumber(), billerTxId);

        return PaymentResponse.success(billerTxId, request.billerCode(),
                request.customerNumber(), request.amount());
    }

    /**
     * Status check — look up a payment by reference number.
     */
    public PaymentResponse status(String referenceNumber) {
        Log.infof("Biller status check: ref=%s", referenceNumber);

        simulateLatency();

        BillerTransaction tx = BillerTransaction.findByReference(referenceNumber);
        if (tx == null) {
            return PaymentResponse.error("Transaction not found");
        }

        return new PaymentResponse(
                "00", "SUCCESS",
                tx.billerTransactionId, tx.billerCode, tx.customerNumber,
                tx.amount, tx.status.name(), tx.completedAt
        );
    }

    private void simulateLatency() {
        try {
            int latency = config.latency().min() +
                    random.nextInt(config.latency().max() - config.latency().min());
            Thread.sleep(latency);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean shouldSimulateFailure() {
        return random.nextInt(100) < config.failureRate();
    }
}
