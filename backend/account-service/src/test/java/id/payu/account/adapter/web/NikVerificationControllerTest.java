package id.payu.account.adapter.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import id.payu.account.config.GlobalExceptionHandler;
import id.payu.account.domain.port.in.VerifyNikUseCase;
import id.payu.account.dto.VerifyNikRequest;
import id.payu.account.dto.VerifyNikResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pure unit tests for NikVerificationController using standalone MockMvc.
 * No Spring context — avoids @EnableJpaRepositories bootstrap (per READY-045 / L-060).
 *
 * Auth tests (401, 403) are @Disabled because standalone MockMvc has no security.
 */
@DisplayName("NikVerificationController")
class NikVerificationControllerTest {

    private MockMvc mockMvc;
    private VerifyNikUseCase verifyNikUseCase;
    private ObjectMapper objectMapper;

    private VerifyNikRequest validRequest;
    private VerifyNikResponse successResponse;
    private VerifyNikResponse notFoundResponse;
    private VerifyNikResponse serviceUnavailableResponse;

    @BeforeEach
    void setUp() {
        verifyNikUseCase = mock(VerifyNikUseCase.class);
        NikVerificationController controller = new NikVerificationController(verifyNikUseCase);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        validRequest = new VerifyNikRequest(
                "3201234567890001",
                "John Doe",
                "Jakarta",
                "1990-01-15"
        );

        successResponse = VerifyNikResponse.success(
                UUID.randomUUID().toString(),
                "3201234567890001",
                true,
                "John Doe",
                "Jakarta",
                LocalDate.of(1990, 1, 15),
                "MALE",
                "Jl. Test No. 123",
                "ACTIVE"
        );

        notFoundResponse = VerifyNikResponse.notFound(
                UUID.randomUUID().toString(),
                "3201234567890001"
        );

        serviceUnavailableResponse = VerifyNikResponse.serviceUnavailable(
                UUID.randomUUID().toString()
        );
    }

    @Nested
    @DisplayName("POST /api/v1/accounts/verify-nik")
    class VerifyNikEndpoint {

        @Test
        @DisplayName("should return 200 OK when NIK verification is successful")
        void shouldReturnOkWhenVerificationSuccessful() throws Exception {
            given(verifyNikUseCase.verifyNik(any(VerifyNikRequest.class)))
                    .willReturn(CompletableFuture.completedFuture(successResponse));

            var mvcResult = mockMvc.perform(post("/api/v1/accounts/verify-nik")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(mvcResult))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.requestId").exists())
                    .andExpect(jsonPath("$.nik").value("3201234567890001"))
                    .andExpect(jsonPath("$.verified").value(true))
                    .andExpect(jsonPath("$.fullName").value("John Doe"))
                    .andExpect(jsonPath("$.responseCode").value("00"));
        }

        @Test
        @DisplayName("should return 200 OK when NIK is not found")
        void shouldReturnOkWhenNikNotFound() throws Exception {
            given(verifyNikUseCase.verifyNik(any(VerifyNikRequest.class)))
                    .willReturn(CompletableFuture.completedFuture(notFoundResponse));

            var mvcResult = mockMvc.perform(post("/api/v1/accounts/verify-nik")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(mvcResult))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.verified").value(false))
                    .andExpect(jsonPath("$.responseCode").value("14"))
                    .andExpect(jsonPath("$.responseMessage").value("NIK not found in Dukcapil database"));
        }

        @Test
        @DisplayName("should return 200 OK when Dukcapil service is unavailable (fallback)")
        void shouldReturnOkWhenServiceUnavailable() throws Exception {
            given(verifyNikUseCase.verifyNik(any(VerifyNikRequest.class)))
                    .willReturn(CompletableFuture.completedFuture(serviceUnavailableResponse));

            var mvcResult = mockMvc.perform(post("/api/v1/accounts/verify-nik")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(mvcResult))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.verified").value(false))
                    .andExpect(jsonPath("$.responseCode").value("503"))
                    .andExpect(jsonPath("$.responseMessage").value("Dukcapil service is temporarily unavailable"));
        }

        @Test
        @DisplayName("should return 400 Bad Request when NIK format is invalid")
        void shouldReturnBadRequestWhenNikInvalid() throws Exception {
            VerifyNikRequest invalidRequest = new VerifyNikRequest(
                    "12345",
                    "John Doe",
                    "Jakarta",
                    "1990-01-15"
            );

            mockMvc.perform(post("/api/v1/accounts/verify-nik")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 Bad Request when birth date format is invalid")
        void shouldReturnBadRequestWhenBirthDateInvalid() throws Exception {
            VerifyNikRequest invalidRequest = new VerifyNikRequest(
                    "3201234567890001",
                    "John Doe",
                    "Jakarta",
                    "15-01-1990"
            );

            mockMvc.perform(post("/api/v1/accounts/verify-nik")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 Bad Request when required fields are missing")
        void shouldReturnBadRequestWhenFieldsMissing() throws Exception {
            String incompleteRequest = """
                    {
                        "nik": "3201234567890001"
                    }
                    """;

            mockMvc.perform(post("/api/v1/accounts/verify-nik")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(incompleteRequest))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @Disabled("Requires Spring Security filter chain. Standalone MockMvc has no security. Re-enable with @SpringBootTest + TestSecurityConfig when JPA bootstrap blocker resolved.")
        @DisplayName("should return 401 Unauthorized when not authenticated")
        void shouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {
            mockMvc.perform(post("/api/v1/accounts/verify-nik")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @Disabled("Requires Spring Security filter chain. Standalone MockMvc has no security.")
        @DisplayName("should return 403 Forbidden when missing required scope")
        void shouldReturnForbiddenWhenMissingScope() throws Exception {
            mockMvc.perform(post("/api/v1/accounts/verify-nik")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 400 Bad Request when NIK contains non-digits")
        void shouldReturnBadRequestWhenNikContainsNonDigits() throws Exception {
            VerifyNikRequest invalidRequest = new VerifyNikRequest(
                    "3201abcd56789001",
                    "John Doe",
                    "Jakarta",
                    "1990-01-15"
            );

            mockMvc.perform(post("/api/v1/accounts/verify-nik")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 Bad Request when full name is missing")
        void shouldReturnBadRequestWhenFullNameMissing() throws Exception {
            VerifyNikRequest invalidRequest = new VerifyNikRequest(
                    "3201234567890001",
                    "",
                    "Jakarta",
                    "1990-01-15"
            );

            mockMvc.perform(post("/api/v1/accounts/verify-nik")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should handle optional birth place field correctly")
        void shouldHandleOptionalBirthPlace() throws Exception {
            VerifyNikRequest requestWithoutBirthPlace = new VerifyNikRequest(
                    "3201234567890001",
                    "John Doe",
                    null,
                    "1990-01-15"
            );

            given(verifyNikUseCase.verifyNik(any(VerifyNikRequest.class)))
                    .willReturn(CompletableFuture.completedFuture(successResponse));

            var mvcResult = mockMvc.perform(post("/api/v1/accounts/verify-nik")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestWithoutBirthPlace)))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(mvcResult))
                    .andExpect(status().isOk());
        }
    }
}
