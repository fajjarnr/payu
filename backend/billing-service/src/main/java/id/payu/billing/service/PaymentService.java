package id.payu.billing.service;

import id.payu.billing.client.WalletClient;
import id.payu.billing.domain.BillPayment;
import id.payu.billing.domain.BillerType;
import id.payu.billing.dto.CreatePaymentRequest;
import id.payu.billing.dto.TopUpRequest;
import io.smallrye.reactive.messaging.kafka.KafkaRecord;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for processing bill payments.
 */
@ApplicationScoped
public class PaymentService {

    private static final Logger LOG = Logger.getLogger(PaymentService.class);

    @Inject
    @RestClient
    WalletClient walletClient;

    @Inject
    @Channel("payment-events")
    Emitter<Map<String, Object>> paymentEvents;

    @Transactional
    public BillPayment createPayment(CreatePaymentRequest request) {
        LOG.infof("Creating payment: biller=%s, customerId=%s, amount=%s",
            request.billerCode(), request.customerId(), request.amount());

        // Validate biller
        BillerType billerType = getBillerType(request.billerCode())
            .orElseThrow(() -> new IllegalArgumentException("Unknown biller: " + request.billerCode()));

        // Calculate admin fee
        BigDecimal adminFee = calculateAdminFee(billerType);

        // Create payment record
        BillPayment payment = new BillPayment();
        payment.accountId = request.accountId();
        payment.billerType = billerType;
        payment.customerId = request.customerId();
        payment.amount = request.amount();
        payment.adminFee = adminFee;
        payment.totalAmount = request.amount().add(adminFee);
        payment.status = BillPayment.PaymentStatus.PENDING;

        payment.persist();
        LOG.infof("Payment created: id=%s, reference=%s", payment.id, payment.referenceNumber);

        // Reserve balance from wallet
        try {
            var reserveResponse = walletClient.reserveBalance(
                request.accountId(),
                new WalletClient.ReserveRequest(payment.totalAmount, payment.referenceNumber)
            );

            if ("RESERVED".equals(reserveResponse.status())) {
                payment.status = BillPayment.PaymentStatus.PROCESSING;
                // Simulate biller processing (in production, call actual biller API)
                processWithBiller(payment);
            } else {
                payment.status = BillPayment.PaymentStatus.FAILED;
                payment.failureReason = "Failed to reserve balance";
            }
        } catch (Exception e) {
            LOG.errorf("Failed to reserve balance: %s", e.getMessage());
            payment.status = BillPayment.PaymentStatus.FAILED;
            payment.failureReason = "Wallet service unavailable";
        }

        payment.persist();

        // Publish event
        publishPaymentEvent(payment);

        return payment;
    }

    @Transactional
    public BillPayment createTopUp(TopUpRequest request) {
        LOG.infof("Creating top-up: provider=%s, walletNumber=%s, amount=%s",
            request.provider(), request.walletNumber(), request.amount());

        // Validate e-wallet provider
        BillerType billerType = getBillerType(request.provider())
            .orElseThrow(() -> new IllegalArgumentException("Unknown e-wallet provider: " + request.provider()));

        // Calculate admin fee (lower for e-wallet top-ups)
        BigDecimal adminFee = calculateTopUpFee(request.amount());

        // Create payment record
        BillPayment payment = new BillPayment();
        payment.accountId = request.accountId();
        payment.billerType = billerType;
        payment.customerId = request.walletNumber();
        payment.amount = request.amount();
        payment.adminFee = adminFee;
        payment.totalAmount = request.amount().add(adminFee);
        payment.status = BillPayment.PaymentStatus.PENDING;

        payment.persist();
        LOG.infof("Top-up created: id=%s, reference=%s", payment.id, payment.referenceNumber);

        // Reserve balance from wallet
        try {
            var reserveResponse = walletClient.reserveBalance(
                request.accountId(),
                new WalletClient.ReserveRequest(payment.totalAmount, payment.referenceNumber)
            );

            if ("RESERVED".equals(reserveResponse.status())) {
                payment.status = BillPayment.PaymentStatus.PROCESSING;
                // Simulate e-wallet provider processing (in production, call actual e-wallet API)
                processWithEwalletProvider(payment);
            } else {
                payment.status = BillPayment.PaymentStatus.FAILED;
                payment.failureReason = "Failed to reserve balance";
            }
        } catch (Exception e) {
            LOG.errorf("Failed to reserve balance: %s", e.getMessage());
            payment.status = BillPayment.PaymentStatus.FAILED;
            payment.failureReason = "Wallet service unavailable";
        }

        payment.persist();

        // Publish event
        publishPaymentEvent(payment);

        return payment;
    }

    public Optional<BillPayment> getPayment(UUID id) {
        return BillPayment.findByIdOptional(id);
    }

    private void processWithBiller(BillPayment payment) {
        payment.status = BillPayment.PaymentStatus.COMPLETED;
        payment.completedAt = LocalDateTime.now();
        payment.billerTransactionId = "BILLER-" + System.currentTimeMillis();
        LOG.infof("Payment completed: id=%s", payment.id);
    }

    private void processWithEwalletProvider(BillPayment payment) {
        payment.status = BillPayment.PaymentStatus.COMPLETED;
        payment.completedAt = LocalDateTime.now();
        payment.billerTransactionId = "EWALLET-" + System.currentTimeMillis();
        LOG.infof("E-wallet top-up completed: id=%s", payment.id);
    }

    private Optional<BillerType> getBillerType(String code) {
        for (BillerType type : BillerType.values()) {
            if (type.getCode().equalsIgnoreCase(code)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }

    private BigDecimal calculateAdminFee(BillerType type) {
        return switch (type.getCategory()) {
            case "electricity" -> new BigDecimal("2500");
            case "water" -> new BigDecimal("2000");
            case "mobile" -> BigDecimal.ZERO;
            case "internet" -> new BigDecimal("2500");
            case "insurance" -> new BigDecimal("2500");
            case "utility" -> new BigDecimal("2500");
            default -> new BigDecimal("2500");
        };
    }

    private BigDecimal calculateTopUpFee(BigDecimal amount) {
        if (amount.compareTo(new BigDecimal("100000")) <= 0) {
            return new BigDecimal("1000");
        } else if (amount.compareTo(new BigDecimal("500000")) <= 0) {
            return new BigDecimal("1500");
        } else {
            return new BigDecimal("2000");
        }
    }

    private void publishPaymentEvent(BillPayment payment) {
        try {
            Map<String, Object> event = Map.of(
                "paymentId", payment.id.toString(),
                "referenceNumber", payment.referenceNumber,
                "accountId", payment.accountId,
                "billerCode", payment.billerType.getCode(),
                "amount", payment.totalAmount,
                "status", payment.status.name(),
                "timestamp", LocalDateTime.now().toString()
            );
            paymentEvents.send(KafkaRecord.of(payment.accountId, event));
            LOG.debugf("Published payment event: %s", event);
        } catch (Exception e) {
            LOG.warnf("Failed to publish payment event: %s", e.getMessage());
        }
    }
}
