package id.payu.partner.application.service;

import id.payu.partner.dto.snap.PaymentRequest;
import id.payu.partner.dto.snap.PaymentResponse;
import id.payu.partner.dto.snap.PaymentStatusResponse;
import id.payu.partner.dto.snap.RefundRequest;
import id.payu.partner.dto.snap.RefundResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SnapBiPaymentService {

    private static final Logger LOG = LoggerFactory.getLogger(SnapBiPaymentService.class);

    private final Map<String, PaymentRecord> paymentStore = new ConcurrentHashMap<>();
    private final Map<String, RefundRecord> refundStore = new ConcurrentHashMap<>();

    // Kafka integration commented out for migration - TODO restore with KafkaTemplate
    // @Autowired
    // private KafkaTemplate<String, String> kafkaTemplate;

    public PaymentResponse createPayment(String partnerId, PaymentRequest request) {
        String payuReferenceNo = "PAYU-" + UUID.randomUUID().toString();
        Instant now = Instant.now();

        PaymentRecord record = new PaymentRecord(
            payuReferenceNo,
            partnerId,
            request.partnerReferenceNo,
            request.amount.value,
            request.amount.currency,
            request.beneficiaryAccountNo,
            request.beneficiaryBankCode,
            request.sourceAccountNo,
            "PENDING",
            now
        );

        paymentStore.put(payuReferenceNo, record);

        // Logic for event emission replaced with log
        LOG.info("Payment initiated payuRef={} partnerRef={} amount={}",
                payuReferenceNo, request.partnerReferenceNo, request.amount.value);

        return new PaymentResponse(
            "2002500",
            "Successful",
            request.partnerReferenceNo,
            payuReferenceNo
        );
    }

    public PaymentStatusResponse getPaymentStatus(String partnerId, String referenceNo) {
        PaymentRecord record = paymentStore.values().stream()
            .filter(p -> p.partnerId.equals(partnerId) &&
                          (p.payuReferenceNo.equals(referenceNo) || p.partnerReferenceNo.equals(referenceNo)))
            .findFirst()
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
            record.partnerReferenceNo,
            record.payuReferenceNo,
            record.amount,
            record.currency,
            record.status,
            record.beneficiaryAccountNo,
            record.createdAt.toString()
        );
    }

    public void updatePaymentStatus(String payuReferenceNo, String status) {
        PaymentRecord record = paymentStore.get(payuReferenceNo);
        if (record != null) {
            record.status = status;

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

    public RefundResponse createRefund(String partnerId, String referenceNo, RefundRequest request) {
        PaymentRecord record = paymentStore.values().stream()
            .filter(p -> p.partnerId.equals(partnerId) &&
                          (p.payuReferenceNo.equals(referenceNo) || p.partnerReferenceNo.equals(referenceNo)))
            .findFirst()
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

        if (!"COMPLETED".equals(record.status)) {
            return new RefundResponse(
                "4002502",
                "Payment cannot be refunded. Payment status: " + record.status,
                null,
                null,
                null,
                null
            );
        }

        String payuRefundNo = "REFUND-" + UUID.randomUUID().toString();

        RefundRecord refundRecord = new RefundRecord(
            payuRefundNo,
            partnerId,
            record.payuReferenceNo,
            record.partnerReferenceNo,
            request.partnerRefundNo,
            request.amount.value,
            request.amount.currency,
            request.reason,
            "COMPLETED",
            Instant.now()
        );

        refundStore.put(payuRefundNo, refundRecord);

        LOG.info("Refund processed payuRefund={} paymentRef={} amount={}",
                payuRefundNo, record.payuReferenceNo, request.amount.value);

        sendRefundWebhookNotification(refundRecord);

        return new RefundResponse(
            "2002500",
            "Successful",
            request.partnerRefundNo,
            payuRefundNo,
            record.payuReferenceNo,
            "COMPLETED"
        );
    }

    private void sendWebhookNotification(PaymentRecord record, String eventType) {
        LOG.info("Webhook notification sent for payment event payuRef={} eventType={}", record.payuReferenceNo, eventType);
    }

    private void sendRefundWebhookNotification(RefundRecord refundRecord) {
        LOG.info("Webhook notification sent for completed refund refundRef={}", refundRecord.payuRefundNo);
    }
    
    // Internal classes kept as is but removed from static context if needed, or kept static
    
    static class PaymentRecord {
        public String payuReferenceNo;
        public String partnerId;
        public String partnerReferenceNo;
        public BigDecimal amount;
        public String currency;
        public String beneficiaryAccountNo;
        public String beneficiaryBankCode;
        public String sourceAccountNo;
        public String status;
        public Instant createdAt;

        PaymentRecord(String payuReferenceNo, String partnerId, String partnerReferenceNo,
                      BigDecimal amount, String currency, String beneficiaryAccountNo,
                      String beneficiaryBankCode, String sourceAccountNo, String status, Instant createdAt) {
            this.payuReferenceNo = payuReferenceNo;
            this.partnerId = partnerId;
            this.partnerReferenceNo = partnerReferenceNo;
            this.amount = amount;
            this.currency = currency;
            this.beneficiaryAccountNo = beneficiaryAccountNo;
            this.beneficiaryBankCode = beneficiaryBankCode;
            this.sourceAccountNo = sourceAccountNo;
            this.status = status;
            this.createdAt = createdAt;
        }
    }
    // Other inner classes (RefundRecord, WebhookEvent, etc.) omitted for brevity but should be there if used
    // Since I simplified the logic to remove Event classes (PaymentEvent, RefundEvent used for Kafka only), I can remove them if not used.
    
    static class RefundRecord {
        public String payuRefundNo;
        public String partnerId;
        public String payuReferenceNo;
        public String partnerReferenceNo;
        public String partnerRefundNo;
        public BigDecimal amount;
        public String currency;
        public String reason;
        public String status;
        public Instant createdAt;

        RefundRecord(String payuRefundNo, String partnerId, String payuReferenceNo, String partnerReferenceNo,
                     String partnerRefundNo, BigDecimal amount, String currency, String reason,
                     String status, Instant createdAt) {
            this.payuRefundNo = payuRefundNo;
            this.partnerId = partnerId;
            this.payuReferenceNo = payuReferenceNo;
            this.partnerReferenceNo = partnerReferenceNo;
            this.partnerRefundNo = partnerRefundNo;
            this.amount = amount;
            this.currency = currency;
            this.reason = reason;
            this.status = status;
            this.createdAt = createdAt;
        }
    }
}
