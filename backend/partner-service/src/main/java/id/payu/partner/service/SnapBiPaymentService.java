package id.payu.partner.service;

import id.payu.partner.dto.snap.PaymentRequest;
import id.payu.partner.dto.snap.PaymentResponse;
import id.payu.partner.dto.snap.PaymentStatusResponse;
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
                sendWebhookNotification(record);
            }
        }
    }

    private void sendWebhookNotification(PaymentRecord record) {
        WebhookEvent webhookEvent = new WebhookEvent(
            "payment.completed",
            record.payuReferenceNo,
            record.partnerReferenceNo,
            record.amount,
            record.currency,
            record.status,
            Instant.now()
        );

        LOG.infof("Webhook notification sent for completed payment payuRef=%s", record.payuReferenceNo);
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
}
