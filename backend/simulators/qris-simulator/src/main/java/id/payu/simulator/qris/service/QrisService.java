package id.payu.simulator.qris.service;

import id.payu.simulator.qris.config.SimulatorConfig;
import id.payu.simulator.qris.dto.*;
import id.payu.simulator.qris.entity.Merchant;
import id.payu.simulator.qris.entity.QrisPayment;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

/**
 * Service for QRIS simulation operations.
 */
@ApplicationScoped
public class QrisService {

    private final Random random = new Random();

    @Inject
    SimulatorConfig config;

    @Inject
    QrCodeGenerator qrCodeGenerator;

    /**
     * Generate QRIS code for payment.
     */
    @Transactional
    public GenerateQrResponse generateQr(GenerateQrRequest request) {
        Log.infof("Generating QRIS for merchant=%s, amount=%s", 
                  request.merchantId(), request.amount());

        simulateLatency();

        if (shouldSimulateFailure()) {
            return GenerateQrResponse.error("Simulated random failure");
        }

        // Find merchant
        Merchant merchant = Merchant.findByMerchantId(request.merchantId());
        if (merchant == null) {
            return GenerateQrResponse.merchantNotFound(request.merchantId());
        }

        if (merchant.status != Merchant.MerchantStatus.ACTIVE) {
            return GenerateQrResponse.merchantBlocked(request.merchantId());
        }

        // Create payment record
        QrisPayment payment = new QrisPayment();
        payment.qrId = generateQrId();
        payment.referenceNumber = generateReferenceNumber();
        payment.merchant = merchant;
        payment.qrType = QrisPayment.QrType.DYNAMIC;
        payment.amount = request.amount();
        payment.tipAmount = request.tipAmount();
        payment.webhookUrl = request.webhookUrl();
        payment.expiresAt = LocalDateTime.now().plusSeconds(
            request.expirySeconds() != null ? request.expirySeconds() : config.qr().expirySeconds()
        );

        // Generate QR content
        payment.qrContent = qrCodeGenerator.generateQrisContent(
            merchant.merchantId,
            merchant.merchantName,
            payment.amount,
            payment.referenceNumber
        );

        // Generate QR image
        payment.qrImageBase64 = qrCodeGenerator.generateQrImage(payment.qrContent);

        payment.persist();

        Log.infof("QRIS generated: qrId=%s, ref=%s, expires=%s", 
                  payment.qrId, payment.referenceNumber, payment.expiresAt);

        return GenerateQrResponse.fromEntity(payment);
    }

    /**
     * Simulate payment for a QR code.
     */
    @Transactional
    public PaymentResponse payQr(PayQrRequest request) {
        Log.infof("Processing payment for qrId=%s, payer=%s", 
                  request.qrId(), request.payerName());

        simulateLatency();

        if (shouldSimulateFailure() || request.simulateFailure()) {
            return PaymentResponse.error("Simulated payment failure");
        }

        // Find QR payment
        QrisPayment payment = QrisPayment.findByQrId(request.qrId());
        if (payment == null) {
            return PaymentResponse.notFound(request.qrId());
        }

        // Check if expired
        if (payment.isExpired()) {
            payment.markAsExpired();
            return PaymentResponse.expired(payment);
        }

        // Check if already paid
        if (payment.status == QrisPayment.PaymentStatus.PAID) {
            return PaymentResponse.alreadyPaid(payment);
        }

        // Check if failed or cancelled
        if (payment.status == QrisPayment.PaymentStatus.FAILED || 
            payment.status == QrisPayment.PaymentStatus.CANCELLED) {
            return PaymentResponse.failed(payment, "QR code is no longer valid");
        }

        // Process payment
        payment.markAsPaid(request.payerName(), request.payerAccount(), request.payerBank());

        // Add tip if provided
        if (request.tipAmount() != null) {
            payment.tipAmount = request.tipAmount();
        }

        Log.infof("Payment completed: qrId=%s, payer=%s, amount=%s", 
                  payment.qrId, payment.payerName, payment.amount);

        return PaymentResponse.success(payment);
    }

    /**
     * Get payment status by QR ID.
     */
    @Transactional
    public PaymentStatusResponse getStatus(String qrId) {
        Log.infof("Getting status for qrId=%s", qrId);

        simulateLatency();

        QrisPayment payment = QrisPayment.findByQrId(qrId);
        if (payment == null) {
            // Try by reference number
            payment = QrisPayment.findByReference(qrId);
        }

        if (payment == null) {
            return PaymentStatusResponse.notFound(qrId);
        }

        // Check and update expired status
        if (payment.status == QrisPayment.PaymentStatus.PENDING && payment.isExpired()) {
            payment.markAsExpired();
        }

        return PaymentStatusResponse.fromEntity(payment);
    }

    private String generateQrId() {
        return "QR-" + System.currentTimeMillis() + "-" + 
               UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private String generateReferenceNumber() {
        return "QRIS-" + System.currentTimeMillis() + "-" + 
               UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private void simulateLatency() {
        int min = config.latency().min();
        int max = config.latency().max();
        int latency = min + random.nextInt(max - min + 1);
        try {
            Thread.sleep(latency);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean shouldSimulateFailure() {
        return random.nextInt(100) < config.failureRate();
    }
}
