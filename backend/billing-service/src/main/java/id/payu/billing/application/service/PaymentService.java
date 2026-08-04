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
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import id.payu.billing.domain.model.PaymentStatus;

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

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public BillPayment createPayment(CreatePaymentRequest request) {
        return createPayment(request, UUID.randomUUID().toString());
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public BillPayment createPayment(CreatePaymentRequest request, String idempotencyKey) {
        BillerType billerType = getBillerType(request.billerCode())
                .orElseThrow(() -> new IllegalArgumentException("Unknown biller: " + request.billerCode()));
        return startOrResume(request.accountId(), billerType, request.customerId(), request.amount(),
                calculateAdminFee(billerType), idempotencyKey);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public BillPayment createTopUp(TopUpRequest request) {
        return createTopUp(request, UUID.randomUUID().toString());
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public BillPayment createTopUp(TopUpRequest request, String idempotencyKey) {
        BillerType billerType = getBillerType(request.provider())
                .orElseThrow(() -> new IllegalArgumentException("Unknown e-wallet provider: " + request.provider()));
        return startOrResume(request.accountId(), billerType, request.walletNumber(), request.amount(),
                calculateTopUpFee(request.amount()), idempotencyKey);
    }

    private BillPayment startOrResume(String accountId, BillerType billerType, String customerId,
                                      BigDecimal amount, BigDecimal adminFee, String idempotencyKey) {
        BillPayment payment = persistencePort.findByIdempotencyKey(idempotencyKey)
                .orElseGet(() -> createCheckpoint(accountId, billerType, customerId, amount, adminFee, idempotencyKey));
        validateReplay(payment, accountId, billerType, customerId, amount);
        return reconcilePayment(payment);
    }

    private BillPayment createCheckpoint(String accountId, BillerType billerType, String customerId,
                                         BigDecimal amount, BigDecimal adminFee, String idempotencyKey) {
        BillPayment payment = new BillPayment();
        payment.setAccountId(accountId);
        payment.setReferenceNumber("BILL-" + idempotencyKey.replace("-", ""));
        payment.setIdempotencyKey(idempotencyKey);
        payment.setBillerType(billerType);
        payment.setCustomerId(customerId);
        payment.setAmount(amount);
        payment.setAdminFee(adminFee);
        payment.setTotalAmount(amount.add(adminFee));
        payment.setStatus(PaymentStatus.PENDING);
        return persistencePort.save(payment);
    }

    private void validateReplay(BillPayment payment, String accountId, BillerType billerType,
                                String customerId, BigDecimal amount) {
        if (!Objects.equals(payment.getAccountId(), accountId)
                || payment.getBillerType() != billerType
                || !Objects.equals(payment.getCustomerId(), customerId)
                || payment.getAmount().compareTo(amount) != 0) {
            throw new IllegalArgumentException("Idempotency key was already used for a different payment");
        }
    }

    private BillPayment reconcilePayment(BillPayment payment) {
        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            return publishEventIfNeeded(payment);
        }
        if (payment.getStatus() == PaymentStatus.FAILED) {
            return publishEventIfNeeded(payment);
        }

        try {
            if (payment.getWalletReservationId() == null) {
                WalletPort.ReserveResult reserveResult = walletPort.reserveBalance(
                        payment.getAccountId(), payment.getTotalAmount(), payment.getReferenceNumber());
                if (!"RESERVED".equals(reserveResult.status())) {
                    payment.setStatus(PaymentStatus.FAILED);
                    payment.setFailureReason("Failed to reserve balance");
                    return publishEventIfNeeded(persistencePort.save(payment));
                }
                payment.setWalletReservationId(reserveResult.reservationId());
                payment.setStatus(PaymentStatus.PROCESSING);
                payment = persistencePort.save(payment);
            }

            if (payment.getBillerTransactionId() == null) {
                BillerPort.PaymentResult result = billerPort.pay(
                        payment.getBillerType().getCode(), payment.getCustomerId(),
                        payment.getAmount(), payment.getReferenceNumber());
                if (result.isSuccess() || result.isDuplicate()) {
                    payment.setBillerTransactionId(result.billerTransactionId() != null
                            ? result.billerTransactionId() : payment.getReferenceNumber());
                    payment.setCompletedAt(LocalDateTime.now());
                    payment = persistencePort.save(payment);
                } else if ("96".equals(result.responseCode())) {
                    payment.setStatus(PaymentStatus.PROCESSING);
                    payment.setFailureReason("Reconciliation required: " + result.responseMessage());
                    return persistencePort.save(payment);
                } else {
                    payment.setStatus(PaymentStatus.FAILED);
                    payment.setFailureReason("Biller rejected: " + result.responseMessage());
                    walletPort.releaseReservation(payment.getWalletReservationId());
                    return publishEventIfNeeded(persistencePort.save(payment));
                }
            }

            walletPort.commitReservation(payment.getWalletReservationId());
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setFailureReason(null);
            return publishEventIfNeeded(persistencePort.save(payment));
        } catch (Exception e) {
            log.warn("Payment checkpoint requires reconciliation: id={}, error={}", payment.getId(), e.getMessage());
            payment.setStatus(PaymentStatus.PROCESSING);
            payment.setFailureReason("Reconciliation required: " + e.getMessage());
            return persistencePort.save(payment);
        }
    }

    @Scheduled(fixedDelayString = "${payu.billing.payment.reconcile-interval-ms:60000}")
    @SchedulerLock(name = "PaymentService_reconcile", lockAtMostFor = "PT30S")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void reconcilePayments() {
        persistencePort.findByStatusIn(List.of(PaymentStatus.PENDING, PaymentStatus.PROCESSING,
                        PaymentStatus.COMPLETED, PaymentStatus.FAILED))
                .forEach(this::reconcilePayment);
    }

    private BillPayment publishEventIfNeeded(BillPayment payment) {
        if (payment.isEventPublished()) {
            return payment;
        }
        try {
            eventPort.publishPaymentEvent(payment);
            payment.setEventPublished(true);
        } catch (Exception e) {
            log.warn("Payment event remains pending: id={}, error={}", payment.getId(), e.getMessage());
        }
        return persistencePort.save(payment);
    }

    @CircuitBreaker(name = "billing", fallbackMethod = "getPaymentFallback")
    @Retry(name = "billing")
    public Optional<BillPayment> getPayment(UUID id) {
        return persistencePort.findById(id);
    }

    @CircuitBreaker(name = "billing", fallbackMethod = "getPaymentByReferenceFallback")
    @Retry(name = "billing")
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

    private BillPayment createPaymentFallback(CreatePaymentRequest request, Exception ex) {
        if (ex instanceof DataIntegrityViolationException
                || ex instanceof IllegalArgumentException
                || ex instanceof ConstraintViolationException
                || ex instanceof HttpMessageNotReadableException
                || ex instanceof AccessDeniedException) {
            throw (RuntimeException) ex;
        }
        log.error("Fallback for createPayment: {}", ex.getMessage());
        throw new RuntimeException("Billing service temporarily unavailable", ex);
    }

    private BillPayment createTopUpFallback(TopUpRequest request, Exception ex) {
        if (ex instanceof DataIntegrityViolationException
                || ex instanceof IllegalArgumentException
                || ex instanceof ConstraintViolationException
                || ex instanceof HttpMessageNotReadableException
                || ex instanceof AccessDeniedException) {
            throw (RuntimeException) ex;
        }
        log.error("Fallback for createTopUp: {}", ex.getMessage());
        throw new RuntimeException("Billing service temporarily unavailable", ex);
    }

    private Optional<BillPayment> getPaymentFallback(UUID id, Exception ex) {
        if (ex instanceof DataIntegrityViolationException
                || ex instanceof IllegalArgumentException
                || ex instanceof ConstraintViolationException
                || ex instanceof HttpMessageNotReadableException
                || ex instanceof AccessDeniedException) {
            throw (RuntimeException) ex;
        }
        log.error("Fallback for getPayment: {}", ex.getMessage());
        throw new RuntimeException("Billing service temporarily unavailable", ex);
    }

    private Optional<BillPayment> getPaymentByReferenceFallback(String referenceNumber, Exception ex) {
        if (ex instanceof DataIntegrityViolationException
                || ex instanceof IllegalArgumentException
                || ex instanceof ConstraintViolationException
                || ex instanceof HttpMessageNotReadableException
                || ex instanceof AccessDeniedException) {
            throw (RuntimeException) ex;
        }
        log.error("Fallback for getPaymentByReference: {}", ex.getMessage());
        throw new RuntimeException("Billing service temporarily unavailable", ex);
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
