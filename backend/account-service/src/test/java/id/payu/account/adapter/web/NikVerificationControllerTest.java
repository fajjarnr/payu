package id.payu.account.adapter.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.account.domain.port.in.VerifyNikUseCase;
import id.payu.account.dto.VerifyNikRequest;
import id.payu.account.dto.VerifyNikResponse;
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

import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NikVerificationController.class)
@DisplayName("NikVerificationController")
class NikVerificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private VerifyNikUseCase verifyNikUseCase;

    private VerifyNikRequest validRequest;
    private VerifyNikResponse successResponse;
    private VerifyNikResponse notFoundResponse;
    private VerifyNikResponse serviceUnavailableResponse;

    @BeforeEach
    void setUp() {
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
        @WithMockUser(authorities = {"SCOPE_account:verify"})
        @DisplayName("should return 200 OK when NIK verification is successful")
        void shouldReturnOkWhenVerificationSuccessful() throws Exception {
            // Given
            given(verifyNikUseCase.verifyNik(any(VerifyNikRequest.class)))
                .willReturn(CompletableFuture.completedFuture(successResponse));

            // When - start async request
            var mvcResult = mockMvc.perform(post("/api/v1/accounts/verify-nik")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(request().asyncStarted())
                .andReturn();

            // Then - wait for async result
            mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").exists())
                .andExpect(jsonPath("$.nik").value("3201234567890001"))
                .andExpect(jsonPath("$.verified").value(true))
                .andExpect(jsonPath("$.fullName").value("John Doe"))
                .andExpect(jsonPath("$.responseCode").value("00"));
        }

        @Test
        @WithMockUser(authorities = {"SCOPE_account:verify"})
        @DisplayName("should return 200 OK when NIK is not found")
        void shouldReturnOkWhenNikNotFound() throws Exception {
            // Given
            given(verifyNikUseCase.verifyNik(any(VerifyNikRequest.class)))
                .willReturn(CompletableFuture.completedFuture(notFoundResponse));

            // When - start async request
            var mvcResult = mockMvc.perform(post("/api/v1/accounts/verify-nik")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(request().asyncStarted())
                .andReturn();

            // Then - wait for async result
            mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(false))
                .andExpect(jsonPath("$.responseCode").value("14"))
                .andExpect(jsonPath("$.responseMessage").value("NIK not found in Dukcapil database"));
        }

        @Test
        @WithMockUser(authorities = {"SCOPE_account:verify"})
        @DisplayName("should return 200 OK when Dukcapil service is unavailable (fallback)")
        void shouldReturnOkWhenServiceUnavailable() throws Exception {
            // Given
            given(verifyNikUseCase.verifyNik(any(VerifyNikRequest.class)))
                .willReturn(CompletableFuture.completedFuture(serviceUnavailableResponse));

            // When - start async request
            var mvcResult = mockMvc.perform(post("/api/v1/accounts/verify-nik")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(request().asyncStarted())
                .andReturn();

            // Then - wait for async result
            mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(false))
                .andExpect(jsonPath("$.responseCode").value("503"))
                .andExpect(jsonPath("$.responseMessage").value("Dukcapil service is temporarily unavailable"));
        }

        @Test
        @WithMockUser(authorities = {"SCOPE_account:verify"})
        @DisplayName("should return 400 Bad Request when NIK format is invalid")
        void shouldReturnBadRequestWhenNikInvalid() throws Exception {
            // Given - invalid NIK format (less than 16 digits)
            VerifyNikRequest invalidRequest = new VerifyNikRequest(
                "12345",
                "John Doe",
                "Jakarta",
                "1990-01-15"
            );

            // When/Then
            mockMvc.perform(post("/api/v1/accounts/verify-nik")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(authorities = {"SCOPE_account:verify"})
        @DisplayName("should return 400 Bad Request when birth date format is invalid")
        void shouldReturnBadRequestWhenBirthDateInvalid() throws Exception {
            // Given - invalid birth date format
            VerifyNikRequest invalidRequest = new VerifyNikRequest(
                "3201234567890001",
                "John Doe",
                "Jakarta",
                "15-01-1990"  // Wrong format
            );

            // When/Then
            mockMvc.perform(post("/api/v1/accounts/verify-nik")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(authorities = {"SCOPE_account:verify"})
        @DisplayName("should return 400 Bad Request when required fields are missing")
        void shouldReturnBadRequestWhenFieldsMissing() throws Exception {
            // Given - missing required fields
            String incompleteRequest = """
                {
                    "nik": "3201234567890001"
                }
                """;

            // When/Then
            mockMvc.perform(post("/api/v1/accounts/verify-nik")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(incompleteRequest))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 403 Forbidden when not authenticated")
        void shouldReturnForbiddenWhenNotAuthenticated() throws Exception {
            // When/Then
            mockMvc.perform(post("/api/v1/accounts/verify-nik")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(authorities = {"SCOPE_other"})
        @DisplayName("should return 403 Forbidden when missing required scope")
        void shouldReturnForbiddenWhenMissingScope() throws Exception {
            // When/Then
            mockMvc.perform(post("/api/v1/accounts/verify-nik")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(authorities = {"SCOPE_account:verify"})
        @DisplayName("should return 400 Bad Request when NIK contains non-digits")
        void shouldReturnBadRequestWhenNikContainsNonDigits() throws Exception {
            // Given - NIK with letters
            VerifyNikRequest invalidRequest = new VerifyNikRequest(
                "3201abcd56789001",
                "John Doe",
                "Jakarta",
                "1990-01-15"
            );

            // When/Then
            mockMvc.perform(post("/api/v1/accounts/verify-nik")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(authorities = {"SCOPE_account:verify"})
        @DisplayName("should return 400 Bad Request when full name is missing")
        void shouldReturnBadRequestWhenFullNameMissing() throws Exception {
            // Given - missing full name
            VerifyNikRequest invalidRequest = new VerifyNikRequest(
                "3201234567890001",
                "",  // Empty full name
                "Jakarta",
                "1990-01-15"
            );

            // When/Then
            mockMvc.perform(post("/api/v1/accounts/verify-nik")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(authorities = {"SCOPE_account:verify"})
        @DisplayName("should handle optional birth place field correctly")
        void shouldHandleOptionalBirthPlace() throws Exception {
            // Given - birth place is optional
            VerifyNikRequest requestWithoutBirthPlace = new VerifyNikRequest(
                "3201234567890001",
                "John Doe",
                null,  // Birth place is optional
                "1990-01-15"
            );

            given(verifyNikUseCase.verifyNik(any(VerifyNikRequest.class)))
                .willReturn(CompletableFuture.completedFuture(successResponse));

            // When - start async request
            var mvcResult = mockMvc.perform(post("/api/v1/accounts/verify-nik")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestWithoutBirthPlace)))
                .andExpect(request().asyncStarted())
                .andReturn();

            // Then - should still process successfully
            mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk());
        }
    }
}
