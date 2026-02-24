package id.payu.billing.application.service;

import id.payu.billing.domain.model.BillPayment;
import id.payu.billing.domain.model.BillerType;
import id.payu.billing.domain.port.in.PayBillUseCase;
import id.payu.billing.domain.port.in.PaymentQueryUseCase;
import id.payu.billing.domain.port.in.TopUpUseCase;
import id.payu.billing.domain.port.out.BillPaymentPersistencePort;
import id.payu.billing.domain.port.out.PaymentEventPort;
import id.payu.billing.domain.port.out.WalletPort;
import id.payu.billing.dto.CreatePaymentRequest;
import id.payu.billing.dto.TopUpRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for processing bill payments.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService implements PayBillUseCase, TopUpUseCase, PaymentQueryUseCase {

    private final BillPaymentPersistencePort persistencePort;
    private final WalletPort walletPort;
    private final PaymentEventPort eventPort;

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

        payment = persistencePort.save(payment);
        log.info("Payment created: id={}, reference={}", payment.getId(), payment.getReferenceNumber());

        // Reserve balance from wallet
        try {
            WalletPort.ReserveResult reserveResult = walletPort.reserveBalance(
                    request.accountId(), payment.getTotalAmount(), payment.getReferenceNumber()
            );

            if ("RESERVED".equals(reserveResult.status())) {
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

        payment = persistencePort.save(payment);

        // Publish event
        eventPort.publishPaymentEvent(payment);

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

        payment = persistencePort.save(payment);
        log.info("Top-up created: id={}, reference={}", payment.getId(), payment.getReferenceNumber());

        // Reserve balance from wallet
        try {
            WalletPort.ReserveResult reserveResult = walletPort.reserveBalance(
                    request.accountId(), payment.getTotalAmount(), payment.getReferenceNumber()
            );

            if ("RESERVED".equals(reserveResult.status())) {
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

        payment = persistencePort.save(payment);

        // Publish event
        eventPort.publishPaymentEvent(payment);

        return payment;
    }

    public Optional<BillPayment> getPayment(UUID id) {
        return persistencePort.findById(id);
    }

    public Optional<BillPayment> getPaymentByReference(String referenceNumber) {
        return persistencePort.findByReferenceNumber(referenceNumber);
    }

    private void processWithBiller(BillPayment payment) {
        payment.setStatus(BillPayment.PaymentStatus.COMPLETED);
        payment.setCompletedAt(LocalDateTime.now());
        payment.setBillerTransactionId("BILLER-" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
        log.info("Payment completed: id={}", payment.getId());
    }

    private void processWithEwalletProvider(BillPayment payment) {
        payment.setStatus(BillPayment.PaymentStatus.COMPLETED);
        payment.setCompletedAt(LocalDateTime.now());
        payment.setBillerTransactionId("EWALLET-" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
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

}
