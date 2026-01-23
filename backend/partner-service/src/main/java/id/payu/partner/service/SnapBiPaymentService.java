package id.payu.partner.service;

import id.payu.partner.dto.snap.PaymentRequest;
import id.payu.partner.dto.snap.PaymentResponse;
import id.payu.partner.dto.snap.PaymentStatusResponse;
import id.payu.partner.dto.snap.RefundRequest;
import id.payu.partner.dto.snap.RefundResponse;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.logging.Logger;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class SnapBiPaymentService {

    private static final Logger LOG = Logger.getLogger(SnapBiPaymentService.class);

    private final Map<String, PaymentRecord> paymentStore = new ConcurrentHashMap<>();
    private final Map<String, RefundRecord> refundStore = new ConcurrentHashMap<>();

    @Inject
    @Channel("payment-events")
    Emitter<String> paymentEventEmitter;

    public Uni<PaymentResponse> createPayment(String partnerId, PaymentRequest request) {
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

        PaymentEvent event = new PaymentEvent(
            payuReferenceNo,
            partnerId,
            request.partnerReferenceNo,
            request.amount.value,
            "PAYMENT_INITIATED"
        );

        try {
            String eventJson = toJson(event);
            paymentEventEmitter.send(eventJson).toCompletableFuture().get();
            
            LOG.infof("Payment initiated payuRef=%s partnerRef=%s amount=%s", 
                payuReferenceNo, request.partnerReferenceNo, request.amount.value);
        } catch (Exception e) {
            LOG.errorf("Failed to send payment event: %s", e.getMessage());
        }

        PaymentResponse response = new PaymentResponse(
            "2002500",
            "Successful",
            request.partnerReferenceNo,
            payuReferenceNo
        );

        return Uni.createFrom().item(response);
    }

    public Uni<PaymentStatusResponse> getPaymentStatus(String partnerId, String referenceNo) {
        PaymentRecord record = paymentStore.values().stream()
            .filter(p -> p.partnerId.equals(partnerId) && 
                          (p.payuReferenceNo.equals(referenceNo) || p.partnerReferenceNo.equals(referenceNo)))
            .findFirst()
            .orElse(null);

        if (record == null) {
            PaymentStatusResponse response = new PaymentStatusResponse(
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
            return Uni.createFrom().item(response);
        }

        PaymentStatusResponse response = new PaymentStatusResponse(
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

        return Uni.createFrom().item(response);
    }

    public void updatePaymentStatus(String payuReferenceNo, String status) {
        PaymentRecord record = paymentStore.get(payuReferenceNo);
        if (record != null) {
            record.status = status;
            
            PaymentEvent event = new PaymentEvent(
                payuReferenceNo,
                record.partnerId,
                record.partnerReferenceNo,
                record.amount,
                status
            );

            try {
                String eventJson = toJson(event);
                paymentEventEmitter.send(eventJson).toCompletableFuture().get();
                
                LOG.infof("Payment status updated payuRef=%s status=%s", payuReferenceNo, status);
            } catch (Exception e) {
                LOG.errorf("Failed to send payment status update event: %s", e.getMessage());
            }

            if ("COMPLETED".equals(status)) {
                sendWebhookNotification(record, "payment.completed");
            } else if ("FAILED".equals(status)) {
                sendWebhookNotification(record, "payment.failed");
            } else if ("EXPIRED".equals(status)) {
                sendWebhookNotification(record, "payment.expired");
            }
        }
    }

    public Uni<RefundResponse> createRefund(String partnerId, String referenceNo, RefundRequest request) {
        PaymentRecord record = paymentStore.values().stream()
            .filter(p -> p.partnerId.equals(partnerId) && 
                          (p.payuReferenceNo.equals(referenceNo) || p.partnerReferenceNo.equals(referenceNo)))
            .findFirst()
            .orElse(null);

        if (record == null) {
            RefundResponse response = new RefundResponse(
                "4042500",
                "Payment not found",
                null,
                null,
                null,
                null
            );
            return Uni.createFrom().item(response);
        }

        if (!"COMPLETED".equals(record.status)) {
            RefundResponse response = new RefundResponse(
                "4002502",
                "Payment cannot be refunded. Payment status: " + record.status,
                null,
                null,
                null,
                null
            );
            return Uni.createFrom().item(response);
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

        RefundEvent event = new RefundEvent(
            payuRefundNo,
            record.payuReferenceNo,
            partnerId,
            request.partnerRefundNo,
            request.amount.value,
            "REFUND_COMPLETED"
        );

        try {
            String eventJson = toJson(event);
            paymentEventEmitter.send(eventJson).toCompletableFuture().get();
            
            LOG.infof("Refund processed payuRefund=%s paymentRef=%s amount=%s", 
                payuRefundNo, record.payuReferenceNo, request.amount.value);
        } catch (Exception e) {
            LOG.errorf("Failed to send refund event: %s", e.getMessage());
        }

        sendRefundWebhookNotification(refundRecord);

        RefundResponse response = new RefundResponse(
            "2002500",
            "Successful",
            request.partnerRefundNo,
            payuRefundNo,
            record.payuReferenceNo,
            "COMPLETED"
        );

        return Uni.createFrom().item(response);
    }

    private void sendWebhookNotification(PaymentRecord record, String eventType) {
        WebhookEvent webhookEvent = new WebhookEvent(
            eventType,
            record.payuReferenceNo,
            record.partnerReferenceNo,
            record.amount,
            record.currency,
            record.status,
            Instant.now()
        );

        LOG.infof("Webhook notification sent for payment event payuRef=%s eventType=%s", record.payuReferenceNo, eventType);
    }

    private void sendRefundWebhookNotification(RefundRecord refundRecord) {
        WebhookEvent webhookEvent = new WebhookEvent(
            "refund.completed",
            refundRecord.payuRefundNo,
            refundRecord.partnerReferenceNo,
            refundRecord.amount,
            refundRecord.currency,
            refundRecord.status,
            Instant.now()
        );

        LOG.infof("Webhook notification sent for completed refund refundRef=%s", refundRecord.payuRefundNo);
    }

    private String toJson(Object obj) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.setVisibility(mapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY)
                .withGetterVisibility(com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE)
                .withIsGetterVisibility(com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE));
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            LOG.error("Failed to serialize object to JSON", e);
            return "{}";
        }
    }

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

    static class PaymentEvent {
        public String payuReferenceNo;
        public String partnerId;
        public String partnerReferenceNo;
        public BigDecimal amount;
        public String status;

        PaymentEvent(String payuReferenceNo, String partnerId, String partnerReferenceNo, 
                     BigDecimal amount, String status) {
            this.payuReferenceNo = payuReferenceNo;
            this.partnerId = partnerId;
            this.partnerReferenceNo = partnerReferenceNo;
            this.amount = amount;
            this.status = status;
        }
    }

    static class WebhookEvent {
        public String eventType;
        public String payuReferenceNo;
        public String partnerReferenceNo;
        public BigDecimal amount;
        public String currency;
        public String status;
        public Instant timestamp;

        WebhookEvent(String eventType, String payuReferenceNo, String partnerReferenceNo,
                     BigDecimal amount, String currency, String status, Instant timestamp) {
            this.eventType = eventType;
            this.payuReferenceNo = payuReferenceNo;
            this.partnerReferenceNo = partnerReferenceNo;
            this.amount = amount;
            this.currency = currency;
            this.status = status;
            this.timestamp = timestamp;
        }
    }

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

    static class RefundEvent {
        public String payuRefundNo;
        public String payuReferenceNo;
        public String partnerId;
        public String partnerRefundNo;
        public BigDecimal amount;
        public String status;

        RefundEvent(String payuRefundNo, String payuReferenceNo, String partnerId, String partnerRefundNo,
                    BigDecimal amount, String status) {
            this.payuRefundNo = payuRefundNo;
            this.payuReferenceNo = payuReferenceNo;
            this.partnerId = partnerId;
            this.partnerRefundNo = partnerRefundNo;
            this.amount = amount;
            this.status = status;
        }
    }
}
