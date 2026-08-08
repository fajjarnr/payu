package id.payu.partner.adapter.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import id.payu.partner.TestSecurityConfig;
import id.payu.partner.application.service.PartnerService;
import id.payu.partner.application.service.SnapBiPaymentService;
import id.payu.partner.application.service.SnapBiSignatureService;
import id.payu.partner.application.service.SnapBiTokenService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * PARTNER-003: Missing required SNAP-BI headers must produce a deterministic
 * 4xx (RFC 9457), never an HTTP 500.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
class SnapBiMissingHeaderTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PartnerService partnerService;

    @MockitoBean
    private SnapBiSignatureService signatureService;

    @MockitoBean
    private SnapBiTokenService tokenService;

    @MockitoBean
    private SnapBiPaymentService paymentService;

    @Test
    @DisplayName("token request without X-CLIENT-KEY returns 400, not 500")
    void missingClientKeyReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/v1/partner/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.error_code").value("MISSING_REQUIRED_HEADER"))
                .andExpect(jsonPath("$.title").value("Missing required header"));
    }

    @Test
    @DisplayName("token request with X-CLIENT-KEY but missing X-TIMESTAMP returns 400, not 500")
    void missingTimestampReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/v1/partner/auth/token")
                        .header("X-CLIENT-KEY", "test-client-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.error_code").value("MISSING_REQUIRED_HEADER"));
    }

    @Test
    @DisplayName("token request with required headers but missing signature returns 400, not 500")
    void missingSignatureReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/v1/partner/auth/token")
                        .header("X-CLIENT-KEY", "test-client-key")
                        .header("X-TIMESTAMP", "2026-08-06T00:00:00Z")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("MISSING_REQUIRED_HEADER"));
    }
}
