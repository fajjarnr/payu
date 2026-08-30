package id.payu.partner.adapter.web;

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
import id.payu.partner.application.service.WebhookDispatcherService;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PAYU-TB-002: SNAP-BI contract test for TokoBapak Go client.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
class SnapBiTokoBapakContractTest {

    private static final String V10_BASE = "/v1.0";
    private static final String TOKOBAPAK_CLIENT_ID = "tokobapak-mvp";
    private static final String TOKOBAPAK_CLIENT_SECRET = "tokobapak-mvp-dev-secret-32chars-long!";
    private static final String SOURCE_ACC = "ACC_TOKOBAPAK_ESCROW";
    private static final String BENEFICIARY_ACC = "ACC_SELLER_001";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private SnapBiSignatureService signatureService;
    @Autowired private PartnerRepository partnerRepository;
    @Autowired private SnapBiPaymentRepository paymentRepository;
    @Autowired private SnapBiRefundRepository refundRepository;
    @Autowired private WebhookSubscriptionRepository webhookSubscriptionRepository;

    @MockitoBean private WalletSettlementPort walletSettlementPort;
    @MockitoBean private ApiKeyService apiKeyService;
    @MockitoBean private SnapBiTokenService tokenService;
    @MockitoBean private id.payu.outbox.service.OutboxService outboxService;
    @MockitoBean private WebhookDispatcherService webhookDispatcherService;

    private PartnerEntity partner;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        refundRepository.deleteAll();
        webhookSubscriptionRepository.deleteAll();
        partnerRepository.deleteAll();

        partner = new PartnerEntity();
        partner.setPartnerCode("TOKOBAPAK_MVP");
        partner.setName("TokoBapak MVP");
        partner.setEmail("tokobapak@payu.test");
        partner.setType("SANDBOX");
        partner.setStatus(PartnerStatus.ACTIVE);
        partner.setActive(true);
        partner.setClientId(TOKOBAPAK_CLIENT_ID);
        partner.setClientSecret(TOKOBAPAK_CLIENT_SECRET);
        partner.setWebhookUrl("http://tokobapak-notification-service:3009/v1/webhooks/payu");
        partner.setTenantId("tokobapak");
        partner = partnerRepository.save(partner);

