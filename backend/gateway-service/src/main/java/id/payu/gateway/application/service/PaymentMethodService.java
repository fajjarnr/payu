package id.payu.gateway.application.service;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service to determine available payment methods with eligibility, fees, and settlement times.
 * Returns methods based on transaction context (amount, KYC status, partner config).
 *
 * Part of E-15 IMP-041: Payment Method Selection API
 */
@ApplicationScoped
public class PaymentMethodService {

    /**
     * Get available payment methods for a given context.
     */
    public List<PaymentMethodInfo> getAvailableMethods(PaymentContext context) {
        List<PaymentMethodInfo> methods = new ArrayList<>();

        // Wallet payment - always available for authenticated users
        methods.add(new PaymentMethodInfo(
                "WALLET", "PayU Wallet", "wallet",
                true, BigDecimal.ZERO, "IDR", "Instant",
                "Pay from your PayU wallet balance",
                Map.of("minAmount", "100", "maxAmount", "50000000")
        ));

        // Virtual Account methods - available for amounts >= 10,000
        if (context.amount() == null || context.amount().compareTo(new BigDecimal("10000")) >= 0) {
            methods.add(new PaymentMethodInfo(
                    "VA_BCA", "BCA Virtual Account", "bank_transfer",
                    true, new BigDecimal("4000"), "IDR", "Instant - 3 hours",
                    "Pay via BCA ATM, Mobile Banking, or Internet Banking",
                    Map.of("minAmount", "10000", "maxAmount", "999999999")
            ));
            methods.add(new PaymentMethodInfo(
                    "VA_BNI", "BNI Virtual Account", "bank_transfer",
                    true, new BigDecimal("4000"), "IDR", "Instant - 3 hours",
                    "Pay via BNI ATM, Mobile Banking, or Internet Banking",
                    Map.of("minAmount", "10000", "maxAmount", "999999999")
            ));
            methods.add(new PaymentMethodInfo(
                    "VA_MANDIRI", "Mandiri Virtual Account", "bank_transfer",
                    true, new BigDecimal("4000"), "IDR", "Instant - 3 hours",
                    "Pay via Mandiri ATM, Livin', or Internet Banking",
                    Map.of("minAmount", "10000", "maxAmount", "999999999")
            ));
            methods.add(new PaymentMethodInfo(
                    "VA_PERMATA", "Permata Virtual Account", "bank_transfer",
                    true, new BigDecimal("3500"), "IDR", "Instant - 3 hours",
                    "Pay via PermataMobile or ATM",
                    Map.of("minAmount", "10000", "maxAmount", "999999999")
            ));
        }

        // QRIS - available for amounts <= 10,000,000
        if (context.amount() == null || context.amount().compareTo(new BigDecimal("10000000")) <= 0) {
            methods.add(new PaymentMethodInfo(
                    "QRIS", "QRIS", "qris",
                    true, BigDecimal.ZERO, "IDR", "Instant",
                    "Scan QR code with any e-wallet or mobile banking app",
                    Map.of("minAmount", "100", "maxAmount", "10000000")
            ));
        }

        // PayLater / Installment - requires KYC
        boolean kycVerified = context.kycStatus() != null && "VERIFIED".equalsIgnoreCase(context.kycStatus());
        if (context.amount() != null && context.amount().compareTo(new BigDecimal("100000")) >= 0) {
            methods.add(new PaymentMethodInfo(
                    "PAYLATER", "PayU PayLater", "paylater",
                    kycVerified, BigDecimal.ZERO, "IDR", "Instant",
                    kycVerified ? "Pay later in 3x, 6x, or 12x installments"
                                : "KYC verification required to use PayLater",
                    Map.of("minAmount", "100000", "maxAmount", "25000000",
                            "tenors", "3,6,12", "kycRequired", "true")
            ));
        }

        // Bank Transfer (manual) - always available
        methods.add(new PaymentMethodInfo(
                "BANK_TRANSFER", "Manual Bank Transfer", "bank_transfer",
                true, BigDecimal.ZERO, "IDR", "1-3 business days",
                "Transfer to PayU bank account with unique code",
                Map.of("minAmount", "10000", "maxAmount", "999999999",
                        "confirmationRequired", "true")
        ));

        Log.infof("Returning %d payment methods for amount=%s", methods.size(),
                context.amount() != null ? context.amount().toPlainString() : "null");

        return methods;
    }

    /**
     * Context for payment method eligibility evaluation.
     */
    public record PaymentContext(
            BigDecimal amount,
            String currency,
            String kycStatus,
            String userId,
            String partnerId
    ) {}

    /**
     * Payment method information including eligibility, fees, and settlement time.
     */
    public record PaymentMethodInfo(
            String code,
            String name,
            String category,
            boolean eligible,
            BigDecimal fee,
            String feeCurrency,
            String estimatedSettlementTime,
            String description,
            Map<String, String> metadata
    ) {}
}
