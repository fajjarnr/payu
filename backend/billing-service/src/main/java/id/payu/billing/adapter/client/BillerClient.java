package id.payu.billing.adapter.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

/**
 * REST client for biller-simulator (or production biller gateway).
 * Follows the same pattern as {@link WalletClient}.
 */
@Component
public class BillerClient {

    private final RestTemplate restTemplate;
    private final String billerServiceBaseUrl;

    public BillerClient(
            RestTemplate restTemplate,
            @Value("${spring.web.client.biller-service.base-url:http://biller-simulator:8080}") String billerServiceBaseUrl) {
        this.restTemplate = restTemplate;
        this.billerServiceBaseUrl = billerServiceBaseUrl;
    }

    /**
     * Inquire about a customer's outstanding bill.
     */
    public InquiryResponse inquiry(InquiryRequest request) {
        String url = billerServiceBaseUrl + "/api/v1/biller/inquiry";
        return restTemplate.postForObject(url, request, InquiryResponse.class);
    }

    /**
     * Submit a payment to the biller.
     */
    public PaymentResponse pay(PaymentRequest request) {
        String url = billerServiceBaseUrl + "/api/v1/biller/pay";
        return restTemplate.postForObject(url, request, PaymentResponse.class);
    }

    // ---- DTOs matching biller-simulator contract ----

    public record InquiryRequest(String billerCode, String customerNumber) {}

    public record InquiryResponse(
            String responseCode,
            String responseMessage,
            String billerCode,
            String customerNumber,
            String customerName,
            BigDecimal outstandingAmount
    ) {}

    public record PaymentRequest(
            String billerCode,
            String customerNumber,
            BigDecimal amount,
            String referenceNumber
    ) {}

    public record PaymentResponse(
            String responseCode,
            String responseMessage,
            String billerTransactionId,
            String billerCode,
            String customerNumber,
            BigDecimal amount,
            String status,
            String completedAt
    ) {}
}
