package id.payu.wallet.contract;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import id.payu.wallet.domain.model.Wallet;
import id.payu.wallet.application.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

/**
 * Base class for Spring Cloud Contract Verifier tests for wallet-service.
 * Sets a JWT principal (so {@code verifyAccountOwnership} passes) and mocks
 * {@link WalletUseCase} (external wallet persistence is not exercised during
 * contract verification).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = "payu.grpc.server.port=0")
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureMessageVerifier
@ActiveProfiles("test")
public abstract class ContractVerifierBase {

    protected static final String ACCOUNT_ID = "550e8400-e29b-41d4-a716-446655440000";

    @Autowired
    protected MockMvc mockMvc;

    @MockitoBean
    protected JwtDecoder jwtDecoder;

    @MockitoBean
    protected WalletService walletUseCase;

    @BeforeEach
    void setUpSecurityContext() {
        RestAssuredMockMvc.mockMvc(mockMvc);

        Jwt jwt = Jwt.withTokenValue("contract-token")
                .header("alg", "none")
                .claim("account_id", ACCOUNT_ID)
                .claim("sub", ACCOUNT_ID)
                .claim("azp", "payu-backend")
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        Wallet wallet = Wallet.builder()
                .accountId(ACCOUNT_ID)
                .currency("IDR")
                .balance(new BigDecimal("50000.0000"))
                .reservedBalance(BigDecimal.ZERO)
                .build();
        given(walletUseCase.getWalletByAccountId(anyString())).willReturn(Optional.empty());
        given(walletUseCase.getWalletByAccountId(ACCOUNT_ID)).willReturn(Optional.of(wallet));
    }
}
