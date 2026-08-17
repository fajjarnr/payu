package id.payu.transaction.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Invariant tests for QRIS QR Payment (Flow 8).
 *
 * <p>Verifies ASPI & Bank Indonesia QRIS standards:</p>
 * <ul>
 *   <li>EMVCo EMV-QR string payload structure (Currency 360 IDR, Country ID).</li>
 *   <li>MDR (Merchant Discount Rate) fee arithmetic and scale 4 precision.</li>
 *   <li>Net settlement invariant: net = gross - mdrFee.</li>
 * </ul>
 */
@DisplayName("QRIS Payment Invariant Tests (Flow 8)")
class QrisPaymentInvariantTest {

    @Nested
    @DisplayName("Fee Calculation & Settlement Invariants")
    class FeeAndSettlementInvariants {

        @Test
        @DisplayName("Net settlement amount equals gross minus MDR fee")
        void netSettlementCalculation() {
            Money gross = Money.idr(new BigDecimal("100000.0000"));
            BigDecimal mdrRate = new BigDecimal("0.0070"); // 0.7% MDR for regular merchants
            BigDecimal feeAmount = gross.getAmount().multiply(mdrRate).setScale(4, RoundingMode.HALF_EVEN);
            Money fee = Money.idr(feeAmount);
            Money net = gross.subtract(fee);

            assertThat(fee.getAmount()).isEqualByComparingTo(new BigDecimal("700.0000"));
            assertThat(net.getAmount()).isEqualByComparingTo(new BigDecimal("99300.0000"));
            assertThat(net.add(fee).getAmount()).isEqualByComparingTo(gross.getAmount());
        }

        @Test
        @DisplayName("MDR fee for micro-merchants (UMI) is 0% (zero fee invariant)")
        void umiZeroFeeInvariant() {
            Money gross = Money.idr(new BigDecimal("50000.0000"));
            BigDecimal mdrRate = BigDecimal.ZERO;
            BigDecimal feeAmount = gross.getAmount().multiply(mdrRate).setScale(4, RoundingMode.HALF_EVEN);
            
            assertThat(feeAmount).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("EMVCo QR Payload Invariants")
    class EmvCoPayloadInvariants {

        @Test
        @DisplayName("QRIS payload must specify IDR (360) and Country Code (ID)")
        void qrisPayloadStructure() {
            String sampleQris = "00020101021226580016ID.CO.PAYU.WWW01189360099900000000015204541153033605802ID5911PayU Merchant6007Jakarta61051234062070703A0163041D3B";
            
            assertThat(sampleQris).startsWith("000201"); // Payload Format Indicator
            assertThat(sampleQris).contains("5303360"); // Currency IDR (360)
            assertThat(sampleQris).contains("5802ID");  // Country ID
        }
    }
}
