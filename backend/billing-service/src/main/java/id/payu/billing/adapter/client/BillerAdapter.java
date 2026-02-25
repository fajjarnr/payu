package id.payu.billing.adapter.client;

import id.payu.billing.domain.port.out.BillerPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Adapter wrapping BillerClient to implement BillerPort.
 * Follows the same Hexagonal pattern as {@link WalletAdapter}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BillerAdapter implements BillerPort {

    private final BillerClient billerClient;

    @Override
    public InquiryResult inquiry(String billerCode, String customerNumber) {
        log.debug("Biller inquiry: billerCode={}, customer={}", billerCode, customerNumber);
        try {
            BillerClient.InquiryResponse response = billerClient.inquiry(
                    new BillerClient.InquiryRequest(billerCode, customerNumber)
            );
            return new InquiryResult(
                    response.responseCode(),
                    response.responseMessage(),
                    response.customerName(),
                    response.outstandingAmount()
            );
        } catch (Exception e) {
            log.error("Biller inquiry failed: billerCode={}, customer={}, error={}",
                    billerCode, customerNumber, e.getMessage());
            return new InquiryResult("96", "SYSTEM_ERROR: " + e.getMessage(), null, null);
        }
    }

    @Override
    public PaymentResult pay(String billerCode, String customerNumber, BigDecimal amount, String referenceNumber) {
        log.debug("Biller payment: billerCode={}, customer={}, amount={}, ref={}",
                billerCode, customerNumber, amount, referenceNumber);
        try {
            BillerClient.PaymentResponse response = billerClient.pay(
                    new BillerClient.PaymentRequest(billerCode, customerNumber, amount, referenceNumber)
            );
            Instant completedAt = response.completedAt() != null
                    ? Instant.parse(response.completedAt())
                    : null;
            return new PaymentResult(
                    response.responseCode(),
                    response.responseMessage(),
                    response.billerTransactionId(),
                    response.status(),
                    completedAt
            );
        } catch (Exception e) {
            log.error("Biller payment failed: billerCode={}, customer={}, ref={}, error={}",
                    billerCode, customerNumber, referenceNumber, e.getMessage());
            return new PaymentResult("96", "SYSTEM_ERROR: " + e.getMessage(), null, "FAILED", null);
        }
    }
}
