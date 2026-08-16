package id.payu.simulator.qris.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure-logic tests for QRIS content generation.
 */
@DisplayName("QrCodeGenerator")
class QrCodeGeneratorTest {

    private final QrCodeGenerator generator = new QrCodeGenerator();

    @Test
    @DisplayName("content starts with EMVCo payload format indicator and dynamic PoI")
    void startsWithPayloadAndDynamicPoI() {
        String content = generator.generateQrisContent("M001", "Warung Sari", BigDecimal.TEN, "REF1");
        assertTrue(content.startsWith("00020101"));
        assertTrue(content.contains("010212"));
    }

    @Test
    @DisplayName("content embeds amount, merchant name, reference and merchant id")
    void embedsCoreFields() {
        String content = generator.generateQrisContent("M001", "Warung Sari", new BigDecimal("15000"), "REF123");
        assertTrue(content.contains("54" + String.format("%02d", "15000".length()) + "15000"));
        assertTrue(content.contains("59" + String.format("%02d", "Warung Sari".length()) + "Warung Sari"));
        assertTrue(content.contains("62" + String.format("%02d", "REF123".length()) + "REF123"));
        assertTrue(content.contains("26" + String.format("%02d", "M001".length()) + "M001"));
    }

    @Test
    @DisplayName("omits amount field when null")
    void omitsAmountWhenNull() {
        String content = generator.generateQrisContent("M001", "Warung Sari", null, "REF1");
        assertFalse(content.contains("54"));
    }

    @Test
    @DisplayName("content is deterministic for the same inputs")
    void deterministicForSameInputs() {
        String a = generator.generateQrisContent("M001", "Warung Sari", BigDecimal.TEN, "REF1");
        String b = generator.generateQrisContent("M001", "Warung Sari", BigDecimal.TEN, "REF1");
        assertEquals(a, b);
    }
}
