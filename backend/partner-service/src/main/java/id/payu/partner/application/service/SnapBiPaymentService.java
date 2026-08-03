package id.payu.partner.application.service;

import id.payu.partner.adapter.persistence.repository.SnapBiPaymentRepository;
import id.payu.partner.adapter.persistence.repository.SnapBiRefundRepository;
import id.payu.partner.adapter.persistence.entity.SnapBiPaymentEntity;
import id.payu.partner.adapter.persistence.entity.SnapBiRefundEntity;
import id.payu.partner.domain.port.out.WalletSettlementPort;
import id.payu.partner.dto.snap.PaymentRequest;
import id.payu.partner.dto.snap.PaymentResponse;
import id.payu.partner.dto.snap.PaymentStatusResponse;
import id.payu.partner.dto.snap.RefundRequest;
import id.payu.partner.dto.snap.RefundResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import id.payu.outbox.service.OutboxService;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * SNAP BI Payment Service.
 *
 * BUG-BE-182 FIX: Replaced in-memory ConcurrentHashMap stores with JPA-backed
 * persistence (SnapBiPaymentEntity / SnapBiRefundEntity entities) so payment and refund
 * records survive service restarts.
 */
@Service
public class SnapBiPaymentService {

    private static final Logger LOG = LoggerFactory.getLogger(SnapBiPaymentService.class);

    private final SnapBiPaymentRepository paymentRepository;
    private final SnapBiRefundRepository refundRepository;
    private final WalletSettlementPort walletSettlementPort;
    private final WebhookDispatcherService webhookDispatcher;
    private final OutboxService outboxService;

    public SnapBiPaymentService(SnapBiPaymentRepository paymentRepository,
                                SnapBiRefundRepository refundRepository,
                                WalletSettlementPort walletSettlementPort,
                                WebhookDispatcherService webhookDispatcher,
                                OutboxService outboxService) {
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
        this.walletSettlementPort = walletSettlementPort;
        this.webhookDispatcher = webhookDispatcher;
        this.outboxService = outboxService;
    }

    @Transactional
    public PaymentResponse createPayment(String partnerId, PaymentRequest request) {
        // BUG-BE-183 FIX: Validate payment amount is positive
        if (request == null || request.amount == null || request.amount.value == null
                || request.amount.value.compareTo(BigDecimal.ZERO) <= 0) {
            return new PaymentResponse(
                "4002500",
                "Amount must be greater than zero",
                request == null ? null : request.partnerReferenceNo,
                null
            );
        }

        if (!"IDR".equalsIgnoreCase(request.amount.currency)) {
            return new PaymentResponse(
                    "4002502",
                    "Only IDR payments are supported",
                    request.partnerReferenceNo,
                    null);
        }

        if (isBlank(request.sourceAccountNo) || isBlank(request.beneficiaryAccountNo)) {
            return new PaymentResponse(
                    "4002501",
                    "Source and beneficiary accounts are required",
                    request.partnerReferenceNo,
                    null);
        }

        // MVP-004: SNAP-BI idempotency via natural key (partnerReferenceNo per partner).
        // A replayed createPayment returns the existing record instead of minting a duplicate.
        // Backed by unique index uq_snap_payment_partner_ref (V15).
        Optional<SnapBiPaymentEntity> existing =
                paymentRepository.findByPartnerIdAndPartnerReferenceNo(partnerId, request.partnerReferenceNo);
        if (existing.isPresent()) {
            SnapBiPaymentEntity e = existing.get();
            LOG.info("Idempotent replay: returning existing payuRef={} for partnerRef={}",
                    e.getPayuReferenceNo(), request.partnerReferenceNo);
            return new PaymentResponse(
                "2002500",
                "Successful",
                e.getPartnerReferenceNo(),
                e.getPayuReferenceNo()
            );
        }

        String payuReferenceNo = "PAYU-" + UUID.randomUUID().toString();

        SnapBiPaymentEntity record = new SnapBiPaymentEntity(
            payuReferenceNo,
            partnerId,
            request.partnerReferenceNo,
            request.amount.value,
            request.amount.currency,
            request.beneficiaryAccountNo,
            request.beneficiaryBankCode,
            request.sourceAccountNo,
            "PENDING"
        );

        paymentRepository.save(record);

        walletSettlementPort.settle(
                request.sourceAccountNo,
                request.beneficiaryAccountNo,
                request.amount.value,
                request.amount.currency,
                payuReferenceNo);

        record.setStatus("COMPLETED");
        paymentRepository.save(record);
        publishPaymentEvent(record, "payment.completed");

        LOG.info("Payment initiated payuRef={} partnerRef={} amount={}",
                payuReferenceNo, request.partnerReferenceNo, request.amount.value);

        return new PaymentResponse(
            "2002500",
            "Successful",
            request.partnerReferenceNo,
            payuReferenceNo
        );
    }

