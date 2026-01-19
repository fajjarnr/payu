package id.payu.account.adapter.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.account.domain.model.User;
import id.payu.account.domain.port.in.RegisterUserUseCase;
import id.payu.account.dto.RegisterUserRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OnboardingController.class)
@DisplayName("OnboardingController")
class OnboardingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RegisterUserUseCase registerUserUseCase;

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
                "3201234567890001"
        );

        registeredUser = User.builder()
                .id(UUID.randomUUID())
                .externalId(validRequest.externalId())
                .username(validRequest.username())
                .email(validRequest.email())
                .phoneNumber(validRequest.phoneNumber())
                .fullName(validRequest.fullName())
                .nik(validRequest.nik())
                .status(User.UserStatus.ACTIVE)
                .kycStatus(User.KycStatus.APPROVED)
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
                    "3201234567890001"
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
