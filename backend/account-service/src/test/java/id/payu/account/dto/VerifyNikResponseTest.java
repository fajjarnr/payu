package id.payu.account.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ACCOUNT-006: VerifyNikResponse DTO coverage.
 */
@DisplayName("VerifyNikResponse")
class VerifyNikResponseTest {

    @Test
    void recordAccessors() {
        VerifyNikResponse response = new VerifyNikResponse(
                "req-1", "3201010101010001", true, "JOHN DOE",
                "JAKARTA", LocalDate.of(1990, 1, 1), "LAKI-LAKI", "Jl. Test",
                "VERIFIED", "NIK_OK", "NIK verified");

        assertThat(response.requestId()).isEqualTo("req-1");
        assertThat(response.nik()).isEqualTo("3201010101010001");
        assertThat(response.verified()).isTrue();
        assertThat(response.fullName()).isEqualTo("JOHN DOE");
        assertThat(response.birthPlace()).isEqualTo("JAKARTA");
        assertThat(response.birthDate()).isEqualTo(LocalDate.of(1990, 1, 1));
        assertThat(response.gender()).isEqualTo("LAKI-LAKI");
        assertThat(response.address()).isEqualTo("Jl. Test");
        assertThat(response.status()).isEqualTo("VERIFIED");
        assertThat(response.responseCode()).isEqualTo("NIK_OK");
        assertThat(response.responseMessage()).isEqualTo("NIK verified");
    }
}