        org.mockito.Mockito.when(tokenService.generateAccessToken(eq(TOKOBAPAK_CLIENT_ID), eq(partner.getId().toString()), anyString()))
                .thenReturn("tokobapak-token-" + partner.getId());
        org.mockito.Mockito.when(tokenService.getClientIdFromToken(eq("tokobapak-token-" + partner.getId())))
                .thenReturn(TOKOBAPAK_CLIENT_ID);
        org.mockito.Mockito.when(tokenService.getClientIdFromToken(anyString())).thenAnswer(inv -> {
            String tok = inv.getArgument(0);
            if (tok != null && tok.equals("tokobapak-token-" + partner.getId())) return TOKOBAPAK_CLIENT_ID;
            return null;
        });
    }

    private String timestamp() {
        return Instant.now().toString().replaceAll("\\.\\d+", "");
    }

    private String obtainAccessToken() throws Exception {
        String body = "{\"grantType\":\"client_credentials\"}";
        String ts = timestamp();
        String sig = signatureService.generateSignatureWithClientKey(TOKOBAPAK_CLIENT_SECRET, "POST", V10_BASE + "/access-token/b2b", ts, body);
        String resp = mockMvc.perform(post(V10_BASE + "/access-token/b2b")
                        .header("X-CLIENT-KEY", TOKOBAPAK_CLIENT_ID)
                        .header("X-TIMESTAMP", ts)
                        .header("X-SIGNATURE", sig)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(resp).get("accessToken").asText();
    }

    @Test
    @DisplayName("TokoBapak Go client \u2014 valid payment with source/beneficiary \u2192 2002500 + settlement + outbox")
    void tokobapakValidPaymentReturns2002500AndPublishes() throws Exception {
        String accessToken = obtainAccessToken();
        String partnerRef = "TOKOBAPAK-ORDER-" + System.currentTimeMillis();
        String paymentBody = """
                {"partnerReferenceNo":"%s","amount":{"value":"110000","currency":"IDR"},"sourceAccountNo":"%s","beneficiaryAccountNo":"%s","beneficiaryBankCode":"014"}
                """.formatted(partnerRef, SOURCE_ACC, BENEFICIARY_ACC);
        String ts = timestamp();
        String sig = signatureService.generateSignature(TOKOBAPAK_CLIENT_SECRET, "POST", V10_BASE + "/transfer-va/payment", accessToken, paymentBody, ts);

        String firstResp = mockMvc.perform(post(V10_BASE + "/transfer-va/payment")
                        .header("Authorization", "Bearer " + accessToken)
                        .header("X-EXTERNAL-ID", "EXT-" + partnerRef)
                        .header("X-TIMESTAMP", ts)
                        .header("X-SIGNATURE", sig)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("2002500"))
                .andExpect(jsonPath("$.partnerReferenceNo").value(partnerRef))
                .andExpect(jsonPath("$.referenceNo").exists())
                .andReturn().getResponse().getContentAsString();

        String payuRef = objectMapper.readTree(firstResp).get("referenceNo").asText();

        verify(walletSettlementPort).settle(eq(SOURCE_ACC), eq(BENEFICIARY_ACC), any(), eq("IDR"), eq(payuRef));
        verify(outboxService).createEvent(eq("SnapBiPayment"), eq(payuRef), eq("PaymentCompleted"), anyMap(), isNull(), eq("payu.partner.payment-completed.v1"));

        String ts2 = timestamp();
        String sig2 = signatureService.generateSignature(TOKOBAPAK_CLIENT_SECRET, "POST", V10_BASE + "/transfer-va/payment", accessToken, paymentBody, ts2);
        mockMvc.perform(post(V10_BASE + "/transfer-va/payment")
                        .header("Authorization", "Bearer " + accessToken)
                        .header("X-EXTERNAL-ID", "EXT-" + partnerRef + "-replay")
                        .header("X-TIMESTAMP", ts2)
                        .header("X-SIGNATURE", sig2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("2002500"))
                .andExpect(jsonPath("$.referenceNo").value(payuRef));
    }

    @Test
    @DisplayName("TokoBapak Go client \u2014 missing sourceAccountNo \u2192 4002501")
    void tokobapakMissingSourceReturns4002501() throws Exception {
        String accessToken = obtainAccessToken();
        String paymentBody = """
                {"partnerReferenceNo":"REF-MISS-SRC","amount":{"value":"50000","currency":"IDR"},"beneficiaryAccountNo":"%s","beneficiaryBankCode":"014"}
                """.formatted(BENEFICIARY_ACC);
        String ts = timestamp();
        String sig = signatureService.generateSignature(TOKOBAPAK_CLIENT_SECRET, "POST", V10_BASE + "/transfer-va/payment", accessToken, paymentBody, ts);

        mockMvc.perform(post(V10_BASE + "/transfer-va/payment")
                        .header("Authorization", "Bearer " + accessToken)
                        .header("X-EXTERNAL-ID", "EXT-MISS-SRC")
                        .header("X-TIMESTAMP", ts)
                        .header("X-SIGNATURE", sig)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("4002501"));
    }

    @Test
    @DisplayName("TokoBapak Go client \u2014 missing beneficiaryAccountNo \u2192 4002501")
    void tokobapakMissingBeneficiaryReturns4002501() throws Exception {
        String accessToken = obtainAccessToken();
        String paymentBody = """
                {"partnerReferenceNo":"REF-MISS-BEN","amount":{"value":"50000","currency":"IDR"},"sourceAccountNo":"%s","beneficiaryBankCode":"014"}
                """.formatted(SOURCE_ACC);
        String ts = timestamp();
        String sig = signatureService.generateSignature(TOKOBAPAK_CLIENT_SECRET, "POST", V10_BASE + "/transfer-va/payment", accessToken, paymentBody, ts);

        mockMvc.perform(post(V10_BASE + "/transfer-va/payment")
                        .header("Authorization", "Bearer " + accessToken)
                        .header("X-EXTERNAL-ID", "EXT-MISS-BEN")
                        .header("X-TIMESTAMP", ts)
                        .header("X-SIGNATURE", sig)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("4002501"));
    }

    @Test
    @DisplayName("TokoBapak Go client \u2014 missing partnerReferenceNo \u2192 4002501")
    void tokobapakMissingReferenceReturns4002501() throws Exception {
        String accessToken = obtainAccessToken();
        String paymentBody = """
                {"amount":{"value":"50000","currency":"IDR"},"sourceAccountNo":"%s","beneficiaryAccountNo":"%s"}
                """.formatted(SOURCE_ACC, BENEFICIARY_ACC);
        String ts = timestamp();
        String sig = signatureService.generateSignature(TOKOBAPAK_CLIENT_SECRET, "POST", V10_BASE + "/transfer-va/payment", accessToken, paymentBody, ts);

        mockMvc.perform(post(V10_BASE + "/transfer-va/payment")
                        .header("Authorization", "Bearer " + accessToken)
                        .header("X-EXTERNAL-ID", "EXT-MISS-REF")
                        .header("X-TIMESTAMP", ts)
                        .header("X-SIGNATURE", sig)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("4002501"));
    }

    @Test
    @DisplayName("TokoBapak Go client \u2014 non-IDR currency \u2192 4002502")
    void tokobapakNonIdrReturns4002502() throws Exception {
        String accessToken = obtainAccessToken();
        String paymentBody = """
                {"partnerReferenceNo":"REF-USD","amount":{"value":"100","currency":"USD"},"sourceAccountNo":"%s","beneficiaryAccountNo":"%s"}
                """.formatted(SOURCE_ACC, BENEFICIARY_ACC);
        String ts = timestamp();
        String sig = signatureService.generateSignature(TOKOBAPAK_CLIENT_SECRET, "POST", V10_BASE + "/transfer-va/payment", accessToken, paymentBody, ts);

        mockMvc.perform(post(V10_BASE + "/transfer-va/payment")
                        .header("Authorization", "Bearer " + accessToken)
                        .header("X-EXTERNAL-ID", "EXT-USD")
                        .header("X-TIMESTAMP", ts)
                        .header("X-SIGNATURE", sig)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("4002502"));
    }
}
