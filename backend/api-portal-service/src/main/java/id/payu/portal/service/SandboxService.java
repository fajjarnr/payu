package id.payu.portal.service;

import id.payu.portal.config.SandboxConfig;
import id.payu.portal.dto.SandboxPaymentRequest;
import id.payu.portal.dto.SandboxPaymentResponse;
import id.payu.portal.dto.SandboxPaymentStatusResponse;
import id.payu.portal.dto.SandboxRefundRequest;
import id.payu.portal.dto.SandboxRefundResponse;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class SandboxService {

    @Inject
    SandboxConfig sandboxConfig;

    private final Random random = new Random();
    private final ConcurrentHashMap<String, SandboxPaymentResponse> paymentStore = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, SandboxRefundResponse> refundStore = new ConcurrentHashMap<>();

    public Uni<SandboxPaymentResponse> createPayment(SandboxPaymentRequest request) {
        return simulateLatency()
            .onItem().transform(ignored -> {
                String paymentReferenceNo = "PAY" + System.currentTimeMillis() + random.nextInt(1000);
                String transactionDate = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
                
                SandboxPaymentResponse response = new SandboxPaymentResponse(
                    request.partnerReferenceNo(),
                    paymentReferenceNo,
                    "REF" + System.currentTimeMillis(),
                    transactionDate,
                    "COMPLETED",
                    new SandboxPaymentResponse.Amount(request.amount().value(), request.amount().currency()),
                    request.beneficiaryAccountNo(),
                    request.beneficiaryBankCode(),
                    request.sourceAccountNo()
                );
                
                paymentStore.put(paymentReferenceNo, response);
                Log.infof("Sandbox: Created payment %s for partner reference %s", 
                    paymentReferenceNo, request.partnerReferenceNo());
                
                return response;
            });
    }

    public Uni<SandboxPaymentStatusResponse> getPaymentStatus(String paymentReferenceNo) {
        return simulateLatency()
            .onItem().transform(ignored -> {
                SandboxPaymentResponse payment = paymentStore.get(paymentReferenceNo);
                if (payment == null) {
                    Log.warnf("Sandbox: Payment not found: %s", paymentReferenceNo);
                    return null;
                }
                
                return new SandboxPaymentStatusResponse(
                    payment.partnerReferenceNo(),
                    payment.paymentReferenceNo(),
                    payment.originalReferenceNo(),
                    payment.transactionDate(),
                    payment.paymentStatus(),
                    new SandboxPaymentStatusResponse.Amount(payment.amount().value(), payment.amount().currency())
                );
            });
    }

    public Uni<SandboxRefundResponse> createRefund(String paymentReferenceNo, SandboxRefundRequest request) {
        return simulateLatency()
            .onItem().transform(ignored -> {
                SandboxPaymentResponse originalPayment = paymentStore.get(paymentReferenceNo);
                if (originalPayment == null) {
                    Log.warnf("Sandbox: Original payment not found for refund: %s", paymentReferenceNo);
                    return null;
                }
                
                String refundReferenceNo = request.refundReferenceNo();
                String refundDate = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
                
                SandboxRefundResponse response = new SandboxRefundResponse(
                    refundReferenceNo,
                    paymentReferenceNo,
                    refundDate,
                    "COMPLETED",
                    new SandboxRefundResponse.Amount(originalPayment.amount().value(), originalPayment.amount().currency())
                );
                
                refundStore.put(refundReferenceNo, response);
                Log.infof("Sandbox: Created refund %s for payment %s", 
                    refundReferenceNo, paymentReferenceNo);
                
                return response;
            });
    }

    public Uni<Void> clearData() {
        return Uni.createFrom().item(() -> {
            int paymentCount = paymentStore.size();
            int refundCount = refundStore.size();
            paymentStore.clear();
            refundStore.clear();
            Log.infof("Sandbox: Cleared %d payments and %d refunds", paymentCount, refundCount);
            return null;
        });
    }

    public Uni<java.util.Map<String, Object>> getStats() {
        return Uni.createFrom().item(() -> {
            return java.util.Map.of(
                "totalPayments", paymentStore.size(),
                "totalRefunds", refundStore.size(),
                "latencyEnabled", sandboxConfig.latency().enabled(),
                "latencyMinMs", sandboxConfig.latency().min(),
                "latencyMaxMs", sandboxConfig.latency().max()
            );
        });
    }

    private Uni<Void> simulateLatency() {
        if (!sandboxConfig.latency().enabled()) {
            return Uni.createFrom().nullItem();
        }
        
        long minDelay = sandboxConfig.latency().min();
        long maxDelay = sandboxConfig.latency().max();
        long delay = minDelay + random.nextLong(maxDelay - minDelay + 1);
        
        Log.debugf("Sandbox: Simulating latency of %d ms", delay);
        
        return Uni.createFrom().voidItem()
            .onItem().delayIt().by(java.time.Duration.ofMillis(delay));
    }
}
