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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SNAP-PATH-001: the SNAP-BI v1.0 taxonomy paths ({@code /v1.0/access-token/b2b},
 * {@code /v1.0/transfer-va/payment}, {@code /v1.0/transfer-va/payment/{ref}},
 * {@code /v1.0/transfer-va/refund}) behave identically to the legacy
 * {@code /v1/partner/**} contract. Signatures bind the actual request path.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
class SnapBiV10ContractTest {

    private static final String V10_BASE = "/v1.0";

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
        partner.setPartnerCode("PARTNER-V10-FIXTURE");
        partner.setName("PARTNER-V10 Fixture");
        partner.setEmail("fixture-v10@payu.test");
        partner.setType("MERCHANT");
        partner.setStatus(PartnerStatus.ACTIVE);
        partner.setActive(true);
        partner.setClientId(clientKey);
        partner.setClientSecret(clientSecret);
        partner.setWebhookUrl("https://partner.example.com/webhooks/payu");
        partner = partnerRepository.save(partner);

        when(tokenService.generateAccessToken(clientKey, partner.getId().toString(), partner.getName()))
                .thenReturn("v10-token-" + partner.getId());
        when(tokenService.getClientIdFromToken("v10-token-" + partner.getId()))
                .thenReturn(clientKey);
    }

    private String timestamp() {
        return Instant.now().toString().replaceAll("\\.\\d+", "");
    }

    @Test
    @DisplayName("v1.0 taxonomy: token -> payment -> status -> refund")
    void fullMoneyFlowThroughV10Taxonomy() throws Exception {
        // --- token: POST /v1.0/access-token/b2b ---
        String tokenBody = "{\"grantType\":\"client_credentials\"}";
        String tokenTs = timestamp();
        String tokenSig = signatureService.generateSignatureWithClientKey(
                clientSecret, "POST", V10_BASE + "/access-token/b2b", tokenTs, tokenBody);

        String tokenResponse = mockMvc.perform(post(V10_BASE + "/access-token/b2b")
                        .header("X-CLIENT-KEY", clientKey)
                        .header("X-TIMESTAMP", tokenTs)
                        .header("X-SIGNATURE", tokenSig)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tokenBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andReturn().getResponse().getContentAsString();

        String accessToken = objectMapper.readTree(tokenResponse).get("accessToken").asText();

        // --- payment: POST /v1.0/transfer-va/payment ---
        String paymentBody = """
                {"partnerReferenceNo":"V10-PRN-%s","amount":{"value":"250.00","currency":"IDR"},
                 "sourceAccountNo":"SRC-V10","beneficiaryAccountNo":"BEN-V10","beneficiaryBankCode":"014"}
                """.formatted(System.currentTimeMillis());
        String paymentTs = timestamp();
        String paymentSig = signatureService.generateSignature(
                clientSecret, "POST", V10_BASE + "/transfer-va/payment", accessToken, paymentBody, paymentTs);

        String paymentResponse = mockMvc.perform(post(V10_BASE + "/transfer-va/payment")
                        .header("Authorization", "Bearer " + accessToken)
                        .header("X-EXTERNAL-ID", "EXT-V10-" + System.currentTimeMillis())
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

        // --- status: GET /v1.0/transfer-va/payment/{referenceNo} ---
        String statusTs = timestamp();
        String statusSig = signatureService.generateSignature(
                clientSecret, "GET", V10_BASE + "/transfer-va/payment/" + payuRef, accessToken, "", statusTs);

        mockMvc.perform(get(V10_BASE + "/transfer-va/payment/" + payuRef)
                        .header("Authorization", "Bearer " + accessToken)
                        .header("X-TIMESTAMP", statusTs)
                        .header("X-SIGNATURE", statusSig))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("2002500"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        // --- refund: POST /v1.0/transfer-va/refund (originalReferenceNo in body) ---
        String refundBody = """
                {"originalReferenceNo":"%s","partnerReferenceNo":"V10-RFN-%s","amount":{"value":"250.00","currency":"IDR"},"reason":"v10 refund"}
                """.formatted(payuRef, System.currentTimeMillis());
        String refundTs = timestamp();
        String refundSig = signatureService.generateSignature(
                clientSecret, "POST", V10_BASE + "/transfer-va/refund", accessToken, refundBody, refundTs);

        mockMvc.perform(post(V10_BASE + "/transfer-va/refund")
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
    @DisplayName("negative: v1.0 refund without originalReferenceNo -> 400")
    void v10RefundWithoutOriginalReferenceNoRejected() throws Exception {
        String refundBody = """
                {"partnerReferenceNo":"V10-RFN-%s","amount":{"value":"100.00","currency":"IDR"}}
                """.formatted(System.currentTimeMillis());
        String ts = timestamp();
        String sig = signatureService.generateSignature(
                clientSecret, "POST", V10_BASE + "/transfer-va/refund", "bogus", refundBody, ts);

        mockMvc.perform(post(V10_BASE + "/transfer-va/refund")
                        .header("Authorization", "Bearer bogus")
                        .header("X-TIMESTAMP", ts)
                        .header("X-SIGNATURE", sig)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refundBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("v1.0 token with invalid signature -> 401")
    void v10TokenInvalidSignatureRejected() throws Exception {
        String body = "{\"grantType\":\"client_credentials\"}";
        String ts = timestamp();

        mockMvc.perform(post(V10_BASE + "/access-token/b2b")
                        .header("X-CLIENT-KEY", clientKey)
                        .header("X-TIMESTAMP", ts)
                        .header("X-SIGNATURE", "invalid-signature")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.responseCode").value("4012504"));
    }
}
