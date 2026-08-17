package id.payu.account.interfaces.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The AuditAspect persists the audited argument's toString() into the audit
 * outbox. These DTOs must never render PII or passwords in toString.
 */
@DisplayName("Request DTO masking")
class RequestDtoMaskingTest {

    @Test
    @DisplayName("RegisterUserRequest.toString masks all PII including the password")
    void registerRequestIsMasked() {
        RegisterUserRequest request = new RegisterUserRequest(
                "ext-1", "user1", "user1@example.com", "+6281111111111",
                "Real Full Name", "3201234567890001", "PlainTextPassword!");

        String rendered = request.toString();
        assertThat(rendered).doesNotContain("user1@example.com", "+6281111111111",
                "Real Full Name", "3201234567890001", "PlainTextPassword!");
        assertThat(rendered).contains("email=****", "password=****", "nik=****");
    }

    @Test
    @DisplayName("VerifyNikRequest.toString masks NIK and full name")
    void verifyNikRequestIsMasked() {
        VerifyNikRequest request = new VerifyNikRequest(
                "3201234567890002", "Real Full Name", "Jakarta", "1990-01-01");

        String rendered = request.toString();
        assertThat(rendered).doesNotContain("3201234567890002", "Real Full Name");
        assertThat(rendered).contains("nik=****", "fullName=****");
    }
}
