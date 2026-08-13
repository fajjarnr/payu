package id.payu.wallet.adapter.web;

import id.payu.wallet.domain.model.Wallet;
import id.payu.wallet.application.service.WalletService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * QAMVP-014 (wallet — money service): real security filter chain enforced.
 * Unauthenticated → 401; authenticated but not the resource owner → 403;
 * owner → 200.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = "payu.grpc.server.port=0")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("QAMVP-014 — wallet security: 401/403/RBAC")
class WalletSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WalletService walletUseCase;

    private static final String ACCOUNT_ID = "acct-001";

    @Test
    @DisplayName("unauthenticated money request is rejected with 401")
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/wallets/{accountId}/balance", ACCOUNT_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("authenticated non-owner is rejected with 403")
    void nonOwnerIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/wallets/{accountId}/balance", ACCOUNT_ID)
                        .with(jwt().jwt(j -> j.claim("account_id", "acct-999"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("authenticated owner gets 200")
    void ownerIsAllowed() throws Exception {
        Wallet wallet = Wallet.builder()
                .accountId(ACCOUNT_ID)
                .currency("IDR")
                .balance(new java.math.BigDecimal("50000.0000"))
                .reservedBalance(java.math.BigDecimal.ZERO)
                .build();
        when(walletUseCase.getWalletByAccountId(ACCOUNT_ID)).thenReturn(Optional.of(wallet));

        mockMvc.perform(get("/api/v1/wallets/{accountId}/balance", ACCOUNT_ID)
                        .with(jwt().jwt(j -> j.claim("account_id", ACCOUNT_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accountId").value(ACCOUNT_ID));
    }
}
