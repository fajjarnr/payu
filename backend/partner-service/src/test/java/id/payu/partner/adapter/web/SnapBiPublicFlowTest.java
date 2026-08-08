package id.payu.partner.adapter.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.partner.TestSecurityConfig;
import id.payu.partner.adapter.persistence.entity.PartnerEntity;
import id.payu.partner.adapter.persistence.repository.PartnerRepository;
import id.payu.partner.adapter.persistence.repository.SnapBiPaymentRepository;
import id.payu.partner.adapter.persistence.repository.SnapBiRefundRepository;
import id.payu.partner.adapter.persistence.repository.WebhookSubscriptionRepository;
import id.payu.partner.application.service.ApiKeyService;
import id.payu.partner.application.service.SnapBiSignatureService;
import id.payu.partner.application.service.SnapBiTokenService;
import id.payu.partner.domain.PartnerStatus;
import id.payu.partner.domain.port.out.WalletSettlementPort;
import org.junit.jupiter.api.BeforeEach;
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

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PARTNER-006: isolated fixture proving the SNAP-BI money flow through the
 * public contract path {@code /v1/partner/**} — token → payment → status →
 * refund — including negative auth cases. The wallet settlement port is mocked;
 * signatures are real SNAP-BI HMAC.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
class SnapBiPublicFlowTest {

    private static final String CONTRACT_BASE = "/v1/partner";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SnapBiSignatureService signatureService;

    @Autowired
    private PartnerRepository partnerRepository;

    @Autowired
    private SnapBiPaymentRepository paymentRepository;

    @Autowired
    private SnapBiRefundRepository refundRepository;

    @Autowired
    private WebhookSubscriptionRepository webhookSubscriptionRepository;

    @MockitoBean
    private WalletSettlementPort walletSettlementPort;

    @MockitoBean
    private ApiKeyService apiKeyService;

    @MockitoBean
    private SnapBiTokenService tokenService;

    private PartnerEntity partner;
    private String clientKey;
    private String clientSecret;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        refundRepository.deleteAll();
        webhookSubscriptionRepository.deleteAll();
        partnerRepository.deleteAll();

        clientKey = "cli_" + UUID.randomUUID();
        clientSecret = "sec_" + UUID.randomUUID();

        partner = new PartnerEntity();
        partner.setPartnerCode("PARTNER-006-FIXTURE");
        partner.setName("PARTNER-006 Fixture");
        partner.setEmail("fixture-006@payu.test");
        partner.setType("MERCHANT");
        partner.setStatus(PartnerStatus.ACTIVE);
        partner.setActive(true);
        partner.setClientId(clientKey);
        partner.setClientSecret(clientSecret);
        partner.setWebhookUrl("https://partner.example.com/webhooks/payu");
        partner = partnerRepository.save(partner);

        // Deterministic token round-trip without the distributed cache:
        // generateAccessToken issues a fixed token; getClientIdFromToken resolves
        // it back to this partner's client key.
        when(tokenService.generateAccessToken(clientKey, partner.getId().toString(), partner.getName()))
                .thenReturn("fixture-token-" + partner.getId());
        when(tokenService.getClientIdFromToken("fixture-token-" + partner.getId()))
                .thenReturn(clientKey);
    }

    private String timestamp() {
        return Instant.now().toString().replaceAll("\\.\\d+", "");
    }

    @Test
    @DisplayName("full flow: token -> payment -> status -> refund")
    void fullMoneyFlowThroughPublicContract() throws Exception {
        // --- token ---
        String tokenBody = "{\"grantType\":\"client_credentials\"}";
        String tokenTs = timestamp();
        String tokenSig = signatureService.generateSignatureWithClientKey(
                clientSecret, "POST", CONTRACT_BASE + "/auth/token", tokenTs, tokenBody);

        String tokenResponse = mockMvc.perform(post(CONTRACT_BASE + "/auth/token")
                        .header("X-CLIENT-KEY", clientKey)
                        .header("X-TIMESTAMP", tokenTs)
                        .header("X-SIGNATURE", tokenSig)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tokenBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn().getResponse().getContentAsString();

        JsonNode tokenJson = objectMapper.readTree(tokenResponse);
        String accessToken = tokenJson.get("accessToken").asText();

        // --- payment ---
        String paymentBody = """
                {"partnerReferenceNo":"PRN-006-%s","amount":{"value":"100.00","currency":"IDR"},
                 "sourceAccountNo":"SRC-006","beneficiaryAccountNo":"BEN-006","beneficiaryBankCode":"014"}
                """.formatted(System.currentTimeMillis());
        String paymentTs = timestamp();
        String paymentSig = signatureService.generateSignature(
                clientSecret, "POST", CONTRACT_BASE + "/payments", accessToken, paymentBody, paymentTs);

        String paymentResponse = mockMvc.perform(post(CONTRACT_BASE + "/payments")
                        .header("Authorization", "Bearer " + accessToken)
                        .header("X-EXTERNAL-ID", "EXT-006-" + System.currentTimeMillis())
                        .header("X-TIMESTAMP", paymentTs)
                        .header("X-SIGNATURE", paymentSig)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("2002500"))
                .andReturn().getResponse().getContentAsString();

        JsonNode paymentJson = objectMapper.readTree(paymentResponse);
        String payuRef = paymentJson.get("referenceNo").asText();

        verify(walletSettlementPort).settle(anyString(), anyString(), any(), anyString(), anyString());

        // --- status ---
        String statusTs = timestamp();
        String statusSig = signatureService.generateSignature(
                clientSecret, "GET", CONTRACT_BASE + "/payments/" + payuRef, accessToken, "", statusTs);

        mockMvc.perform(get(CONTRACT_BASE + "/payments/" + payuRef)
                        .header("Authorization", "Bearer " + accessToken)
                        .header("X-TIMESTAMP", statusTs)
                        .header("X-SIGNATURE", statusSig))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("2002500"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        // --- refund ---
        String refundBody = """
                {"partnerRefundNo":"RFN-006-%s","amount":{"value":"100.00","currency":"IDR"},"reason":"fixture refund"}
                """.formatted(System.currentTimeMillis());
        String refundTs = timestamp();
        String refundSig = signatureService.generateSignature(
                clientSecret, "POST", CONTRACT_BASE + "/payments/" + payuRef + "/refund",
                accessToken, refundBody, refundTs);

        mockMvc.perform(post(CONTRACT_BASE + "/payments/" + payuRef + "/refund")
                        .header("Authorization", "Bearer " + accessToken)
                        .header("X-TIMESTAMP", refundTs)
                        .header("X-SIGNATURE", refundSig)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refundBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("2002500"));

        verify(walletSettlementPort).reverse(anyString(), anyString(), any(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("negative: token with wrong client key -> 401")
    void tokenWithWrongClientKeyRejected() throws Exception {
        String body = "{\"grantType\":\"client_credentials\"}";
        String ts = timestamp();
        String sig = signatureService.generateSignatureWithClientKey(
                clientSecret, "POST", CONTRACT_BASE + "/auth/token", ts, body);

        mockMvc.perform(post(CONTRACT_BASE + "/auth/token")
                        .header("X-CLIENT-KEY", "wrong-client-key")
                        .header("X-TIMESTAMP", ts)
                        .header("X-SIGNATURE", sig)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.responseCode").value("4012502"));
    }

    @Test
    @DisplayName("negative: token with invalid signature -> 401")
    void tokenWithInvalidSignatureRejected() throws Exception {
        String body = "{\"grantType\":\"client_credentials\"}";
        String ts = timestamp();

        mockMvc.perform(post(CONTRACT_BASE + "/auth/token")
                        .header("X-CLIENT-KEY", clientKey)
                        .header("X-TIMESTAMP", ts)
                        .header("X-SIGNATURE", "invalid-signature")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.responseCode").value("4012504"));
    }

    @Test
    @DisplayName("negative: payment without valid bearer token -> 401")
    void paymentWithoutValidTokenRejected() throws Exception {
        String paymentBody = """
                {"partnerReferenceNo":"PRN-NEG-%s","amount":{"value":"100.00","currency":"IDR"},
                 "sourceAccountNo":"SRC-NEG","beneficiaryAccountNo":"BEN-NEG"}
                """.formatted(System.currentTimeMillis());
        String ts = timestamp();
        String sig = signatureService.generateSignature(
                clientSecret, "POST", CONTRACT_BASE + "/payments", "bogus-token", paymentBody, ts);

        mockMvc.perform(post(CONTRACT_BASE + "/payments")
                        .header("Authorization", "Bearer bogus-token")
                        .header("X-EXTERNAL-ID", "EXT-NEG-" + System.currentTimeMillis())
                        .header("X-TIMESTAMP", ts)
                        .header("X-SIGNATURE", sig)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.responseCode").value("4012506"));

        verify(walletSettlementPort, never()).settle(any(), any(), any(), any(), any());
    }
}