    @Transactional(readOnly = true)
    public PaymentStatusResponse getPaymentStatus(String partnerId, String referenceNo) {
        SnapBiPaymentEntity record = paymentRepository.findByPartnerIdAndPayuReferenceNo(partnerId, referenceNo)
            .or(() -> paymentRepository.findByPartnerIdAndPartnerReferenceNo(partnerId, referenceNo))
            .orElse(null);

        if (record == null) {
            return new PaymentStatusResponse(
                "4042500",
                "Payment not found",
                null,
                null,
                null,
                null,
                null,
                null,
                null
            );
        }

        return new PaymentStatusResponse(
            "2002500",
            "Successful",
            record.getPartnerReferenceNo(),
            record.getPayuReferenceNo(),
            record.getAmount(),
            record.getCurrency(),
            record.getStatus(),
            record.getBeneficiaryAccountNo(),
            record.getCreatedAt().toString()
        );
    }

    @Transactional
    public void updatePaymentStatus(String payuReferenceNo, String status) {
        SnapBiPaymentEntity record = paymentRepository.findByPayuReferenceNo(payuReferenceNo).orElse(null);
        if (record != null) {
            if (status.equals(record.getStatus())) {
                return;
            }
            record.setStatus(status);
            paymentRepository.save(record);

            LOG.info("Payment status updated payuRef={} status={}", payuReferenceNo, status);

            if ("COMPLETED".equals(status)) {
                publishPaymentEvent(record, "payment.completed");
            } else if ("FAILED".equals(status)) {
                publishPaymentEvent(record, "payment.failed");
            } else if ("EXPIRED".equals(status)) {
                publishPaymentEvent(record, "payment.expired");
            }
        }
    }

