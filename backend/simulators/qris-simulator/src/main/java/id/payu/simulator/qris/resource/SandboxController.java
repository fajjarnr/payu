package id.payu.simulator.qris.resource;

import id.payu.simulator.qris.interfaces.dto.*;
import id.payu.simulator.qris.service.QrisService;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Sandbox controller for deterministic QRIS test scenarios.
 * Provides predictable responses for integration testing.
 */
@Path("/sandbox")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SandboxController {

    @Inject
    QrisService qrisService;

    // Test merchant IDs for deterministic responses
    private static final String TEST_MERCHANT_001 = "TEST-MERCHANT-001";
    private static final String TEST_MERCHANT_002 = "TEST-MERCHANT-002";
    private static final String TEST_MERCHANT_INVALID = "TEST-MERCHANT-INVALID";
    private static final String TEST_MERCHANT_BLOCKED = "TEST-MERCHANT-BLOCKED";

    /**
     * Deterministic QR generation for sandbox testing.
     */
    @POST
    @Path("/generate")
    public Response sandboxGenerate(SandboxGenerateRequest request) {
        Log.infof("Sandbox QR generation for merchant={}", request.merchantId());

        String merchantId = request.merchantId();

        // Deterministic: invalid merchant
        if (TEST_MERCHANT_INVALID.equals(merchantId)) {
            return Response.status(404)
                    .entity(new GenerateQrResponse(
                            "14",
                            "Merchant not found",
                            null,
                            null,
                            null,
                            null
                    ))
                    .build();
        }

        // Deterministic: blocked merchant
        if (TEST_MERCHANT_BLOCKED.equals(merchantId)) {
            return Response.status(403)
                    .entity(new GenerateQrResponse(
                            "62",
                            "Merchant blocked",
                            null,
                            null,
                            null,
                            null
                    ))
                    .build();
        }

        // Generate deterministic QR
        String qrId = "SBX-QR-" + System.currentTimeMillis();
        String qrContent = generateDeterministicQrContent(merchantId, request.amount(), qrId);
        String qrImageBase64 = generateMockQrImage();

        return Response.status(201)
                .entity(new GenerateQrResponse(
                        "00",
                        "QR generated successfully",
                        qrId,
                        qrContent,
                        qrImageBase64,
                        java.time.LocalDateTime.now().plusMinutes(15).toString()
                ))
                .build();
    }

    /**
     * Deterministic QR payment for sandbox testing.
     */
    @POST
    @Path("/pay")
    public Response sandboxPay(SandboxPayRequest request) {
        Log.infof("Sandbox QR payment for qrId={}", request.qrId());

        String qrId = request.qrId();

        // Deterministic: expired QR
        if (qrId.contains("EXPIRED")) {
            return Response.status(410)
                    .entity(new PaymentResponse(
                            "54",
                            "QR code expired",
                            qrId,
                            null,
                            null,
                            "EXPIRED"
                    ))
                    .build();
        }

        // Deterministic: already paid
        if (qrId.contains("PAID")) {
            return Response.status(409)
                    .entity(new PaymentResponse(
                            "55",
                            "QR already paid",
                            qrId,
                            null,
                            null,
                            "PAID"
                    ))
                    .build();
        }

        // Deterministic: insufficient funds for large amounts
        if (request.amount() != null && request.amount().compareTo(new BigDecimal("999999999")) > 0) {
            return Response.status(400)
                    .entity(new PaymentResponse(
                            "51",
                            "Insufficient funds",
                            qrId,
                            null,
                            null,
                            "FAILED"
                    ))
                    .build();
        }

        // Success
        return Response.ok()
                .entity(new PaymentResponse(
                        "00",
                        "Payment successful",
                        qrId,
                        "TRX" + System.currentTimeMillis(),
                        java.time.LocalDateTime.now().toString(),
                        "COMPLETED"
                ))
                .build();
    }

    /**
     * Get sandbox test scenarios.
     */
    @GET
    @Path("/scenarios")
    public Response getScenarios() {
        Map<String, Object> scenarios = new HashMap<>();

        scenarios.put("success_retail", Map.of(
                "description", "Successful payment to retail merchant",
                "merchantId", TEST_MERCHANT_001,
                "expectedResponse", "00 - Success"
        ));

        scenarios.put("success_fnb", Map.of(
                "description", "Successful payment to F\u0026B merchant",
                "merchantId", TEST_MERCHANT_002,
                "expectedResponse", "00 - Success"
        ));

        scenarios.put("merchant_not_found", Map.of(
                "description", "QR generation for invalid merchant",
                "merchantId", TEST_MERCHANT_INVALID,
                "expectedResponse", "14 - Merchant not found"
        ));

        scenarios.put("merchant_blocked", Map.of(
                "description", "QR generation for blocked merchant",
                "merchantId", TEST_MERCHANT_BLOCKED,
                "expectedResponse", "62 - Merchant blocked"
        ));

        scenarios.put("qr_expired", Map.of(
                "description", "Payment to expired QR",
                "qrId", "SBX-QR-EXPIRED-001",
                "expectedResponse", "54 - Expired"
        ));

        scenarios.put("qr_already_paid", Map.of(
                "description", "Payment to already-paid QR",
                "qrId", "SBX-QR-PAID-001",
                "expectedResponse", "55 - Already paid"
        ));

        scenarios.put("insufficient_funds", Map.of(
                "description", "Payment with insufficient funds",
                "merchantId", TEST_MERCHANT_001,
                "amount", "999999999999",
                "expectedResponse", "51 - Insufficient funds"
        ));

        return Response.ok(scenarios).build();
    }

    /**
     * Get sandbox test merchants.
     */
    @GET
    @Path("/test-merchants")
    public Response getTestMerchants() {
        Map<String, Object> merchants = new HashMap<>();

        merchants.put("retail", Map.of(
                "merchantId", TEST_MERCHANT_001,
                "name", "Test Retail Store",
                "category", "RETAIL",
                "status", "ACTIVE"
        ));

        merchants.put("fnb", Map.of(
                "merchantId", TEST_MERCHANT_002,
                "name", "Test Restaurant",
                "category", "FOOD_BEVERAGE",
                "status", "ACTIVE"
        ));

        return Response.ok(merchants).build();
    }

    private String generateDeterministicQrContent(String merchantId, BigDecimal amount, String qrId) {
        // Generate a deterministic QRIS-format content string
        return String.format("000201010212%s0108%s520400005303360%s%s5802ID5914Test Merchant6007JAKARTA%s",
                merchantId,
                amount != null ? amount.toString() : "0",
                qrId,
                System.currentTimeMillis(),
                "6304" + System.currentTimeMillis() % 10000);
    }

    private String generateMockQrImage() {
        // Return a mock base64-encoded QR image (small 1x1 PNG)
        return "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==";
    }

    // Request/Response records
    public record SandboxGenerateRequest(
            String merchantId,
            BigDecimal amount,
            String description,
            String expiryMinutes) {}
    public record SandboxPayRequest(
            String qrId,
            String payerId,
            BigDecimal amount,
            String paymentMethod) {}
    public record GenerateQrResponse(
            String responseCode,
            String responseMessage,
            String qrId,
            String qrContent,
            String qrImageBase64,
            String expiresAt) {}
    public record PaymentResponse(
            String responseCode,
            String responseMessage,
            String qrId,
            String transactionId,
            String completedAt,
            String status) {}
}
