package id.payu.account.adapter.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.account.domain.model.KycStatus;
import id.payu.account.domain.model.User;
import id.payu.account.domain.model.UserStatus;
import id.payu.account.domain.port.in.RegisterUserUseCase;
import id.payu.account.dto.RegisterUserRequest;
import id.payu.account.config.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pure unit tests for OnboardingController using standalone MockMvc.
 * No Spring context — avoids @EnableJpaRepositories bootstrap that blocks
 * @WebMvcTest in this project (per READY-045 / L-060).
 *
 * The 403 auth test is @Disabled because standalone MockMvc has no Spring Security
 * filter chain. It can be re-enabled with @SpringBootTest + TestSecurityConfig when
 * the JPA bootstrap blocker is resolved.
 */
@DisplayName("OnboardingController")
class OnboardingControllerTest {

    private MockMvc mockMvc;
    private RegisterUserUseCase registerUserUseCase;
    private ObjectMapper objectMapper;

    private RegisterUserRequest validRequest;
    private User registeredUser;

    @BeforeEach
    void setUp() {
        registerUserUseCase = mock(RegisterUserUseCase.class);
        OnboardingController controller = new OnboardingController(registerUserUseCase);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();

        objectMapper = new ObjectMapper();

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
        @DisplayName("should return 200 OK when registration is successful")
        void shouldReturnOkWhenRegistrationSuccessful() throws Exception {
            given(registerUserUseCase.registerUser(any(RegisterUserRequest.class)))
                    .willReturn(CompletableFuture.completedFuture(registeredUser));

            var mvcResult = mockMvc.perform(post("/api/v1/accounts/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(mvcResult))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value(validRequest.email()))
                    .andExpect(jsonPath("$.username").value(validRequest.username()));
        }

        @Test
        @DisplayName("should return 400 Bad Request when email is invalid")
        void shouldReturnBadRequestWhenEmailInvalid() throws Exception {
            RegisterUserRequest invalidRequest = new RegisterUserRequest(
                    UUID.randomUUID().toString(),
                    "testuser",
                    "invalid-email",
                    "+6281234567890",
                    "John Doe",
                    "3201234567890001",
                    "SecureP@ss123"
            );

            mockMvc.perform(post("/api/v1/accounts/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @Disabled("Requires Spring Security filter chain. Standalone MockMvc has no security. Re-enable with @SpringBootTest + TestSecurityConfig when JPA bootstrap blocker resolved.")
        @DisplayName("should return 403 Forbidden when not authenticated")
        void shouldReturnForbiddenWhenNotAuthenticated() throws Exception {
            mockMvc.perform(post("/api/v1/accounts/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 400 Bad Request when required fields are missing")
        void shouldReturnBadRequestWhenFieldsMissing() throws Exception {
            String incompleteRequest = """
                    {
                        "username": "testuser"
                    }
                    """;

            mockMvc.perform(post("/api/v1/accounts/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(incompleteRequest))
                    .andExpect(status().isBadRequest());
        }
    }
}