    @Transactional
    public RefundResponse createRefund(String partnerId, String referenceNo, RefundRequest request) {
        // Serialize cumulative refund checks on the payment parent row.
        SnapBiPaymentEntity record = paymentRepository.findForUpdateByPartnerIdAndReferenceNo(partnerId, referenceNo)
            .orElse(null);

        if (record == null) {
            return new RefundResponse(
                "4042500",
                "Payment not found",
                null,
                null,
                null,
                null
            );
        }

        if (!"COMPLETED".equals(record.getStatus())) {
            return new RefundResponse(
                "4002502",
                "Payment cannot be refunded. Payment status: " + record.getStatus(),
                null,
                null,
                null,
                null
            );
        }

        // BUG-BE-181 FIX: Validate refund amount does not exceed original payment amount
        if (request.amount == null || request.amount.value == null
                || request.amount.value.compareTo(BigDecimal.ZERO) <= 0) {
            return new RefundResponse(
                "4002502",
                "Refund amount must be greater than zero",
                null,
                null,
                null,
                null
            );
        }

        // MVP-004: refund idempotency via natural key (partnerRefundNo per partner+payment).
        // Replayed refund returns the existing refund instead of creating a duplicate.
        // Backed by unique index uq_snap_refund_partner_ref (V15).
        Optional<SnapBiRefundEntity> existingRefund = refundRepository
                .findByPartnerIdAndPayuReferenceNoAndPartnerRefundNo(
                        partnerId, record.getPayuReferenceNo(), request.partnerRefundNo);
        if (existingRefund.isPresent()) {
            SnapBiRefundEntity re = existingRefund.get();
            LOG.info("Idempotent refund replay: payuRefund={}", re.getPayuRefundNo());
            return new RefundResponse(
                "2002500",
                "Successful",
                re.getPartnerRefundNo(),
                re.getPayuRefundNo(),
                re.getPayuReferenceNo(),
                re.getStatus()
            );
        }

        // Check cumulative refunds using database aggregate query
        BigDecimal totalRefunded = refundRepository.sumRefundedAmountByPayuReferenceNo(record.getPayuReferenceNo());

        BigDecimal newTotal = totalRefunded.add(request.amount.value);
        if (newTotal.compareTo(record.getAmount()) > 0) {
            return new RefundResponse(
                "4002502",
                "Refund amount exceeds original payment. Original: " + record.getAmount()
                        + ", already refunded: " + totalRefunded
                        + ", requested: " + request.amount.value,
                null,
                null,
                null,
                null
            );
        }

        String payuRefundNo = "REFUND-" + UUID.randomUUID().toString();

        SnapBiRefundEntity refundRecord = new SnapBiRefundEntity(
            payuRefundNo,
            partnerId,
            record.getPayuReferenceNo(),
            record.getPartnerReferenceNo(),
            request.partnerRefundNo,
            request.amount.value,
            request.amount.currency,
            request.reason,
            "COMPLETED"
        );

        refundRepository.save(refundRecord);

        LOG.info("Refund processed payuRefund={} paymentRef={} amount={}",
                payuRefundNo, record.getPayuReferenceNo(), request.amount.value);

        publishRefundEvent(refundRecord);

        return new RefundResponse(
            "2002500",
            "Successful",
            request.partnerRefundNo,
            payuRefundNo,
            record.getPayuReferenceNo(),
            "COMPLETED"
        );
    }

    private void publishPaymentEvent(SnapBiPaymentEntity record, String eventType) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("event", eventType);
        payload.put("payuReferenceNo", record.getPayuReferenceNo());
        payload.put("partnerReferenceNo", record.getPartnerReferenceNo());
        payload.put("partnerId", record.getPartnerId());
        payload.put("amount", record.getAmount());
        payload.put("currency", record.getCurrency());
        payload.put("sourceAccountNo", record.getSourceAccountNo());
        payload.put("beneficiaryAccountNo", record.getBeneficiaryAccountNo());
        payload.put("status", record.getStatus());

        String eventId = record.getPayuReferenceNo() + ":" + eventType;
        webhookDispatcher.dispatch(eventType, eventId, payload);
        outboxService.createEvent(
                "SnapBiPayment",
                record.getPayuReferenceNo(),
                toOutboxEventType(eventType),
                payload,
                null,
                toTopic(eventType));
    }

    private void publishRefundEvent(SnapBiRefundEntity refundRecord) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("event", "payment.refunded");
        payload.put("payuRefundNo", refundRecord.getPayuRefundNo());
        payload.put("payuReferenceNo", refundRecord.getPayuReferenceNo());
        payload.put("partnerRefundNo", refundRecord.getPartnerRefundNo());
        payload.put("amount", refundRecord.getAmount());
        payload.put("currency", refundRecord.getCurrency());
        payload.put("status", refundRecord.getStatus());

        String eventId = refundRecord.getPayuRefundNo() + ":payment.refunded";
        webhookDispatcher.dispatch("payment.refunded", eventId, payload);
        outboxService.createEvent(
                "SnapBiRefund",
                refundRecord.getPayuRefundNo(),
                "PaymentRefunded",
                payload,
                null,
                "payu.partner.payment-refunded.v1");
    }

    private String toOutboxEventType(String eventType) {
        return switch (eventType) {
            case "payment.completed" -> "PaymentCompleted";
            case "payment.failed" -> "PaymentFailed";
            case "payment.expired" -> "PaymentExpired";
            default -> throw new IllegalArgumentException("Unsupported payment event: " + eventType);
        };
    }

    private String toTopic(String eventType) {
        return "payu.partner." + eventType.replace('.', '-') + ".v1";
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
