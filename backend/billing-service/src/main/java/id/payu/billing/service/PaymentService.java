package id.payu.billing.service;

import id.payu.billing.client.WalletClient;
import id.payu.billing.domain.BillPayment;
import id.payu.billing.domain.BillerType;
import id.payu.billing.dto.CreatePaymentRequest;
import id.payu.billing.dto.TopUpRequest;
import id.payu.billing.repository.BillPaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for processing bill payments.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final BillPaymentRepository billPaymentRepository;
    private final WalletClient walletClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public BillPayment createPayment(CreatePaymentRequest request) {
        log.info("Creating payment: biller={}, customerId={}, amount={}",
                request.billerCode(), request.customerId(), request.amount());

        // Validate biller
        BillerType billerType = getBillerType(request.billerCode())
                .orElseThrow(() -> new IllegalArgumentException("Unknown biller: " + request.billerCode()));

        // Calculate admin fee
        BigDecimal adminFee = calculateAdminFee(billerType);

        // Create payment record
        BillPayment payment = new BillPayment();
        payment.setAccountId(request.accountId());
        payment.setBillerType(billerType);
        payment.setCustomerId(request.customerId());
        payment.setAmount(request.amount());
        payment.setAdminFee(adminFee);
        payment.setTotalAmount(request.amount().add(adminFee));
        payment.setStatus(BillPayment.PaymentStatus.PENDING);

        payment = billPaymentRepository.save(payment);
        log.info("Payment created: id={}, reference={}", payment.getId(), payment.getReferenceNumber());

        // Reserve balance from wallet
        try {
            WalletClient.ReserveResponse reserveResponse = walletClient.reserveBalance(
                    request.accountId(),
                    new WalletClient.ReserveRequest(payment.getTotalAmount(), payment.getReferenceNumber())
            );

            if ("RESERVED".equals(reserveResponse.status())) {
                payment.setStatus(BillPayment.PaymentStatus.PROCESSING);
                // Simulate biller processing (in production, call actual biller API)
                processWithBiller(payment);
            } else {
                payment.setStatus(BillPayment.PaymentStatus.FAILED);
                payment.setFailureReason("Failed to reserve balance");
            }
        } catch (Exception e) {
            log.error("Failed to reserve balance: {}", e.getMessage());
            payment.setStatus(BillPayment.PaymentStatus.FAILED);
            payment.setFailureReason("Wallet service unavailable");
        }

        payment = billPaymentRepository.save(payment);

        // Publish event
        publishPaymentEvent(payment);

        return payment;
    }

    @Transactional
    public BillPayment createTopUp(TopUpRequest request) {
        log.info("Creating top-up: provider={}, walletNumber={}, amount={}",
                request.provider(), request.walletNumber(), request.amount());

        // Validate e-wallet provider
        BillerType billerType = getBillerType(request.provider())
                .orElseThrow(() -> new IllegalArgumentException("Unknown e-wallet provider: " + request.provider()));

        // Calculate admin fee (lower for e-wallet top-ups)
        BigDecimal adminFee = calculateTopUpFee(request.amount());

        // Create payment record
        BillPayment payment = new BillPayment();
        payment.setAccountId(request.accountId());
        payment.setBillerType(billerType);
        payment.setCustomerId(request.walletNumber());
        payment.setAmount(request.amount());
        payment.setAdminFee(adminFee);
        payment.setTotalAmount(request.amount().add(adminFee));
        payment.setStatus(BillPayment.PaymentStatus.PENDING);

        payment = billPaymentRepository.save(payment);
        log.info("Top-up created: id={}, reference={}", payment.getId(), payment.getReferenceNumber());

        // Reserve balance from wallet
        try {
            WalletClient.ReserveResponse reserveResponse = walletClient.reserveBalance(
                    request.accountId(),
                    new WalletClient.ReserveRequest(payment.getTotalAmount(), payment.getReferenceNumber())
            );

            if ("RESERVED".equals(reserveResponse.status())) {
                payment.setStatus(BillPayment.PaymentStatus.PROCESSING);
                // Simulate e-wallet provider processing (in production, call actual e-wallet API)
                processWithEwalletProvider(payment);
            } else {
                payment.setStatus(BillPayment.PaymentStatus.FAILED);
                payment.setFailureReason("Failed to reserve balance");
            }
        } catch (Exception e) {
            log.error("Failed to reserve balance: {}", e.getMessage());
            payment.setStatus(BillPayment.PaymentStatus.FAILED);
            payment.setFailureReason("Wallet service unavailable");
        }

        payment = billPaymentRepository.save(payment);

        // Publish event
        publishPaymentEvent(payment);

        return payment;
    }

    public Optional<BillPayment> getPayment(UUID id) {
        return billPaymentRepository.findById(id);
    }

    public Optional<BillPayment> getPaymentByReference(String referenceNumber) {
        return billPaymentRepository.findByReferenceNumber(referenceNumber);
    }

    private void processWithBiller(BillPayment payment) {
        payment.setStatus(BillPayment.PaymentStatus.COMPLETED);
        payment.setCompletedAt(LocalDateTime.now());
        payment.setBillerTransactionId("BILLER-" + System.currentTimeMillis());
        log.info("Payment completed: id={}", payment.getId());
    }

    private void processWithEwalletProvider(BillPayment payment) {
        payment.setStatus(BillPayment.PaymentStatus.COMPLETED);
        payment.setCompletedAt(LocalDateTime.now());
        payment.setBillerTransactionId("EWALLET-" + System.currentTimeMillis());
        log.info("E-wallet top-up completed: id={}", payment.getId());
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
                    "paymentId", payment.getId().toString(),
                    "referenceNumber", payment.getReferenceNumber(),
                    "accountId", payment.getAccountId(),
                    "billerCode", payment.getBillerType().getCode(),
                    "amount", payment.getTotalAmount(),
                    "status", payment.getStatus().name(),
                    "timestamp", LocalDateTime.now().toString()
            );
            kafkaTemplate.send("payment-events", payment.getAccountId(), event);
            log.debug("Published payment event: {}", event);
        } catch (Exception e) {
            log.warn("Failed to publish payment event: {}", e.getMessage());
        }
    }
}
