package id.payu.backoffice.adapter.web;

import id.payu.backoffice.application.service.KycReviewService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import id.payu.backoffice.config.TestSecurityConfig;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * QAMVP-014 (backoffice): unauthenticated → 401; authenticated without
 * admin/backoffice authority → 403 (RBAC); with authority → request proceeds.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@DisplayName("QAMVP-014 — backoffice security: 401/403 RBAC")
class BackofficeSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private KycReviewService kycReviewService;

    @Test
    @DisplayName("unauthenticated request is rejected with 401")
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/backoffice/kyc-reviews/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("authenticated user without admin/backoffice authority is rejected with 403")
    void missingAuthorityIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/backoffice/kyc-reviews/{id}", UUID.randomUUID())
                        .with(jwt().authorities(() -> "ROLE_USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("authenticated backoffice user reaches the endpoint (RBAC allows)")
    void authorizedRequestProceeds() throws Exception {
        when(kycReviewService.getById(any(UUID.class))).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/backoffice/kyc-reviews/{id}", UUID.randomUUID())
                        .with(jwt().authorities(() -> "backoffice")))
                .andExpect(status().isNotFound());
    }
}
