package id.payu.billing.application.service;

import id.payu.billing.domain.model.BillPayment;
import id.payu.billing.domain.model.BillerType;
import id.payu.billing.domain.port.in.PayBillUseCase;
import id.payu.billing.domain.port.in.PaymentQueryUseCase;
import id.payu.billing.domain.port.in.TopUpUseCase;
import id.payu.billing.domain.port.out.BillPaymentPersistencePort;
import id.payu.billing.domain.port.out.BillerPort;
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
    private final BillerPort billerPort;
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
        String reservationId = null;
        try {
            WalletPort.ReserveResult reserveResult = walletPort.reserveBalance(
                    request.accountId(), payment.getTotalAmount(), payment.getReferenceNumber()
            );

            if ("RESERVED".equals(reserveResult.status())) {
                reservationId = reserveResult.reservationId();
                payment.setStatus(BillPayment.PaymentStatus.PROCESSING);

                // Process with biller via BillerPort (calls biller-simulator in dev)
                BillerPort.PaymentResult billerResult = billerPort.pay(
                        request.billerCode(), request.customerId(),
                        request.amount(), payment.getReferenceNumber()
                );

                if (billerResult.isSuccess()) {
                    payment.setStatus(BillPayment.PaymentStatus.COMPLETED);
                    payment.setCompletedAt(LocalDateTime.now());
                    payment.setBillerTransactionId(billerResult.billerTransactionId());
                    // Commit the reservation after successful biller processing
                    walletPort.commitReservation(reservationId);
                } else if (billerResult.isDuplicate()) {
                    // Idempotent: payment was already processed
                    payment.setStatus(BillPayment.PaymentStatus.COMPLETED);
                    payment.setCompletedAt(LocalDateTime.now());
                    payment.setBillerTransactionId(billerResult.billerTransactionId());
                    walletPort.commitReservation(reservationId);
                    log.warn("Duplicate biller reference detected for payment {}", payment.getId());
                } else {
                    payment.setStatus(BillPayment.PaymentStatus.FAILED);
                    payment.setFailureReason("Biller rejected: " + billerResult.responseMessage());
                    walletPort.releaseReservation(reservationId);
                    reservationId = null; // Already released
                }
            } else {
                payment.setStatus(BillPayment.PaymentStatus.FAILED);
                payment.setFailureReason("Failed to reserve balance");
            }
        } catch (Exception e) {
            log.error("Payment processing failed: {}", e.getMessage());
            payment.setStatus(BillPayment.PaymentStatus.FAILED);
            payment.setFailureReason("Payment processing failed: " + e.getMessage());
            // Release the reservation if it was acquired
            if (reservationId != null) {
                try {
                    walletPort.releaseReservation(reservationId);
                    log.info("Released reservation {} after payment failure", reservationId);
                } catch (Exception releaseEx) {
                    log.error("CRITICAL: Failed to release reservation {} — manual intervention required", reservationId, releaseEx);
                }
            }
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
        String reservationId = null;
        try {
            WalletPort.ReserveResult reserveResult = walletPort.reserveBalance(
                    request.accountId(), payment.getTotalAmount(), payment.getReferenceNumber()
            );

            if ("RESERVED".equals(reserveResult.status())) {
                reservationId = reserveResult.reservationId();
                payment.setStatus(BillPayment.PaymentStatus.PROCESSING);

                // Process with e-wallet provider via BillerPort (same simulator handles e-wallets)
                BillerPort.PaymentResult providerResult = billerPort.pay(
                        request.provider(), request.walletNumber(),
                        request.amount(), payment.getReferenceNumber()
                );

                if (providerResult.isSuccess() || providerResult.isDuplicate()) {
                    payment.setStatus(BillPayment.PaymentStatus.COMPLETED);
                    payment.setCompletedAt(LocalDateTime.now());
                    payment.setBillerTransactionId(providerResult.billerTransactionId());
                    // Commit the reservation after successful provider processing
                    walletPort.commitReservation(reservationId);
                } else {
                    payment.setStatus(BillPayment.PaymentStatus.FAILED);
                    payment.setFailureReason("Provider rejected: " + providerResult.responseMessage());
                    walletPort.releaseReservation(reservationId);
                    reservationId = null;
                }
            } else {
                payment.setStatus(BillPayment.PaymentStatus.FAILED);
                payment.setFailureReason("Failed to reserve balance");
            }
        } catch (Exception e) {
            log.error("Top-up processing failed: {}", e.getMessage());
            payment.setStatus(BillPayment.PaymentStatus.FAILED);
            payment.setFailureReason("Top-up processing failed: " + e.getMessage());
            // Release the reservation if it was acquired
            if (reservationId != null) {
                try {
                    walletPort.releaseReservation(reservationId);
                    log.info("Released reservation {} after top-up failure", reservationId);
                } catch (Exception releaseEx) {
                    log.error("CRITICAL: Failed to release reservation {} — manual intervention required", reservationId, releaseEx);
                }
            }
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
