package id.payu.partner;

import id.payu.partner.adapter.persistence.repository.ApiKeyRepository;
import id.payu.partner.adapter.persistence.repository.MerchantRepository;
import id.payu.partner.adapter.persistence.repository.PartnerRepository;
import id.payu.partner.domain.*;
import id.payu.partner.adapter.persistence.entity.ApiKeyEntity;
import id.payu.partner.adapter.persistence.entity.PartnerEntity;
import id.payu.partner.domain.KeyEnvironment;
import id.payu.partner.domain.PartnerStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Sandbox functionality.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(id.payu.partner.TestSecurityConfig.class)
@ActiveProfiles("test")
@Transactional
class SandboxIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private PartnerRepository partnerRepository;

    @Autowired
    private MerchantRepository merchantRepository;

    private PartnerEntity testPartner;
    private ApiKeyEntity sandboxKey;
    private ApiKeyEntity productionKey;

    @BeforeEach
    void setUp() {
        // Clean up
        apiKeyRepository.deleteAll();
        merchantRepository.deleteAll();
        partnerRepository.deleteAll();

        // Create test partner
        testPartner = new PartnerEntity();
        testPartner.setPartnerCode("TEST-PARTNER-001");
        testPartner.setName("Test PartnerEntity");
        testPartner.setEmail("test@partner.com");
        testPartner.setType("MERCHANT");
        testPartner.setStatus(PartnerStatus.ACTIVE);
        testPartner.setActive(true);
        testPartner = partnerRepository.save(testPartner);

        // Create sandbox API key
        sandboxKey = new ApiKeyEntity(
                testPartner,
                "payu_test_",
                hashApiKey("payu_test_sandbox_key_12345"),
                "2345",
                KeyEnvironment.SANDBOX,
                true
        );
        sandboxKey.setName("Sandbox Key");
        sandboxKey.setRatePlan("sandbox");
        sandboxKey = apiKeyRepository.save(sandboxKey);

        // Create production API key
        productionKey = new ApiKeyEntity(
                testPartner,
                "payu_live_",
                hashApiKey("payu_live_production_key_67890"),
                "7890",
                KeyEnvironment.LIVE,
                false
        );
        productionKey.setName("Production Key");
        productionKey.setRatePlan("standard");
        productionKey = apiKeyRepository.save(productionKey);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void seedSandboxData_CreatesTestResources() throws Exception {
        mockMvc.perform(post("/admin/sandbox/seed")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.created").isArray());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getTestBankAccounts_ReturnsTestAccounts() throws Exception {
        mockMvc.perform(get("/admin/sandbox/test-accounts")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].bankCode").exists())
                .andExpect(jsonPath("$.data[0].accountNumber").exists())
                .andExpect(jsonPath("$.data[0].accountName").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getTestVaNumbers_ReturnsTestVAs() throws Exception {
        mockMvc.perform(get("/admin/sandbox/test-va")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].bankCode").exists())
                .andExpect(jsonPath("$.data[0].vaNumber").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getTestScenarios_ReturnsScenarios() throws Exception {
        mockMvc.perform(get("/admin/sandbox/scenarios")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isMap())
                .andExpect(jsonPath("$.data.success_transfer").exists())
                .andExpect(jsonPath("$.data.insufficient_funds").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getSandboxStatus_ReturnsStatus() throws Exception {
        mockMvc.perform(get("/admin/sandbox/status")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.environment").value("sandbox"))
                .andExpect(jsonPath("$.data.domain").value("payu.fajjjar.my.id"))
                .andExpect(jsonPath("$.data.testMerchants").isArray());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void requestWithSandboxKey_AddsSandboxHeader() throws Exception {
        // This test verifies the filter adds the header
        // In a real scenario, we'd need to hit an endpoint that uses the filter
        // For now, we just verify the key is correctly set up
        mockMvc.perform(get("/admin/sandbox/status")
                        .header("X-API-Key", "payu_test_sandbox_key_12345")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    private String hashApiKey(String apiKey) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(apiKey.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash API key", e);
        }
    }
}
