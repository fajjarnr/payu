package id.payu.wallet.contract;

import id.payu.wallet.adapter.grpc.WalletGrpcService;
import id.payu.wallet.application.service.WalletService;
import id.payu.wallet.domain.model.Wallet;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

/**
 * Base class for Spring Cloud Contract Verifier tests for wallet-service.
 * <p>
 * Sets up a mock JWT authentication in the security context so that
 * {@code @PreAuthorize("isAuthenticated()")} and {@code verifyAccountOwnership()}
 * work correctly. Also mocks {@link WalletUseCase} since external dependencies
 * (database, Kafka) are not available during contract verification.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc(addFilters = false) // Bypass security filter chain; auth set via @BeforeEach
@AutoConfigureMessageVerifier
@ActiveProfiles("test")
public abstract class ContractVerifierBase {

    protected static final String TEST_ACCOUNT_ID = "550e8400-e29b-41d4-a716-446655440000";

    @Autowired
    protected MockMvc mockMvc;

    @MockBean
    protected WalletService walletService;

    @MockBean
    protected WalletGrpcService walletGrpcService;

    @BeforeEach
    void setUpSecurityContext() {
        Jwt mockJwt = Jwt.withTokenValue("test-contract-token")
                .header("alg", "RS256")
                .claim("account_id", TEST_ACCOUNT_ID)
                .claim("sub", TEST_ACCOUNT_ID)
                .issuedAt(java.time.Instant.now())
                .expiresAt(java.time.Instant.now().plusSeconds(3600))
                .build();
        Authentication authentication = new JwtAuthenticationToken(
                mockJwt,
                java.util.List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Wire RestAssuredMockMvc to the Spring MockMvc instance for contract tests
        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    @BeforeEach
    void setUpContractMocks() {
        // Stub WalletService to return a test wallet for any account ID lookup.
        // The contract uses a regex URL to match any UUID in the path.
        Wallet testWallet = Wallet.builder()
                .id(UUID.randomUUID())
                .accountId(TEST_ACCOUNT_ID)
                .balance(new BigDecimal("10000000"))
                .reservedBalance(new BigDecimal("5000000"))
                .currency("IDR")
                .status(Wallet.WalletStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        given(walletService.getWalletByAccountId(TEST_ACCOUNT_ID))
                .willReturn(Optional.of(testWallet));

        // Also stub any account ID lookup as a fallback for the regex URL match
        given(walletService.getWalletByAccountId(anyString()))
                .willReturn(Optional.of(testWallet));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }
}
