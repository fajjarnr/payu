package id.payu.auth.contract;

import id.payu.auth.adapter.security.KeycloakService;
import id.payu.auth.dto.LoginResponse;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

/**
 * Base class for Spring Cloud Contract Verifier tests for auth-service.
 * Provides MockMvc setup and mocks KeycloakService since the real
 * Keycloak instance is not available during contract verification.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@AutoConfigureMessageVerifier
@ActiveProfiles("test")
public abstract class ContractVerifierBase {

    @Autowired
    protected MockMvc mockMvc;

    @MockBean
    protected KeycloakService keycloakService;

    @BeforeEach
    void setUpContractMocks() {
        // Wire RestAssuredMockMvc to the Spring MockMvc instance
        RestAssuredMockMvc.mockMvc(mockMvc);

        // Stub KeycloakService to return a successful login response for any valid-looking credentials.
        // The contract sends anyNonBlankString() for username/password, so we always return success.
        given(keycloakService.loginBlocking(anyString(), anyString()))
                .willReturn(new LoginResponse(
                        "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.test-access-token",
                        "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.test-refresh-token",
                        3600L,
                        "Bearer"
                ));
    }
}
