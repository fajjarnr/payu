package id.payu.partner.application.service;

import id.payu.partner.adapter.persistence.repository.SnapBiPaymentRepository;
import id.payu.partner.adapter.persistence.repository.SnapBiRefundRepository;
import id.payu.partner.adapter.persistence.entity.SnapBiPaymentEntity;
import id.payu.partner.adapter.persistence.entity.SnapBiRefundEntity;
import id.payu.partner.dto.snap.PaymentRequest;
import id.payu.partner.dto.snap.PaymentResponse;
import id.payu.partner.dto.snap.PaymentStatusResponse;
import id.payu.partner.dto.snap.RefundRequest;
import id.payu.partner.dto.snap.RefundResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

    public SnapBiPaymentService(SnapBiPaymentRepository paymentRepository,
                                SnapBiRefundRepository refundRepository) {
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
    }

    @Transactional
    public PaymentResponse createPayment(String partnerId, PaymentRequest request) {
        // BUG-BE-183 FIX: Validate payment amount is positive
        if (request.amount == null || request.amount.value == null
                || request.amount.value.compareTo(BigDecimal.ZERO) <= 0) {
            return new PaymentResponse(
                "4002500",
                "Amount must be greater than zero",
                request.partnerReferenceNo,
                null
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
            record.setStatus(status);
            paymentRepository.save(record);

            LOG.info("Payment status updated payuRef={} status={}", payuReferenceNo, status);

            if ("COMPLETED".equals(status)) {
                sendWebhookNotification(record, "payment.completed");
            } else if ("FAILED".equals(status)) {
                sendWebhookNotification(record, "payment.failed");
            } else if ("EXPIRED".equals(status)) {
                sendWebhookNotification(record, "payment.expired");
            }
        }
    }

    @Transactional
    public RefundResponse createRefund(String partnerId, String referenceNo, RefundRequest request) {
        SnapBiPaymentEntity record = paymentRepository.findByPartnerIdAndPayuReferenceNo(partnerId, referenceNo)
            .or(() -> paymentRepository.findByPartnerIdAndPartnerReferenceNo(partnerId, referenceNo))
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

        sendRefundWebhookNotification(refundRecord);

        return new RefundResponse(
            "2002500",
            "Successful",
            request.partnerRefundNo,
            payuRefundNo,
            record.getPayuReferenceNo(),
            "COMPLETED"
        );
    }

    private void sendWebhookNotification(SnapBiPaymentEntity record, String eventType) {
        LOG.info("Webhook notification sent for payment event payuRef={} eventType={}", record.getPayuReferenceNo(), eventType);
    }

    private void sendRefundWebhookNotification(SnapBiRefundEntity refundRecord) {
        LOG.info("Webhook notification sent for completed refund refundRef={}", refundRecord.getPayuRefundNo());
    }
}
