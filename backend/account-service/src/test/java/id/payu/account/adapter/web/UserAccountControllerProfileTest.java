package id.payu.account.adapter.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.account.domain.model.KycStatus;
import id.payu.account.domain.model.User;
import id.payu.account.domain.model.UserStatus;
import id.payu.account.domain.port.out.UserPersistencePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GRPC-008: inter-service user profile endpoint must exist and return the
 * data lending-service credit scoring relies on (kycStatus, createdAt).
 */
class UserAccountControllerProfileTest {

    private static final String EXTERNAL_ID = "keycloak-sub-123";

    private MockMvc mockMvc;
    private UserPersistencePort userPersistencePort;

    @BeforeEach
    void setUp() {
        userPersistencePort = mock(UserPersistencePort.class);
        UserAccountController controller = new UserAccountController(userPersistencePort, "payu-backend");
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        // Simulate trusted service request for tests
        org.springframework.security.oauth2.jwt.Jwt jwt = org.springframework.security.oauth2.jwt.Jwt
                .withTokenValue("test")
                .header("alg", "RS256")
                .claim("azp", "payu-backend")
                .claim("sub", "test-user")
                .build();
        org.springframework.security.core.context.SecurityContextHolder.getContext()
                .setAuthentication(new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken(jwt));
    }

    private User user() {
        return User.builder()
                .id(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .externalId(EXTERNAL_ID)
                .username("tester")
                .email("tester@payu.id")
                .phoneNumber("08123456789")
                .fullName("Test User")
                .nik("3201010101010001")
                .status(UserStatus.ACTIVE)
                .kycStatus(KycStatus.VERIFIED)
                .createdAt(LocalDateTime.of(2025, 1, 1, 0, 0))
                .updatedAt(LocalDateTime.of(2025, 1, 1, 0, 0))
                .build();
    }

    @Test
    void returnsProfileForKnownExternalId() throws Exception {
        given(userPersistencePort.findByExternalId(EXTERNAL_ID))
                .willReturn(Optional.of(user()));

        mockMvc.perform(get("/api/v1/accounts/users/{userId}", EXTERNAL_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.externalId").value(EXTERNAL_ID))
                .andExpect(jsonPath("$.data.kycStatus").value("VERIFIED"))
                .andExpect(jsonPath("$.data.createdAt").exists());
    }

    @Test
    void returnsNotFoundForUnknownExternalId() throws Exception {
        given(userPersistencePort.findByExternalId("nobody")).willReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/accounts/users/{userId}", "nobody"))
                .andExpect(status().isNotFound());
    }
}
