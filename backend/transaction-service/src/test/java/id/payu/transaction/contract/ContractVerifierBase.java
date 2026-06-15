package id.payu.transaction.contract;

import id.payu.transaction.application.cqrs.command.InitiateTransferCommandResult;
import id.payu.transaction.domain.port.in.TransactionUseCase;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.cloud.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

/**
 * Base class for Spring Cloud Contract Verifier tests for transaction-service.
 * <p>
 * Sets up a mock JWT authentication in the security context so that
 * {@code @PreAuthorize} annotations and {@code extractUserId()} work correctly.
 * Also mocks {@link TransactionUseCase} since external dependencies (wallet-service, BI-FAST)
 * are not available during contract verification.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc(addFilters = false) // Bypass security filter chain; auth set via @BeforeEach
@AutoConfigureMessageVerifier
@ActiveProfiles("test")
public abstract class ContractVerifierBase {

    protected static final String TEST_ACCOUNT_ID = "550e8400-e29b-41d4-a716-446655440000";

    @Autowired
    protected MockMvc mockMvc;

    @MockitoBean
    protected TransactionUseCase transactionUseCase;

    @BeforeEach
    void setUpSecurityContext() {
        Jwt mockJwt = Jwt.withTokenValue("test-contract-token")
                .header("alg", "RS256")
                .claim("account_id", TEST_ACCOUNT_ID)
                .claim("sub", TEST_ACCOUNT_ID)
                .claim("scope", "write:transaction read:transaction write:payment")
                .issuedAt(java.time.Instant.now())
                .expiresAt(java.time.Instant.now().plusSeconds(3600))
                .build();
        Authentication authentication = new JwtAuthenticationToken(
                mockJwt,
                java.util.List.of(
                        new SimpleGrantedAuthority("write:transaction"),
                        new SimpleGrantedAuthority("read:transaction"),
                        new SimpleGrantedAuthority("write:payment")
                )
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Wire RestAssuredMockMvc to the Spring MockMvc instance for contract tests
        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    @BeforeEach
    void setUpContractMocks() {
        // Stub TransactionUseCase to return a successful transfer for any request
        given(transactionUseCase.initiateTransfer(any(id.payu.transaction.dto.InitiateTransferRequest.class), anyString()))
                .willReturn(new InitiateTransferCommandResult(
                        UUID.randomUUID(),
                        "REF-" + UUID.randomUUID().toString().substring(0, 8),
                        "PENDING",
                        BigDecimal.ZERO,
                        "2 seconds"
                ));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }
}
