package id.payu.compliance.unit;

import id.payu.compliance.exception.ComplianceDomainException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ComplianceDomainExceptionTest {

    @Test
    void shouldCreateExceptionWithMessage() {
        String message = "Test error message";
        ComplianceDomainException exception = new ComplianceDomainException(message);

        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
    }

    @Test
    void shouldCreateExceptionWithMessageAndCause() {
        String message = "Test error message";
        Throwable cause = new RuntimeException("Root cause");
        ComplianceDomainException exception = new ComplianceDomainException(message, cause);

        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void shouldBeRuntimeException() {
        ComplianceDomainException exception = new ComplianceDomainException("Test");

        assertTrue(exception instanceof RuntimeException);
    }
}
