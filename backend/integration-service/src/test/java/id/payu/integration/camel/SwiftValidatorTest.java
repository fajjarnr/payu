package id.payu.integration.camel;

import id.payu.integration.adapter.camel.validator.SwiftValidator;
import id.payu.integration.domain.model.IntegrationMessage;
import id.payu.integration.domain.model.MessageDirection;
import id.payu.integration.domain.model.MessageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SwiftValidator.
 */
public class SwiftValidatorTest {

    private SwiftValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SwiftValidator();
    }

    @Test
    void testValidMT103Message() {
        String swiftMessage = "{1:F01PAYUIDJAAXXX0000000000}{2:I103PAYUIDJAXXXXN}{4:\n" +
                ":20:REF123456\n" +
                ":23B:CRED\n" +
                ":32A:240101USD1000000,00\n" +
                ":50K:/1234567890\nJOHN DOE\n" +
                ":59:/9876543210\nJANE SMITH\n" +
                ":71A:SHA\n" +
                "-}{5:{CHK:000000000000}}";

        IntegrationMessage message = IntegrationMessage.builder()
                .messageId("test-id")
                .type(MessageType.SWIFT_MT103)
                .direction(MessageDirection.INBOUND)
                .rawPayload(swiftMessage)
                .build();

        SwiftValidator.ValidationResult result = validator.validate(message);

        assertTrue(result.valid(), "Expected valid message but got errors: " + result.getErrorMessage());
        assertFalse(result.hasErrors());
    }

    @Test
    void testInvalidMessageMissingRequiredFields() {
        String swiftMessage = "{1:F01PAYUIDJAAXXX0000000000}{2:I103PAYUIDJAXXXXN}{4:\n" +
                ":20:REF123456\n" +
                "-}{5:{CHK:000000000000}}";

        IntegrationMessage message = IntegrationMessage.builder()
                .messageId("test-id")
                .type(MessageType.SWIFT_MT103)
                .rawPayload(swiftMessage)
                .build();

        SwiftValidator.ValidationResult result = validator.validate(message);

        assertFalse(result.valid());
        assertTrue(result.hasErrors());
        assertTrue(result.getErrorMessage().contains("23B"));
        assertTrue(result.getErrorMessage().contains("32A"));
    }

    @Test
    void testEmptyPayload() {
        IntegrationMessage message = IntegrationMessage.builder()
                .messageId("test-id")
                .type(MessageType.SWIFT_MT103)
                .rawPayload("")
                .build();

        SwiftValidator.ValidationResult result = validator.validate(message);

        assertFalse(result.valid());
        assertTrue(result.getErrorMessage().contains("empty"));
    }

    @Test
    void testDetectMessageType() {
        String mt103 = "{2:I103PAYUIDJAXXXXN}";
        String mt202 = "{2:I202PAYUIDJAXXXXN}";
        String mt940 = "{2:O940PAYUIDJAXXXXN}";

        assertEquals("MT103", validator.detectMessageType(mt103));
        assertEquals("MT202", validator.detectMessageType(mt202));
        assertEquals("MT940", validator.detectMessageType(mt940));
    }

    @Test
    void testIsValidFormat() {
        String valid = "{1:F01}{2:I103}{4:}";
        String invalid = "invalid message";
        String empty = "";

        assertTrue(validator.isValidFormat(valid));
        assertFalse(validator.isValidFormat(invalid));
        assertFalse(validator.isValidFormat(empty));
        assertFalse(validator.isValidFormat(null));
    }
}
