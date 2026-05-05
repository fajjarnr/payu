package id.payu.integration.camel;

import id.payu.integration.adapter.camel.transformer.SwiftTransformer;
import id.payu.integration.domain.model.IntegrationMessage;
import id.payu.integration.domain.model.MessageDirection;
import id.payu.integration.domain.model.MessageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SwiftTransformer.
 */
public class SwiftTransformerTest {

    private SwiftTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new SwiftTransformer();
    }

    @Test
    void testToInternalFormat() {
        String swiftMessage = "{1:F01PAYUIDJAAXXX0000000000}{2:I103PAYUIDJAXXXXN}{4:\n" +
                ":20:REF123456\n" +
                ":32A:240101USD1000000,00\n" +
                ":50K:/1234567890\nJOHN DOE\n" +
                ":59:/9876543210\nJANE SMITH\n" +
                ":71A:SHA\n" +
                "-}{5:{CHK:000000000000}}";

        IntegrationMessage message = IntegrationMessage.builder()
                .messageId("test-id")
                .type(MessageType.SWIFT_MT103)
                .direction(MessageDirection.INBOUND)
                .sourceSystem("SWIFT")
                .targetSystem("PAYU")
                .rawPayload(swiftMessage)
                .build();

        String result = transformer.toInternalFormat(message);

        assertNotNull(result);
        assertTrue(result.contains("\"messageId\":\"test-id\""));
        assertTrue(result.contains("\"messageType\":\"SWIFT_MT103\""));
        assertTrue(result.contains("\"transactionReference\":\"REF123456\""));
        assertTrue(result.contains("\"currency\":\"USD\""));
        assertTrue(result.contains("\"amount\":\"1000000.00\""));
    }

    @Test
    void testFromInternalFormat() {
        Map<String, Object> internalData = Map.of(
                "messageType", "SWIFT_MT103",
                "transactionReference", "REF789",
                "valueDate", "240101",
                "currency", "IDR",
                "amount", "5000000.00",
                "orderingCustomer", "JOHN DOE",
                "beneficiaryCustomer", "JANE SMITH",
                "charges", "SHA"
        );

        String result = transformer.fromInternalFormat(internalData);

        assertNotNull(result);
        assertTrue(result.contains(":20:REF789"));
        assertTrue(result.contains(":32A:240101IDR5000000.00"));
        assertTrue(result.contains(":50K:JOHN DOE"));
        assertTrue(result.contains(":59:JANE SMITH"));
        assertTrue(result.contains(":71A:SHA"));
    }

    @Test
    void testEmptyPayload() {
        IntegrationMessage message = IntegrationMessage.builder()
                .messageId("test-id")
                .type(MessageType.SWIFT_MT103)
                .direction(MessageDirection.INBOUND)
                .rawPayload("")
                .build();

        String result = transformer.toInternalFormat(message);

        assertNotNull(result);
        assertTrue(result.contains("\"messageId\":\"test-id\""));
    }
}
