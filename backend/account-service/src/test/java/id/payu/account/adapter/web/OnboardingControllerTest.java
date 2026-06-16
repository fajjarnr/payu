package id.payu.account.adapter.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.account.domain.model.KycStatus;
import id.payu.account.domain.model.User;
import id.payu.account.domain.model.UserStatus;
import id.payu.account.domain.port.in.RegisterUserUseCase;
import id.payu.account.dto.RegisterUserRequest;
import id.payu.outbox.service.OutboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Disabled("READY-045 web-slice JPA bootstrap issue: AccountServiceApplication @EnableJpaRepositories forces JPA bootstrap even with auto-config excludes. Needs test-specific @ContextConfiguration without @EnableJpaRepositories. Re-enable in future sprint.")
@SpringBootTest(
    properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                + "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration,"
                + "org.springframework.boot.data.jpa.autoconfigure.JpaRepositoriesAutoConfiguration,"
                + "org.springframework.cloud.vault.core.VaultAutoConfiguration,"
                + "id.payu.outbox.config.OutboxAutoConfiguration"
    }
)
@AutoConfigureMockMvc
@Import(id.payu.account.config.TestSecurityConfig.class)
@ActiveProfiles("test")
@DisplayName("OnboardingController")
class OnboardingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RegisterUserUseCase registerUserUseCase;

    // Mock security beans
    @MockitoBean
    private JwtDecoder jwtDecoder;

    // Mock shared library dependencies
    @MockitoBean(name = "cacheInvalidationPublisher")
    private Object cacheInvalidationPublisher;

    // Mock KafkaTemplate + OutboxService for outbox-based KafkaUserEventPublisherAdapter
    @MockitoBean
    private KafkaTemplate<Object, Object> kafkaTemplate;

    @MockitoBean
    private OutboxService outboxService;

    private RegisterUserRequest validRequest;
    private User registeredUser;

    @BeforeEach
    void setUp() {
        validRequest = new RegisterUserRequest(
                UUID.randomUUID().toString(),
                "testuser",
                "test@example.com",
                "+6281234567890",
                "John Doe",
                "3201234567890001",
                "SecureP@ss123"
        );

        registeredUser = User.builder()
                .id(UUID.randomUUID())
                .externalId(validRequest.externalId())
                .username(validRequest.username())
                .email(validRequest.email())
                .phoneNumber(validRequest.phoneNumber())
                .fullName(validRequest.fullName())
                .nik(validRequest.nik())
                .status(UserStatus.ACTIVE)
                .kycStatus(KycStatus.APPROVED)
                .build();
    }

    @Nested
    @DisplayName("POST /api/v1/accounts/register")
    class RegisterEndpoint {

        @Test
        @WithMockUser
        @DisplayName("should return 200 OK when registration is successful")
        void shouldReturnOkWhenRegistrationSuccessful() throws Exception {
            // Given
            given(registerUserUseCase.registerUser(any(RegisterUserRequest.class)))
                    .willReturn(CompletableFuture.completedFuture(registeredUser));

            // When - start async request
            var mvcResult = mockMvc.perform(post("/api/v1/accounts/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            // Then - wait for async result
            mockMvc.perform(asyncDispatch(mvcResult))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value(validRequest.email()))
                    .andExpect(jsonPath("$.username").value(validRequest.username()));
                    // Enum handling might differ in JSON (string representation check)
        }

        @Test
        @WithMockUser
        @DisplayName("should return 400 Bad Request when email is invalid")
        void shouldReturnBadRequestWhenEmailInvalid() throws Exception {
            // Given - invalid email format
            RegisterUserRequest invalidRequest = new RegisterUserRequest(
                    UUID.randomUUID().toString(),
                    "testuser",
                    "invalid-email",
                    "+6281234567890",
                    "John Doe",
                    "3201234567890001",
                    "SecureP@ss123"
            );

            // When/Then
            mockMvc.perform(post("/api/v1/accounts/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 403 Forbidden when not authenticated")
        void shouldReturnForbiddenWhenNotAuthenticated() throws Exception {
            // When/Then
            mockMvc.perform(post("/api/v1/accounts/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser
        @DisplayName("should return 400 Bad Request when required fields are missing")
        void shouldReturnBadRequestWhenFieldsMissing() throws Exception {
            // Given - missing required fields
            String incompleteRequest = """
                    {
                        "username": "testuser"
                    }
                    """;

            // When/Then
            mockMvc.perform(post("/api/v1/accounts/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(incompleteRequest))
                    .andExpect(status().isBadRequest());
        }
    }
}
